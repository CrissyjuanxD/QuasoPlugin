package Managers;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

// Nombres de los mundos del server, configurables en mundos.yml para no depender de que el mundo se llame "world"
public final class Mundos {

    private static String principal = "Crossiants";
    private static String eventos = "Crossiants";
    private static String wardenCave = "wardencave";
    private static Logger logger = Logger.getLogger("QuasoPlugin");
    private static boolean avisado;

    private Mundos() {}

    // Lee mundos.yml (lo crea con los valores de fábrica si no existe). Se llama al prender el plugin
    public static void cargar(JavaPlugin plugin) {
        logger = plugin.getLogger();
        File archivo = new File(plugin.getDataFolder(), "mundos.yml");
        if (!archivo.exists()) plugin.saveResource("mundos.yml", false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        principal = config.getString("principal", principal);
        eventos = config.getString("eventos", principal);
        wardenCave = config.getString("warden_cave", wardenCave);
        avisado = false;
    }

    public static String principal() {
        return principal;
    }

    public static String eventos() {
        return eventos;
    }

    public static String wardenCave() {
        return wardenCave;
    }

    // El mundo principal. Si el nombre de mundos.yml no existe usa el primero que cargó el server y avisa una vez
    public static World mundoPrincipal() {
        return buscar(principal);
    }

    // Donde se juegan los eventos (Hot Potato, Skybattle y Build Battle)
    public static World mundoEventos() {
        return buscar(eventos);
    }

    private static World buscar(String nombre) {
        World mundo = Bukkit.getWorld(nombre);
        if (mundo != null) return mundo;
        if (!avisado) {
            avisado = true;
            logger.warning("mundos.yml: no existe el mundo '" + nombre + "', se usa '" + Bukkit.getWorlds().get(0).getName() + "'.");
        }
        return Bukkit.getWorlds().get(0);
    }
}
