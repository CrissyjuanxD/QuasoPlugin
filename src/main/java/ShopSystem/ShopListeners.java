package ShopSystem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopListeners implements Listener {

    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopListeners(ShopManager shopManager, ShopGUI shopGUI) {
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    // --- REUTILIZACIÓN DE LÓGICA DE TU CÓDIGO ANTIGUO ---
    // Compara items ignorando el Lore para permitir mochilas usadas
    private boolean isSimilarIgnoreLore(ItemStack recipeItem, ItemStack playerItem) {
        if (playerItem == null || playerItem.getType() == Material.AIR) return false;
        if (recipeItem.getType() != playerItem.getType()) return false;

        ItemMeta rMeta = recipeItem.getItemMeta();
        ItemMeta pMeta = playerItem.getItemMeta();

        if (rMeta == null && pMeta == null) return true;
        if (rMeta == null || pMeta == null) return false;

        // Comparar Nombre (si la receta lo pide)
        if (rMeta.hasDisplayName()) {
            if (!pMeta.hasDisplayName()) return false;
            if (!rMeta.getDisplayName().equals(pMeta.getDisplayName())) return false;
        }

        // Comparar CustomModelData (Crucial para items custom como tus mochilas)
        if (rMeta.hasCustomModelData()) {
            if (!pMeta.hasCustomModelData()) return false;
            if (rMeta.getCustomModelData() != pMeta.getCustomModelData()) return false;
        }

        // IMPORTANTE: NO comparamos Lore. Esto permite que la mochila con stats pase.
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Villager villager = (Villager) event.getRightClicked();
        if (!villager.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) return;

        Player player = event.getPlayer();

        if (player.isOp() && player.isSneaking()) {
            event.setCancelled(true);

            if (shopManager.editingShops.containsKey(player.getUniqueId()) &&
                    shopManager.editingTradeIndex.containsKey(player.getUniqueId()) &&
                    shopManager.editingSlotType.containsKey(player.getUniqueId())) {

                String currentShopId = shopManager.editingShops.get(player.getUniqueId());
                String clickedId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);

                if (currentShopId != null && currentShopId.equals(clickedId)) {
                    ItemStack handItem = player.getInventory().getItemInMainHand();

                    if (handItem != null && handItem.getType() != Material.AIR) {
                        ShopCommands.applyItemToTrade(player, shopManager, handItem.clone());
                        shopManager.editingTradeIndex.remove(player.getUniqueId());
                        shopManager.editingSlotType.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.YELLOW + "Item configurado. Puedes abrir la GUI de nuevo.");
                        shopGUI.openConfigGUI(player, villager);
                    } else {
                        // Cancelar edición si mano vacía
                        shopManager.editingTradeIndex.remove(player.getUniqueId());
                        shopManager.editingSlotType.remove(player.getUniqueId());
                        player.sendMessage(ChatColor.GRAY + "Cancelado. Abriendo menú...");
                        shopGUI.openConfigGUI(player, villager);
                    }
                    return;
                }
            }
            shopGUI.openConfigGUI(player, villager);
            return;
        }
        shopManager.updateTradesForPlayer(villager, player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().startsWith(ChatColor.GOLD + "Configurar")) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.LIME_STAINED_GLASS_PANE) return;

        int slot = event.getSlot();
        int tradeIndex = -1;
        String type = null;

        int row = slot / 9;
        int col = slot % 9;

        if (col <= 2) {
            tradeIndex = row;
            if (col == 0) type = "Ingrediente 1";
            if (col == 1) type = "Ingrediente 2";
            if (col == 2) type = "Resultado";
        } else if (col >= 5) {
            tradeIndex = row + 6;
            if (col == 5) type = "Ingrediente 1";
            if (col == 6) type = "Ingrediente 2";
            if (col == 7) type = "Resultado";
        }

        if (tradeIndex != -1 && type != null) {
            shopManager.editingTradeIndex.put(player.getUniqueId(), tradeIndex);
            shopManager.editingSlotType.put(player.getUniqueId(), type);

            if (event.isRightClick()) {
                ItemStack empty = new ItemStack(Material.STRUCTURE_VOID);
                ShopCommands.applyItemToTrade(player, shopManager, empty);
                player.sendMessage(ChatColor.RED + "Slot limpiado.");
                shopManager.editingTradeIndex.remove(player.getUniqueId());
                shopManager.editingSlotType.remove(player.getUniqueId());
            } else {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Editando Tradeo #" + (tradeIndex + 1) + " - " + type);
                player.sendMessage(ChatColor.YELLOW + "Opciones:");
                player.sendMessage(ChatColor.GRAY + "1. Usa " + ChatColor.AQUA + "/trade <item> <cantidad>");
                player.sendMessage(ChatColor.GRAY + "2. Agáchate y click derecho al aldeano con el item en la mano.");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        shopManager.editingShops.remove(event.getPlayer().getUniqueId());
        shopManager.editingTradeIndex.remove(event.getPlayer().getUniqueId());
        shopManager.editingSlotType.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager v = (Villager) event.getEntity();
            if (v.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager v = (Villager) event.getEntity();
            if (v.getPersistentDataContainer().has(shopManager.shopIdKey, PersistentDataType.STRING)) {
                String id = v.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
                shopManager.removeShopFromFile(id);
            }
        }
    }

}
