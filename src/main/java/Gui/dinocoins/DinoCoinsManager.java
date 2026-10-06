package Gui.dinocoins;

import Handlers.DatabaseManager;
import items.EconomyItems;
import items.EconomyItemsFunctions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DinoCoinsManager implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final EconomyItemsFunctions functions;
    private final NamespacedKey backpackKey;
    private final Map<UUID, Integer> cachedDinoCoins = new ConcurrentHashMap<>();
    private final Map<UUID, Long> balanceVersions = new ConcurrentHashMap<>();

    public DinoCoinsManager(JavaPlugin plugin, DatabaseManager dbManager, EconomyItemsFunctions functions) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.functions = functions;
        this.backpackKey = new NamespacedKey(plugin, "backpack_uuid");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) updatePlayerTotalAsync(player);
        }, 200L, 200L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) updatePlayerTotalAsync(event.getPlayer());
        }, 120L);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EconomyItemsFunctions.BackpackHolder holder && holder.isWallet()) {
            updatePlayerTotalAsync((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EconomyItemsFunctions.BackpackHolder holder && holder.isWallet()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerTotalAsync((Player) event.getWhoClicked()), 2L);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof EconomyItemsFunctions.BackpackHolder holder && holder.isWallet()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerTotalAsync((Player) event.getWhoClicked()), 2L);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        updatePlayerTotalAsync(event.getPlayer());
    }

    public void updatePlayerTotalAsync(Player player) {
        // Bukkit se lee en el hilo principal; solo las consultas y el registro van al hilo asíncrono.
        UUID playerId = player.getUniqueId();
        Set<String> carriedWallets = getCarriedWallets(player, false);
        Map<String, ItemStack[]> snapshot = functions.snapshotBackpacks();
        long version = balanceVersions.merge(playerId, 1L, Long::sum);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int[] totals = calculateTotals(playerId, carriedWallets, snapshot);
            synchronized (balanceVersions) {
                if (balanceVersions.get(playerId) == version) {
                    cachedDinoCoins.put(playerId, totals[0]);
                    dbManager.setCurrencyBalances(playerId, totals[0], totals[1]);
                }
            }
        });
    }

    public int getCachedDinoCoins(UUID playerId) {
        return cachedDinoCoins.getOrDefault(playerId, 0);
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            balanceVersions.merge(playerId, 1L, Long::sum);
            int[] totals = calculateTotals(playerId, getCarriedWallets(player, false), functions.snapshotBackpacks());
            synchronized (balanceVersions) {
                dbManager.setCurrencyBalances(playerId, totals[0], totals[1]);
            }
        }
    }

    private Set<String> getCarriedWallets(Player player, boolean createIds) {
        Set<String> uuids = new LinkedHashSet<>();
        List<ItemStack> items = new ArrayList<>(Arrays.asList(player.getInventory().getContents()));
        if (!createIds) items.addAll(Arrays.asList(player.getEnderChest().getContents()));
        for (ItemStack item : items) {
            if (!isMonedero(item)) continue;
            String uuid = item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.STRING);
            if (uuid == null && createIds) uuid = functions.ensureWalletId(item);
            if (uuid != null) uuids.add(uuid);
        }
        String openWallet = functions.getOpenWallet(player.getUniqueId());
        if (openWallet != null) uuids.add(openWallet);
        return uuids;
    }

    private int[] calculateTotals(UUID playerId, Set<String> carriedWallets, Map<String, ItemStack[]> snapshot) {
        Set<String> processedUuids = new LinkedHashSet<>(carriedWallets);
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(playerId)) {
            if (info.level == EconomyItemsFunctions.WALLET_LEVEL) {
                processedUuids.add(info.uuid);
            }
        }
        int[] totals = new int[2];
        for (String uuid : processedUuids) {
            ItemStack[] contents = snapshot.get(uuid);
            if (contents == null) contents = dbManager.loadBackpackContents(uuid);
            if (contents == null) continue;
            for (ItemStack item : contents) {
                if (EconomyItemsFunctions.isDinoCoin(item)) totals[0] += item.getAmount();
                if (EconomyItemsFunctions.isDinoFicha(item)) totals[1] += item.getAmount();
            }
        }
        return totals;
    }

    // Viciont: búsqueda universal en inventario, ender chest, monederos registrados y caché.
    public int calculatePhysicalDinoCoins(Player player) {
        return calculateTotals(player.getUniqueId(), getCarriedWallets(player, false), functions.snapshotBackpacks())[0];
    }

    public int calculatePhysicalDinoFichas(Player player) {
        return calculateTotals(player.getUniqueId(), getCarriedWallets(player, false), functions.snapshotBackpacks())[1];
    }

    public boolean addPhysicalDinoCoins(Player player, int amount) {
        if (amount <= 0) return false;
        int remaining = amount;
        Set<String> targetUuids = new LinkedHashSet<>();

        targetUuids.addAll(getCarriedWallets(player, true));
        // Prioridad 2: Monederos guardados en DB (en cofres)
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(player.getUniqueId())) {
            if (info.level == EconomyItemsFunctions.WALLET_LEVEL) {
                targetUuids.add(info.uuid);
            }
        }

        for (String uuid : targetUuids) {
            if (remaining <= 0) break;

            ItemStack[] contents = functions.getBackpackContents(uuid);
            if (contents == null) contents = new ItemStack[EconomyItemsFunctions.WALLET_SIZE];

            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) break;
                ItemStack c = contents[i];
                if (c == null || c.getType() == Material.AIR) {
                    int toAdd = Math.min(remaining, 64);
                    ItemStack vithium = EconomyItems.createVithiumCoin();
                    vithium.setAmount(toAdd);
                    contents[i] = vithium;
                    remaining -= toAdd;
                    changed = true;
                } else if (isDinoCoin(c) && c.getAmount() < 64) {
                    int space = 64 - c.getAmount();
                    int toAdd = Math.min(remaining, space);
                    c.setAmount(c.getAmount() + toAdd);
                    remaining -= toAdd;
                    changed = true;
                }
            }
            if (changed) {
                functions.updateBackpackContents(uuid, contents);
                try { dbManager.saveBackpack(uuid, player.getUniqueId(), player.getName(), "Monedero", EconomyItemsFunctions.WALLET_LEVEL, contents); }
                catch (java.sql.SQLException error) {
                    plugin.getLogger().severe("Error guardando monedero: " + error.getMessage());
                }
            }
        }
        updatePlayerTotalAsync(player);
        return remaining == 0;
    }

    public boolean removePhysicalDinoCoins(Player player, int amount) {
        if (amount <= 0) return false;
        int remaining = amount;
        Set<String> targetUuids = new LinkedHashSet<>();

        targetUuids.addAll(getCarriedWallets(player, false));
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(player.getUniqueId())) {
            if (info.level == EconomyItemsFunctions.WALLET_LEVEL) {
                targetUuids.add(info.uuid);
            }
        }

        for (String uuid : targetUuids) {
            if (remaining <= 0) break;

            ItemStack[] contents = functions.getBackpackContents(uuid);
            if (contents == null) continue;

            boolean changed = false;
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) break;
                ItemStack c = contents[i];
                if (isDinoCoin(c)) {
                    if (c.getAmount() <= remaining) {
                        remaining -= c.getAmount();
                        contents[i] = null;
                        changed = true;
                    } else {
                        c.setAmount(c.getAmount() - remaining);
                        remaining = 0;
                        changed = true;
                    }
                }
            }
            if (changed) {
                functions.updateBackpackContents(uuid, contents);
                try { dbManager.saveBackpack(uuid, player.getUniqueId(), player.getName(), "Monedero", EconomyItemsFunctions.WALLET_LEVEL, contents); }
                catch (java.sql.SQLException error) {
                    plugin.getLogger().severe("Error guardando monedero: " + error.getMessage());
                }
            }
        }
        updatePlayerTotalAsync(player);
        return remaining == 0;
    }

    private boolean isMonedero(ItemStack item) {
        return functions.isMonedero(item);
    }

    private boolean isDinoCoin(ItemStack item) {
        return EconomyItemsFunctions.isDinoCoin(item);
    }
}
