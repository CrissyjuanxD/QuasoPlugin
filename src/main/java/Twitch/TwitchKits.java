package Twitch;

import Dificultades.OneChanges;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Los kits del canal. Sub: mes 1 hierro con Protección II y 25 DinoCoins, mes 2 diamante con Protección III, arco
// (Infinidad y Poder II), espada con Filo II y 40, desde el mes 3 Netherite con Protección IV, arco (Infinidad y
// Poder III), espada de Netherite con Filo IV y 64; todos con manzanas encantadas y de oro, carne corrupta, tarta de
// calabaza mejorada y una mochila. VIP: 15 DinoCoins, manzanas encantadas y de oro, tarta y Mochila Nivel 1
final class TwitchKits {

    enum Kit {
        SUB_1("sub1", "Kit de Sub · Mes 1", "#B57BFF", 25, "IRON", 2, 4, 8, 15, 15, null, 0, 0, 1),
        SUB_2("sub2", "Kit de Sub · Mes 2", "#9146FF", 40, "DIAMOND", 3, 7, 20, 25, 25, "DIAMOND_SWORD", 2, 2, 2),
        SUB_3("sub3", "Kit de Sub · Mes 3+", "#6A1BE0", 64, "NETHERITE", 4, 10, 32, 35, 35, "NETHERITE_SWORD", 4, 3, 3),
        VIP("vip", "Kit VIP", "#E005B9", 15, null, 0, 2, 6, 0, 12, null, 0, 0, 1);

        final String id;
        final String title;
        final String color;
        final int coins;
        final String armor;
        final int protection;
        final int enchantedApples;
        final int apples;
        final int steak;
        final int pie;
        final String sword;
        final int sharpness;
        // Nivel de Poder del arco con Infinidad (0 = sin arco)
        final int bowPower;
        final int backpack;

        Kit(String id, String title, String color, int coins, String armor, int protection, int enchantedApples, int apples,
            int steak, int pie, String sword, int sharpness, int bowPower, int backpack) {
            this.id = id;
            this.title = title;
            this.color = color;
            this.coins = coins;
            this.armor = armor;
            this.protection = protection;
            this.enchantedApples = enchantedApples;
            this.apples = apples;
            this.steak = steak;
            this.pie = pie;
            this.sword = sword;
            this.sharpness = sharpness;
            this.bowPower = bowPower;
            this.backpack = backpack;
        }

        String colored() {
            return ChatColor.of(color) + title;
        }

        static Kit byId(String id) {
            for (Kit kit : values()) {
                if (kit.id.equalsIgnoreCase(id)) return kit;
            }
            return null;
        }
    }

    private TwitchKits() {}

    // El kit de sub que toca según cuántos cobró esa cuenta de Twitch
    static Kit subKit(int claimedBefore) {
        if (claimedBefore <= 0) return Kit.SUB_1;
        if (claimedBefore == 1) return Kit.SUB_2;
        return Kit.SUB_3;
    }

    // Cofre de 27: la armadura arriba, las DinoCoins en el centro con la comida a los lados y abajo la espada, la
    // mochila y el arco (con 1 flecha, que Infinidad la necesita para disparar)
    static ItemStack[] contents(Kit kit) {
        ItemStack[] slots = new ItemStack[27];
        if (kit.armor != null) {
            String[] pieces = {"_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS"};
            int[] armorSlots = {1, 3, 5, 7};
            for (int i = 0; i < pieces.length; i++) {
                slots[armorSlots[i]] = enchanted(Material.valueOf(kit.armor + pieces[i]), Enchantment.PROTECTION, kit.protection);
            }
        }
        slots[10] = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, kit.enchantedApples);
        slots[11] = new ItemStack(Material.GOLDEN_APPLE, kit.apples);
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(kit.coins);
        slots[13] = coins;
        if (kit.steak > 0) {
            ItemStack steak = OneChanges.corruptedSteak();
            steak.setAmount(kit.steak);
            slots[15] = steak;
        }
        ItemStack pie = OneChanges.improvedPumpkinPie();
        pie.setAmount(kit.pie);
        slots[kit.steak > 0 ? 16 : 15] = pie;
        if (kit.sword != null) slots[20] = enchanted(Material.valueOf(kit.sword), Enchantment.SHARPNESS, kit.sharpness);
        slots[22] = backpack(kit.backpack);
        if (kit.bowPower > 0) {
            ItemStack bow = enchanted(Material.BOW, Enchantment.POWER, kit.bowPower);
            bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
            slots[24] = bow;
            slots[25] = new ItemStack(Material.ARROW);
        }
        return slots;
    }

    static ItemStack chest(Kit kit, String playerName) {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.setDisplayName(kit.colored());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.of("#D3D3D3") + "Del canal de Crosszy para " + ChatColor.WHITE + playerName);
        lore.add(ChatColor.of("#A0A0A0") + new SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(new Date()));
        meta.setLore(lore);
        if (meta instanceof BlockStateMeta stateMeta && stateMeta.getBlockState() instanceof Chest state) {
            ItemStack[] contents = contents(kit);
            for (int i = 0; i < contents.length; i++) state.getInventory().setItem(i, contents[i]);
            stateMeta.setBlockState(state);
        }
        chest.setItemMeta(meta);
        return chest;
    }

    // Igual que las recompensas de las misiones: si no hay lugar el cofre cae a los pies
    static void give(Player player, ItemStack chest) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(chest);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(ChatColor.of("#FFA07A") + "Tu inventario estaba lleno: el cofre quedó en el suelo.");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.4f);
    }

    private static ItemStack enchanted(Material type, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(type);
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }

    private static ItemStack backpack(int level) {
        return switch (level) {
            case 2 -> EconomyItems.createGreenMochila();
            case 3 -> EconomyItems.createRedMochila();
            default -> EconomyItems.createNormalMochila();
        };
    }
}
