package Handlers;

import Dificultades.Change;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChangesAnuncioTest {

    private static Change etapa(String titulo, List<String> cambios) {
        return new Change() {
            public String id() { return "dos"; }
            public String description() { return ""; }
            public void apply() {}
            public void revert() {}
            public boolean isApplied() { return true; }
            public String tituloAnuncio() { return titulo; }
            public List<String> anuncio() { return cambios; }
        };
    }

    // El título y una línea por cambio, entre dos líneas vacías
    @Test void theChatAnnouncementListsEveryChange() {
        List<String> lineas = ChangesHandler.lineasAnuncio(etapa("¡Se abrió la Infested Warden Cave!", List.of("Uno", "Dos")));
        assertEquals(5, lineas.size());
        assertEquals("", lineas.getFirst());
        assertTrue(lineas.get(1).contains("¡Se abrió la Infested Warden Cave!"));
        assertTrue(lineas.get(2).endsWith("Uno"));
        assertTrue(lineas.get(3).endsWith("Dos"));
        assertEquals("", lineas.getLast());
    }
}
