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

    @Test void eachMarkedMobHasOneDropBelowThirtyPercentAndNoDropAtTheBoundary() {
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
                new FloralMobDrops(plugin(),()->.299999).onDeath(event);
                assertEquals(List.of(vanillaDrop,expected.get(i)),drops);
                drops.clear();
                new FloralMobDrops(plugin(),()->.30).onDeath(event);
                assertTrue(drops.isEmpty());
            }
        }
    }

    @Test void vanillaMobsWithTheSameDisplayNameDoNotGetFloralDrops() {
        Zombie zombie=mock(Zombie.class);
        when(zombie.getCustomName()).thenReturn("Zombie Floral");
        when(zombie.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        EntityDeathEvent event=mock(EntityDeathEvent.class);
        when(event.getEntity()).thenReturn(zombie);
        new FloralMobDrops(plugin(),()->0).onDeath(event);
        verify(event,never()).getDrops();
    }
}
