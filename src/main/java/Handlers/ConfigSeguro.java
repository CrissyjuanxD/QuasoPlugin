package Handlers;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Protege el config.yml: le pone comillas a los valores que YAML no acepta sin ellas (una contraseña que empieza con
// @, ` o %) y, si aun así no se puede leer, no deja que el plugin lo guarde vacío encima y borre lo que había
public final class ConfigSeguro {

    // clave: valor, con el valor empezando por un carácter que no puede ir sin comillas
    private static final Pattern SIN_COMILLAS = Pattern.compile("^(\\s*[^#\\s'\"][^:#]*:[ \\t]+)([@`%][^\\r\\n]*?)([ \\t]+#.*)?\\s*$");

    private static volatile boolean rota;

    private ConfigSeguro() {}

    public static boolean rota() {
        return rota;
    }

    // Arregla y revisa config.yml antes de que Paper lo lea. Se llama en cada reloadConfig
    public static void revisar(JavaPlugin plugin) {
        File archivo = new File(plugin.getDataFolder(), "config.yml");
        if (!archivo.exists()) {
            rota = false;
            return;
        }
        try {
            List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            List<String> claves = new ArrayList<>();
            List<String> arregladas = comillas(lineas, claves);
            if (!claves.isEmpty()) {
                Files.write(archivo.toPath(), arregladas, StandardCharsets.UTF_8);
                plugin.getLogger().warning("config.yml: se le pusieron comillas a " + String.join(", ", claves)
                        + " (un valor que empieza con @, ` o % tiene que ir entre comillas).");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("config.yml: no se pudo revisar (" + e.getMessage() + ").");
        }
        try {
            new YamlConfiguration().load(archivo);
            rota = false;
        } catch (IOException | InvalidConfigurationException e) {
            rota = true;
            plugin.getLogger().severe("config.yml tiene un error y no se puede leer (revisa la línea que marca el error). Mientras"
                    + " tanto el plugin no lo va a guardar, para no borrar lo que tienes. Arréglalo y usa /quasoreload.");
        }
    }

    // Devuelve las líneas con comillas simples en los valores que las necesitan; en claves quedan los nombres arreglados
    static List<String> comillas(List<String> lineas, List<String> claves) {
        List<String> salida = new ArrayList<>(lineas.size());
        for (String linea : lineas) {
            Matcher m = SIN_COMILLAS.matcher(linea);
            if (!m.matches()) {
                salida.add(linea);
                continue;
            }
            String valor = m.group(2).strip().replace("'", "''");
            salida.add(m.group(1) + "'" + valor + "'" + (m.group(3) != null ? m.group(3) : ""));
            claves.add(m.group(1).strip().replace(":", ""));
        }
        return salida;
    }
}
