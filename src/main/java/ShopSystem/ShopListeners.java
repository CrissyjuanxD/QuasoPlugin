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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ShopListeners implements Listener {

    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopListeners(ShopManager shopManager, ShopGUI shopGUI) {
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) return;
        Villager villager = (Villager) event.getRightClicked();
        if (!villager.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) return;

        Player player = event.getPlayer();

        // Actualizamos como se hacía nativamente
        shopManager.updateTradesForPlayer(villager, player);

        if (player.isOp() && player.isSneaking()) {
            event.setCancelled(true);

            String clickedId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
            boolean hasPendingEdit = shopManager.editingTradeIndex.containsKey(player.getUniqueId())
                    && shopManager.editingSlotType.containsKey(player.getUniqueId());
            String currentShopId = shopManager.editingShops.get(player.getUniqueId());
            boolean isSameShop = clickedId != null && clickedId.equals(currentShopId);

            if (hasPendingEdit && isSameShop) {
                ItemStack handItem = player.getInventory().getItemInMainHand();

                if (handItem != null && handItem.getType() != Material.AIR) {
                    // Usamos la lógica de actualización del código viejo directamente
                    int tradeIdx = shopManager.editingTradeIndex.get(player.getUniqueId());
                    String slotType = shopManager.editingSlotType.get(player.getUniqueId());

                    shopManager.updateVillagerTrade(villager, tradeIdx, slotType, handItem.clone());
                    shopManager.saveShopTrades(currentShopId, villager.getRecipes());

                    player.sendMessage(ChatColor.GREEN + "Tradeo actualizado: " + slotType + " -> " + handItem.getType());
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    player.sendMessage(ChatColor.GRAY + "Cancelado. Abriendo menú...");
                }

                shopManager.editingTradeIndex.remove(player.getUniqueId());
                shopManager.editingSlotType.remove(player.getUniqueId());
                shopGUI.openConfigGUI(player, villager);
                return;
            }

            shopGUI.openConfigGUI(player, villager);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle() == null) return;
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
                ItemStack empty = new ItemStack(Material.AIR);

                String shopId = shopManager.editingShops.get(player.getUniqueId());
                Villager villager = shopManager.getVillagerById(shopId);

                if (villager != null) {
                    shopManager.updateVillagerTrade(villager, tradeIndex, type, empty);
                    shopManager.saveShopTrades(shopId, villager.getRecipes());
                }

                player.sendMessage(ChatColor.RED + "Slot limpiado.");
                shopManager.editingTradeIndex.remove(player.getUniqueId());
                shopManager.editingSlotType.remove(player.getUniqueId());

                // Re-abrir para refrescar la GUI
                if (villager != null) shopGUI.openConfigGUI(player, villager);
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