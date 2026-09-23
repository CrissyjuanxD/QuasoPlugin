package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static Events.MissionSystem.MissionRewards.*;

public class Mission96 extends BaseMission {
    // Noche de la BloodMoon que está aguantando cada jugador, y la noche en la que murió
    private final Map<UUID, Long> surviving = new HashMap<>();
    private final Map<UUID, Long> died = new HashMap<>();

    public Mission96(JavaPlugin plugin, MissionHandler handler) {
        super(plugin, handler, 96, "El último en pie", MissionDifficulty.DIFICIL, 20,
                "Sobrevive 3 BloodMoons seguidas sin morir. Si mueres en una, vuelves a 0.");
        counter("lunas", "BloodMoons sin morir", 3);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        return of(custom("amulet_bloodmoon", 1), item(Material.ENCHANTED_GOLDEN_APPLE, 5));
    }

    @Override
    protected int tickSeconds() {
        return 5;
    }

    // Al terminar la BloodMoon suma una si el jugador la aguantó entera sin morir
    @Override
    protected void tick(Player player) {
        World world = Bukkit.getWorlds().get(0);
        UUID id = player.getUniqueId();
        if (MissionUtils.isBloodMoon(world)) {
            long night = MissionUtils.dayId(world);
            if (!Long.valueOf(night).equals(died.get(id))) surviving.putIfAbsent(id, night);
        } else if (surviving.remove(id) != null) {
            add(player, "lunas", 1);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = Bukkit.getWorlds().get(0);
        if (!tracking(player) || !MissionUtils.isBloodMoon(world)) return;

        died.put(player.getUniqueId(), MissionUtils.dayId(world));
        surviving.remove(player.getUniqueId());
        if (data(player).getProgressInt("lunas") > 0) {
            set(player, "lunas", 0);
            sendBar(player, ChatColor.RED + "✖ Moriste en la BloodMoon, vuelves a 0");
        }
    }

    @Override
    public void onQuit(Player player) {
        super.onQuit(player);
        surviving.remove(player.getUniqueId());
    }
}
