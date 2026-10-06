package items;

import Casino.CasinoInventoryHolder;
import Gui.dinocoins.DinoCoinsManager;
import Handlers.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockedConstruction;
import org.mockito.ArgumentCaptor;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WalletEconomyTest {
    private static RegistryAccess testRegistryAccess;
    @BeforeAll
    static void initializePaperInventoryTypes() {
        testRegistryAccess = support.PaperTestRegistry.initialize();
    }

    private JavaPlugin plugin;
    private DatabaseManager database;
    private EconomyItemsFunctions functions;
    private Player player;
    private PlayerInventory playerInventory;
    private InventoryView view;
    private BukkitScheduler scheduler;
    private UUID playerId;

    @BeforeEach
    void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("QuasoPlugin");
        when(plugin.namespace()).thenReturn("quasoplugin");
        database = mock(DatabaseManager.class);
        functions = new EconomyItemsFunctions(plugin, database);
        player = mock(Player.class);
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Crosszy");
        when(player.isOnline()).thenReturn(true);
        playerInventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getContents()).thenReturn(new ItemStack[36]);
        Inventory ender = inventory(null, 27);
        when(ender.getContents()).thenReturn(new ItemStack[27]);
        when(player.getEnderChest()).thenReturn(ender);
        view = mock(InventoryView.class);
        when(view.getTitle()).thenReturn("Inventario");
        when(view.getType()).thenReturn(InventoryType.CRAFTING);
        when(player.getOpenInventory()).thenReturn(view);
        scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> run(call.getArgument(1)));
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong())).thenAnswer(call -> run(call.getArgument(1)));
        when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> run(call.getArgument(1)));
    }

    private BukkitTask run(Runnable runnable) { runnable.run(); return mock(BukkitTask.class); }

    private ItemStack item(Material type, int model, int amount) {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(item.getType()).thenReturn(type);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.hasCustomModelData()).thenReturn(true);
        when(meta.getCustomModelData()).thenReturn(model);
        AtomicInteger count = new AtomicInteger(amount);
        when(item.getAmount()).thenAnswer(call -> count.get());
        doAnswer(call -> { count.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.clone()).thenAnswer(call -> item(type, model, count.get()));
        return item;
    }

    private ItemStack wallet(String uuid) {
        ItemStack wallet = item(Material.ECHO_SHARD, 2025, 1);
        ItemMeta meta = wallet.getItemMeta();
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(meta.getDisplayName()).thenReturn("Mi dinero");
        when(data.get(new NamespacedKey(plugin, "backpack_uuid"), PersistentDataType.STRING)).thenReturn(uuid);
        return wallet;
    }

    private Inventory inventory(InventoryHolder holder, int size) {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(holder);
        when(inventory.getSize()).thenReturn(size);
        when(inventory.getType()).thenReturn(InventoryType.CHEST);
        return inventory;
    }

    private InventoryClickEvent click(Inventory top, ItemStack incoming, InventoryAction action, boolean topClick, int slot) {
        when(view.getTopInventory()).thenReturn(top);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getView()).thenReturn(view);
        when(event.getClickedInventory()).thenReturn(topClick ? top : playerInventory);
        when(event.getRawSlot()).thenReturn(slot);
        when(event.getAction()).thenReturn(action);
        when(event.getClick()).thenReturn(ClickType.LEFT);
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) when(event.getCurrentItem()).thenReturn(incoming);
        else if (action == InventoryAction.HOTBAR_SWAP) {
            when(event.getClick()).thenReturn(ClickType.NUMBER_KEY);
            when(event.getHotbarButton()).thenReturn(2);
            when(playerInventory.getItem(2)).thenReturn(incoming);
        } else when(event.getCursor()).thenReturn(incoming);
        return event;
    }

    @Test
    void currenciesExplainWalletStorageAndWalletHasItsOwnItemModel() {
        QuasoPlugin currentPlugin = mock(QuasoPlugin.class);
        when(currentPlugin.getConfig()).thenReturn(new YamlConfiguration());
        try (MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class);
             MockedStatic<QuasoPlugin> current = mockStatic(QuasoPlugin.class);
             MockedConstruction<ItemStack> stacks = mockConstruction(ItemStack.class, (stack, context) -> {
                 ItemMeta meta = mock(ItemMeta.class);
                 PersistentDataContainer data = mock(PersistentDataContainer.class);
                 when(meta.getPersistentDataContainer()).thenReturn(data);
                 when(stack.getItemMeta()).thenReturn(meta);
             })) {
            registry.when(RegistryAccess::registryAccess).thenReturn(testRegistryAccess);
            current.when(QuasoPlugin::getInstance).thenReturn(currentPlugin);
            ItemStack coin = EconomyItems.createVithiumCoin();
            ItemStack token = EconomyItems.createVithiumToken();
            ItemStack wallet = EconomyItems.createMonedero();
            for (ItemStack currency : List.of(coin, token)) {
                ArgumentCaptor<List<String>> lore = ArgumentCaptor.forClass(List.class);
                verify(currency.getItemMeta()).setLore(lore.capture());
                assertTrue(lore.getValue().stream().anyMatch(line -> line.contains("Solo se puede almacenar en un monedero.")));
            }
            verify(wallet.getItemMeta()).setItemModel(NamespacedKey.minecraft("monedero"));
            assertEquals(3, stacks.constructed().size());
        }
    }

    @Test
    void onlyCurrenciesEnterWalletThroughClicksShiftAndHotbar() {
        Inventory wallet = inventory(new EconomyItemsFunctions.BackpackHolder("wallet", "Renombrado", 6), 18);
        Inventory backpack = inventory(new EconomyItemsFunctions.BackpackHolder("bag", "Mochila", 1), 18);
        Inventory chest = inventory(null, 27);
        for (ItemStack currency : List.of(item(Material.SUNFLOWER, 2000, 32), item(Material.GOLD_NUGGET, 2010, 12))) {
            for (InventoryAction action : List.of(InventoryAction.PLACE_ALL, InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.HOTBAR_SWAP)) {
                boolean topClick = action != InventoryAction.MOVE_TO_OTHER_INVENTORY;
                InventoryClickEvent allowed = click(wallet, currency, action, topClick, topClick ? 0 : 20);
                functions.onInventoryClick(allowed);
                verify(allowed, never()).setCancelled(true);
                for (Inventory forbidden : List.of(backpack, chest)) {
                    InventoryClickEvent blocked = click(forbidden, currency, action, topClick, 0);
                    functions.onInventoryClick(blocked);
                    verify(blocked).setCancelled(true);
                }
            }
        }
        for (ItemStack other : List.of(item(Material.STONE, 0, 1), wallet("nested"), item(Material.ECHO_SHARD, 2020, 1))) {
            InventoryClickEvent blocked = click(wallet, other, InventoryAction.PLACE_ALL, true, 0);
            functions.onInventoryClick(blocked);
            verify(blocked).setCancelled(true);
        }
        InventoryClickEvent offhand = click(chest, null, InventoryAction.HOTBAR_SWAP, true, 0);
        when(offhand.getClick()).thenReturn(ClickType.SWAP_OFFHAND);
        when(playerInventory.getItemInOffHand()).thenAnswer(call -> item(Material.SUNFLOWER, 2000, 1));
        functions.onInventoryClick(offhand);
        verify(offhand).setCancelled(true);
    }

    @Test
    void allowsWithdrawalAndCasinoBetsWithoutAllowingCurrencyStorageElsewhere() {
        Inventory chest = inventory(null, 27);
        InventoryClickEvent withdraw = click(chest, item(Material.SUNFLOWER, 2000, 16), InventoryAction.MOVE_TO_OTHER_INVENTORY, true, 0);
        functions.onInventoryClick(withdraw);
        verify(withdraw, never()).setCancelled(true);
        Inventory casino = inventory(new CasinoInventoryHolder(49), 54);
        InventoryClickEvent bet = click(casino, item(Material.GOLD_NUGGET, 2010, 8), InventoryAction.PLACE_ALL, true, 49);
        functions.onInventoryClick(bet);
        verify(bet, never()).setCancelled(true);
        for (InventoryClickEvent blocked : List.of(
                click(casino, item(Material.SUNFLOWER, 2000, 8), InventoryAction.PLACE_ALL, true, 49),
                click(casino, item(Material.GOLD_NUGGET, 2010, 8), InventoryAction.PLACE_ALL, true, 48),
                click(casino, item(Material.GOLD_NUGGET, 2010, 8), InventoryAction.MOVE_TO_OTHER_INVENTORY, false, 60))) {
            functions.onInventoryClick(blocked);
            verify(blocked).setCancelled(true);
        }
    }

    @Test
    void blocksDragAndHopperStorageButAllowsDraggingInsidePlayerInventory() {
        Inventory top = inventory(new EconomyItemsFunctions.BackpackHolder("wallet", "Monedero", 6), 18);
        when(view.getTopInventory()).thenReturn(top);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);
        when(drag.getRawSlots()).thenReturn(Set.of(0, 1));
        when(drag.getOldCursor()).thenAnswer(call -> item(Material.STONE, 0, 8));
        functions.onInventoryDrag(drag);
        verify(drag).setCancelled(true);
        reset(drag);
        when(drag.getView()).thenReturn(view);
        when(drag.getRawSlots()).thenReturn(Set.of(20, 21));
        when(drag.getOldCursor()).thenAnswer(call -> item(Material.STONE, 0, 8));
        functions.onInventoryDrag(drag);
        verify(drag, never()).setCancelled(true);
        InventoryMoveItemEvent hopper = mock(InventoryMoveItemEvent.class);
        when(hopper.getItem()).thenAnswer(call -> item(Material.GOLD_NUGGET, 2010, 1));
        functions.onInventoryMoveItem(hopper);
        verify(hopper).setCancelled(true);
    }

    @Test
    void opensExactlyEighteenSlotsAndKeepsWalletTypeWhenHeldItemChanges() throws Exception {
        ItemStack wallet = wallet("wallet");
        Inventory inventory = inventory(null, 18);
        when(inventory.getContents()).thenReturn(new ItemStack[18]);
        PlayerInteractEvent interact = mock(PlayerInteractEvent.class);
        when(interact.getItem()).thenReturn(wallet);
        when(interact.getPlayer()).thenReturn(player);
        when(interact.getHand()).thenReturn(EquipmentSlot.HAND);
        when(interact.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(18), anyString())).thenAnswer(call -> {
                InventoryHolder holder = call.getArgument(0);
                when(inventory.getHolder()).thenReturn(holder);
                return inventory;
            });
            functions.onPlayerInteract(interact);
            verify(player).openInventory(inventory);
            assertEquals("wallet", functions.getOpenWallet(playerId));
            when(playerInventory.getItemInMainHand()).thenAnswer(call -> item(Material.STONE, 0, 1));
            InventoryCloseEvent close = mock(InventoryCloseEvent.class);
            when(close.getPlayer()).thenReturn(player);
            when(close.getInventory()).thenReturn(inventory);
            functions.onInventoryClose(close);
            verify(database).saveBackpack(eq("wallet"), eq(playerId), eq("Crosszy"), eq("Mi dinero"), eq(6), any());
            assertNull(functions.getOpenWallet(playerId));
        }
    }

    private DinoCoinsManager manager(MockedStatic<Bukkit> bukkit) {
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        return new DinoCoinsManager(plugin, database, functions);
    }

    @Test
    void recordsBothCurrenciesOnceAndIncludesRegisteredWalletsStoredInChests() {
        ItemStack wallet = wallet("carried");
        when(playerInventory.getContents()).thenReturn(new ItemStack[]{wallet, wallet});
        when(player.getEnderChest().getContents()).thenReturn(new ItemStack[]{wallet});
        when(database.getPlayerBackpacks(playerId)).thenReturn(List.of(
                new DatabaseManager.BackpackInfo("carried", "Renombrado", 6, ""),
                new DatabaseManager.BackpackInfo("stored", "Caja", 6, ""),
                new DatabaseManager.BackpackInfo("normal", "Monedero falso", 1, "")));
        functions.updateBackpackContents("carried", new ItemStack[]{item(Material.SUNFLOWER, 2000, 32), item(Material.GOLD_NUGGET, 2010, 12)});
        when(database.loadBackpackContents("stored")).thenAnswer(call -> new ItemStack[]{item(Material.SUNFLOWER, 2000, 64)});
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            DinoCoinsManager manager = manager(bukkit);
            assertEquals(96, manager.calculatePhysicalDinoCoins(player));
            assertEquals(12, manager.calculatePhysicalDinoFichas(player));
            manager.updatePlayerTotalAsync(player);
            verify(database).setCurrencyBalances(playerId, 96, 12);
            verify(database, never()).loadBackpackContents("normal");
        }
    }

    @Test
    void anOlderAsyncSnapshotCannotOverwriteTheLatestBalance() {
        List<Runnable> pending = new ArrayList<>();
        when(scheduler.runTaskAsynchronously(any(Plugin.class), any(Runnable.class))).thenAnswer(call -> {
            pending.add(call.getArgument(1));
            return mock(BukkitTask.class);
        });
        when(database.getPlayerBackpacks(playerId)).thenReturn(List.of(new DatabaseManager.BackpackInfo("wallet", "Monedero", 6, "")));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            DinoCoinsManager manager = manager(bukkit);
            functions.updateBackpackContents("wallet", new ItemStack[]{item(Material.SUNFLOWER, 2000, 1)});
            manager.updatePlayerTotalAsync(player);
            functions.updateBackpackContents("wallet", new ItemStack[]{item(Material.SUNFLOWER, 2000, 2)});
            manager.updatePlayerTotalAsync(player);
            pending.get(1).run();
            pending.get(0).run();
            verify(database).setCurrencyBalances(playerId, 2, 0);
            verify(database, never()).setCurrencyBalances(playerId, 1, 0);
        }
    }

    @Test
    void respectsWalletCapacityAndNeverSpendsDinoFichasAsCoins() {
        when(database.getPlayerBackpacks(playerId)).thenReturn(List.of(new DatabaseManager.BackpackInfo("wallet", "Monedero", 6, "")));
        ItemStack[] contents = new ItemStack[18];
        Arrays.fill(contents, item(Material.GOLD_NUGGET, 2010, 64));
        contents[0] = item(Material.SUNFLOWER, 2000, 63);
        functions.updateBackpackContents("wallet", contents);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            DinoCoinsManager manager = manager(bukkit);
            assertFalse(manager.addPhysicalDinoCoins(player, 2));
            assertEquals(64, functions.getBackpackContents("wallet")[0].getAmount());
            assertTrue(manager.removePhysicalDinoCoins(player, 64));
            assertNull(functions.getBackpackContents("wallet")[0]);
            assertEquals(17 * 64, manager.calculatePhysicalDinoFichas(player));
            assertFalse(manager.removePhysicalDinoCoins(player, 1));
            assertFalse(manager.addPhysicalDinoCoins(player, -1));
            assertEquals(18, functions.getBackpackContents("wallet").length);
        }
    }
}
