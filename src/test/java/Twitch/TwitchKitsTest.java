package Twitch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwitchKitsTest {

    // Los kits como están en la lista del canal
    @Test
    void theKitsMatchTheChannelList() {
        kit(TwitchKits.Kit.SUB_1, 25, 4, 8, 15, 15, "IRON", 2, null, 0, 0, 1);
        kit(TwitchKits.Kit.SUB_2, 40, 7, 20, 25, 25, "DIAMOND", 3, "DIAMOND_SWORD", 2, 2, 2);
        kit(TwitchKits.Kit.SUB_3, 64, 10, 32, 35, 35, "NETHERITE", 4, "NETHERITE_SWORD", 4, 3, 3);
        kit(TwitchKits.Kit.VIP, 15, 2, 6, 0, 12, null, 0, null, 0, 0, 1);
        assertEquals(TwitchKits.Kit.SUB_1, TwitchKits.subKit(0));
        assertEquals(TwitchKits.Kit.SUB_2, TwitchKits.subKit(1));
        assertEquals(TwitchKits.Kit.SUB_3, TwitchKits.subKit(5));
    }

    private static void kit(TwitchKits.Kit kit, int coins, int enchanted, int apples, int steak, int pie, String armor,
                            int protection, String sword, int sharpness, int bowPower, int backpack) {
        assertEquals(coins, kit.coins, kit.id);
        assertEquals(enchanted, kit.enchantedApples, kit.id);
        assertEquals(apples, kit.apples, kit.id);
        assertEquals(steak, kit.steak, kit.id);
        assertEquals(pie, kit.pie, kit.id);
        assertEquals(armor, kit.armor, kit.id);
        assertEquals(protection, kit.protection, kit.id);
        assertEquals(sword, kit.sword, kit.id);
        assertEquals(sharpness, kit.sharpness, kit.id);
        assertEquals(bowPower, kit.bowPower, kit.id);
        assertEquals(backpack, kit.backpack, kit.id);
    }
}
