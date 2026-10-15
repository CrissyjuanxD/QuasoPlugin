package Handlers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSeguroTest {

    @Test
    void quotesValuesThatStartWithAtAndKeepsTheRest() throws Exception {
        List<String> lineas = List.of(
                "Database1:",
                "  host: \"127.0.0.1\" # El host",
                "  port: 3306",
                "  password: @vh'I4^3O9w # la clave",
                "  otra: `raro",
                "  ya: '@bien'",
                "# password: @comentario");
        List<String> claves = new ArrayList<>();
        List<String> salida = ConfigSeguro.comillas(lineas, claves);
        assertEquals(List.of("password", "otra"), claves);
        assertEquals("  password: '@vh''I4^3O9w' # la clave", salida.get(3));
        assertEquals("  ya: '@bien'", salida.get(5));
        assertEquals(lineas.get(6), salida.get(6));

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n", salida));
        assertEquals("@vh'I4^3O9w", config.getString("Database1.password"));
        assertEquals("`raro", config.getString("Database1.otra"));
        assertEquals("127.0.0.1", config.getString("Database1.host"));
        assertEquals(3306, config.getInt("Database1.port"));
    }
}
