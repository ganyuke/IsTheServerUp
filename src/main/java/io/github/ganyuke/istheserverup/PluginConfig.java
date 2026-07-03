package io.github.ganyuke.istheserverup;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class PluginConfig {
    @Setting("webserver-address")
    @Comment("The IP and port the web server will bind to.")
    private String webserverAddress = "127.0.0.1:8080";

    @Setting("webserver-threads")
    @Comment("The number of worker threads allocated for handling incoming HTTP requests.")
    private int webserverThreads = 4;

    @Setting("webserver-shutdown-delay-seconds")
    @Comment("Maximum time (in seconds) to wait for active requests to finish when stopping the web server.")
    private int webserverShutdownDelay = 5;

    @Setting("response-content-type")
    @Comment("The HTTP Content-Type header value sent with the response.")
    private String responseContentType = "text/plain; charset=utf-8";

    @Setting("response-up-text")
    @Comment("The body text returned when the backend server is online.")
    private String responseUpText = "UP\n";

    @Setting("response-down-text")
    @Comment("The body text returned when the backend server is offline or unreachable.")
    private String responseDownText = "DOWN\n";

    @Setting("ping-cache-duration-ms")
    @Comment("Time (in milliseconds) to cache backend status results to prevent hammering backend servers with pings.")
    private int pingCacheDuration = 1000;

    @Setting("ping-timeout-ms")
    @Comment("How long to wait (in milliseconds) for a backend server to respond to a ping before timing out.")
    private int pingTimeout = 1000;

    public String getWebServerAddress() {
        return webserverAddress;
    }

    public int getWebserverThreads() {
        return webserverThreads;
    }

    public int getWebserverShutdownDelaySeconds() {
        return webserverShutdownDelay;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getResponseUpText() {
        return responseUpText;
    }

    public String getResponseDownText() {
        return responseDownText;
    }

    public int getPingCacheDuration() {
        return pingCacheDuration;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }
}
