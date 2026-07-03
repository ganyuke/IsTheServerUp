package io.github.ganyuke.istheserverup;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class IsTheServerUp {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private BackendHealthWebServer webServer;
    private PluginConfig config;

    @Inject
    public IsTheServerUp(ProxyServer proxyServer, @DataDirectory Path dataDirectory, Logger logger) {
        this.proxy = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    private PluginConfig loadConfig() {
        try {
            if (Files.notExists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path configFile = dataDirectory.resolve("config.conf");

            HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .path(configFile)
                    .build();

            CommentedConfigurationNode node = loader.load();

            PluginConfig configInstance = node.get(PluginConfig.class);
            if (configInstance == null) {
                configInstance = new PluginConfig();
            }

            node.set(PluginConfig.class, configInstance);
            loader.save(node);

            return configInstance;

        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return new PluginConfig(); // Fallback to defaults
        }
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = loadConfig();
        startWebServer();
    }

    private void startWebServer() {
        String addressStr = config.getWebServerAddress();
        String[] parts = addressStr.split(":");

        String host = parts[0];
        int port = 8080;

        if (parts.length > 1) {
            try {
                int parsedPort = Integer.parseInt(parts[1]);
                if (parsedPort < 0 || parsedPort > 65535) {
                    logger.warn("Invalid webserver port '{}'. Falling back to {}.", parsedPort, port);
                } else {
                    port = parsedPort;
                }
            } catch (NumberFormatException e) {
                logger.warn("Non-numeric webserver port '{}'. Falling back to {}.", parts[1], port);
            }
        }

        try {
            InetSocketAddress bindAddress = new InetSocketAddress(host, port);
            webServer = new BackendHealthWebServer(proxy, bindAddress, config);
            webServer.start();
            logger.info("Backend health webserver started on {}:{}", host, port);
        } catch (IOException e) {
            if (e instanceof java.net.BindException) {
                logger.error("Failed to start backend health webserver: unable to bind to port {} - is it already in use?", port);
            } else {
                logger.error("Failed to start backend health webserver", e);
            }
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (webServer != null) {
            webServer.stop();
            logger.info("Backend health webserver stopped");
        }
    }

}
