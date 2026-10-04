package InfestedCaves;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class WardenDatapack {

    private static final String NAME = "QuasoWardenCave";
    private static final String[] FILES = {
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

    // Archivos de versiones anteriores que se borran al instalar (Sigilo se cambió por Visión Abisal)
    private static final String[] REMOVED = {
            "data/quaso/enchantment/sigilo.json"
    };

    private WardenDatapack() {}

    // Copia el datapack (los biomas, la lista de biomas donde sale la Ancient City y los encantamientos) a world/datapacks.
    // Devuelve true si lo instaló o lo actualizó, y en ese caso hay que reiniciar porque se carga al prender el server
    public static boolean install(JavaPlugin plugin) {
        Path target = Bukkit.getServer().getLevelDirectory().resolve("datapacks").resolve(NAME);
        boolean changed = false;
        for (String file : REMOVED) {
            try {
                if (Files.deleteIfExists(target.resolve(file))) changed = true;
            } catch (IOException e) {
                plugin.getLogger().warning("No se pudo borrar " + file + " del datapack: " + e.getMessage());
            }
        }
        for (String file : FILES) {
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
        return true;
    }
}
