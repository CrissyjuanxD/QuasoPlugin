package Dificultades;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BloodFragmentsTest {

    // De 100 tiradas, 50 sueltan fragmentos: 40 sueltan 1 y 10 sueltan 2
    @Test
    void bloodMoonMobsDropFragmentsHalfOfTheTime() {
        int drops = 0, doubles = 0;
        for (int roll = 0; roll < 100; roll++) {
            int amount = OneChanges.bloodFragments(roll);
            if (amount > 0) drops++;
            if (amount == 2) doubles++;
            assertTrue(amount >= 0 && amount <= 2);
        }
        assertEquals(50, drops);
        assertEquals(10, doubles);
    }
}
