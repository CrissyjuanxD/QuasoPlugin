package Twitch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Un Twitch de mentira en localhost con lo justo para el device code flow, los tokens y Helix. */
final class FakeTwitch implements AutoCloseable {
    final HttpServer server;
    final Map<String, String> subs = new HashMap<>();   // user_id -> tier
    final Set<String> vips = new HashSet<>();
    final Set<String> validRefresh = Collections.synchronizedSet(new HashSet<>());
    final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    final AtomicInteger refreshes = new AtomicInteger();
    final AtomicInteger revokes = new AtomicInteger();
    volatile int pendingPolls = 1;
    volatile String accessToken = "access-1";
    volatile String login = "crosszy";
    volatile String userId = "1000";
    volatile boolean helixUnauthorizedOnce;

    FakeTwitch() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth2/device", exchange -> {
            log(exchange);
            send(exchange, 200, "{\"device_code\":\"dev-1\",\"expires_in\":1800,\"interval\":1,\"user_code\":\"ABCDEFGH\","
                    + "\"verification_uri\":\"https://www.twitch.tv/activate?device-code=ABCDEFGH\"}");
        });
        server.createContext("/oauth2/token", exchange -> {
            Map<String, String> form = form(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requests.add("POST /oauth2/token " + form.get("grant_type"));
            if ("refresh_token".equals(form.get("grant_type"))) {
                refreshes.incrementAndGet();
                if (!validRefresh.remove(form.get("refresh_token"))) {
                    send(exchange, 400, "{\"status\":400,\"message\":\"Invalid refresh token\"}");
                    return;
                }
                int n = refreshes.get() + 1;
                accessToken = "access-" + n;
                validRefresh.add("refresh-" + n);
                send(exchange, 200, "{\"access_token\":\"access-" + n + "\",\"refresh_token\":\"refresh-" + n + "\",\"expires_in\":14000}");
                return;
            }
            if (pendingPolls-- > 0) {
                send(exchange, 400, "{\"status\":400,\"message\":\"authorization_pending\"}");
                return;
            }
            validRefresh.add("refresh-1");
            send(exchange, 200, "{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"refresh-1\",\"expires_in\":14000,\"scope\":[],\"token_type\":\"bearer\"}");
        });
        server.createContext("/oauth2/validate", exchange -> {
            log(exchange);
            if (!("OAuth " + accessToken).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                send(exchange, 401, "{\"status\":401,\"message\":\"invalid access token\"}");
                return;
            }
            send(exchange, 200, "{\"client_id\":\"abc\",\"login\":\"" + login + "\",\"scopes\":[],\"user_id\":\"" + userId + "\",\"expires_in\":5000}");
        });
        server.createContext("/oauth2/revoke", exchange -> {
            log(exchange);
            revokes.incrementAndGet();
            send(exchange, 200, "");
        });
        server.createContext("/helix/subscriptions", exchange -> {
            if (!authorized(exchange)) return;
            StringBuilder data = new StringBuilder();
            for (String id : ids(exchange)) {
                String tier = subs.get(id);
                if (tier == null) continue;
                if (!data.isEmpty()) data.append(',');
                data.append("{\"broadcaster_id\":\"1000\",\"gifter_login\":\"\",\"is_gift\":false,\"tier\":\"").append(tier)
                        .append("\",\"user_id\":\"").append(id).append("\",\"user_login\":\"u").append(id).append("\"}");
            }
            send(exchange, 200, "{\"data\":[" + data + "],\"pagination\":{},\"total\":0,\"points\":0}");
        });
        server.createContext("/helix/channels/vips", exchange -> {
            if (!authorized(exchange)) return;
            StringBuilder data = new StringBuilder();
            for (String id : ids(exchange)) {
                if (!vips.contains(id)) continue;
                if (!data.isEmpty()) data.append(',');
                data.append("{\"user_id\":\"").append(id).append("\"}");
            }
            send(exchange, 200, "{\"data\":[" + data + "],\"pagination\":{}}");
        });
        server.createContext("/helix/users", exchange -> {
            if (!authorized(exchange)) return;
            String query = exchange.getRequestURI().getQuery();
            if (query.contains("login=nadie")) send(exchange, 200, "{\"data\":[]}");
            else send(exchange, 200, "{\"data\":[{\"id\":\"555\",\"login\":\"fulano\",\"display_name\":\"Fulano\"}]}");
        });
        server.start();
    }

    String idUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String helixUrl() {
        return idUrl() + "/helix";
    }

    TwitchApi api() {
        return new TwitchApi("abc", "", idUrl(), helixUrl());
    }

    private boolean authorized(HttpExchange exchange) throws IOException {
        log(exchange);
        boolean header = ("Bearer " + accessToken).equals(exchange.getRequestHeaders().getFirst("Authorization"))
                && "abc".equals(exchange.getRequestHeaders().getFirst("Client-Id"));
        if (helixUnauthorizedOnce || !header) {
            helixUnauthorizedOnce = false;
            send(exchange, 401, "{\"status\":401,\"message\":\"Invalid OAuth token\"}");
            return false;
        }
        return true;
    }

    private void log(HttpExchange exchange) {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
    }

    private static List<String> ids(HttpExchange exchange) {
        List<String> ids = new ArrayList<>();
        for (String part : exchange.getRequestURI().getQuery().split("&")) {
            if (part.startsWith("user_id=")) ids.add(part.substring(8));
        }
        return ids;
    }

    private static Map<String, String> form(String body) {
        Map<String, String> map = new HashMap<>();
        for (String part : body.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) map.put(URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
