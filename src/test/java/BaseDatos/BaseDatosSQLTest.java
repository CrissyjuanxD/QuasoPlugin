package BaseDatos;

import BaseDatos.BaseDatosSQL.Columna;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BaseDatosSQLTest {

    private static final Columna UUID = new Columna("uuid", "varchar(36)", false, false, null, true);
    private static final Columna NAME = new Columna("name", "varchar(16)", true, false, null, false);
    private static final Columna MONEDAS = new Columna("dinocoins", "int(11)", true, false, "0", false);
    private static final Columna XP = new Columna("xp", "double", true, false, "0", false);
    private static final Columna ACTIVA = new Columna("is_active", "tinyint(1)", true, false, "1", false);
    private static final Columna CONTENIDO = new Columna("contents", "longtext", true, false, null, false);

    // Los nombres van entre comillas invertidas y una comilla dentro del nombre se duplica
    @Test
    void identifiersAreQuotedAndEscaped() {
        assertEquals("`players`", BaseDatosSQL.id("players"));
        assertEquals("`ra``ro`", BaseDatosSQL.id("ra`ro"));
    }

    @Test
    void likeSearchEscapesWildcards() {
        assertEquals("50\\%\\_a\\\\b", BaseDatosSQL.escaparLike("50%_a\\b"));
    }

    // NULL solo en columnas que lo aceptan
    @Test
    void nullIsOnlyAllowedInNullableColumns() {
        assertNull(BaseDatosSQL.convertir(NAME, "NULL"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> BaseDatosSQL.convertir(UUID, "NULL"));
        assertTrue(error.getMessage().contains("uuid"));
    }

    @Test
    void numbersAreValidated() {
        assertEquals("250", BaseDatosSQL.convertir(MONEDAS, " 250 "));
        assertEquals("-3", BaseDatosSQL.convertir(MONEDAS, "-3"));
        assertThrows(IllegalArgumentException.class, () -> BaseDatosSQL.convertir(MONEDAS, "12.5"));
        assertThrows(IllegalArgumentException.class, () -> BaseDatosSQL.convertir(MONEDAS, "mil"));
        assertNull(BaseDatosSQL.convertir(MONEDAS, ""));
        assertEquals("12.5", BaseDatosSQL.convertir(XP, "12,5"));
    }

    @Test
    void booleansAcceptYesAndNo() {
        assertEquals("1", BaseDatosSQL.convertir(ACTIVA, "sí"));
        assertEquals("1", BaseDatosSQL.convertir(ACTIVA, "true"));
        assertEquals("0", BaseDatosSQL.convertir(ACTIVA, "no"));
        assertThrows(IllegalArgumentException.class, () -> BaseDatosSQL.convertir(ACTIVA, "quizás"));
        assertTrue(ACTIVA.booleana());
        assertFalse(ACTIVA.entera());
    }

    // Un varchar(16) no deja pasar más de 16 caracteres
    @Test
    void textRespectsColumnLength() {
        assertEquals(16, NAME.largoMaximo());
        assertEquals(BaseDatosSQL.MAX_EDITABLE, CONTENIDO.largoMaximo());
        assertEquals("Steve", BaseDatosSQL.convertir(NAME, "Steve"));
        assertThrows(IllegalArgumentException.class, () -> BaseDatosSQL.convertir(NAME, "UnNombreMuyLargoDeVerdad"));
    }

    // La fila se identifica por la clave primaria, o por todas las columnas si no hay
    @Test
    void rowsAreIdentifiedByPrimaryKey() {
        List<Columna> columnas = List.of(UUID, NAME, MONEDAS);
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("uuid", "1b2c3d4e-0000-0000-0000-000000000000");
        fila.put("name", "Steve");
        fila.put("dinocoins", "5");
        assertEquals(Map.of("uuid", "1b2c3d4e-0000-0000-0000-000000000000"), BaseDatosSQL.clave(columnas, fila));
        assertEquals(" WHERE `uuid` <=> ?", BaseDatosSQL.donde(BaseDatosSQL.claves(columnas)));
        assertEquals(" ORDER BY `uuid`", BaseDatosSQL.ordenPor(columnas));

        List<Columna> sinClave = List.of(NAME, MONEDAS);
        assertEquals(" WHERE `name` <=> ? AND `dinocoins` <=> ?", BaseDatosSQL.donde(BaseDatosSQL.claves(sinClave)));
        assertEquals(" ORDER BY `name`", BaseDatosSQL.ordenPor(sinClave));
    }

    // El botón de la fila muestra el nombre y la clave, con el UUID cortado
    @Test
    void rowSummaryShowsNameAndShortKey() {
        Map<String, String> fila = new LinkedHashMap<>();
        fila.put("uuid", "1b2c3d4e-0000-0000-0000-000000000000");
        fila.put("name", "Steve");
        fila.put("dinocoins", "5");
        assertEquals("Steve · 1b2c3d4e…", BaseDatosSQL.resumen(List.of(UUID, NAME, MONEDAS), fila));
    }

    @Test
    void sizesAreReadable() {
        assertEquals("512 B", BaseDatosSQL.tamano(512));
        assertEquals("1,5 KB", BaseDatosSQL.tamano(1536));
        assertEquals("2,0 MB", BaseDatosSQL.tamano(2L * 1024 * 1024));
    }

    // Las tablas de QuasoPlugin son las que vacía el reset total
    @Test
    void pluginTablesAreKnown() {
        assertTrue(new BaseDatosSQL.Tabla("players", 0, 0).propia());
        assertFalse(new BaseDatosSQL.Tabla("protections", 0, 0).propia());
        assertEquals(7, BaseDatosSQL.PROPIAS.size());
    }
}
