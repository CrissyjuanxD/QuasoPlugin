package BloodMoon;

import Handlers.ActionBarHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LocaleReaderTest {
    @TempDir Path directory;
    private JavaPlugin plugin;
    private Path messages;

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("BloodMoonMessagesTest"));
        when(plugin.getResource(anyString())).thenAnswer(call -> getClass().getClassLoader().getResourceAsStream(call.getArgument(0)));
        messages = directory.resolve("bloodmoon/mensajes.yml");
    }

    @Test void allBundledMessagesAreAvailableOnTheFirstStartupWithoutReloading() throws Exception {
        assertFalse(Files.exists(messages));
        LocaleReader reader = new LocaleReader(plugin);
        try (var stream = plugin.getResource("bloodmoon-defaults/mensajes.yml")) {
            var defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String id : defaults.getKeys(false)) {
                assertFalse(reader.GetLocaleString(id).contains("No se encontró el mensaje"), id);
            }
        }
        assertTrue(reader.GetLocaleString("BloodMoonWarningBody").contains("Ha empezado una BloodMoon."));
        assertTrue(reader.GetLocaleString("BloodMoonWarningBody").contains("\n"));
        assertTrue(reader.GetLocaleString("BloodMoonTitleBar").contains("۞"));
        assertTrue(Files.isRegularFile(messages));
        assertNotNull(YamlConfiguration.loadConfiguration(messages.toFile()).getString("BloodMoonWarningBody"));
    }

    @Test void bloodMoonActionBarsUseTheSharedQueueAndKeepTheirSymbolAndColors() {
        BloodMoon bloodMoon = mock(BloodMoon.class);
        when(bloodMoon.getPlugin()).thenReturn(plugin);
        Player player = mock(Player.class);
        ActionBarHandler actionBars = mock(ActionBarHandler.class);
        String message = LocaleReader.RED + "Una horda se acerca";
        try (var singleton = mockStatic(BloodMoon.class);
             var shared = mockStatic(ActionBarHandler.class)) {
            singleton.when(BloodMoon::GetInstance).thenReturn(bloodMoon);
            shared.when(() -> ActionBarHandler.get(plugin)).thenReturn(actionBars);
            LocaleReader.actionBar(player, message);
            verify(actionBars).sendNotification(player, "bloodmoon:" + message, LocaleReader.ORANGE + "۞ " + message);
            verify(player, never()).spigot();
        }
    }

    @Test void silencedActionBarsDoNotTakeATurnInTheMissionQueue() {
        Player player = mock(Player.class);
        try (var singleton = mockStatic(BloodMoon.class);
             var shared = mockStatic(ActionBarHandler.class)) {
            LocaleReader.actionBar(player, "");
            LocaleReader.actionBar(player, null);
            singleton.verifyNoInteractions();
            shared.verifyNoInteractions();
            verifyNoInteractions(player);
        }
    }

    @Test void incompleteFilesReceiveMissingMessagesAndPreserveCustomizedAndSilencedEntries() throws Exception {
        Files.createDirectories(messages.getParent());
        Files.writeString(messages, "BloodMoonWarningTitle: '&6Mi BloodMoon'\nBloodMoonWarningBody: '%void%'\n");
        LocaleReader reader = new LocaleReader(plugin);
        assertTrue(reader.GetLocaleString("BloodMoonWarningTitle").contains("Mi BloodMoon"));
        assertEquals("", reader.GetLocaleString("BloodMoonWarningBody"));
        assertTrue(ChatColor.stripColor(reader.GetLocaleString("HordeArrived")).contains("Una horda acecha a $p"));
        var saved = YamlConfiguration.loadConfiguration(messages.toFile());
        assertEquals("&6Mi BloodMoon", saved.getString("BloodMoonWarningTitle"));
        assertEquals("%void%", saved.getString("BloodMoonWarningBody"));
        assertNotNull(saved.getString("HordeArrived"));
    }

    @Test void reloadingUsesEditsAndImmediatelyRestoresMissingMessages() throws Exception {
        LocaleReader reader = new LocaleReader(plugin);
        var edited = YamlConfiguration.loadConfiguration(messages.toFile());
        edited.set("BloodMoonWarningTitle", "&6Título cambiado");
        edited.set("HordeArrived", null);
        edited.save(messages.toFile());
        reader.RefreshLocales();
        assertTrue(reader.GetLocaleString("BloodMoonWarningTitle").contains("Título cambiado"));
        assertTrue(ChatColor.stripColor(reader.GetLocaleString("HordeArrived")).contains("Una horda acecha a $p"));
    }

    @Test void aSaveFailureStillLeavesBundledMessagesAvailableInMemory() throws Exception {
        Files.writeString(directory.resolve("bloodmoon"), "El directorio está bloqueado por un archivo.");
        LocaleReader reader = new LocaleReader(plugin);
        assertTrue(reader.GetLocaleString("BloodMoonWarningBody").contains("Ha empezado una BloodMoon."));
    }

    @Test void previousDefaultMessagesReceiveTheNewPaletteWithoutOverwritingCustomOrSilencedMessages() throws Exception {
        YamlConfiguration previous;
        YamlConfiguration defaults;
        try (var stream = plugin.getResource("bloodmoon-defaults/mensajes-v1.yml")) {
            previous = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        try (var stream = plugin.getResource("bloodmoon-defaults/mensajes.yml")) {
            defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        previous.set("BloodMoonWarningTitle", "&6Mi noche especial");
        previous.set("BloodMoonWarningBody", "%void%");
        previous.save(messages.toFile());
        LocaleReader reader = new LocaleReader(plugin);
        var saved = YamlConfiguration.loadConfiguration(messages.toFile());
        for (String id : defaults.getKeys(false)) {
            if (id.equals("BloodMoonWarningTitle") || id.equals("BloodMoonWarningBody")) continue;
            assertEquals(defaults.getString(id), saved.getString(id), id);
            assertEquals(LocaleReader.color(defaults.getString(id)), reader.GetLocaleString(id), id);
        }
        assertEquals("&6Mi noche especial", saved.getString("BloodMoonWarningTitle"));
        assertEquals("%void%", saved.getString("BloodMoonWarningBody"));
        assertEquals("", reader.GetLocaleString("BloodMoonWarningBody"));
        reader.RefreshLocales();
        assertEquals("&6Mi noche especial", YamlConfiguration.loadConfiguration(messages.toFile()).getString("BloodMoonWarningTitle"));
    }
}
