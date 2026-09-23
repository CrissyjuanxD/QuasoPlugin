package Events.MissionSystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission95 extends BaseMission {
    private final List<NamespacedKey> advancements = new ArrayList<>();

    public Mission95(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 95, "Cazador de logros", MissionDifficulty.MUY_DIFICIL, 25,
                "Consigue todos los logros de Minecraft.");

        // Solo los logros de Minecraft que se ven en el menú (las recetas no cuentan)
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            NamespacedKey key = advancement.getKey();
            if (key.getNamespace().equals(NamespacedKey.MINECRAFT) && advancement.getDisplay() != null
                    && !key.getKey().startsWith("recipes/")) {
                advancements.add(key);
            }
        }
        if (advancements.isEmpty()) plugin.getLogger().warning("Misión 95: no se encontraron logros de Minecraft.");
        counter("logros", "Logros", Math.max(1, advancements.size()));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.BEACON, 1), custom("doubletotem", 1));
    }

    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        int done = 0;
        for (NamespacedKey key : advancements) {
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement != null && player.getAdvancementProgress(advancement).isDone()) done++;
        }
        return done;
    }

    @Override
    protected int tickSeconds() {
        return 15;
    }

    @Override
    protected void tick(Player player) {
        check(player);
    }
}
