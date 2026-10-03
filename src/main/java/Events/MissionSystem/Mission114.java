package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Nautilus;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission114 extends BaseMission {

    public Mission114(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 114, "Nautilus de guerra", MissionDifficulty.DIFICIL, 18,
                "Ponle armadura de Netherite a un Nautilus y entra montado a un Monumento Oceánico.");
        flag("armadura", "Nautilus con armadura de Netherite");
        flag("monumento", "Entrar montado a un Monumento");
        extraOf(48);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SPONGE, 16), potion(PotionType.LONG_WATER_BREATHING, 3));
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    @Override
    protected void tick(Player player) {
        if (!(player.getVehicle() instanceof Nautilus nautilus)) return;
        ItemStack armor = nautilus.getInventory().getArmor();
        if (armor == null || armor.getType() != Material.NETHERITE_NAUTILUS_ARMOR) return;

        mark(player, "armadura");
        if (player.getWorld().hasStructureAt(player.getLocation(), Structure.MONUMENT)) mark(player, "monumento");
    }
}
