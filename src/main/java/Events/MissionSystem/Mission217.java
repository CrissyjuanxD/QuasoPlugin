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

public class Mission217 extends BaseMission {

    public Mission217(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 217, "Telarañas rojas", MissionDifficulty.MEDIA, 14,
                "Mata 15 Arañas BloodMoon. Cuidado con su salto: si te cae encima te hace sangrar y te deja lento.");
        counter("aranas", "Arañas BloodMoon", 15);
        extraOf(58);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("frasco_de_velocidad", 3), item(Material.GOLDEN_APPLE, 6));
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (BloodMoonMobs.tipo(event.getEntity()) != BloodMoonMobs.Tipo.ARANA) return;
        Player killer = MissionUtils.killer(event.getEntity());
        if (killer != null) add(killer, "aranas", 1);
    }
}
