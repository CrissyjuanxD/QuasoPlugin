package items;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

// Te saca de la Warden Cave y te manda a tu spawn (cama o spawn del mundo) con /magictp; se gasta al usarla
public class WardenReturnItem implements Listener {
    private static final NamespacedKey KEY = new NamespacedKey("quasoplugin", "retorno_warden");
    private static final int COOLDOWN_TICKS = 100;

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#6BD2D6") + "" + ChatColor.BOLD + "Brújula de Retorno");
        meta.setLore(List.of(
                "",
                ChatColor.of("#AEECEF") + "Te saca de la Warden Cave",
                ChatColor.of("#AEECEF") + "y te lleva a tu spawn.",
                "",
                ChatColor.GRAY + "Se gasta al usarla.",
                ChatColor.GRAY + "Uso:",
                ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho",
                ""
        ));
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("recovery_compass"));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isReturnItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() == null || !isReturnItem(event.getItem())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) {
            player.sendMessage(ChatColor.of("#FFA07A") + "La Brújula de Retorno solo funciona dentro de la Warden Cave.");
            return;
        }

        ItemStack item = player.getInventory().getItem(event.getHand());
        if (player.hasCooldown(item)) return;
        player.setCooldown(item, COOLDOWN_TICKS);
        if (player.getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);

        player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1f, 0.8f);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "magictp " + player.getName() + " spawn");
    }
}
