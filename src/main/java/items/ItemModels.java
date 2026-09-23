package items;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

// En la 26.2 el custom model data ya no cambia la textura: cada item usa un item model.
// Los nombres van en config.yml (modelos.<clave>) para poner los del resource pack sin recompilar;
// por defecto son modelos vanilla para que se vea bien aunque el pack todavía no los tenga
public final class ItemModels {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("gui_panel", "minecraft:black_stained_glass_pane");
        DEFAULTS.put("mision_completada", "minecraft:lime_banner");
        DEFAULTS.put("mision_pendiente", "minecraft:guster_banner_pattern");
        DEFAULTS.put("mision_bloqueada", "minecraft:map");
        DEFAULTS.put("ficha_mision", "minecraft:popped_chorus_fruit");
        DEFAULTS.put("libro_misiones", "minecraft:map");
        DEFAULTS.put("retorno_warden", "minecraft:recovery_compass");
    }

    private ItemModels() {}

    // Agrega a config.yml los modelos que falten, así aparecen para cambiarlos
    public static void load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            String path = "modelos." + entry.getKey();
            if (!config.isString(path)) {
                config.set(path, entry.getValue());
                changed = true;
            }
        }
        if (changed) plugin.saveConfig();
    }

    public static void apply(ItemMeta meta, String key) {
        String value = QuasoPlugin.getInstance().getConfig().getString("modelos." + key, DEFAULTS.get(key));
        NamespacedKey model = value != null ? NamespacedKey.fromString(value) : null;
        if (model == null) {
            QuasoPlugin.getInstance().getLogger().warning("El item model '" + value + "' de modelos." + key + " no es válido.");
            return;
        }
        meta.setItemModel(model);
    }
}
