package Dificultades.CustomMobs;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class CorruptedBeeScaleTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    @Test void loadedFloralBeesGrowWithoutHealingOrChangingOrdinaryBees() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("QuasoPlugin");
        when(plugin.namespace()).thenReturn("quasoplugin");
        Bee floral = mock(Bee.class), ordinary = mock(Bee.class);
        AttributeInstance scale = mock(AttributeInstance.class);
        var floralData = mock(PersistentDataContainer.class);
        when(floral.getPersistentDataContainer()).thenReturn(floralData);
        when(floralData.has(new NamespacedKey("quasoplugin", "corrupted_bee"), PersistentDataType.BYTE)).thenReturn(true);
        when(floral.getAttribute(Attribute.SCALE)).thenReturn(scale);
        when(floral.getUniqueId()).thenReturn(UUID.randomUUID());
        when(ordinary.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        EntitiesLoadEvent event = mock(EntitiesLoadEvent.class);
        when(event.getEntities()).thenReturn(List.of(floral, ordinary, mock(Entity.class)));

        new CorruptedBee(plugin).onEntitiesLoad(event);

        verify(scale).setBaseValue(1.2);
        verify(floral, never()).setHealth(anyDouble());
        verify(ordinary, never()).getAttribute(Attribute.SCALE);
        verify(ordinary, never()).setHealth(anyDouble());
    }
}
