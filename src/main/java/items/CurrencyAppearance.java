package items;

import Events.MissionSystem.MissionHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Actualiza también las monedas ya entregadas, sin recrear sus datos ni cambiar cantidades. */
public final class CurrencyAppearance implements Listener {
    public static boolean refresh(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String name;
        if (EconomyItemsFunctions.isDinoCoin(item)) {
            name = ChatColor.of(EconomyItems.DINOCOINS_COLOR) + "DinoCoins " + ChatColor.of("#3f2163") + "۞";
        } else if (EconomyItemsFunctions.isDinoFicha(item)) {
            name = ChatColor.of(EconomyItems.DINOFICHAS_COLOR) + "DinoFichas " + ChatColor.of("#99714f") + "۞";
        } else if (MissionHandler.getTokenMission(item) > 0) {
            String old = item.getItemMeta().hasDisplayName() ? ChatColor.stripColor(item.getItemMeta().getDisplayName()) : "";
            String tag = old.startsWith("Ficha de Misión ") ? old.substring("Ficha de Misión ".length())
                    : "#" + MissionHandler.getTokenMission(item);
            name = ChatColor.of(EconomyItems.MISSION_TOKEN_COLOR) + "Ficha de Misión " + ChatColor.of("#ffc183") + tag;
        } else return false;
        var meta = item.getItemMeta();
        if (name.equals(meta.getDisplayName()) && meta.hasEnchantmentGlintOverride()
                && !meta.getEnchantmentGlintOverride() && !meta.hasEnchant(Enchantment.UNBREAKING)) return false;
        meta.setDisplayName(name);
        meta.removeEnchant(Enchantment.UNBREAKING);
        meta.setEnchantmentGlintOverride(false);
        item.setItemMeta(meta);
        return true;
    }

    public static void refresh(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (refresh(item)) inventory.setItem(slot, item);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { refresh(event.getPlayer().getInventory()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        refresh(event.getInventory());
        refresh(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack item = event.getItem().getItemStack();
        if (refresh(item)) event.getItem().setItemStack(item);
    }
}
