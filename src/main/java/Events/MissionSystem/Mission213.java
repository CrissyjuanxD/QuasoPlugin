package Events.MissionSystem;

import BloodMoon.BloodMoonMobs;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

public class Mission213 extends BaseMission {

    public Mission213(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 213, "Primera sangre", MissionDifficulty.MEDIA, 13,
                "Mata un Zombie, un Esqueleto, una Araña y un Creeper de BloodMoon (los de nombre rojo).");
        for (BloodMoonMobs.Tipo tipo : BloodMoonMobs.Tipo.values()) flag(tipo.id(), tipo.nombre());
        extraOf(9);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("blood_fragment", 3), item(Material.GOLDEN_APPLE, 4));
    }

    // Cada tipo de mob de BloodMoon marca su objetivo
    @EventHandler
    public void onKill(EntityDeathEvent event) {
        BloodMoonMobs.Tipo tipo = BloodMoonMobs.tipo(event.getEntity());
        if (tipo == null) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) mark(killer, tipo.id());
    }
}
