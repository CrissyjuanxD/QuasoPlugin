package Bosses;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueenBeeHandlerTest {
    private Bee bee;
    private JavaPlugin plugin;
    private QueenBeeHandler boss;
    private Player attacker;

    @BeforeAll static void initializePaper() { support.PaperTestRegistry.initialize(); }

    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("quasoplugin");
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        World world = mock(World.class);
        bee = mock(Bee.class);
        when(bee.getUniqueId()).thenReturn(UUID.randomUUID());
        when(bee.getType()).thenReturn(org.bukkit.entity.EntityType.BEE);
        when(bee.getWorld()).thenReturn(world);
        when(bee.getLocation()).thenAnswer(call -> new Location(world, 0, 70, 0));
        when(bee.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(bee.getHealth()).thenReturn(200.0);
        AttributeInstance health = mock(AttributeInstance.class);
        AtomicReference<Double> maximum = new AtomicReference<>(600.0);
        when(health.getValue()).thenAnswer(call -> maximum.get());
        doAnswer(call -> { maximum.set(call.getArgument(0)); return null; }).when(health).setBaseValue(anyDouble());
        when(bee.getAttribute(nullable(Attribute.class))).thenReturn(health);
        boss = new QueenBeeHandler(plugin, bee);
        attacker = mock(Player.class);
        when(attacker.getUniqueId()).thenReturn(UUID.randomUUID());
        when(attacker.getGameMode()).thenReturn(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack weapon = mock(ItemStack.class);
        when(weapon.getType()).thenReturn(Material.MACE);
        when(inventory.getItemInMainHand()).thenReturn(weapon);
        when(attacker.getInventory()).thenReturn(inventory);
        clearInvocations(bee);
    }

    @AfterEach void cleanup() {
        QueenBeeHandler.ACTIVE_BOSSES.clear();
        HandlerList.unregisterAll(boss);
    }

    @Test void aBlockedLethalMaceHitCannotTriggerDeathAtHighestPriority() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(bee);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getFinalDamage()).thenReturn(1000.0);
        AtomicBoolean cancelled = new AtomicBoolean();
        when(event.isCancelled()).thenAnswer(call -> cancelled.get());
        doAnswer(call -> { cancelled.set(call.getArgument(0)); return null; }).when(event).setCancelled(anyBoolean());

        boss.onEntityDamage(event);
        boss.onGenericDamage(event);

        assertTrue(cancelled.get());
        verify(bee, never()).setHealth(anyDouble());
        verify(event, never()).setCancelled(false);
    }

    @Test void explosionImmunityRemainsCancelledAtHighestPriority() {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(bee);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION);
        when(event.getFinalDamage()).thenReturn(1000.0);
        AtomicBoolean cancelled = new AtomicBoolean();
        when(event.isCancelled()).thenAnswer(call -> cancelled.get());
        doAnswer(call -> { cancelled.set(call.getArgument(0)); return null; }).when(event).setCancelled(anyBoolean());

        boss.onEntityDamageExplosions(event);
        boss.onGenericDamage(event);

        assertTrue(cancelled.get());
        verify(bee, never()).setHealth(anyDouble());
    }

    @Test void arrowsKeepTheirDamageAndCreditTheirShooter() {
        Projectile arrow = mock(Projectile.class);
        when(arrow.getShooter()).thenReturn(attacker);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(bee);
        when(event.getDamager()).thenReturn(arrow);
        when(event.getFinalDamage()).thenReturn(20.0);

        boss.onEntityDamage(event);
        boss.onGenericDamage(event);

        assertTrue(boss.attackers.contains(attacker.getUniqueId()));
        verify(event, never()).setDamage(anyDouble());
        verify(event, never()).setCancelled(true);
    }

    @Test void floralReinforcementsDoNotGrantBossRewardsOrMissionProgress() throws Exception {
        var pluginField = Events.MissionSystem.MissionUtils.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        Object original = pluginField.get(null);
        pluginField.set(null, plugin);
        try {
            when(bee.getCustomName()).thenReturn("§dGuardian Abeja Floral");
            assertNull(Events.MissionSystem.MissionUtils.bossId(bee));
            when(bee.getCustomName()).thenReturn("§d§lAbeja Floral");
            assertEquals("abeja_reina", Events.MissionSystem.MissionUtils.bossId(bee));
        } finally {
            pluginField.set(null, original);
        }
    }

    @Test void adoptingTheFloralHealthDoesNotHealAnExistingQueen() {
        var attribute = bee.getAttribute(Attribute.MAX_HEALTH);
        attribute.setBaseValue(1000.0);
        when(bee.getHealth()).thenReturn(500.0);
        clearInvocations(bee);

        new QueenBeeHandler(plugin, bee);

        assertEquals(600.0, attribute.getValue());
        verify(bee).setHealth(300.0);
    }
}
