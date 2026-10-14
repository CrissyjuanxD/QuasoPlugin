package Events.MissionSystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoMisionTest {

    @Test
    void eachTypeIsWrittenItsOwnWayInCommands() {
        assertEquals("1", TipoMision.token(1, 0));
        assertEquals("1ex", TipoMision.token(120, 1));
        assertEquals("170tra", TipoMision.token(170, 0));
        assertEquals(TipoMision.TRABAJO, TipoMision.de(141, 0));
        assertEquals(TipoMision.EXTRA, TipoMision.de(101, 2));
        assertEquals(TipoMision.NORMAL, TipoMision.de(100, 0));
    }

    // La segunda extra de una misión va con su orden y las de la 201 en adelante también son extras
    @Test
    void theSecondExtraOfAMissionCarriesItsOrder() {
        assertEquals("12ex", TipoMision.token(110, 12, 0));
        assertEquals("12ex2", TipoMision.token(204, 12, 1));
        assertEquals("12ex3", TipoMision.token(205, 12, 2));
        assertEquals(TipoMision.EXTRA, TipoMision.de(204, 12));
        assertArrayEquals(new int[]{12, 1}, TipoMision.partesExtra("12ex"));
        assertArrayEquals(new int[]{12, 2}, TipoMision.partesExtra("12ex2"));
    }

    @Test
    void theTokensGoBackToTheirMissionNumber() {
        assertEquals(1, TipoMision.parse("1", (parent, orden) -> -1));
        assertEquals(120, TipoMision.parse("1ex", (parent, orden) -> parent == 1 && orden == 0 ? 120 : -1));
        assertEquals(-1, TipoMision.parse("3ex", (parent, orden) -> parent == 1 ? 120 : -1));
        assertEquals(170, TipoMision.parse("170TRA", (parent, orden) -> -1));
        assertEquals(-1, TipoMision.parse("50tra", (parent, orden) -> -1));
        assertEquals(-1, TipoMision.parse("hola", (parent, orden) -> -1));
        assertEquals(120, TipoMision.parse("120", (parent, orden) -> -1));
    }

    @Test
    void theExtraOrderPicksTheRightExtra() {
        int[] extras = {110, 204, 205};
        assertEquals(110, TipoMision.parse("12ex", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(110, TipoMision.parse("12ex1", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(204, TipoMision.parse("12EX2", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(205, TipoMision.parse("12ex3", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(-1, TipoMision.parse("12ex4", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(-1, TipoMision.parse("12ex0", (parent, orden) -> orden < extras.length ? extras[orden] : -1));
        assertEquals(-1, TipoMision.parse("ex2", (parent, orden) -> 1));
    }
}
