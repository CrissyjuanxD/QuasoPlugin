package BloodMoon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BloodMoonCycleTest {
    @Test void theDefaultCycleStartsOnTheFifthNightAndNotDuringTheDay() {
        BloodMoonCycle cycle = new BloodMoonCycle(0, 5, -1);
        assertEquals(5, cycle.remaining(0));
        assertFalse(cycle.isDue(3, 18000));
        assertFalse(cycle.isDue(4, 11999));
        assertTrue(cycle.isDue(4, 12000));
        cycle.started(4, 5);
        assertFalse(cycle.isDue(8, 18000));
        assertTrue(cycle.isDue(9, 12000));
    }
    @Test void eveningWarningsDoNotRepeatWhenWorldTimeIsPaused() {
        BloodMoonCycle cycle = new BloodMoonCycle(0, 5, -1);
        assertFalse(cycle.shouldWarn(0, 10999));
        assertTrue(cycle.shouldWarn(0, 11000));
        for (int i = 0; i < 100; i++) assertFalse(cycle.shouldWarn(0, 12000));
        assertTrue(cycle.shouldWarn(1, 11000));
    }
    @Test void aSavedCalendarSurvivesRestartWithoutAddingAnotherInterval() {
        BloodMoonCycle cycle = new BloodMoonCycle(20, 5, 22);
        assertEquals(3, cycle.remaining(20));
        assertTrue(cycle.isDue(22, 18000));
    }
    @Test void movingTimeBackwardReschedulesInsteadOfWaitingForTheOldDate() {
        BloodMoonCycle cycle = new BloodMoonCycle(100, 5, 104);
        cycle.observe(1, 5);
        assertEquals(5, cycle.nextNight());
        assertTrue(cycle.shouldWarn(1, 11000));
    }
}
