package Events.MissionSystem;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission53 extends BaseMission {

    public Mission53(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 53, "Armor 101%", MissionDifficulty.DIFICIL, 20,
                "Ponte la armadura de Warden completa con Protección V. Los 4 libros salen de las misiones 14, 24, 39 y 47.");
        flag("armadura", "Armadura de Warden con Protección V");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.EFFICIENCY, 6), custom("splash_resistance_3", 3));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    @Override
    protected void tick(Player player) {
        if (!MissionUtils.fullWardenArmor(player)) return;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece.getEnchantmentLevel(Enchantment.PROTECTION) < 5) return;
        }
        mark(player, "armadura");
    }
}
