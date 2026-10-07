package Twitch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TwitchApiTest {
    private FakeTwitch twitch;
    private TwitchApi api;
    @TempDir Path folder;

    @BeforeEach void start() throws Exception {
        twitch = new FakeTwitch();
        api = twitch.api();
    }

    @AfterEach void stop() {
        twitch.close();
    }

    @Test void deviceFlowWaitsUntilThePlayerAuthorizes() throws Exception {
        TwitchApi.DeviceCode code = api.deviceCode("user:read:subscriptions").get(5, TimeUnit.SECONDS);
        assertEquals("ABCDEFGH", code.userCode());
        assertTrue(code.verificationUri().contains("twitch.tv/activate"));
        TwitchApi.Token token = api.waitForToken(code).get(10, TimeUnit.SECONDS);
        assertEquals("access-1", token.access());
        assertEquals("refresh-1", token.refresh());
        long polls = twitch.requests.stream().filter(r -> r.endsWith("device_code")).count();
        assertEquals(2, polls, "Primero authorization_pending y después el token");
    }

    @Test void cancelledFlowStopsPolling() throws Exception {
        twitch.pendingPolls = 1000;
        TwitchApi.DeviceCode code = api.deviceCode("x").get(5, TimeUnit.SECONDS);
        CompletableFuture<TwitchApi.Token> waiting = api.waitForToken(code);
        waiting.cancel(false);
        Thread.sleep(2500);
        long polls = twitch.requests.stream().filter(r -> r.endsWith("device_code")).count();
        assertTrue(polls <= 1, "Cancelado no sigue preguntando: " + polls);
    }

    @Test void validateTellsWhoOwnsTheToken() throws Exception {
        TwitchApi.TokenInfo info = api.validate("access-1").get(5, TimeUnit.SECONDS);
        assertEquals("crosszy", info.login());
        assertEquals("1000", info.userId());
    }

    @Test void twitchErrorsKeepTheirStatusAndMessage() {
        ExecutionException error = assertThrows(ExecutionException.class, () -> api.validate("otro").get(5, TimeUnit.SECONDS));
        TwitchApi.ApiException api = assertInstanceOf(TwitchApi.ApiException.class, TwitchApi.unwrap(error.getCause()));
        assertEquals(401, api.status);
        assertEquals("invalid access token", api.getMessage());
    }

    @Test void subsAndVipsAreAskedInBatchesOf20() throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 45; i++) ids.add(String.valueOf(i));
        twitch.subs.put("3", "1000");
        twitch.subs.put("44", "3000");
        twitch.vips.add("7");
        Map<String, TwitchApi.Sub> subs = api.subscriptions("access-1", "1000", ids).get(5, TimeUnit.SECONDS);
        Set<String> vips = api.vips("access-1", "1000", ids).get(5, TimeUnit.SECONDS);
        assertEquals(Set.of("3", "44"), subs.keySet());
        assertEquals("3000", subs.get("44").tier());
        assertEquals(Set.of("7"), vips);
        assertEquals(3, twitch.requests.stream().filter(r -> r.startsWith("GET /helix/subscriptions")).count());
    }

    @Test void unknownTwitchUserIsNull() throws Exception {
        assertNull(api.userByLogin("access-1", "nadie").get(5, TimeUnit.SECONDS));
        assertEquals("555", api.userByLogin("access-1", "Fulano").get(5, TimeUnit.SECONDS).id());
    }

    @Test void viewerTokensAreRevoked() throws Exception {
        api.revoke("access-1").get(5, TimeUnit.SECONDS);
        assertEquals(1, twitch.revokes.get());
    }

    // ---------------------------------------------------------------- Token del canal

    private TwitchChannel connected(long expiresAt) {
        File file = folder.resolve("canal.yml").toFile();
        TwitchChannel channel = new TwitchChannel(api, file, Logger.getLogger("test"));
        twitch.validRefresh.add("refresh-1");
        channel.connect(new TwitchApi.Token("access-1", "refresh-1", expiresAt), new TwitchApi.TokenInfo("1000", "crosszy", 0), "CrissyjuanxD");
        return channel;
    }

    @Test void expiredTokenIsRefreshedOnlyOnceForParallelCalls() throws Exception {
        TwitchChannel channel = connected(System.currentTimeMillis() - 1000);
        CompletableFuture<Map<String, TwitchApi.Sub>> first = channel.call(token -> api.subscriptions(token, "1000", List.of("1")));
        CompletableFuture<Set<String>> second = channel.call(token -> api.vips(token, "1000", List.of("1")));
        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        assertEquals(1, twitch.refreshes.get(), "El refresh token de una app pública sirve una sola vez");
        assertTrue(channel.connected());
    }

    @Test void unauthorizedHelixCallRefreshesAndRetries() throws Exception {
        TwitchChannel channel = connected(System.currentTimeMillis() + 3_600_000);
        twitch.helixUnauthorizedOnce = true;
        twitch.subs.put("1", "1000");
        Map<String, TwitchApi.Sub> subs = channel.call(token -> api.subscriptions(token, "1000", List.of("1"))).get(5, TimeUnit.SECONDS);
        assertEquals(Set.of("1"), subs.keySet());
        assertEquals(1, twitch.refreshes.get());
    }

    @Test void rejectedRefreshDisconnectsAndIsSaved() throws Exception {
        TwitchChannel channel = connected(System.currentTimeMillis() - 1000);
        twitch.validRefresh.clear();
        assertThrows(ExecutionException.class, () -> channel.call(token -> api.vips(token, "1000", List.of("1"))).get(5, TimeUnit.SECONDS));
        assertFalse(channel.connected());
        TwitchChannel reloaded = new TwitchChannel(api, folder.resolve("canal.yml").toFile(), Logger.getLogger("test"));
        assertFalse(reloaded.connected());
        assertNotNull(reloaded.lastError());
    }

    @Test void connectionSurvivesARestart() {
        connected(System.currentTimeMillis() + 3_600_000);
        TwitchChannel reloaded = new TwitchChannel(api, folder.resolve("canal.yml").toFile(), Logger.getLogger("test"));
        assertTrue(reloaded.connected());
        assertEquals("crosszy", reloaded.login());
        assertEquals("1000", reloaded.broadcasterId());
    }
}
