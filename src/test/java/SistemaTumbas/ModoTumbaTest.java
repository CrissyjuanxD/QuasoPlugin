package SistemaTumbas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModoTumbaTest {

    @Test
    void theMixedGraveIsPrivateForItsFirstMinutesAndThenOpen() {
        long creada = 1_000_000;
        assertFalse(ModoTumba.MIXTA.abiertaParaTodos(creada, creada + 19 * 60_000L, 20));
        assertTrue(ModoTumba.MIXTA.abiertaParaTodos(creada, creada + 20 * 60_000L, 20));
        assertFalse(ModoTumba.PRIVADA.abiertaParaTodos(creada, creada + 999 * 60_000L, 20));
        assertTrue(ModoTumba.ABIERTA.abiertaParaTodos(creada, creada, 20));
    }

    @Test
    void theMixedGraveLastsItsPrivateAndOpenMinutes() {
        assertEquals(30, ModoTumba.MIXTA.minutosDeVida(45, 20, 10));
        assertEquals(45, ModoTumba.PRIVADA.minutosDeVida(45, 20, 10));
        assertEquals(ModoTumba.ABIERTA, ModoTumba.parse(" Abierta ", ModoTumba.MIXTA));
        assertEquals(ModoTumba.MIXTA, ModoTumba.parse("otra", ModoTumba.MIXTA));
    }

    @Test
    void theClockCountsHoursMinutesAndSeconds() {
        assertEquals("00:05:00", ModoTumba.reloj(300_000));
        assertEquals("01:15:09", ModoTumba.reloj(4_509_000));
        assertEquals("00:00:00", ModoTumba.reloj(-5));
    }
}
