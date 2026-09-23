package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static Events.MissionSystem.MissionRewards.*;

public class Mission88 extends BaseMission {
    private static final Map<Material, String> HEADS = new LinkedHashMap<>();

    static {
        HEADS.put(Material.ZOMBIE_HEAD, "Zombie");
        HEADS.put(Material.SKELETON_SKULL, "Esqueleto");
        HEADS.put(Material.CREEPER_HEAD, "Creeper");
        HEADS.put(Material.WITHER_SKELETON_SKULL, "Wither Skeleton");
        HEADS.put(Material.PIGLIN_HEAD, "Piglin");
        HEADS.put(Material.DRAGON_HEAD, "Dragón");
    }

    public Mission88(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 88, "Cazador de cabezas", MissionDifficulty.DIFICIL, 18,
                "Consigue las cabezas de zombie, esqueleto, creeper, wither skeleton, piglin y dragón.");
        HEADS.forEach((head, label) -> flag(head.name(), "Cabeza de " + label));
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.LOOTING, 4), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @Override
    protected int tickSeconds() {
        return 3;
    }

    @Override
    protected void tick(Player player) {
        for (Material head : HEADS.keySet()) {
            if (MissionUtils.count(player, head) > 0) mark(player, head.name());
        }
    }
}
