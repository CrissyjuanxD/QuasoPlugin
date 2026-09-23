package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission110 extends BaseMission {

    public Mission110(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 110, "Caballo acorazado", MissionDifficulty.DIFICIL, 17,
                "Craftea una Armadura de Caballo de Netherite y pónsela a un caballo.");
        flag("craftear", "Craftear la armadura");
        flag("poner", "Ponérsela a un caballo");
        extraOf(29);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(item(Material.SADDLE, 1), item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && event.getRecipe().getResult().getType() == Material.NETHERITE_HORSE_ARMOR) {
            mark(player, "craftear");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (event.getWhoClicked() instanceof Player player && result != null && result.getType() == Material.NETHERITE_HORSE_ARMOR) {
            mark(player, "craftear");
        }
    }

    @Override
    protected int tickSeconds() {
        return 2;
    }

    // Vale el caballo que monta o uno suyo que esté cerca
    @Override
    protected void tick(Player player) {
        if (player.getVehicle() instanceof Horse horse && hasArmor(horse)) {
            mark(player, "poner");
            return;
        }
        for (Entity nearby : player.getNearbyEntities(8, 4, 8)) {
            if (nearby instanceof Horse horse && player.equals(horse.getOwner()) && hasArmor(horse)) {
                mark(player, "poner");
                return;
            }
        }
    }

    private boolean hasArmor(Horse horse) {
        ItemStack armor = horse.getInventory().getArmor();
        return armor != null && armor.getType() == Material.NETHERITE_HORSE_ARMOR;
    }
}
