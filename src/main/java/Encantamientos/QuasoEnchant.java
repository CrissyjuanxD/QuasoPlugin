package Encantamientos;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Los encantamientos del datapack (data/quaso/enchantment): 3 de la Warden Cave y 2 del End. No salen en la mesa de
// encantamientos ni con aldeanos, solo en libros que sueltan los jefes, los mobs y los cofres (EnchantDrops)
public enum QuasoEnchant {
    PASO_IGNEO("paso_igneo", 2, Group.WARDEN_CAVE,
            "Caminas sobre la lava: se vuelve obsidiana,",
            "después obsidiana llorosa y otra vez lava."),
    PURIFICACION("purificacion", 5, Group.WARDEN_CAVE,
            "Más daño a los mobs de la Warden Cave",
            "y a los Warden (+2.5 por nivel)."),
    VISION_ABISAL("vision_abisal", 1, Group.WARDEN_CAVE,
            "Te protege de la Oscuridad 5 minutos;",
            "después se recarga 2 minutos y vuelve."),
    ANCLAJE("anclaje", 2, Group.END,
            "Resistes el empuje. I: la levitación dura la mitad",
            "y la Ender Spider a veces no te tepea. II: inmune."),
    RETORNO_DEL_VACIO("retorno_del_vacio", 1, Group.END,
            "Si caes al vacío del End vuelves al último",
            "suelo firme. Se recarga en 5 minutos.");

    public enum Group { WARDEN_CAVE, END }

    private final NamespacedKey key;
    // El mismo max_level del JSON
    private final int maxLevel;
    private final Group group;
    private final String[] description;
    private Enchantment cached;

    QuasoEnchant(String id, int maxLevel, Group group, String... description) {
        this.key = new NamespacedKey("quaso", id);
        this.maxLevel = maxLevel;
        this.group = group;
        this.description = description;
    }

    // El encantamiento del registro, o null si el datapack todavía no se cargó (hace falta reiniciar)
    public Enchantment get() {
        if (cached == null) cached = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
        return cached;
    }

    public int level(ItemStack item) {
        Enchantment enchantment = get();
        if (item == null || enchantment == null || item.getType().isAir()) return 0;
        return item.getEnchantmentLevel(enchantment);
    }

    public ItemStack book(int level) {
        Enchantment enchantment = get();
        if (enchantment == null) return null;
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(enchantment, Math.max(1, Math.min(level, maxLevel)), true);
        String color = group == Group.WARDEN_CAVE ? "#7FE3E6" : "#D3B1F0";
        List<String> lore = new ArrayList<>();
        for (String line : description) lore.add(ChatColor.of(color) + line);
        meta.setLore(lore);
        items.ItemModels.apply(meta, "libro_" + key.getKey());
        book.setItemMeta(meta);
        return book;
    }

    public static boolean allLoaded() {
        for (QuasoEnchant enchant : values()) {
            if (enchant.get() == null) return false;
        }
        return true;
    }

    // Libro al azar de ese grupo. Sale en nivel 1 y cada nivel más tiene la probabilidad upgrade
    public static ItemStack randomBook(Group group, Random random, double upgrade) {
        List<QuasoEnchant> options = new ArrayList<>();
        for (QuasoEnchant enchant : values()) {
            if (enchant.group == group && enchant.get() != null) options.add(enchant);
        }
        if (options.isEmpty()) return null;
        QuasoEnchant enchant = options.get(random.nextInt(options.size()));
        int level = 1;
        while (level < enchant.maxLevel && random.nextDouble() < upgrade) level++;
        return enchant.book(level);
    }

    // Para /giveqp: libro_<id>_<nivel>, por ejemplo libro_paso_igneo_2
    public static ItemStack fromCommand(String name) {
        for (QuasoEnchant enchant : values()) {
            String prefix = "libro_" + enchant.key.getKey() + "_";
            if (!name.startsWith(prefix)) continue;
            try {
                return enchant.book(Integer.parseInt(name.substring(prefix.length())));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static List<String> commandNames() {
        List<String> names = new ArrayList<>();
        for (QuasoEnchant enchant : values()) {
            for (int level = 1; level <= enchant.maxLevel; level++) names.add("libro_" + enchant.key.getKey() + "_" + level);
        }
        return names;
    }
}
