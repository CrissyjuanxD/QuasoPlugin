package Trabajos;

import Events.MissionSystem.MisionTrabajo;
import Handlers.DatabaseManager;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrabajosTest {

    @Test
    void theCurveIsSlowButNotTediousAndPaysMoreTheHigherYouGo() {
        assertEquals(45, TrabajoNiveles.xpParaNivel(1));
        assertTrue(TrabajoNiveles.xpParaNivel(100) > TrabajoNiveles.xpParaNivel(50));

        assertEquals(2, TrabajoNiveles.monedasPorNivel(1));
        assertEquals(2, TrabajoNiveles.monedasPorNivel(20));
        assertEquals(3, TrabajoNiveles.monedasPorNivel(21));
        assertEquals(6, TrabajoNiveles.monedasPorNivel(100));

        assertEquals(0, TrabajoNiveles.bonus(4));
        assertEquals(13, TrabajoNiveles.bonus(5));
        assertEquals(40, TrabajoNiveles.bonus(50));
        assertEquals(70, TrabajoNiveles.bonus(100));
        assertEquals(1830, TrabajoNiveles.experiencia(100));

        assertEquals(5, TrabajoNiveles.proximoBonus(0));
        assertEquals(10, TrabajoNiveles.proximoBonus(5));
        assertEquals(-1, TrabajoNiveles.proximoBonus(100));

        int total = 0;
        for (int nivel = 1; nivel <= Trabajo.NIVEL_MAXIMO; nivel++) total += TrabajoNiveles.monedasTotales(nivel);
        assertTrue(total > 1150 && total < 1350, "Un trabajo completo paga " + total + " DinoCoins");
    }

    @Test
    void theSixJobsSitInTheirSlotsWithTheirOwnItemModel() {
        int[] slots = {1, 3, 5, 7, 11, 15};
        assertEquals(6, Trabajo.values().length);
        for (int i = 0; i < slots.length; i++) {
            assertEquals(Trabajo.values()[i], Trabajo.porSlot(slots[i]));
        }
        assertNull(Trabajo.porSlot(22));
        assertEquals("trabajo_mineria", Trabajo.MINERIA.modelo());
        assertEquals(Trabajo.LENADOR, Trabajo.porId("Leñador"));
        assertEquals(Trabajo.PESCADOR, Trabajo.porId("pescador"));
    }

    @Test
    void xpTablesRewardTheHardThingsAndIgnoreWhatIsNotTheJob() {
        assertEquals(30, TrabajosXp.xpMineria(Material.ANCIENT_DEBRIS));
        assertTrue(TrabajosXp.xpMineria(Material.DEEPSLATE_DIAMOND_ORE) > TrabajosXp.xpMineria(Material.DIAMOND_ORE));
        assertEquals(0, TrabajosXp.xpMineria(Material.DIRT));

        assertEquals(1.2, TrabajosXp.xpLenador(Material.OAK_LOG));
        assertEquals(1.4, TrabajosXp.xpLenador(Material.WARPED_STEM));
        assertEquals(0, TrabajosXp.xpLenador(Material.STRIPPED_OAK_LOG));

        assertEquals(0.5, TrabajosXp.xpConstructor(Material.STONE_BRICKS));
        assertEquals(0.5, TrabajosXp.xpConstructor(Material.WHITE_CONCRETE));
        assertEquals(0.3, TrabajosXp.xpConstructor(Material.OAK_PLANKS));
        assertEquals(0.1, TrabajosXp.xpConstructor(Material.COBBLESTONE));
        assertEquals(0, TrabajosXp.xpConstructor(Material.TORCH));
        assertEquals(0, TrabajosXp.xpConstructor(Material.CHEST));
        assertEquals(0, TrabajosXp.xpConstructor(Material.SCAFFOLDING));
        assertEquals(0, TrabajosXp.xpConstructor(Material.DIAMOND_ORE));

        assertEquals(4, TrabajosXp.xpMonstruo(8));
        assertEquals(6, TrabajosXp.xpMonstruo(20));
        assertEquals(15, TrabajosXp.xpMonstruo(500));

        assertEquals(20, TrabajosXp.xpElite(0));
        assertEquals(25, TrabajosXp.xpElite(10));
        assertEquals(40, TrabajosXp.xpElite(40));
        assertEquals(60, TrabajosXp.xpElite(200));
        assertTrue(TrabajosXp.xpElite(1) > TrabajosXp.xpMonstruo(500));
    }

    @Test
    void placingOresLogsOrMelonsMakesThemWorthNothing() {
        assertTrue(TrabajosXp.seRastrea(Material.DIAMOND_ORE));
        assertTrue(TrabajosXp.seRastrea(Material.STONE));
        assertTrue(TrabajosXp.seRastrea(Material.BIRCH_LOG));
        assertTrue(TrabajosXp.seRastrea(Material.MELON));
        assertTrue(TrabajosXp.seRastrea(Material.SUGAR_CANE));
        assertFalse(TrabajosXp.seRastrea(Material.WHEAT));
        assertFalse(TrabajosXp.seRastrea(Material.OAK_PLANKS));
    }

    @Test
    void everyBlockOfAChunkHasItsOwnPosition() {
        Set<Integer> vistas = new HashSet<>();
        for (int y = -64; y < 320; y += 7) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    assertTrue(vistas.add(BloquesColocados.posicion(x, y, z)));
                }
            }
        }
        assertEquals(BloquesColocados.posicion(1, 70, 2), BloquesColocados.posicion(-15, 70, 18));
    }

    @Test
    void levelsOfEveryJobSurviveASwitchAndTheDatabaseRoundTrip() {
        DatabaseManager.JobsData guardado = new DatabaseManager.JobsData("mineria", 1234L, Map.of(
                "mineria", new DatabaseManager.JobProgress(12, 30.5),
                "guerrero", new DatabaseManager.JobProgress(150, -3),
                "inventado", new DatabaseManager.JobProgress(5, 0)));
        DatosTrabajo datos = DatosTrabajo.desde(guardado);
        assertEquals(Trabajo.MINERIA, datos.activo);
        assertEquals(12, datos.nivel(Trabajo.MINERIA));
        assertEquals(100, datos.nivel(Trabajo.GUERRERO));
        assertEquals(0, datos.xp(Trabajo.GUERRERO));
        assertEquals(0, datos.nivel(Trabajo.PESCADOR));

        DatabaseManager.JobsData copia = datos.copia();
        assertEquals("mineria", copia.activeJob());
        assertEquals(1234L, copia.joinedAt());
        assertEquals(2, copia.progress().size());
        assertEquals(30.5, copia.progress().get("mineria").xp());
    }

    @Test
    void jobMissionsTakeNumbers141To200() {
        assertEquals(141, MisionTrabajo.PRIMERA);
        assertEquals(200, MisionTrabajo.ULTIMA);
        assertTrue(MisionTrabajo.es(141));
        assertTrue(MisionTrabajo.es(200));
        assertFalse(MisionTrabajo.es(140));
        assertFalse(MisionTrabajo.es(201));
    }
}
