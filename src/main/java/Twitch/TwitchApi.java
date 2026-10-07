package Twitch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

// Las llamadas a Twitch. Todo es asíncrono, nada corre en el hilo del server
final class TwitchApi {

    // Con user_id la API devuelve hasta 20 por página, así que se pregunta de a 20
    private static final int BATCH = 20;

    record DeviceCode(String deviceCode, String userCode, String verificationUri, long expiresAt, int interval, String scopes) {}

    record Token(String access, String refresh, long expiresAt) {}

    record TokenInfo(String userId, String login, long expiresAt) {}

    record Sub(String tier, boolean gift, String gifter) {}

    record User(String id, String login, String displayName) {}

    static final class ApiException extends RuntimeException {
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String clientId;
    private final String clientSecret;
    private final String idUrl;
    private final String helixUrl;

    // Las direcciones se pueden cambiar con -Dquaso.twitch.id y -Dquaso.twitch.helix para probar sin Twitch
    TwitchApi(String clientId, String clientSecret) {
        this(clientId, clientSecret, System.getProperty("quaso.twitch.id", "https://id.twitch.tv"),
                System.getProperty("quaso.twitch.helix", "https://api.twitch.tv/helix"));
    }

    TwitchApi(String clientId, String clientSecret, String idUrl, String helixUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.idUrl = idUrl;
        this.helixUrl = helixUrl;
    }

    boolean configured() {
        return clientId != null && !clientId.isBlank();
    }

    // ---------------------------------------------------------------- Vincular cuentas (device code flow)

    // Pide el código que el jugador pone en twitch.tv/activate
    CompletableFuture<DeviceCode> deviceCode(String scopes) {
        return post(idUrl + "/oauth2/device", form("client_id", clientId, "scopes", scopes)).thenApply(json -> new DeviceCode(
                json.get("device_code").getAsString(),
                json.get("user_code").getAsString(),
                json.get("verification_uri").getAsString(),
                System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000L,
                Math.max(1, json.has("interval") ? json.get("interval").getAsInt() : 5),
                scopes));
    }

    // Espera a que el jugador acepte en Twitch. Se puede cancelar con cancel() sobre lo que devuelve
    CompletableFuture<Token> waitForToken(DeviceCode code) {
        CompletableFuture<Token> result = new CompletableFuture<>();
        pollToken(code, code.interval(), result);
        return result;
    }

    private void pollToken(DeviceCode code, int interval, CompletableFuture<Token> result) {
        if (result.isDone()) return;
        if (System.currentTimeMillis() > code.expiresAt()) {
            result.completeExceptionally(new ApiException(0, "el código venció"));
            return;
        }
        CompletableFuture.delayedExecutor(interval, TimeUnit.SECONDS).execute(() -> {
            if (result.isDone()) return;
            post(idUrl + "/oauth2/token", withSecret(form("client_id", clientId, "scopes", code.scopes(),
                    "device_code", code.deviceCode(), "grant_type", "urn:ietf:params:oauth:grant-type:device_code")))
                    .thenApply(this::token)
                    .whenComplete((token, error) -> {
                        if (error == null) {
                            result.complete(token);
                            return;
                        }
                        Throwable cause = unwrap(error);
                        String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
                        // Mientras no acepta Twitch responde authorization_pending; un corte de red también se reintenta
                        if (message.contains("authorization_pending") || cause instanceof IOException) {
                            pollToken(code, interval, result);
                        } else if (message.contains("slow_down")) {
                            pollToken(code, interval + 5, result);
                        } else {
                            result.completeExceptionally(cause);
                        }
                    });
        });
    }

    CompletableFuture<Token> refresh(String refreshToken) {
        return post(idUrl + "/oauth2/token", withSecret(form("client_id", clientId, "grant_type", "refresh_token",
                "refresh_token", refreshToken))).thenApply(this::token);
    }

    // De quién es el token (y si sigue sirviendo)
    CompletableFuture<TokenInfo> validate(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(idUrl + "/oauth2/validate"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "OAuth " + accessToken)
                .GET().build();
        return send(request).thenApply(json -> new TokenInfo(
                json.get("user_id").getAsString(),
                json.get("login").getAsString(),
                System.currentTimeMillis() + (json.has("expires_in") ? json.get("expires_in").getAsLong() : 0) * 1000L));
    }

    // El token del jugador solo sirve para saber quién es: apenas se vincula se le avisa a Twitch que ya no se usa
    CompletableFuture<Void> revoke(String accessToken) {
        return post(idUrl + "/oauth2/revoke", form("client_id", clientId, "token", accessToken)).thenApply(json -> null);
    }

    private Token token(JsonObject json) {
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
        return new Token(json.get("access_token").getAsString(),
                json.has("refresh_token") ? json.get("refresh_token").getAsString() : null,
                System.currentTimeMillis() + expiresIn * 1000L);
    }

    // ---------------------------------------------------------------- Helix (con el token del canal)

    // Las subs activas de esos usuarios. El que no aparece no está suscrito
    CompletableFuture<Map<String, Sub>> subscriptions(String token, String broadcasterId, Collection<String> userIds) {
        List<CompletableFuture<JsonObject>> calls = new ArrayList<>();
        for (List<String> batch : batches(userIds)) {
            calls.add(helix(token, "/subscriptions", "broadcaster_id", broadcasterId, batch));
        }
        return CompletableFuture.allOf(calls.toArray(new CompletableFuture[0])).thenApply(ignored -> {
            Map<String, Sub> subs = new HashMap<>();
            for (CompletableFuture<JsonObject> call : calls) {
                for (JsonElement element : data(call.join())) {
                    JsonObject sub = element.getAsJsonObject();
                    String gifter = sub.has("gifter_login") && !sub.get("gifter_login").isJsonNull() ? sub.get("gifter_login").getAsString() : "";
                    subs.put(sub.get("user_id").getAsString(), new Sub(sub.get("tier").getAsString(),
                            sub.has("is_gift") && sub.get("is_gift").getAsBoolean(), gifter));
                }
            }
            return subs;
        });
    }

    // Cuáles de esos usuarios son VIP del canal
    CompletableFuture<Set<String>> vips(String token, String broadcasterId, Collection<String> userIds) {
        List<CompletableFuture<JsonObject>> calls = new ArrayList<>();
        for (List<String> batch : batches(userIds)) {
            calls.add(helix(token, "/channels/vips", "broadcaster_id", broadcasterId, batch));
        }
        return CompletableFuture.allOf(calls.toArray(new CompletableFuture[0])).thenApply(ignored -> {
            Set<String> vips = new HashSet<>();
            for (CompletableFuture<JsonObject> call : calls) {
                for (JsonElement element : data(call.join())) vips.add(element.getAsJsonObject().get("user_id").getAsString());
            }
            return vips;
        });
    }

    CompletableFuture<User> userByLogin(String token, String login) {
        String url = helixUrl + "/users?login=" + encode(login.toLowerCase());
        return send(helixRequest(token, url)).thenApply(json -> {
            JsonArray data = data(json);
            if (data.isEmpty()) return null;
            JsonObject user = data.get(0).getAsJsonObject();
            return new User(user.get("id").getAsString(), user.get("login").getAsString(), user.get("display_name").getAsString());
        });
    }

    private CompletableFuture<JsonObject> helix(String token, String path, String key, String value, List<String> userIds) {
        StringBuilder url = new StringBuilder(helixUrl).append(path).append('?').append(key).append('=').append(encode(value));
        for (String id : userIds) url.append("&user_id=").append(encode(id));
        return send(helixRequest(token, url.toString()));
    }

    private HttpRequest helixRequest(String token, String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Client-Id", clientId)
                .GET().build();
    }

    // ---------------------------------------------------------------- HTTP

    private CompletableFuture<JsonObject> post(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return send(request);
    }

    // Twitch responde los errores como {"status":400,"message":"..."}: se pasan a ApiException con ese mensaje
    private CompletableFuture<JsonObject> send(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonObject json;
            try {
                JsonElement parsed = response.body() == null || response.body().isBlank() ? new JsonObject() : JsonParser.parseString(response.body());
                json = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            } catch (RuntimeException e) {
                json = new JsonObject();
            }
            if (response.statusCode() / 100 != 2) {
                String message = json.has("message") ? json.get("message").getAsString() : "HTTP " + response.statusCode();
                throw new ApiException(response.statusCode(), message);
            }
            return json;
        });
    }

    private String withSecret(String form) {
        if (clientSecret == null || clientSecret.isBlank()) return form;
        return form + "&client_secret=" + encode(clientSecret);
    }

    private static String form(String... pairs) {
        StringJoiner joiner = new StringJoiner("&");
        for (int i = 0; i + 1 < pairs.length; i += 2) joiner.add(encode(pairs[i]) + "=" + encode(pairs[i + 1]));
        return joiner.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static JsonArray data(JsonObject json) {
        return json.has("data") && json.get("data").isJsonArray() ? json.getAsJsonArray("data") : new JsonArray();
    }

    private static List<List<String>> batches(Collection<String> ids) {
        List<List<String>> batches = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String id : ids) {
            current.add(id);
            if (current.size() == BATCH) {
                batches.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    static Throwable unwrap(Throwable error) {
        while (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        return error;
    }
}
