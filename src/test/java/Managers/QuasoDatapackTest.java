package Managers;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuasoDatapackTest {

    @Test
    void everyDatapackFileIsInTheJarAndReadable() throws Exception {
        List<String> files = QuasoDatapack.files();
        assertTrue(files.contains("data/quaso/worldgen/biome/picos_helados.json"));
        assertEquals(20, files.stream().filter(f -> f.startsWith("data/minecraft/structure/end_city/")).count());
        for (String file : files) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("datapack/QuasoPlugin/" + file)) {
                assertNotNull(in, file);
                byte[] data = in.readAllBytes();
                if (file.endsWith(".nbt")) {
                    // Las estructuras van comprimidas: si Maven las filtrara como texto se romperían
                    assertEquals(0x1f, data[0] & 0xff, file);
                    assertEquals(0x8b, data[1] & 0xff, file);
                } else {
                    JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
                }
            }
        }
    }
}
