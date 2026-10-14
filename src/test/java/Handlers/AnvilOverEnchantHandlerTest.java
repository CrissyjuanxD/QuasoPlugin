package Handlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnvilOverEnchantHandlerTest {

    // Poder V + libro V sube a VI solo en el Arco Mejorado; en un arco normal el tope es V
    @Test
    void powerSixOnlyOnTheImprovedBow() {
        assertEquals(6, AnvilOverEnchantHandler.poder(5, 5, 6, true));
        assertEquals(6, AnvilOverEnchantHandler.poder(6, 6, 6, true));
        assertEquals(6, AnvilOverEnchantHandler.poder(0, 6, 6, true));
        assertEquals(5, AnvilOverEnchantHandler.poder(4, 4, 6, true));
        assertEquals(5, AnvilOverEnchantHandler.poder(5, 5, 5, false));
        assertEquals(5, AnvilOverEnchantHandler.poder(0, 6, 5, false));
        assertEquals(3, AnvilOverEnchantHandler.poder(0, 3, 5, false));
    }
}
