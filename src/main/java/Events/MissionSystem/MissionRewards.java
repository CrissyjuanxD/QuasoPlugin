package Events.MissionSystem;

import imp.crissyjuanxd.QuasoPlugin;
import items.CustomPotions;
import items.EconomyItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class MissionRewards {
    private static final int COIN_SLOT = 13;
    private static final int[] LEFT = {11, 10, 12, 9, 2, 20, 1, 19, 3, 21, 0, 18};
    private static final int[] RIGHT = {15, 16, 14, 17, 6, 24, 7, 25, 5, 23, 8, 26};
    private static final List<Supplier<ItemStack>> DRINKS = List.of(
            CustomPotions::getTequila, CustomPotions::getMargarita, CustomPotions::getMezcal,
            CustomPotions::getPulque, CustomPotions::getBeer, CustomPotions::getRum,
            CustomPotions::getVodka, CustomPotions::getWhisky, CustomPotions::getSake,
            CustomPotions::getGin, CustomPotions::getAzulito, CustomPotions::getMichelada
    );

    private MissionRewards() {}

    // Cofre de 27: DinoCoins al centro, el 1er objeto a la izquierda, el 2do a la derecha y el resto botellas de XP
    public static List<ItemStack> chest(int coins, MissionDifficulty difficulty, List<List<ItemStack>> groups) {
        ItemStack[] slots = new ItemStack[27];
        ItemStack coin = EconomyItems.createVithiumCoin();
        coin.setAmount(coins);
        slots[COIN_SLOT] = coin;

        int[] next = {0, 0};
        for (int g = 0; g < groups.size(); g++) {
            int side = g % 2;
            int[] order = side == 0 ? LEFT : RIGHT;
            for (ItemStack stack : groups.get(g)) {
                if (stack == null) continue;
                for (ItemStack piece : split(stack)) {
                    int slot = -1;
                    while (slot == -1 && next[side] < order.length) {
                        int candidate = order[next[side]++];
                        if (slots[candidate] == null) slot = candidate;
                    }
                    if (slot == -1) slot = firstEmpty(slots);
                    if (slot == -1) break;
                    slots[slot] = piece;
                }
            }
        }

        List<ItemStack> result = new ArrayList<>(27);
        for (ItemStack slot : slots) {
            result.add(slot != null ? slot : new ItemStack(Material.EXPERIENCE_BOTTLE, difficulty.getXpPerSlot()));
        }
        return result;
    }

    // Cada parte puede ser un ItemStack o una lista de ItemStacks (por ejemplo varias bebidas distintas)
    @SuppressWarnings("unchecked")
    public static List<List<ItemStack>> of(Object... parts) {
        List<List<ItemStack>> groups = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof ItemStack stack) groups.add(Collections.singletonList(stack));
            else if (part instanceof Collection<?> collection) groups.add(new ArrayList<>((Collection<ItemStack>) collection));
        }
        return groups;
    }

    // Cualquier item custom del plugin por su nombre de /giveqp
    public static ItemStack custom(String id, int amount) {
        ItemStack item = QuasoPlugin.getInstance().getItemManager().getItem(id, amount, null);
        if (item == null) Bukkit.getLogger().warning("[Misiones] El item de recompensa '" + id + "' no existe.");
        return item;
    }

    public static ItemStack item(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    public static ItemStack book(Enchantment enchantment, int level) {
        return book(enchantment, level, 1);
    }

    public static ItemStack book(Enchantment enchantment, int level, int amount) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, amount);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        book.setItemMeta(meta);
        return book;
    }

    public static ItemStack potion(PotionType type, int amount) {
        ItemStack potion = new ItemStack(Material.POTION, amount);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack enchanted(Material material, Enchantment first, int firstLevel, Enchantment second, int secondLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(first, firstLevel, true);
        if (second != null) meta.addEnchant(second, secondLevel, true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack crosszyHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        // Solo con el nombre: el skin se resuelve después y no bloquea el server buscando el UUID
        meta.setPlayerProfile(Bukkit.createProfile("Crosszy"));
        meta.setDisplayName("§cCabeza de Crosszy");
        head.setItemMeta(meta);
        return head;
    }

    // Bebidas distintas del bar al azar
    public static List<ItemStack> drinks(int amount) {
        List<Supplier<ItemStack>> pool = new ArrayList<>(DRINKS);
        Collections.shuffle(pool);
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < amount; i++) result.add(pool.get(i % pool.size()).get());
        return result;
    }

    public static List<String> drinkNames() {
        List<String> names = new ArrayList<>();
        for (Supplier<ItemStack> drink : DRINKS) {
            ItemMeta meta = drink.get().getItemMeta();
            if (meta != null) names.add(org.bukkit.ChatColor.stripColor(meta.getDisplayName()));
        }
        return names;
    }

    private static List<ItemStack> split(ItemStack stack) {
        List<ItemStack> parts = new ArrayList<>();
        int max = Math.max(1, stack.getMaxStackSize());
        int amount = stack.getAmount();
        while (amount > 0) {
            ItemStack part = stack.clone();
            part.setAmount(Math.min(max, amount));
            parts.add(part);
            amount -= part.getAmount();
        }
        return parts;
    }

    private static int firstEmpty(ItemStack[] slots) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) return i;
        }
        return -1;
    }
}
