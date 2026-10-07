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

    @Test
    void theTokensGoBackToTheirMissionNumber() {
        assertEquals(1, TipoMision.parse("1", parent -> -1));
        assertEquals(120, TipoMision.parse("1ex", parent -> parent == 1 ? 120 : -1));
        assertEquals(-1, TipoMision.parse("3ex", parent -> parent == 1 ? 120 : -1));
        assertEquals(170, TipoMision.parse("170TRA", parent -> -1));
        assertEquals(-1, TipoMision.parse("50tra", parent -> -1));
        assertEquals(-1, TipoMision.parse("hola", parent -> -1));
        assertEquals(120, TipoMision.parse("120", parent -> -1));
    }
}
