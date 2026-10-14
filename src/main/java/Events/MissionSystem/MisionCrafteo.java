package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// Las misiones extra de "fabrica tal cosa", para que se vea qué recetas hay. Cada objetivo se marca cuando el jugador
// tiene el item en el inventario o puesto (se revisa cada 3 segundos), así cuenta igual si lo hizo con shift, en la
// herrería o en el alto horno. Una vez marcado se queda aunque después lo mejore o lo pierda
public abstract class MisionCrafteo extends BaseMission {
    private final Map<String, Predicate<ItemStack>> items = new LinkedHashMap<>();

    protected MisionCrafteo(JavaPlugin plugin, MissionHandler handler, int number, String name,
                            MissionDifficulty difficulty, int coins, String description) {
        super(plugin, handler, number, name, difficulty, coins, description);
    }

    protected void objetivo(String key, String label, Predicate<ItemStack> item) {
        flag(key, label);
        items.put(key, item);
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        for (Map.Entry<String, Predicate<ItemStack>> entry : items.entrySet()) {
            if (MissionUtils.has(player, entry.getValue())) mark(player, entry.getKey());
        }
    }

    // Un item custom por su id (cualquiera de los que se pasan)
    static Predicate<ItemStack> id(String... ids) {
        Set<String> set = Set.of(ids);
        return item -> {
            String customId = MissionUtils.customId(item);
            return customId != null && set.contains(customId);
        };
    }

    // Una pieza de la armadura de Warden según su tipo (casco, peto...)
    static Predicate<ItemStack> warden(Material base) {
        return item -> item.getType() == base && MissionUtils.isWardenArmor(item);
    }
}
