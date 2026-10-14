package Dificultades.CustomMobs;

import items.FloralMobItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FloralMobDropsTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("QuasoPlugin");
        when(plugin.namespace()).thenReturn("quasoplugin");
        return plugin;
    }

    @Test void eachMarkedMobHasOneDropBelowFortyPercentFromDayTwelveAndNoDropAtTheBoundary() {
        try (var factories = mockStatic(FloralMobItems.class)) {
            ItemStack flesh=mock(ItemStack.class), eye=mock(ItemStack.class), powder=mock(ItemStack.class);
            factories.when(FloralMobItems::createFlesh).thenReturn(flesh);
            factories.when(FloralMobItems::createEye).thenReturn(eye);
            factories.when(FloralMobItems::createGunpowder).thenReturn(powder);
            List<LivingEntity> mobs = List.of(mock(Zombie.class),mock(Spider.class),mock(Creeper.class));
            List<String> keys = List.of("corrupted_zombie","corruptedspider","bombita");
            List<ItemStack> expected = List.of(flesh,eye,powder);
            for (int i=0;i<3;i++) {
                var data=mock(PersistentDataContainer.class);
                when(data.has(new NamespacedKey("quasoplugin",keys.get(i)),PersistentDataType.BYTE)).thenReturn(true);
                when(mobs.get(i).getPersistentDataContainer()).thenReturn(data);
                var event=mock(EntityDeathEvent.class);
                when(event.getEntity()).thenReturn(mobs.get(i));
                List<ItemStack> drops = new ArrayList<>();
                ItemStack vanillaDrop=mock(ItemStack.class); drops.add(vanillaDrop);
                when(event.getDrops()).thenReturn(drops);
                new FloralMobDrops(plugin(),()->.399999,()->true).onDeath(event);
                assertEquals(List.of(vanillaDrop,expected.get(i)),drops);
                drops.clear();
                new FloralMobDrops(plugin(),()->.40,()->true).onDeath(event);
                assertTrue(drops.isEmpty());
            }
        }
    }

    @Test void theMiniFloralBeeDropsItsStingerButBossSummonsDropNothing() {
        try (var factories = mockStatic(FloralMobItems.class)) {
            ItemStack stinger = mock(ItemStack.class);
            factories.when(FloralMobItems::createStinger).thenReturn(stinger);
            Bee bee = mock(Bee.class);
            var data = mock(PersistentDataContainer.class);
            when(data.has(new NamespacedKey("quasoplugin", "corrupted_bee"), PersistentDataType.BYTE)).thenReturn(true);
            when(bee.getPersistentDataContainer()).thenReturn(data);
            var event = mock(EntityDeathEvent.class);
            when(event.getEntity()).thenReturn(bee);
            List<ItemStack> drops = new ArrayList<>();
            when(event.getDrops()).thenReturn(drops);
            new FloralMobDrops(plugin(), () -> .1, () -> true).onDeath(event);
            assertEquals(List.of(stinger), drops);

            drops.clear();
            when(data.has(new NamespacedKey("quasoplugin", FloralMobDrops.INVOCADO), PersistentDataType.BYTE)).thenReturn(true);
            new FloralMobDrops(plugin(), () -> 0, () -> true).onDeath(event);
            assertTrue(drops.isEmpty());
        }
    }

    // Antes del día 12 es 10% y siempre 1, aunque tenga Saqueo III
    @Test void beforeDayTwelveItIsTenPercentAndLootingDoesNothing() {
        try (var factories = mockStatic(FloralMobItems.class)) {
            ItemStack flesh = mock(ItemStack.class);
            factories.when(FloralMobItems::createFlesh).thenReturn(flesh);
            Zombie zombie = mock(Zombie.class);
            var data = mock(PersistentDataContainer.class);
            when(data.has(new NamespacedKey("quasoplugin", "corrupted_zombie"), PersistentDataType.BYTE)).thenReturn(true);
            when(zombie.getPersistentDataContainer()).thenReturn(data);
            var event = mock(EntityDeathEvent.class);
            when(event.getEntity()).thenReturn(zombie);
            List<ItemStack> drops = new ArrayList<>();
            when(event.getDrops()).thenReturn(drops);

            new FloralMobDrops(plugin(), () -> .10, () -> false).onDeath(event);
            assertTrue(drops.isEmpty());
            new FloralMobDrops(plugin(), () -> .099, () -> false).onDeath(event);
            assertEquals(List.of(flesh), drops);
            verify(flesh).setAmount(1);
            verify(zombie, never()).getKiller();
        }
    }

    // Sin Saqueo o con I sale 1; con II de 1 a 2 y con III de 1 a 3
    @Test void lootingGivesUpToItsLevelOnceTheFortyPercentPasses() {
        for (int saqueo = 0; saqueo <= 1; saqueo++) {
            assertEquals(1, FloralMobDrops.cantidad(saqueo, 0));
            assertEquals(1, FloralMobDrops.cantidad(saqueo, .999));
        }
        assertEquals(1, FloralMobDrops.cantidad(2, .49));
        assertEquals(2, FloralMobDrops.cantidad(2, .5));
        assertEquals(1, FloralMobDrops.cantidad(3, .2));
        assertEquals(2, FloralMobDrops.cantidad(3, .5));
        assertEquals(3, FloralMobDrops.cantidad(3, .999));
    }

    @Test void vanillaMobsWithTheSameDisplayNameDoNotGetFloralDrops() {
        Zombie zombie=mock(Zombie.class);
        when(zombie.getCustomName()).thenReturn("Zombie Floral");
        when(zombie.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        EntityDeathEvent event=mock(EntityDeathEvent.class);
        when(event.getEntity()).thenReturn(zombie);
        new FloralMobDrops(plugin(),()->0,()->false).onDeath(event);
        verify(event,never()).getDrops();
    }
}
