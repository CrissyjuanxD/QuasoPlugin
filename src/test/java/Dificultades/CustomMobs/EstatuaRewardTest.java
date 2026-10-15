package Dificultades.CustomMobs;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.CopperGolem;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class EstatuaRewardTest {
    @BeforeAll static void initialize() { support.PaperTestRegistry.initialize(); }

    @Test void loadingOldStatuesResizesOnlyRewardGolems() {
        CopperGolem tagged = mock(CopperGolem.class);
        CopperGolem legacy = mock(CopperGolem.class);
        CopperGolem natural = mock(CopperGolem.class);
        AttributeInstance taggedScale = mock(AttributeInstance.class);
        AttributeInstance legacyScale = mock(AttributeInstance.class);
        when(tagged.getScoreboardTags()).thenReturn(Set.of("reward_statue"));
        when(legacy.getCustomName()).thenReturn(Estatua_Reward.STATUE_NAME);
        when(tagged.getAttribute(Attribute.SCALE)).thenReturn(taggedScale);
        when(legacy.getAttribute(Attribute.SCALE)).thenReturn(legacyScale);
        EntitiesLoadEvent event = mock(EntitiesLoadEvent.class);
        when(event.getEntities()).thenReturn(List.of(tagged, legacy, natural));

        new Estatua_Reward().onEntitiesLoad(event);

        verify(taggedScale).setBaseValue(4.5);
        verify(legacyScale).setBaseValue(4.5);
        verify(natural, never()).getAttribute(any());
    }
}
