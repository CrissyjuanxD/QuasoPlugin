package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission12 extends BaseMission {

    public Mission12(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 12, "Cazador de Abejas", MissionDifficulty.DIFICIL, 18,
                "Mata a la Abeja Reina. Usa /bosstp para ir a su dungeon e interactúa con el panal del altar.");
        flag("reina", "Abeja Reina derrotada");
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(book(Enchantment.UNBREAKING, 4, 3), item(Material.GOLD_BLOCK, 10));
    }

    // Cuenta para todos los que pelearon, no solo para el que dio el último golpe
    @EventHandler
    public void onBoss(BossDefeatedEvent event) {
        if (!event.getBossId().equals("abeja_reina")) return;
        for (Player player : event.getPlayers()) mark(player, "reina");
    }
}
