package Dificultades;

import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedSpider;
import Dificultades.CustomMobs.CorruptedZombies;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static Dificultades.ExtraChanges.Floral.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExtraChangesTest {

    @Test
    void aQuarterOfZombiesAndSpidersAndAFifthOfCreepersTurnFloral() {
        assertEquals(ZOMBIE, ExtraChanges.floral(EntityType.ZOMBIE, true, 24));
        assertEquals(NINGUNO, ExtraChanges.floral(EntityType.ZOMBIE, true, 25));
        assertEquals(NINGUNO, ExtraChanges.floral(EntityType.ZOMBIE, false, 0));
        assertEquals(SPIDER, ExtraChanges.floral(EntityType.SPIDER, true, 24));
        assertEquals(NINGUNO, ExtraChanges.floral(EntityType.SPIDER, true, 25));
        assertEquals(BOMBITA, ExtraChanges.floral(EntityType.CREEPER, true, 19));
        assertEquals(NINGUNO, ExtraChanges.floral(EntityType.CREEPER, true, 20));
        for (EntityType otro : new EntityType[]{EntityType.HUSK, EntityType.DROWNED, EntityType.CAVE_SPIDER, EntityType.SKELETON}) {
            assertEquals(NINGUNO, ExtraChanges.floral(otro, true, 0), otro.name());
        }
    }

    @Test
    void onlyNaturalOverworldSpawnsAreConverted() {
        try (MockedConstruction<CorruptedZombies> zombies = mockConstruction(CorruptedZombies.class);
             MockedConstruction<CorruptedSpider> spiders = mockConstruction(CorruptedSpider.class);
             MockedConstruction<Bombita> bombitas = mockConstruction(Bombita.class)) {
            ExtraChanges extra = spy(new ExtraChanges(mock(JavaPlugin.class)));
            assertEquals("extra", extra.id());

            World overworld = world("world", World.Environment.NORMAL);
            extra.onNaturalSpawn(spawn(mock(Zombie.class), overworld, CreatureSpawnEvent.SpawnReason.SPAWNER));
            extra.onNaturalSpawn(spawn(mock(Zombie.class), world("wardencave", World.Environment.NORMAL), CreatureSpawnEvent.SpawnReason.NATURAL));
            extra.onNaturalSpawn(spawn(mock(Zombie.class), world("world_nether", World.Environment.NETHER), CreatureSpawnEvent.SpawnReason.NATURAL));
            verify(extra, never()).convertir(any(), anyInt());
            // Bebé: se tira la suerte pero nunca se convierte, así no cambia lo que se verifica abajo
            Zombie natural = mob(Zombie.class, EntityType.ZOMBIE);
            extra.onNaturalSpawn(spawn(natural, overworld, CreatureSpawnEvent.SpawnReason.NATURAL));
            verify(extra).convertir(eq(natural), anyInt());

            Zombie adulto = mob(Zombie.class, EntityType.ZOMBIE);
            when(adulto.isAdult()).thenReturn(true);
            Zombie bebe = mob(Zombie.class, EntityType.ZOMBIE);
            Spider spider = mob(Spider.class, EntityType.SPIDER);
            Creeper creeper = mob(Creeper.class, EntityType.CREEPER);
            extra.convertir(adulto, 0);
            extra.convertir(bebe, 0);
            extra.convertir(spider, 0);
            extra.convertir(creeper, 0);
            extra.convertir(mob(Skeleton.class, EntityType.SKELETON), 0);
            extra.convertir(mob(Spider.class, EntityType.SPIDER), 99);

            verify(zombies.constructed().getFirst()).transformToCorruptedZombie(adulto);
            verify(zombies.constructed().getFirst(), never()).transformToCorruptedZombie(bebe);
            verify(spiders.constructed().getFirst(), times(1)).transformspawnCorruptedSpider(any());
            verify(spiders.constructed().getFirst()).transformspawnCorruptedSpider(spider);
            verify(bombitas.constructed().getFirst()).transformToBombita(creeper);
        }
    }

    private static World world(String name, World.Environment environment) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(world.getEnvironment()).thenReturn(environment);
        return world;
    }

    private static <T extends org.bukkit.entity.LivingEntity> T mob(Class<T> type, EntityType entityType) {
        T mob = mock(type);
        when(mob.getType()).thenReturn(entityType);
        return mob;
    }

    private static CreatureSpawnEvent spawn(org.bukkit.entity.LivingEntity entity, World world, CreatureSpawnEvent.SpawnReason reason) {
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        org.bukkit.Location location = mock(org.bukkit.Location.class);
        when(location.getWorld()).thenReturn(world);
        when(event.getLocation()).thenReturn(location);
        when(event.getEntity()).thenReturn(entity);
        when(event.getSpawnReason()).thenReturn(reason);
        return event;
    }
}
