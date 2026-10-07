package Twitch;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

// /twitchadmin kits: mirar qué trae cada kit sin poder sacar nada
final class TwitchKitPreview implements Listener {

    private static final int[] KIT_SLOTS = {1, 3, 5, 7};

    private final JavaPlugin plugin;

    private static final class Menu implements InventoryHolder {
        private final TwitchKits.Kit kit;
        private Inventory inventory;

        Menu(TwitchKits.Kit kit) {
            this.kit = kit;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    TwitchKitPreview(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void open(Player player) {
        Menu menu = new Menu(null);
        Inventory inventory = Bukkit.createInventory(menu, 9, ChatColor.of("#9146FF") + "Kits de Twitch");
        menu.inventory = inventory;
        TwitchKits.Kit[] kits = TwitchKits.Kit.values();
        for (int i = 0; i < kits.length; i++) {
            ItemStack icon = new ItemStack(Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(kits[i].colored());
            meta.setLore(List.of(ChatColor.GRAY + "Clic para ver lo que trae"));
            icon.setItemMeta(meta);
            inventory.setItem(KIT_SLOTS[i], icon);
        }
        player.openInventory(inventory);
    }

    private void open(Player player, TwitchKits.Kit kit) {
        Menu menu = new Menu(kit);
        Inventory inventory = Bukkit.createInventory(menu, 27, kit.colored());
        menu.inventory = inventory;
        inventory.setContents(TwitchKits.contents(kit));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);
        if (menu.kit != null || !(event.getWhoClicked() instanceof Player player)) return;
        TwitchKits.Kit[] kits = TwitchKits.Kit.values();
        for (int i = 0; i < kits.length; i++) {
            if (event.getRawSlot() == KIT_SLOTS[i]) {
                TwitchKits.Kit kit = kits[i];
                Bukkit.getScheduler().runTask(plugin, () -> open(player, kit));
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
}
