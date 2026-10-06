package items;

import BloodMoon.BloodMoon;
import BloodMoon.BloodMoonHordeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AmuletBloodMTest {
    private AmuletBloodM amuletHandler;
    private BloodMoon bloodMoon;
    private Player player;
    private World world;
    private ItemStack amulet;
    private AtomicInteger diamonds;
    private AtomicBoolean enchanted;
    private final Map<String, Object> saved = new HashMap<>();
    private final List<Runnable> sessions = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("quasoplugin");
        bloodMoon = mock(BloodMoon.class);
        world = mock(World.class);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(bloodMoon.isActive(world)).thenReturn(true);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Crosszy");
        when(player.isOnline()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0));
        when(player.spigot()).thenReturn(mock(Player.Spigot.class));
        when(world.getPlayers()).thenReturn(List.of(player));
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getOpenInventory()).thenReturn(mock(InventoryView.class));
        amulet = mock(ItemStack.class);
        when(amulet.getType()).thenReturn(Material.TORCHFLOWER_SEEDS);
        when(amulet.hasItemMeta()).thenReturn(true);
        ItemMeta meta = mock(ItemMeta.class);
        when(amulet.getItemMeta()).thenReturn(meta);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.has(any(NamespacedKey.class), eq(PersistentDataType.BYTE))).thenReturn(true);
        when(data.getOrDefault(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), anyInt()))
                .thenAnswer(call -> saved.getOrDefault(((NamespacedKey) call.getArgument(0)).getKey(), call.getArgument(2)));
        doAnswer(call -> { saved.put(((NamespacedKey) call.getArgument(0)).getKey(), call.getArgument(2)); return null; })
                .when(data).set(any(NamespacedKey.class), eq(PersistentDataType.INTEGER), anyInt());
        enchanted = new AtomicBoolean();
        when(amulet.containsEnchantment(Enchantment.UNBREAKING)).thenAnswer(call -> enchanted.get());
        when(meta.addEnchant(eq(Enchantment.UNBREAKING), anyInt(), anyBoolean())).thenAnswer(call -> { enchanted.set(true); return true; });
        when(meta.removeEnchant(eq(Enchantment.UNBREAKING))).thenAnswer(call -> { enchanted.set(false); return true; });
        ItemStack diamond = mock(ItemStack.class);
        when(diamond.getType()).thenReturn(Material.DIAMOND);
        diamonds = new AtomicInteger(3);
        when(diamond.getAmount()).thenAnswer(call -> diamonds.get());
        doAnswer(call -> { diamonds.set(call.getArgument(0)); return null; }).when(diamond).setAmount(anyInt());
        when(inventory.getContents()).thenAnswer(call -> new ItemStack[]{amulet, diamond});
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer(call -> {
            if ((long) call.getArgument(3) == 4L) sessions.add(call.getArgument(1));
            BukkitTask task = mock(BukkitTask.class);
            when(task.getTaskId()).thenReturn(sessions.size() + 1);
            return task;
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getPlayer(player.getUniqueId())).thenReturn(player);
        amuletHandler = new AmuletBloodM(plugin, bloodMoon);
    }
    @AfterEach void cleanup() { amuletHandler.shutdown(); bukkit.close(); }
    private void click() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(amulet);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        amuletHandler.onInteract(event);
    }
    @Test void anActiveAmuletCancelsTheEntireHordeWithoutDeletingNearbyMobs() {
        click();
        assertTrue(amuletHandler.isProtecting(player));
        assertEquals(2, diamonds.get());
        BloodMoonHordeEvent event = new BloodMoonHordeEvent(player, List.of(new Location(world, 10, 64, 0)));
        amuletHandler.onHordeSpawn(event);
        assertTrue(event.isCancelled());
        sessions.getFirst().run();
        verify(player, never()).getNearbyEntities(anyDouble(), anyDouble(), anyDouble());
        verify(world, never()).getEntities();
    }
    @Test void anotherPlayersHordeIsBlockedIfAPlannedSpawnEntersTheProtectedRadius() {
        click();
        Player target = mock(Player.class);
        when(target.getLocation()).thenReturn(new Location(world, 30, 64, 0));
        BloodMoonHordeEvent inside = new BloodMoonHordeEvent(target, List.of(new Location(world, 19, 64, 0)));
        amuletHandler.onHordeSpawn(inside);
        assertTrue(inside.isCancelled());
        BloodMoonHordeEvent outside = new BloodMoonHordeEvent(target, List.of(new Location(world, 25, 64, 0)));
        amuletHandler.onHordeSpawn(outside);
        assertFalse(outside.isCancelled());
    }
    @Test void switchingTheAmuletOffAndOnPreservesDiamondCreditAndPartialUse() {
        click();
        for (int i = 0; i < 10; i++) sessions.getFirst().run();
        click();
        assertFalse(amuletHandler.isProtecting(player));
        assertEquals(40, saved.get("amulet_durability_ticks"));
        int diamondsAfterFirstActivation = diamonds.get();
        click();
        assertTrue(amuletHandler.isProtecting(player));
        assertEquals(diamondsAfterFirstActivation, diamonds.get());
        for (int i = 0; i < 26; i++) sessions.getLast().run();
        assertEquals(249, saved.get("amulet_usos"));
    }
    @Test void endingTheBloodMoonDisablesProtectionAndPersistsTheAmulet() {
        click();
        when(bloodMoon.isActive(world)).thenReturn(false);
        assertFalse(amuletHandler.isProtecting(player));
        sessions.getFirst().run();
        assertFalse(enchanted.get());
        assertEquals(1, saved.get("amulet_diamond_ticks"));
    }
    @Test void noDiamondsMeansNoActivationOrProtection() {
        diamonds.set(0);
        click();
        assertFalse(amuletHandler.isProtecting(player));
        assertTrue(sessions.isEmpty());
        assertFalse(enchanted.get());
    }
}
