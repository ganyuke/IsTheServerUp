package io.github.ganyuke.istheserverup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackendHealthWebServer {
    private final ProxyServer proxyServer;
    private final HttpServer httpServer;
    private final ExecutorService executor;
    private final PingOptions pingOptions;
    private final PluginConfig pluginConfig;

    // hashmaps for caching backend status so
    // you can't spam the crap out of the backend
    private final ConcurrentHashMap<String, CachedStatus> cache = new ConcurrentHashMap<>();
    private record CachedStatus(CompletableFuture<BackendStatus> future, long expiresAt) {
        public boolean isValid() {
            return System.currentTimeMillis() <= expiresAt;
        }
    }

    public BackendHealthWebServer(ProxyServer proxyServer, InetSocketAddress bindAddress, PluginConfig config) throws IOException {
        this.proxyServer = proxyServer;
        this.pluginConfig = config;

        this.executor = Executors.newFixedThreadPool(config.getWebserverThreads(), runnable -> {
            Thread thread = new Thread(runnable, "itsu-velocity-health-webserver");
            thread.setDaemon(true);
            return thread;
        });

        this.pingOptions = PingOptions.builder().timeout(Duration.ofMillis(config.getPingTimeout())).build();

        this.httpServer = HttpServer.create(bindAddress, 0);
        this.httpServer.setExecutor(executor);
        this.httpServer.createContext("/health", this::handleHealthRequest);
    }

    private void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", pluginConfig.getResponseContentType());
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpServer.stop(pluginConfig.getWebserverShutdownDelaySeconds());
        executor.shutdownNow();
    }

    private void handleHealthRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method Not Allowed\n");
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if (!path.startsWith("/health/")) {
            send(exchange, 404, "Not Found\n");
            return;
        }

        String serverName = path.substring("/health/".length());

        if (serverName.isBlank() || serverName.contains("/")) {
            send(exchange, 400, "Bad Request\n");
            return;
        }

        RegisteredServer server = proxyServer.getServer(serverName).orElse(null);

        if (server == null) {
            send(exchange, 404, "Unknown backend: " + serverName + "\n");
            return;
        }

        getOrCheckStatus(server).whenCompleteAsync((status, throwable) -> {
            try {
                if (throwable != null || !status.up()) {
                    send(exchange, 503, pluginConfig.getResponseDownText());
                    return;
                }

                send(exchange, 200, pluginConfig.getResponseUpText());
            } catch (IOException e) {
                // Nothing useful to do here; client probably disconnected.
            }
        }, executor);
    }

    private CompletableFuture<BackendStatus> getOrCheckStatus(RegisteredServer server) {
        String name = server.getServerInfo().getName();

        CachedStatus cached = cache.get(name);
        if (cached != null && cached.isValid()) {
            return cached.future();
        }

        // acquire mutex and update cache with new request
        return cache.compute(name, (key, current) -> {
            // double check if another thread updated
            if (current != null && current.isValid()) {
                return current;
            }

            // actually check backend server for status
            CompletableFuture<BackendStatus> freshPing = server.ping(pingOptions)
                    .thenApply(ping -> BackendStatus.up(name))
                    .exceptionally(error -> BackendStatus.down(name));

            return new CachedStatus(freshPing, System.currentTimeMillis() + pluginConfig.getPingCacheDuration());
        }).future();
    }

    private record BackendStatus(String name, boolean up) {
        static BackendStatus up(String name) {
            return new BackendStatus(name, true);
        }

        static BackendStatus down(String name) {
            return new BackendStatus(name, false);
        }
    }
}