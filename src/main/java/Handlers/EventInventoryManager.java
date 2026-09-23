package Handlers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.Predicate;

public class EventInventoryManager implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    private Predicate<String> isInEventCondition = (name) -> false;

    public EventInventoryManager(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setIsInEventCondition(Predicate<String> condition) {
        this.isInEventCondition = condition;
    }

    public void saveAndClearInventory(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        ItemStack[] contents = player.getInventory().getContents();

        ItemStack[] clonedContents = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) clonedContents[i] = contents[i].clone();
        }

        player.getInventory().clear();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dbManager.saveEventInventory(uuid, name, clonedContents);
        });
    }

    public void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] contents = dbManager.getEventInventory(uuid);

            if (contents != null) {
                if (!player.isOnline()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setContents(contents);
                        player.updateInventory();

                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            dbManager.deleteEventInventory(uuid);
                        });
                    }
                });
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ItemStack[] contents = dbManager.getEventInventory(player.getUniqueId());

            if (contents != null) {
                if (isInEventCondition.test(name)) {
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.getInventory().setContents(contents);
                        player.updateInventory();
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            dbManager.deleteEventInventory(player.getUniqueId());
                        });
                        player.sendMessage(ChatColor.GREEN + "۞ Tu inventario del evento ha sido restaurado exitosamente.");
                    }
                });
            }
        });
    }
}