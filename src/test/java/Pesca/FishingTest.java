package Pesca;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static Pesca.FishingMiniGame.Resultado.*;
import static org.junit.jupiter.api.Assertions.*;

class FishingTest {

    @Test
    void theBarHasTwoGreenSlotsFourOrangeOnEachSideAndRedEverywhereElse() {
        Map<FishingMiniGame.Resultado, Integer> cuenta = new EnumMap<>(FishingMiniGame.Resultado.class);
        for (int i = 0; i < FishingMiniGame.CASILLAS; i++) cuenta.merge(FishingMiniGame.zona(i, 10), 1, Integer::sum);
        assertEquals(2, cuenta.get(PERFECTA));
        assertEquals(8, cuenta.get(BUENA));
        assertEquals(14, cuenta.get(NORMAL));
        assertEquals(BUENA, FishingMiniGame.zona(6, 10));
        assertEquals(NORMAL, FishingMiniGame.zona(5, 10));
        assertEquals(PERFECTA, FishingMiniGame.zona(11, 10));
        assertEquals(BUENA, FishingMiniGame.zona(15, 10));
        assertEquals(NORMAL, FishingMiniGame.zona(16, 10));
    }

    @Test
    void theGreenZoneMovesButAlwaysKeepsItsOrangeAndSomeRedOnBothEnds() {
        Random random = new Random(7);
        int menor = Integer.MAX_VALUE;
        int mayor = Integer.MIN_VALUE;
        for (int i = 0; i < 2000; i++) {
            int verde = FishingMiniGame.verdeAleatorio(random);
            menor = Math.min(menor, verde);
            mayor = Math.max(mayor, verde);
            assertEquals(NORMAL, FishingMiniGame.zona(0, verde));
            assertEquals(NORMAL, FishingMiniGame.zona(FishingMiniGame.CASILLAS - 1, verde));
        }
        assertTrue(mayor - menor >= 10, "El verde debería cambiar de lugar entre partidas");
    }

    @Test
    void theClickIsJudgedWhereTheMarkerWasWhenThePlayerSawIt() {
        assertEquals(0, FishingMiniGame.retraso(0, 30));
        assertEquals(2, FishingMiniGame.retraso(100, 30));
        assertEquals(4, FishingMiniGame.retraso(190, 30));
        assertEquals(FishingMiniGame.MAX_RETRASO, FishingMiniGame.retraso(2000, 30));
        assertEquals(3, FishingMiniGame.retraso(2000, 4));
        assertEquals(0, FishingMiniGame.retraso(-5, 30));
    }

    @Test
    void bothLootTablesAddUpTo100AndPerfectCatchesFavourTheRareOnes() {
        int normal = 0;
        int perfecta = 0;
        int rarosNormal = 0;
        int rarosPerfecta = 0;
        for (FishingLoot.Premio premio : FishingLoot.PREMIOS) {
            normal += premio.peso();
            perfecta += premio.pesoPerfecta();
            if (premio.rareza() == FishingLoot.Rareza.RARO || premio.rareza() == FishingLoot.Rareza.EPICO) {
                rarosNormal += premio.peso();
                rarosPerfecta += premio.pesoPerfecta();
            }
        }
        assertEquals(100, normal);
        assertEquals(100, perfecta);
        assertEquals(18, rarosNormal);
        assertEquals(36, rarosPerfecta);

        Random random = new Random(3);
        for (int i = 0; i < 1000; i++) {
            assertNotNull(FishingLoot.elegir(random, false));
            assertNotNull(FishingLoot.elegir(random, true));
        }
    }

    @Test
    void onlyGoodAndPerfectCatchesCanBeSpecialAndLuckOfTheSeaHelpsTheGoodOnes() {
        assertEquals(0, FishingLoot.chanceEspecial(NORMAL, 3));
        assertEquals(40, FishingLoot.chanceEspecial(BUENA, 0));
        assertEquals(55, FishingLoot.chanceEspecial(BUENA, 3));
        assertEquals(55, FishingLoot.chanceEspecial(BUENA, 10));
        assertEquals(100, FishingLoot.chanceEspecial(PERFECTA, 0));
        assertEquals(0, FishingLoot.chanceEspecial(ESCAPO, 3));
    }
}
