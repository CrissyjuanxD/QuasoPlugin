package Habilidades;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HabilidadesModeloTest {

    @Test
    void eachLevelHasItsOwnOnAndOffModel() {
        assertEquals("habilidad_vitalidad_1_on", HabilidadesGUI.modelo(HabilidadesType.VITALIDAD, 1, true));
        assertEquals("habilidad_vitalidad_4_off", HabilidadesGUI.modelo(HabilidadesType.VITALIDAD, 4, false));
        assertEquals("habilidad_agilidad_5_off", HabilidadesGUI.modelo(HabilidadesType.AGILIDAD, 5, false));
        assertEquals("habilidad_resistencia_8_on", HabilidadesGUI.modelo(HabilidadesType.RESISTENCIA, 8, true));
    }
}
