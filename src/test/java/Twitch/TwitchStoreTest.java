package Twitch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TwitchStoreTest {
    @TempDir Path folder;
    private final UUID steve = UUID.randomUUID();
    private final UUID alex = UUID.randomUUID();

    private TwitchStore store() {
        return new TwitchStore(new File(folder.toFile(), "datos.yml"), Logger.getLogger("test"));
    }

    private static TwitchAccount account(String id, String login) {
        TwitchAccount account = new TwitchAccount(id);
        account.login = login;
        return account;
    }

    @Test void claimsStayWithTheTwitchAccountWhenItMovesToAnotherPlayer() {
        TwitchStore store = store();
        TwitchAccount account = account("42", "steve_tw");
        store.link(account, steve, "Steve");
        account.subRacha.observar(true, 1000, 1000);
        account.subRacha.reclamar(32L * 86_400_000);
        account.subKits = 1;

        store.unlink(account);
        assertNull(store.linkedTo(steve));
        store.link(store.byTwitchId("42"), alex, "Alex");
        TwitchAccount moved = store.linkedTo(alex);
        assertSame(account, moved);
        assertFalse(moved.subRacha.puedeReclamar(32L * 86_400_000), "Pasarla a otra cuenta no da otro kit en el mismo mes");
        assertEquals(1, moved.subKits);
    }

    @Test void aPlayerHasOnlyOneLinkedAccount() {
        TwitchStore store = store();
        TwitchAccount first = account("1", "uno");
        TwitchAccount second = account("2", "dos");
        store.link(first, steve, "Steve");
        store.link(second, steve, "Steve");
        assertSame(second, store.linkedTo(steve));
        assertNull(first.player);
    }

    @Test void simulatedAccountWinsWhileItExists() {
        TwitchStore store = store();
        TwitchAccount real = account("1", "real");
        store.link(real, steve, "Steve");
        TwitchAccount test = account("sim-" + steve, "(prueba)");
        test.player = steve;
        test.playerName = "Steve";
        test.simulated = "sub";
        store.putSimulated(test);
        assertSame(test, store.byPlayer(steve));
        store.removeSimulated(steve);
        assertSame(real, store.byPlayer(steve));
    }

    @Test void everythingIsSavedAndLoaded() {
        TwitchStore store = store();
        TwitchAccount account = account("42", "steve_tw");
        store.link(account, steve, "Steve");
        account.sub = true;
        account.tier = "2000";
        account.subKits = 3;
        account.previousRole = "YMiembro";
        account.subRacha.observar(true, 5000, 1000);
        account.subRacha.observar(true, 5500, 1000);
        account.subRacha.reclamar(1000);
        store.log("Steve", "steve_tw", "Kit de Sub · Mes 3+", "");
        store.log("Alex", "alex_tw", "Kit VIP", "dado por Crosszy");
        store.flyEnabled = false;
        store.flyActive.add(steve);
        store.write(store.serialize());

        TwitchStore loaded = store();
        TwitchAccount back = loaded.linkedTo(steve);
        assertNotNull(back);
        assertEquals("steve_tw", back.login);
        assertTrue(back.sub);
        assertEquals("Tier 2", back.tierName());
        assertEquals(3, back.subKits);
        assertEquals("YMiembro", back.previousRole);
        assertEquals(500, back.subRacha.observado);
        assertEquals(1, back.subRacha.mesReclamado);
        assertFalse(loaded.flyEnabled);
        assertTrue(loaded.flyActive.contains(steve));
        assertEquals(2, loaded.history(null).size());
        assertEquals("Kit VIP", loaded.history(null).get(0).kit(), "El más nuevo primero");
        assertEquals(1, loaded.history("steve").size());
        assertSame(back, loaded.find("STEVE_TW"));
    }
}
