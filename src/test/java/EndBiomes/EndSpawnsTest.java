package EndBiomes;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EndSpawnsTest {

    private static Map<EndSpawns.Mob, Integer> table(EndBiomeMap.Zone zone) {
        Map<EndSpawns.Mob, Integer> count = new EnumMap<>(EndSpawns.Mob.class);
        for (int roll = 0; roll < 100; roll++) count.merge(EndSpawns.reemplazo(zone, roll), 1, Integer::sum);
        return count;
    }

    @Test
    void partOfTheEndermenBecomeEndMobsInEveryZone() {
        Map<EndSpawns.Mob, Integer> vanilla = table(EndBiomeMap.Zone.VANILLA);
        assertEquals(75, vanilla.get(EndSpawns.Mob.NINGUNO));
        assertEquals(9, vanilla.get(EndSpawns.Mob.ENDER_BLAZE));
        assertEquals(9, vanilla.get(EndSpawns.Mob.ENDER_SPIDER));
        assertEquals(7, vanilla.get(EndSpawns.Mob.ENDER_CREEPER));

        Map<EndSpawns.Mob, Integer> prismatic = table(EndBiomeMap.Zone.PRISMATICO);
        assertEquals(80, prismatic.get(EndSpawns.Mob.NINGUNO));
        assertTrue(prismatic.get(EndSpawns.Mob.ENDER_SPIDER) > prismatic.get(EndSpawns.Mob.ENDER_BLAZE));

        Map<EndSpawns.Mob, Integer> paramo = table(EndBiomeMap.Zone.MARCHITO);
        assertEquals(71, paramo.get(EndSpawns.Mob.NINGUNO));
        assertEquals(5, paramo.get(EndSpawns.Mob.SHULKER_NEGRO));
    }

    @Test
    void looseBlackShulkersOnlySpawnInTheWasteland() {
        assertNull(table(EndBiomeMap.Zone.VANILLA).get(EndSpawns.Mob.SHULKER_NEGRO));
        assertNull(table(EndBiomeMap.Zone.PRISMATICO).get(EndSpawns.Mob.SHULKER_NEGRO));
    }

    @Test
    void everyShulkerDropsTheBoxOfItsColor() {
        assertEquals(Material.SHULKER_BOX, EndShulkers.boxOf(null));
        assertEquals(Material.PURPLE_SHULKER_BOX, EndShulkers.boxOf(DyeColor.PURPLE));
        for (DyeColor color : DyeColor.values()) assertNotNull(EndShulkers.boxOf(color));
    }
}
