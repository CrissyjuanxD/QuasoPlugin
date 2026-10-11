package Gui;

import Managers.ItemManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Catálogo administrativo: los iconos son muestras, nunca los items entregados. */
public final class ItemCatalog implements Listener {
    public static final String PERMISSION = "viciont_hardcore3.command.givevct";
    private static final int PAGE_SIZE = 45;
    private final JavaPlugin plugin;
    private final ItemManager items;

    public ItemCatalog(JavaPlugin plugin, ItemManager items) {
        this.plugin = plugin;
        this.items = items;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        if (!player.hasPermission(PERMISSION)) return;
        open(player, items.getRegisteredItems().stream().distinct().toList(), 0);
    }

    private void open(Player player, List<String> keys, int page) {
        if (!player.hasPermission(PERMISSION)) return;
        int pages = Math.max(1, (keys.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        Page holder = new Page(player.getUniqueId(), keys, Math.clamp(page, 0, pages - 1));
        holder.inventory = Bukkit.createInventory(holder, 54,
                ChatColor.of("#c6a5e8") + "۞ Items · " + (holder.page + 1) + "/" + pages);
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = holder.page * PAGE_SIZE + slot;
            if (index >= keys.size()) break;
            String key = keys.get(index);
            ItemStack sample = items.getItem(key, 1, player);
            if (sample == null) continue;
            sample = sample.clone();
            ItemMeta meta = sample.getItemMeta();
            List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
            lore.add("");
            lore.add(ChatColor.of("#a9c8ef") + "/giveqp " + key);
            lore.add(ChatColor.of("#d8bee9") + "Clic: recibir uno");
            meta.setLore(lore);
            sample.setItemMeta(meta);
            holder.inventory.setItem(slot, sample);
        }
        if (holder.page > 0) holder.inventory.setItem(45, button(Material.ARROW, "← Página anterior"));
        holder.inventory.setItem(49, button(Material.BARRIER, "Cerrar"));
        if (holder.page + 1 < pages) holder.inventory.setItem(53, button(Material.ARROW, "Página siguiente →"));
        player.openInventory(holder.inventory);
    }

    private ItemStack button(Material material, String title) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#c6a5e8") + title);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Page page)) return;
        event.setCancelled(true); // También bloquea shift, teclas numéricas y doble clic desde abajo.
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!page.viewer.equals(player.getUniqueId()) || !player.hasPermission(PERMISSION)) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= page.inventory.getSize()) return;
        if (!event.isLeftClick() && !event.isRightClick()) return;
        if (slot == 49) {
            Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
        } else if (slot == 45 || slot == 53) {
            int next = page.page + (slot == 45 ? -1 : 1);
            if (next >= 0 && next * PAGE_SIZE < page.keys.size()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline() && player.getOpenInventory().getTopInventory() == page.inventory)
                        open(player, page.keys, next);
                });
            }
        } else if (slot < PAGE_SIZE) {
            int index = page.page * PAGE_SIZE + slot;
            if (index >= page.keys.size()) return;
            // Volver a crearlo conserva identidades únicas de monederos y evita entregar el lore del menú.
            ItemStack item = items.getItem(page.keys.get(index), 1, player);
            if (item == null) return;
            player.getInventory().addItem(item).values().forEach(leftover ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            player.sendMessage(ChatColor.of("#c6a5e8") + "۞ " + ChatColor.of("#a9c8ef")
                    + "Has recibido " + item.getAmount() + " × " + page.keys.get(index) + ".");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Page) event.setCancelled(true);
    }

    static final class Page implements InventoryHolder {
        final UUID viewer;
        final List<String> keys;
        final int page;
        Inventory inventory;

        Page(UUID viewer, List<String> keys, int page) {
            this.viewer = viewer;
            this.keys = keys;
            this.page = page;
        }

        @Override public Inventory getInventory() { return inventory; }
    }
}
