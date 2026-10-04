package Managers;

import EndBiomes.EndBiome;
import InfestedCaves.WardenBiome;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

// El datapack del plugin: los biomas de la Warden Cave y del End, la lista de biomas donde sale la Ancient City y
// los encantamientos. Es uno solo para todo el plugin
public final class QuasoDatapack {

    private static final String NAME = "QuasoPlugin";
    // Datapacks de versiones anteriores que se borran (antes los biomas iban en uno aparte)
    private static final String[] OLD_PACKS = {"QuasoWardenCave"};
    private static final String[] BASE_FILES = {
            "pack.mcmeta",
            "data/quaso/worldgen/biome/caverna_sculk.json",
            "data/quaso/worldgen/biome/pantano_profundo.json",
            "data/quaso/worldgen/biome/abismo_flotante.json",
            "data/quaso/worldgen/biome/ruinas_de_ceniza.json",
            "data/minecraft/tags/worldgen/biome/has_structure/ancient_city.json",
            "data/quaso/enchantment/paso_igneo.json",
            "data/quaso/enchantment/purificacion.json",
            "data/quaso/enchantment/vision_abisal.json",
            "data/quaso/enchantment/anclaje.json",
            "data/quaso/enchantment/retorno_del_vacio.json"
    };

    private QuasoDatapack() {}

    private static List<String> files() {
        List<String> files = new ArrayList<>(Arrays.asList(BASE_FILES));
        for (EndBiome biome : EndBiome.values()) files.add("data/quaso/worldgen/biome/" + biome.key().getKey() + ".json");
        return files;
    }

    // Copia el datapack a world/datapacks. Devuelve true si lo instaló o lo actualizó, y en ese caso hay que
    // reiniciar porque se carga al prender el server
    public static boolean install(JavaPlugin plugin) {
        Path datapacks = Bukkit.getServer().getLevelDirectory().resolve("datapacks");
        boolean changed = false;
        for (String old : OLD_PACKS) {
            Path dir = datapacks.resolve(old);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
                changed = true;
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo borrar el datapack viejo " + old + ": " + e.getMessage());
            }
        }

        Path target = datapacks.resolve(NAME);
        for (String file : files()) {
            try (InputStream in = plugin.getResource("datapack/" + NAME + "/" + file)) {
                if (in == null) {
                    plugin.getLogger().warning("Falta en el jar: datapack/" + NAME + "/" + file);
                    continue;
                }
                byte[] data = in.readAllBytes();
                Path out = target.resolve(file);
                if (Files.exists(out) && Arrays.equals(Files.readAllBytes(out), data)) continue;
                Files.createDirectories(out.getParent());
                Files.write(out, data);
                changed = true;
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo instalar el datapack de QuasoPlugin: " + e.getMessage());
                return false;
            }
        }
        return changed;
    }

    public static boolean biomesLoaded() {
        for (WardenBiome biome : WardenBiome.values()) {
            if (!biome.isLoaded()) return false;
        }
        for (EndBiome biome : EndBiome.values()) {
            if (biome.get() == null) return false;
        }
        return true;
    }
}
