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
        String playerName = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            dbManager.claimWallets(playerId, playerName, carriedWallets, EconomyItemsFunctions.WALLET_LEVEL);
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
        return change(player, amount, loadWallets(player, true)) == 0;
    }

    public boolean removePhysicalDinoCoins(Player player, int amount) {
        if (amount <= 0) return false;
        return change(player, -amount, loadWallets(player, false)) == 0;
    }

    // /dinocoins add y remove: la base de datos se lee y se escribe fuera del hilo del server. done recibe true si se
    // movió toda la cantidad
    public void changeAsync(Player player, int delta, java.util.function.Consumer<Boolean> done) {
        changeAsyncMoved(player, delta, moved -> done.accept(moved == Math.abs(delta)));
    }

    // Como changeAsync, pero avisa cuántas DinoCoins se movieron de verdad (para devolverlas si falta algo)
    public void changeAsyncMoved(Player player, int delta, java.util.function.IntConsumer done) {
        if (delta == 0) {
            done.accept(0);
            return;
        }
        Set<String> carried = getCarriedWallets(player, delta > 0);
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, ItemStack[]> stored = loadStored(playerId, carried, true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    done.accept(0);
                    return;
                }
                done.accept(Math.abs(delta) - change(player, delta, merge(carried, stored)));
            });
        });
    }

    // Paga directo a los monederos; lo que no entra (o si no tiene monedero) va al inventario o al suelo
    public void deposit(Player player, int amount) {
        if (amount <= 0) return;
        changeAsyncMoved(player, amount, moved -> {
            if (moved < amount && player.isOnline()) giveLoose(player, amount - moved);
        });
    }

    public static void giveLoose(Player player, int amount) {
        while (amount > 0) {
            ItemStack coins = EconomyItems.createVithiumCoin();
            coins.setAmount(Math.min(64, amount));
            amount -= coins.getAmount();
            for (ItemStack left : player.getInventory().addItem(coins).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }

    // /dinocoins get sin consultar MySQL en el hilo del server
    public void totalsAsync(Player player, java.util.function.Consumer<int[]> done) {
        UUID playerId = player.getUniqueId();
        Set<String> carried = getCarriedWallets(player, false);
        Map<String, ItemStack[]> snapshot = functions.snapshotBackpacks();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int[] totals = calculateTotals(playerId, carried, snapshot);
            Bukkit.getScheduler().runTask(plugin, () -> done.accept(totals));
        });
    }

    // Los monederos del jugador, primero los que lleva y después los guardados en cofres. El valor es lo que hay
    // adentro (null = nunca se guardó, está vacío). Uno que no se pudo leer de la base de datos no aparece: así no se
    // guarda vacío por un error
    private Map<String, ItemStack[]> loadWallets(Player player, boolean createIds) {
        Set<String> carried = getCarriedWallets(player, createIds);
        return merge(carried, loadStored(player.getUniqueId(), carried, false));
    }

    // skipLoaded: desde otro hilo no se mira la memoria (los inventarios abiertos son del hilo del server)
    private Map<String, ItemStack[]> loadStored(UUID playerId, Set<String> carried, boolean skipLoaded) {
        Set<String> uuids = new LinkedHashSet<>(carried);
        for (DatabaseManager.BackpackInfo info : dbManager.getPlayerBackpacks(playerId)) {
            if (info.level == EconomyItemsFunctions.WALLET_LEVEL) uuids.add(info.uuid);
        }
        Map<String, ItemStack[]> stored = new LinkedHashMap<>();
        for (String uuid : uuids) {
            ItemStack[] loaded = skipLoaded ? null : functions.getLoadedBackpackContents(uuid);
            if (loaded != null) {
                stored.put(uuid, loaded);
                continue;
            }
            try {
                stored.put(uuid, dbManager.loadBackpackContentsStrict(uuid));
            } catch (java.sql.SQLException error) {
                plugin.getLogger().severe("No se pudo leer el monedero " + uuid + ", no se toca: " + error.getMessage());
            }
        }
        return stored;
    }

    // Lo que está en memoria (abierto o en caché) siempre gana sobre lo leído antes de la base de datos
    private Map<String, ItemStack[]> merge(Set<String> carried, Map<String, ItemStack[]> stored) {
        Map<String, ItemStack[]> wallets = new LinkedHashMap<>();
        for (Map.Entry<String, ItemStack[]> entry : stored.entrySet()) {
            ItemStack[] loaded = functions.getLoadedBackpackContents(entry.getKey());
            wallets.put(entry.getKey(), loaded != null ? loaded : entry.getValue());
        }
        return wallets;
    }

    // delta > 0 agrega DinoCoins (de a 64 por slot) y delta < 0 las saca; nunca toca las DinoFichas. Devuelve lo
    // que no se pudo mover
    private int change(Player player, int delta, Map<String, ItemStack[]> wallets) {
        int remaining = Math.abs(delta);
        Map<String, ItemStack[]> changed = new LinkedHashMap<>();
        for (Map.Entry<String, ItemStack[]> entry : wallets.entrySet()) {
            if (remaining <= 0) break;
            ItemStack[] contents = entry.getValue();
            if (contents == null) {
                if (delta < 0) continue;
                contents = new ItemStack[EconomyItemsFunctions.WALLET_SIZE];
            }
            int before = remaining;
            remaining = delta > 0 ? fill(contents, remaining) : take(contents, remaining);
            if (remaining != before) changed.put(entry.getKey(), contents);
        }
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        for (Map.Entry<String, ItemStack[]> entry : changed.entrySet()) functions.updateBackpackContents(entry.getKey(), entry.getValue());
        Runnable save = () -> {
            for (Map.Entry<String, ItemStack[]> entry : changed.entrySet()) {
                try {
                    dbManager.saveBackpack(entry.getKey(), playerId, playerName, "Monedero", EconomyItemsFunctions.WALLET_LEVEL, entry.getValue());
                } catch (java.sql.SQLException error) {
                    plugin.getLogger().severe("Error guardando monedero: " + error.getMessage());
                }
            }
        };
        if (!changed.isEmpty()) {
            if (Bukkit.isPrimaryThread()) Bukkit.getScheduler().runTaskAsynchronously(plugin, save);
            else save.run();
        }
        updatePlayerTotalAsync(player);
        return remaining;
    }

    private int fill(ItemStack[] contents, int remaining) {
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack c = contents[i];
            if (c == null || c.getType() == Material.AIR) {
                int toAdd = Math.min(remaining, 64);
                ItemStack coin = EconomyItems.createVithiumCoin();
                coin.setAmount(toAdd);
                contents[i] = coin;
                remaining -= toAdd;
            } else if (isDinoCoin(c) && c.getAmount() < 64) {
                int toAdd = Math.min(remaining, 64 - c.getAmount());
                c.setAmount(c.getAmount() + toAdd);
                remaining -= toAdd;
            }
        }
        return remaining;
    }

    private int take(ItemStack[] contents, int remaining) {
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack c = contents[i];
            if (!isDinoCoin(c)) continue;
            if (c.getAmount() <= remaining) {
                remaining -= c.getAmount();
                contents[i] = null;
            } else {
                c.setAmount(c.getAmount() - remaining);
                remaining = 0;
            }
        }
        return remaining;
    }

    private boolean isMonedero(ItemStack item) {
        return functions.isMonedero(item);
    }

    private boolean isDinoCoin(ItemStack item) {
        return EconomyItemsFunctions.isDinoCoin(item);
    }
}
