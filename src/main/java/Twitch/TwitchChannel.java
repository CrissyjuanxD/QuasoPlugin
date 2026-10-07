package Twitch;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;

// La conexión con el canal: el token que autorizó Crosszy para ver sus subs y sus VIPs. Se guarda en
// twitch/canal.yml y se renueva solo; si Twitch lo invalida hay que volver a conectar con /twitchadmin canal conectar
final class TwitchChannel {

    static final String SCOPES = "channel:read:subscriptions channel:read:vips";
    private static final long VALIDATE_EVERY = 60 * 60 * 1000L;
    private static final long REFRESH_MARGIN = 5 * 60 * 1000L;

    private TwitchApi api;
    private final File file;
    private final Logger logger;

    private String accessToken;
    private String refreshToken;
    private long expiresAt;
    private long lastValidated;
    private String broadcasterId;
    private String login;
    private String connectedBy;
    private long connectedAt;
    private String lastError;
    private CompletableFuture<String> refreshing;

    TwitchChannel(TwitchApi api, File file, Logger logger) {
        this.api = api;
        this.file = file;
        this.logger = logger;
        load();
    }

    // Al recargar twitch.yml se sigue usando esta misma conexión con la app nueva: así una renovación del token
    // que esté en curso no deja guardado un refresh token que ya no sirve
    synchronized void useApi(TwitchApi api) {
        this.api = api;
    }

    synchronized boolean connected() {
        return accessToken != null && broadcasterId != null;
    }

    synchronized String broadcasterId() {
        return broadcasterId;
    }

    synchronized String login() {
        return login;
    }

    synchronized String connectedBy() {
        return connectedBy;
    }

    synchronized long connectedAt() {
        return connectedAt;
    }

    synchronized long expiresAt() {
        return expiresAt;
    }

    synchronized String lastError() {
        return lastError;
    }

    synchronized void connect(TwitchApi.Token token, TwitchApi.TokenInfo info, String by) {
        accessToken = token.access();
        refreshToken = token.refresh();
        expiresAt = token.expiresAt();
        lastValidated = System.currentTimeMillis();
        broadcasterId = info.userId();
        login = info.login();
        connectedBy = by;
        connectedAt = System.currentTimeMillis();
        lastError = null;
        save();
    }

    synchronized void disconnect(String reason) {
        accessToken = null;
        refreshToken = null;
        expiresAt = 0;
        lastError = reason;
        save();
    }

    // Llama a la API con un token que sirva; si Twitch dice 401 lo renueva y prueba una vez más
    <T> CompletableFuture<T> call(Function<String, CompletableFuture<T>> request) {
        return token().thenCompose(token -> request.apply(token).handle((value, error) -> {
            if (error == null) return CompletableFuture.completedFuture(value);
            Throwable cause = TwitchApi.unwrap(error);
            if (cause instanceof TwitchApi.ApiException api && api.status == 401) {
                return refresh(token).thenCompose(request);
            }
            return CompletableFuture.<T>failedFuture(cause);
        }).thenCompose(future -> future));
    }

    // Twitch pide revisar el token una vez por hora
    CompletableFuture<Void> validateIfNeeded() {
        String token;
        synchronized (this) {
            if (!connected() || System.currentTimeMillis() - lastValidated < VALIDATE_EVERY) return CompletableFuture.completedFuture(null);
            token = accessToken;
        }
        TwitchApi current;
        synchronized (this) {
            current = api;
        }
        return current.validate(token).handle((info, error) -> {
            if (error == null) {
                synchronized (this) {
                    lastValidated = System.currentTimeMillis();
                }
                return CompletableFuture.<Void>completedFuture(null);
            }
            Throwable cause = TwitchApi.unwrap(error);
            if (cause instanceof TwitchApi.ApiException api && api.status == 401) {
                return refresh(token).thenApply(ignored -> (Void) null);
            }
            return CompletableFuture.<Void>failedFuture(cause);
        }).thenCompose(future -> future);
    }

    private CompletableFuture<String> token() {
        synchronized (this) {
            if (!connected()) return CompletableFuture.failedFuture(new TwitchApi.ApiException(0, "el canal no está conectado"));
            if (System.currentTimeMillis() < expiresAt - REFRESH_MARGIN) return CompletableFuture.completedFuture(accessToken);
            return refresh(accessToken);
        }
    }

    // Una sola renovación a la vez: el refresh token de una app pública sirve una sola vez, si se usara dos veces
    // la segunda fallaría y se perdería la conexión
    private synchronized CompletableFuture<String> refresh(String failedToken) {
        if (refreshing != null && !refreshing.isDone()) return refreshing;
        if (accessToken != null && !accessToken.equals(failedToken) && System.currentTimeMillis() < expiresAt - REFRESH_MARGIN) {
            return CompletableFuture.completedFuture(accessToken);
        }
        if (refreshToken == null) return CompletableFuture.failedFuture(new TwitchApi.ApiException(401, "no hay refresh token"));
        String current = refreshToken;
        refreshing = api.refresh(current).handle((token, error) -> {
            synchronized (this) {
                if (error != null) {
                    Throwable cause = TwitchApi.unwrap(error);
                    // 400/401 = Twitch ya no acepta ese refresh token; un corte de red no desconecta
                    if (cause instanceof TwitchApi.ApiException api && (api.status == 400 || api.status == 401)) {
                        logger.warning("[Twitch] Se perdió la conexión con el canal: " + cause.getMessage());
                        disconnect("Twitch rechazó la renovación del token (" + cause.getMessage() + ")");
                    }
                    throw new java.util.concurrent.CompletionException(cause);
                }
                accessToken = token.access();
                if (token.refresh() != null) refreshToken = token.refresh();
                expiresAt = token.expiresAt();
                lastValidated = System.currentTimeMillis();
                save();
                return accessToken;
            }
        });
        return refreshing;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        accessToken = yml.getString("access_token");
        refreshToken = yml.getString("refresh_token");
        expiresAt = yml.getLong("expira");
        broadcasterId = yml.getString("canal_id");
        login = yml.getString("canal");
        connectedBy = yml.getString("conectado_por");
        connectedAt = yml.getLong("conectado_el");
        lastError = yml.getString("ultimo_error");
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().setHeader(java.util.List.of(
                "Conexión con el canal de Twitch. No compartas este archivo: tiene el acceso a las subs del canal.",
                "Se llena solo con /twitchadmin canal conectar"));
        yml.set("canal", login);
        yml.set("canal_id", broadcasterId);
        yml.set("conectado_por", connectedBy);
        yml.set("conectado_el", connectedAt);
        yml.set("access_token", accessToken);
        yml.set("refresh_token", refreshToken);
        yml.set("expira", expiresAt);
        yml.set("ultimo_error", lastError);
        try {
            file.getParentFile().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            logger.warning("[Twitch] No se pudo guardar " + file.getName() + ": " + e.getMessage());
        }
    }
}
