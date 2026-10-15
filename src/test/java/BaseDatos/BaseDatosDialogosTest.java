package BaseDatos;

import BaseDatos.BaseDatosSQL.Columna;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class BaseDatosDialogosTest {

    private static final Columna NAME = new Columna("name", "varchar(16)", true, false, null, false);
    private static final Columna ACTIVA = new Columna("is_active", "tinyint(1)", true, false, "1", false);

    // 8 filas por página
    @Test
    void pagesAreCountedFromRows() {
        assertEquals(1, BaseDatosDialogos.paginas(0));
        assertEquals(1, BaseDatosDialogos.paginas(8));
        assertEquals(2, BaseDatosDialogos.paginas(9));
    }

    // Un nulo se muestra vacío: dejarlo vacío (o el interruptor apagado) no lo cambia
    @Test
    void untouchedFieldsAreNotSaved() {
        assertTrue(BaseDatosDialogos.sinCambio(NAME, "Steve", "Steve"));
        assertFalse(BaseDatosDialogos.sinCambio(NAME, "Steve", "Alex"));
        assertTrue(BaseDatosDialogos.sinCambio(NAME, null, ""));
        assertFalse(BaseDatosDialogos.sinCambio(NAME, null, "NULL"));
        assertTrue(BaseDatosDialogos.sinCambio(ACTIVA, "1", "1"));
        assertFalse(BaseDatosDialogos.sinCambio(ACTIVA, "1", "0"));
        assertTrue(BaseDatosDialogos.sinCambio(ACTIVA, null, "0"));
    }

    @Test
    void backupNamesAreReadable() {
        assertEquals("2026-10-15 14:03:22 · 12 filas",
                BaseDatosDialogos.nombreCopia(new File("2026-10-15_14-03-22_12filas.json")));
        assertEquals("player_missions", BaseDatosDialogos.carpeta("player_missions"));
        assertEquals("a_b_c", BaseDatosDialogos.carpeta("a/b.c"));
    }

    @Test
    void longValuesAreCut() {
        assertTrue(BaseDatosDialogos.valor(null, 10).endsWith("NULL"));
        assertTrue(BaseDatosDialogos.valor("x".repeat(50), 10).contains("(50 caracteres)"));
        assertEquals("3 d 2 h", BaseDatosDialogos.duracion(3 * 86400 + 2 * 3600));
        assertEquals("5 min", BaseDatosDialogos.duracion(300));
    }

    // Cada tabla del plugin dice qué pasa en el juego si se borra
    @Test
    void everyPluginTableExplainsItsEffect() {
        for (String tabla : BaseDatosSQL.PROPIAS) {
            assertFalse(BaseDatosDialogos.efecto(tabla).contains("No es de QuasoPlugin"), tabla);
        }
        assertTrue(BaseDatosDialogos.efecto("otra").contains("No es de QuasoPlugin"));
    }
}
