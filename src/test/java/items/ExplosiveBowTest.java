package items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplosiveBowTest {

    // 3, 6 y 10 flechas por disparo; con Infinidad la mitad
    @Test
    void infinityHalvesTheArrowCost() {
        assertEquals(3, ExplosiveBow.costo(1, false));
        assertEquals(6, ExplosiveBow.costo(2, false));
        assertEquals(10, ExplosiveBow.costo(3, false));
        assertEquals(1, ExplosiveBow.costo(1, true));
        assertEquals(3, ExplosiveBow.costo(2, true));
        assertEquals(5, ExplosiveBow.costo(3, true));
    }
}
