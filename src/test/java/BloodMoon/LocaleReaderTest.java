package BloodMoon;

import org.bukkit.configuration.file.YamlConfiguration;
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

    @Test void incompleteFilesReceiveMissingMessagesAndPreserveCustomizedAndSilencedEntries() throws Exception {
        Files.createDirectories(messages.getParent());
        Files.writeString(messages, "BloodMoonWarningTitle: '&6Mi BloodMoon'\nBloodMoonWarningBody: '%void%'\n");
        LocaleReader reader = new LocaleReader(plugin);
        assertTrue(reader.GetLocaleString("BloodMoonWarningTitle").contains("Mi BloodMoon"));
        assertEquals("", reader.GetLocaleString("BloodMoonWarningBody"));
        assertTrue(reader.GetLocaleString("HordeArrived").contains("Una horda acecha a $p"));
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
        assertTrue(reader.GetLocaleString("HordeArrived").contains("Una horda acecha a $p"));
    }

    @Test void aSaveFailureStillLeavesBundledMessagesAvailableInMemory() throws Exception {
        Files.writeString(directory.resolve("bloodmoon"), "El directorio está bloqueado por un archivo.");
        LocaleReader reader = new LocaleReader(plugin);
        assertTrue(reader.GetLocaleString("BloodMoonWarningBody").contains("Ha empezado una BloodMoon."));
    }
}
