package Twitch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RachaTest {
    private static final long HOUR = 60 * 60 * 1000L;
    private static final long DAY = 24 * HOUR;
    private static final long MONTH = 32 * DAY;
    private static final long GAP = 2 * HOUR;
    private static final long START = 1_700_000_000_000L;

    // Observa la sub cada 10 minutos entre dos momentos, como hace el server prendido
    private static void seen(Racha racha, boolean active, long from, long to) {
        for (long t = from; t <= to; t += 10 * 60 * 1000L) racha.observar(active, t, GAP);
    }

    @Test void firstMonthCanBeClaimedRightAway() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        assertTrue(racha.puedeReclamar(MONTH));
        racha.reclamar(MONTH);
        assertFalse(racha.puedeReclamar(MONTH));
        assertEquals(MONTH, racha.falta(MONTH));
    }

    @Test void aOneMonthSubCannotClaimTwice() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        racha.reclamar(MONTH);
        // La sub dura 31 días y no se renueva: el día 31 todavía no llega al mes 2
        seen(racha, true, START, START + 31 * DAY);
        assertFalse(racha.puedeReclamar(MONTH));
        racha.observar(false, START + 31 * DAY + HOUR, GAP);
        assertFalse(racha.activa());
        assertFalse(racha.puedeReclamar(MONTH));
    }

    @Test void renewedSubUnlocksTheNextMonth() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        racha.reclamar(MONTH);
        seen(racha, true, START, START + 32 * DAY);
        assertEquals(2, racha.mesActual(MONTH));
        assertTrue(racha.puedeReclamar(MONTH));
        racha.reclamar(MONTH);
        assertFalse(racha.puedeReclamar(MONTH));
    }

    @Test void missedMonthsAreNotStacked() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        seen(racha, true, START, START + 3 * MONTH);
        assertEquals(4, racha.mesActual(MONTH));
        racha.reclamar(MONTH);
        assertFalse(racha.puedeReclamar(MONTH), "Solo el mes actual: los anteriores se pierden");
    }

    @Test void serverDowntimeDoesNotCount() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        racha.reclamar(MONTH);
        seen(racha, true, START, START + 10 * DAY);
        // Server apagado 20 días: al volver la sub sigue activa pero esos días no suman
        racha.observar(true, START + 30 * DAY, GAP);
        assertEquals(1, racha.mesActual(MONTH));
        assertEquals(22 * DAY, racha.falta(MONTH));
    }

    @Test void aShortRestartStillCounts() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        racha.observar(true, START + HOUR, GAP);
        assertEquals(HOUR, racha.observado);
    }

    @Test void resubscribingAfterALapseGivesANewKit() {
        Racha racha = new Racha();
        racha.observar(true, START, GAP);
        racha.reclamar(MONTH);
        racha.observar(false, START + 31 * DAY, GAP);
        racha.observar(true, START + 40 * DAY, GAP);
        assertTrue(racha.puedeReclamar(MONTH), "Volvió a pagar: es un mes nuevo");
    }
}
