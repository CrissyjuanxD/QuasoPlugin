package items;

import Handlers.DatabaseManager;
import Casino.CasinoInventoryHolder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyItemsFunctions implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final NamespacedKey backpackKey;

    private final Map<UUID, String> mochilasAbiertas = new ConcurrentHashMap<>();
    private final Map<String, ItemStack[]> mochilasCache = new ConcurrentHashMap<>();
    private final Map<String, Inventory> openBackpacks = new HashMap<>();
    public static final int WALLET_LEVEL = 6;
    public static final int WALLET_SIZE = 18;

    public static final class BackpackHolder implements InventoryHolder {
        public final String uuid;
        public final String itemName;
        public final int level;
        private Inventory inventory;

        public BackpackHolder(String uuid, String itemName, int level) {
            this.uuid = uuid;
            this.itemName = itemName;
            this.level = level;
        }

        public boolean isWallet() { return level == WALLET_LEVEL; }
        @Override
        public Inventory getInventory() { return inventory; }
    }

    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cooldownGancho = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> pendingDeletion = new ConcurrentHashMap<>();

    public EconomyItemsFunctions(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.backpackKey = new NamespacedKey(plugin, "backpack_uuid");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        if (item.getType() == Material.SUNFLOWER && item.hasItemMeta() &&
                item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 2000) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack leftItem = event.getInventory().getItem(0);
        if (leftItem == null || leftItem.getType() == Material.AIR) return;

        if (isMochila(leftItem)) {
            String renameText = event.getView().getRenameText();
            ItemStack result = event.getResult();

            if (result != null && renameText != null && !renameText.isEmpty()) {
                ItemMeta meta = result.getItemMeta();
                ChatColor color = getMochilaColor(leftItem);

                String cleanName = ChatColor.stripColor(renameText);
                meta.setDisplayName(color + "" + ChatColor.BOLD + cleanName);

                result.setItemMeta(meta);
                event.setResult(result);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        Player player = event.getPlayer();

        if (isMochila(item)) {
            event.setCancelled(true);

            if (event.getHand() == EquipmentSlot.OFF_HAND) return;

            if (event.getAction().toString().contains("RIGHT")) {

                if (player.getOpenInventory().getType() != InventoryType.CRAFTING &&
                        player.getOpenInventory().getType() != InventoryType.CREATIVE) {
                    return;
                }

                if (processing.contains(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "⌚ Procesando... espera un segundo.");
                    return;
                }

                player.setCooldown(item.getType(), 20);

                player.updateInventory();

                abrirMochila(player, item);
            }
            return;
        }

        if (!event.getAction().toString().contains("RIGHT")) return;

        if (processing.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (isEnderBag(item)) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
            player.openInventory(player.getEnderChest());
            return;
        }

        if (isGancho(item)) {
            event.setCancelled(true);
            usarGancho(player, item);
            return;
        }

        if (checkAndUseYunque(player, item)) {
            event.setCancelled(true);
            return;
        }

        if (isManzanaPanico(item)) {
            event.setCancelled(true);
            usarManzanaPanico(player, item);
            return;
        }
    }

    private void abrirMochila(Player player, ItemStack mochila) {
        processing.add(player.getUniqueId());

        ItemMeta meta = mochila.getItemMeta();
        int modelData = meta.getCustomModelData();
        int size = getBackpackSize(modelData);

        String mochilaId = getMochilaId(mochila);
        if (mochilaId == null) {
            mochilaId = UUID.randomUUID().toString();
            setMochilaId(mochila, mochilaId);
            if (player.getInventory().getItemInMainHand().getType() == mochila.getType()) {
                player.getInventory().setItemInMainHand(mochila);
            } else {
                player.getInventory().setItemInOffHand(mochila);
            }
        }

        final String finalId = mochilaId;
        final boolean wallet = modelData == 2025;
        final String guiTitle = getMochilaColor(mochila) + "" + ChatColor.BOLD + (wallet ? "Monedero" : "Mochila");
        BackpackHolder holder = new BackpackHolder(finalId,
                meta.getDisplayName(), getLevelByModel(modelData));
        Inventory mochilaInv = Bukkit.createInventory(holder, size, guiTitle);
        holder.inventory = mochilaInv;

        if (mochilasCache.containsKey(finalId)) {
            ItemStack[] contents = mochilasCache.get(finalId);
            cargarContenidoSeguro(mochilaInv, contents);
            openInventorySafely(player, mochilaInv, finalId);
        } else {
            player.sendMessage(ChatColor.GRAY + "⚙ Cargando mochila...");
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack[] contents = dbManager.loadBackpackContents(finalId);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline()) {
                                processing.remove(player.getUniqueId());
                                return;
                            }
                            if (contents != null) {
                                mochilasCache.put(finalId, contents);
                                cargarContenidoSeguro(mochilaInv, contents);
                            }
                            openInventorySafely(player, mochilaInv, finalId);
                        }
                    }.runTask(plugin);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private void openInventorySafely(Player player, Inventory inv, String mochilaId) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || openBackpacks.containsKey(mochilaId)) {
                    processing.remove(player.getUniqueId());
                    player.sendMessage(ChatColor.RED + "⚠ Esta mochila o monedero ya está abierto.");
                    return;
                }
                player.openInventory(inv);
                mochilasAbiertas.put(player.getUniqueId(), mochilaId);
                openBackpacks.put(mochilaId, inv);
                player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.0f);

                processing.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 2L);
    }

    private void cargarContenidoSeguro(Inventory inv, ItemStack[] contents) {
        if (contents == null) return;
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) contents[i] = new ItemStack(Material.AIR);
        }
        if (contents.length > inv.getSize()) {
            ItemStack[] resized = new ItemStack[inv.getSize()];
            System.arraycopy(contents, 0, resized, 0, inv.getSize());
            inv.setContents(resized);
        } else {
            inv.setContents(contents);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.getInventory().getHolder() instanceof BackpackHolder holder
                && Objects.equals(mochilasAbiertas.get(playerId), holder.uuid)) {
            String mochilaId = mochilasAbiertas.remove(playerId);
            Inventory inv = event.getInventory();
            openBackpacks.remove(mochilaId);
            ItemStack[] contents = inv.getContents();

            processing.add(playerId);

            mochilasCache.put(mochilaId, contents);

            final String fItemName = holder.itemName;
            final int fLevel = holder.level;
            final String playerName = player.getName();

            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean success = false;
                    try {
                        dbManager.saveBackpack(mochilaId, playerId, playerName, fItemName, fLevel, contents);
                        success = true;
                    } catch (SQLException e) {
                        plugin.getLogger().severe("ERROR GUARDANDO MOCHILA: " + e.getMessage());
                    } finally {
                        boolean finalSuccess = success;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                processing.remove(playerId);
                                if (!finalSuccess && player.isOnline()) {
                                    player.sendMessage(ChatColor.RED + "⚠ Error guardando. Items en memoria.");
                                }
                            }
                        }.runTask(plugin);
                    }
                }
            }.runTaskAsynchronously(plugin);

            player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if (title.startsWith(ChatColor.DARK_RED + "Mochilas de: ") ||
                title.startsWith(ChatColor.RED + "BORRAR Mochilas de: ")) {

            if (title.startsWith(ChatColor.DARK_RED + "Mochilas de: ")) {
                handleAdminGuiClick(event, player, event.getCurrentItem());
            } else {
                handleDeleteGuiClick(event, player, event.getCurrentItem());
            }
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            if (isMochila(cursor)) {
                event.setCancelled(true);
                return;
            }
        }

        Inventory top = event.getView().getTopInventory();
        boolean topClick = event.getClickedInventory() == top;
        BackpackHolder holder = top.getHolder() instanceof BackpackHolder h ? h : null;
        ItemStack incoming = null;
        if (topClick) {
            switch (event.getAction()) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> incoming = cursor;
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> incoming = event.getClick() == ClickType.SWAP_OFFHAND
                        ? player.getInventory().getItemInOffHand()
                        : event.getHotbarButton() >= 0 ? player.getInventory().getItem(event.getHotbarButton()) : null;
                default -> { }
            }
        } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            incoming = current;
        }

        if (holder != null && (isMochila(incoming) || isMochila(current))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "⚠ No puedes meter una mochila o monedero dentro de otra.");
            return;
        }
        if (!isEmpty(incoming) && !canStore(top, event.getRawSlot(), incoming)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + (holder != null && holder.isWallet()
                    ? "⚠ El Monedero solo puede guardar DinoCoins y DinoFichas."
                    : "⚠ Las DinoCoins y DinoFichas solo se pueden guardar en un Monedero."));
        }
        // Las bundles vanilla no deben servir como otro monedero.
        if (event.getClick() == ClickType.RIGHT && ((isCurrency(cursor) && isBundle(current))
                || (isCurrency(current) && isBundle(cursor)))) {
            event.setCancelled(true);
        }
    }

    private boolean isEmpty(ItemStack item) { return item == null || item.getType().isAir(); }
    private boolean isBundle(ItemStack item) { return item != null && item.getItemMeta() instanceof BundleMeta; }

    private boolean canStore(Inventory top, int slot, ItemStack item) {
        if (top.getHolder() instanceof BackpackHolder holder) {
            return holder.isWallet() ? isCurrency(item) : !isCurrency(item) && !isMochila(item);
        }
        if (!isCurrency(item)) return true;
        if (top.getHolder() instanceof CasinoInventoryHolder casino) {
            return slot == casino.getTokenSlot() && isDinoFicha(item);
        }
        return top.getType() == InventoryType.CRAFTING || top.getType() == InventoryType.CREATIVE
                || top.getType() == InventoryType.PLAYER;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && (isMochila(event.getOldCursor())
                    && top.getHolder() instanceof BackpackHolder || !canStore(top, slot, event.getOldCursor()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isCurrency(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (isCurrency(event.getItem().getItemStack())) event.setCancelled(true);
    }

    private void handleAdminGuiClick(InventoryClickEvent event, Player player, ItemStack current) {
        event.setCancelled(true);
        if (current == null || !current.hasItemMeta()) return;

        String idRaw = getMochilaIdFromLore(current);
        if (idRaw == null) return;

        if (event.isLeftClick()) {
            ItemStack copy = getBackpackItemByModel(current.getItemMeta().getCustomModelData());
            setMochilaId(copy, idRaw);
            ItemMeta meta = copy.getItemMeta();
            meta.setDisplayName(current.getItemMeta().getDisplayName());
            copy.setItemMeta(meta);

            player.getInventory().addItem(copy);
            player.sendMessage(ChatColor.GREEN + "✔ Copia recuperada exitosamente.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            player.closeInventory();

        } else if (event.isRightClick()) {
            player.closeInventory();
            ItemStack temp = getBackpackItemByModel(current.getItemMeta().getCustomModelData());
            ItemMeta tempMeta = temp.getItemMeta();
            tempMeta.setDisplayName(current.getItemMeta().getDisplayName());
            temp.setItemMeta(tempMeta);
            setMochilaId(temp, idRaw);

            player.sendMessage(ChatColor.YELLOW + "🕵 Espiando contenido de la mochila...");
            abrirMochila(player, temp);
        }
    }

    private void handleDeleteGuiClick(InventoryClickEvent event, Player player, ItemStack current) {
        event.setCancelled(true);
        if (current == null || !current.hasItemMeta()) return;

        String idRaw = getMochilaIdFromLore(current);
        if (idRaw == null) return;

        if (event.isLeftClick()) {
            player.closeInventory();
            pendingDeletion.put(player.getUniqueId(), idRaw);

            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ CONFIRMACIÓN DE BORRADO ⚠");
            player.sendMessage(ChatColor.RED + "Estás a punto de eliminar la mochila ID: " + ChatColor.YELLOW + idRaw);
            player.sendMessage(ChatColor.GRAY + "Escribe " + ChatColor.GREEN + "si" + ChatColor.GRAY + " en el chat para confirmar.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 0.5f);
        }
    }

    @EventHandler
    public void onChatConfirm(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (pendingDeletion.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String msg = event.getMessage().toLowerCase();
            String idToDelete = pendingDeletion.remove(player.getUniqueId());

            if (msg.equals("si") || msg.equals("yes")) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        boolean deleted = dbManager.deleteBackpack(idToDelete);
                        mochilasCache.remove(idToDelete);

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (deleted) {
                                    player.sendMessage(ChatColor.GREEN + "✔ Mochila eliminada correctamente.");
                                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
                                } else {
                                    player.sendMessage(ChatColor.RED + "❌ No se pudo borrar (¿Ya no existe?).");
                                }
                            }
                        }.runTask(plugin);
                    }
                }.runTaskAsynchronously(plugin);
            } else {
                player.sendMessage(ChatColor.YELLOW + "✖ Operación cancelada.");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (mochilasAbiertas.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }

        processing.remove(player.getUniqueId());
    }

    public boolean isMochila(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!EconomyItems.isMaterialMochila(item.getType())) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        int cmd = meta.getCustomModelData();
        return cmd >= 2020 && cmd <= 2027;
    }

    private int getBackpackSize(int modelData) {
        switch (modelData) {
            case 2020: return 18;
            case 2021: return 27;
            case 2022: return 36;
            case 2025: return WALLET_SIZE;
            case 2023: return 45;
            case 2024: return 54;
            default: return 27;
        }
    }

    private boolean isEnderBag(ItemStack item) { return checkModelData(item, 2030); }
    private boolean isGancho(ItemStack item) { return checkModelData(item, 10) && item.getType() == Material.FISHING_ROD; }
    private boolean isManzanaPanico(ItemStack item) { return checkModelData(item, 10) && item.getType() == Material.APPLE; }

    private static boolean checkModelData(ItemStack item, int id) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == id;
    }

    private boolean checkAndUseYunque(Player player, ItemStack item) {
        if (!item.getType().toString().contains("SPAWN_EGG")) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return false;
        int cmd = meta.getCustomModelData();
        if (cmd == 2040) { usarYunque(player, item, 0.25); return true; }
        else if (cmd == 2050) { usarYunque(player, item, 1.0); return true; }
        return false;
    }

    private void usarGancho(Player player, ItemStack gancho) {
        if (cooldownGancho.contains(player.getUniqueId())) return;
        cooldownGancho.add(player.getUniqueId());
        player.setCooldown(Material.FISHING_ROD, 40);
        new BukkitRunnable() { @Override public void run() { cooldownGancho.remove(player.getUniqueId()); } }.runTaskLater(plugin, 40);

        if (gancho.getItemMeta() instanceof Damageable dmg && dmg.getDamage() < gancho.getType().getMaxDurability()) {
            dmg.setDamage(dmg.getDamage() + 1);
            gancho.setItemMeta(dmg);
        } else {
            player.getInventory().removeItem(gancho);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.6);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.5f);
    }

    private void usarYunque(Player player, ItemStack yunque, double porcentaje) {
        repararArmadura(player, porcentaje);
        yunque.setAmount(yunque.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
    }

    private void usarManzanaPanico(Player player, ItemStack manzana) {
        EconomyItems.applyPanicAppleEffects(player);
        manzana.setAmount(manzana.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);
    }

    private void repararArmadura(Player player, double porcentaje) {
        ItemStack[] armadura = player.getInventory().getArmorContents();
        for (ItemStack item : armadura) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!(item.getItemMeta() instanceof Damageable dmg) || dmg.getDamage() <= 0) continue;

            int repairAmount = (int) (item.getType().getMaxDurability() * porcentaje);
            dmg.setDamage(Math.max(0, dmg.getDamage() - repairAmount));
            item.setItemMeta(dmg);
        }
        player.getInventory().setArmorContents(armadura);
    }

    private String getMochilaId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.get(backpackKey, PersistentDataType.STRING);
    }

    private void setMochilaId(ItemStack item, String uuid) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(backpackKey, PersistentDataType.STRING, uuid);
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.removeIf(l -> l.contains("ID:"));
        lore.add(ChatColor.DARK_GRAY + "ID: " + uuid);
        meta.setLore(lore);
        if (meta instanceof BundleMeta) {
            ((BundleMeta) meta).setItems(new ArrayList<>());
        }
        item.setItemMeta(meta);
    }

    private String getMochilaIdFromLore(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains("ID:")) return ChatColor.stripColor(line).replace("ID:", "").trim();
        }
        return null;
    }

    public ItemStack getBackpackItemByModel(int model) {
        if (model == 2020) return EconomyItems.createNormalMochila();
        if (model == 2021) return EconomyItems.createGreenMochila();
        if (model == 2022) return EconomyItems.createRedMochila();
        if (model == 2023) return EconomyItems.createBlueMochila();
        if (model == 2024) return EconomyItems.createPurpleMochila();
        if (model == 2025) return EconomyItems.createMonedero();
        return EconomyItems.createNormalMochila();
    }

    public ItemStack getBackpackItemByLevel(int level) {
        switch(level) {
            case 1: return getBackpackItemByModel(2020);
            case 2: return getBackpackItemByModel(2021);
            case 3: return getBackpackItemByModel(2022);
            case 4: return getBackpackItemByModel(2023);
            case 5: return getBackpackItemByModel(2024);
            case WALLET_LEVEL: return getBackpackItemByModel(2025);
            default: return getBackpackItemByModel(2020);
        }
    }

    private int getLevelByModel(int model) {
        if (model == 2020) return 1;
        if (model == 2021) return 2;
        if (model == 2022) return 3;
        if (model == 2023) return 4;
        if (model == 2024) return 5;
        if (model == 2025) return WALLET_LEVEL;
        return 1;
    }

    public ChatColor getMochilaColor(ItemStack mochila) {
        int cmd = mochila.getItemMeta().getCustomModelData();
        switch (cmd) {
            case 2021: return ChatColor.BLUE;
            case 2022: return ChatColor.GOLD;
            case 2023: return ChatColor.RED;
            case 2024: return ChatColor.DARK_PURPLE;
            case 2025: return ChatColor.of("#AC68E3");
            default: return ChatColor.GREEN;
        }
    }

    public String getOpenWallet(UUID playerId) {
        Inventory inv = openBackpacks.get(mochilasAbiertas.get(playerId));
        return inv != null && inv.getHolder() instanceof BackpackHolder holder && holder.isWallet() ? holder.uuid : null;
    }

    public String ensureWalletId(ItemStack item) {
        String uuid = getMochilaId(item);
        if (uuid == null && isMonedero(item)) {
            uuid = UUID.randomUUID().toString();
            setMochilaId(item, uuid);
        }
        return uuid;
    }

    public boolean isMonedero(ItemStack item) { return isMochila(item) && checkModelData(item, 2025); }
    public static boolean isDinoCoin(ItemStack item) { return item != null && item.getType() == Material.SUNFLOWER && checkModelData(item, 2000); }
    public static boolean isDinoFicha(ItemStack item) { return item != null && item.getType() == Material.GOLD_NUGGET && checkModelData(item, 2010); }
    public static boolean isCurrency(ItemStack item) { return isDinoCoin(item) || isDinoFicha(item); }

    // Lectura en el hilo principal: los contenidos abiertos tienen prioridad sobre caché y BD.
    public Map<String, ItemStack[]> snapshotBackpacks() {
        Map<String, ItemStack[]> snapshot = new HashMap<>();
        mochilasCache.forEach((id, contents) -> snapshot.put(id, copyContents(contents)));
        openBackpacks.forEach((id, inv) -> snapshot.put(id, copyContents(inv.getContents())));
        return snapshot;
    }

    public ItemStack[] getBackpackContents(String uuid) {
        Inventory open = openBackpacks.get(uuid);
        ItemStack[] contents = open != null ? open.getContents() : mochilasCache.get(uuid);
        if (contents == null) contents = dbManager.loadBackpackContents(uuid);
        return contents == null ? null : copyContents(contents);
    }

    public void updateBackpackContents(String uuid, ItemStack[] contents) {
        mochilasCache.put(uuid, copyContents(contents));
        Inventory open = openBackpacks.get(uuid);
        if (open != null) open.setContents(copyContents(contents));
    }

    private ItemStack[] copyContents(ItemStack[] contents) {
        return Arrays.stream(contents).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }

    public void shutdown() {
        for (UUID viewer : new ArrayList<>(mochilasAbiertas.keySet())) {
            Player player = Bukkit.getPlayer(viewer);
            Inventory inv = openBackpacks.get(mochilasAbiertas.remove(viewer));
            if (player != null && inv != null && inv.getHolder() instanceof BackpackHolder holder) {
                ItemStack[] contents = inv.getContents();
                mochilasCache.put(holder.uuid, contents);
                try {
                    dbManager.saveBackpack(holder.uuid, viewer, player.getName(), holder.itemName, holder.level, contents);
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error guardando mochila al apagar: " + e.getMessage());
                }
            }
        }
        openBackpacks.clear();
    }

    public JavaPlugin getPlugin() { return plugin; }
    public DatabaseManager getDbManager() { return dbManager; }
}