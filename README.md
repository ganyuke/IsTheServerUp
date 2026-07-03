# IsTheServerUp

Expose the health status of individual backend servers for the uptime monitor of your choice.

IsTheServerUp is a Velocity plugin for uptime monitors like [Uptime Kuma](https://github.com/louislam/uptime-kuma) and [Gatus](https://github.com/TwiN/gatus) to answer the question asked by your friends since time immemorial: "Is the server up?"

## Features

- Easily expose your individual backends' health status through your Velocity proxy - if Velocity can reach your server, your players can reach it.
- Bring your own uptime monitor to poll `/health/{backend_name}` endpoints for uptime.
- Simple, lightweight, and zero extra dependencies (other than Velocity of course).

## How can this plugin help me?

> saya: "miss elaina, is the Minecraft server up yet?"<br>
> elaina: "check the uptime monitor it's at uptime.unitedmagic.org"<br>
> saya: "but i want to hear it from you"<br>
> elaina: "i'm flattered but you should really just check the status page"<br>
> saya: "but miss elaina i love you so much that i don't want to hear it from anyone else"<br>
> elaina: "riiiight okay then well have a good one, saya"<br>
> saya: "wait miss elaina please wait! i need to know if the server is up"<br>
> elaina: "no"<br>
> saya: "no as in you don't want to tell me or no as in the server is not up"<br>
> elaina: "no"

## How do I use it?

1. Install this plugin into your Velocity proxy's `plugins/` folder.
2. (Re)start Velocity to let the plugin autopopulate the config, then stop Velocity.
3. Edit the configuration to your liking then start Velocity.
4. Point the uptime checker of your choice at the backend endpoint. If it's `200`, it's up. If it's `503`, it's not.
5. Done.

If you want to use TLS, you should front the webserver with a proper reverse proxy like [NGINX](https://nginx.org/en/) or [Caddy](https://nginx.org/en/).

### Caddy and Gatus example

This is roughly the setup that I use with Caddy and Gatus.

I am running Caddy on the local machine to reverse proxy my localhost-bound IsTheServerUp web server. The config for Caddy is below:

```
metrics.unitedmagic.org:80 {
        handle_path /velocity* {
                reverse_proxy http://127.0.0.1:8080
        }
}
```

I use Gatus on another machine through a VPN tunnel to monitor the endpoints. Here is a snippet of my Gatus configuration relevant to this plugin:

```yaml
endpoints:
  - name: UMA SMP
    group: Minecraft Servers
    url: "http://metrics.unitedmagic.org/velocity/health/smp"
    interval: 60s
    conditions:
      - "[STATUS] == 200"
  - name: UMA Skyblock
    group: Minecraft Servers
    url: "http://metrics.unitedmagic.org/velocity/health/skyblock"
    interval: 60s
    conditions:
      - "[STATUS] == 200"
```

## Configuration

This plugin uses a HOCON configuration file. You can find the default generated configuration below.

```properties
# The IP and port the web server will bind to.
webserver-address="127.0.0.1:8080"
# The number of worker threads allocated for handling incoming HTTP requests.
webserver-threads=4
# Maximum time (in seconds) to wait for active requests to finish when stopping the web server.
webserver-shutdown-delay-seconds=5
# The HTTP Content-Type header value sent with the response.
response-content-type="text/plain; charset=utf-8"
# The body text returned when the backend server is online.
response-up-text="UP\n"
# The body text returned when the backend server is offline or unreachable.
response-down-text="DOWN\n"
# Time (in milliseconds) to cache backend status results to prevent hammering backend servers with pings.
ping-cache-duration-ms=1000
# How long to wait (in milliseconds) for a backend server to respond to a ping before timing out.
ping-timeout-ms=1000
```

## License

Unless otherwise noted, all source code in this repository is licensed under the **Mozilla Public License 2.0** (SPDX: **MPL-2.0**). Please view the [`LICENSE`](https://raw.githubusercontent.com/ganyuke/IsTheServerUp/refs/heads/main/LICENSE) file for the terms you are afforded under the MPL-2.0.