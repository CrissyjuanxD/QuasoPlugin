package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WardenCaveItems {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey("quasoplugin", "warden_cave_item");
    // Lo que tarda el mineral crudo en el alto horno
    public static final int MINUTOS_FUNDICION = 3;
    private static final int MINUTOS_FUNDICION_VIEJO = 4;

    private final JavaPlugin plugin;

    public WardenCaveItems(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Cada bioma de la Warden Cave tiene su mineral y su fragmento, y cada uno va en la mejora de una pieza
    public enum Variant {
        CIAN("Cian", "cian", "#3ad4d4", "de la Caverna Sculk", "Casco", 705, 709),
        VERDE("Verde", "verde", "#5fd16a", "del Pantano Profundo", "Botas", 706, 710),
        MORADO("Morado", "morado", "#b46be0", "del Abismo Flotante", "Peto", 707, 711),
        GRIS("Gris", "gris", "#a7a7b0", "de las Ruinas de Ceniza", "Pantalón", 708, 712);

        private final String displayName;
        private final String id;
        private final String color;
        private final String biome;
        private final String piece;
        private final int rawModelData;
        private final int fragmentModelData;

        Variant(String displayName, String id, String color, String biome, String piece,
                int rawModelData, int fragmentModelData) {
            this.displayName = displayName;
            this.id = id;
            this.color = color;
            this.biome = biome;
            this.piece = piece;
            this.rawModelData = rawModelData;
            this.fragmentModelData = fragmentModelData;
        }

        public String displayName() {
            return displayName;
        }

        public String color() {
            return color;
        }

        // "de la Caverna Sculk", "del Pantano Profundo"...
        public String biome() {
            return biome;
        }

        public String rawId() {
            return "mineral_crudo_" + id;
        }

        public String fragmentId() {
            return "fragmento_profundo_" + id;
        }

        public String ingotId() {
            return "lingote_profundo_" + id;
        }

        // "Casco", "Botas"...: la pieza de Warden que sale de este bioma
        public String piece() {
            return piece;
        }
    }

    public static final String TEJIDO = "tejido_sculk";
    public static final String NUCLEO = "nucleo_sonico";
    // Fragmentos de su color que lleva un Lingote Profundo de bioma; la pieza de Warden pide 5 lingotes (40 minerales)
    public static final int FRAGMENTOS_POR_LINGOTE = 8;
    public static final int LINGOTES_POR_PIEZA = 5;

    // Marca el item con su id para reconocerlo aunque cambien el nombre o el lore
    private static void mark(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        meta.setItemModel(NamespacedKey.minecraft(id));
    }

    public static boolean isWardenCaveItem(ItemStack item) {
        return idOf(item) != null;
    }

    public static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    // Por debajo es un fragmento de prismarina (no se funde en ningún horno vanilla) con otra textura;
    // así solo entra en la receta del alto horno de la etapa 2
    public static ItemStack createRawOre(Variant variant) {
        return createRawOre(variant, MINUTOS_FUNDICION);
    }

    // El mineral como era antes (4 minutos en el lore): el alto horno lo sigue aceptando
    public static ItemStack createOldRawOre(Variant variant) {
        return createRawOre(variant, MINUTOS_FUNDICION_VIEJO);
    }

    public static Variant rawVariant(ItemStack item) {
        String id = idOf(item);
        if (id == null) return null;
        for (Variant variant : Variant.values()) {
            if (variant.rawId().equals(id)) return variant;
        }
        return null;
    }

    // Un mineral crudo con el lore viejo pasa al de ahora (así se apila con los nuevos). Devuelve si lo cambió
    public static boolean updateRawOreLore(ItemStack item) {
        Variant variant = rawVariant(item);
        if (variant == null) return false;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = createRawOre(variant).getItemMeta().getLore();
        if (Objects.equals(meta.getLore(), lore)) return false;
        meta.setLore(lore);
        item.setItemMeta(meta);
        return true;
    }

    private static ItemStack createRawOre(Variant variant, int minutos) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(variant.color) + "" + ChatColor.BOLD + "Mineral Crudo " + variant.displayName);
            meta.setCustomModelData(variant.rawModelData);
            mark(meta, variant.rawId());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Roca infectada sacada de las vetas");
            lore.add(ChatColor.of(variant.color) + variant.biome + ChatColor.GRAY + ".");
            lore.add("");
            lore.add(ChatColor.GRAY + "Se funde en el " + ChatColor.WHITE + "alto horno" + ChatColor.GRAY + " (" + minutos + " minutos).");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.UNCOMMON);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createFragment(Variant variant) {
        ItemStack item = new ItemStack(Material.COPPER_NUGGET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(variant.color) + "" + ChatColor.BOLD + "Fragmento Profundo " + variant.displayName);
            meta.setCustomModelData(variant.fragmentModelData);
            mark(meta, variant.fragmentId());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Fundido de las vetas");
            lore.add(ChatColor.of(variant.color) + variant.biome + ChatColor.GRAY + ".");
            lore.add("");
            lore.add(ChatColor.GRAY + "Va en los " + ChatColor.WHITE + "Lingotes Profundos" + ChatColor.GRAY + " y en la mejora de:");
            lore.add(ChatColor.of(variant.color) + "> " + variant.piece + " de Warden");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.RARE);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static List<ItemStack> allFragments() {
        List<ItemStack> fragments = new ArrayList<>();
        for (Variant variant : Variant.values()) {
            fragments.add(createFragment(variant));
        }
        return fragments;
    }

    public static ItemStack createWardenEnergy() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Energía de Warden");
            meta.setCustomModelData(700);
            mark(meta, "energia_warden");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Un eco cristalizado que aún vibra");
            lore.add(ChatColor.of("#008b8b") + "con la acústica de la oscuridad.");
            lore.add(ChatColor.of("#006666") + "Contiene la esencia pura de las");
            lore.add(ChatColor.of("#006666") + "profundidades de las Warden Caves.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Lingote Profundo de un bioma: 8 Fragmentos de su color y 1 lingote de oro. Va en la mejora de la pieza de su bioma
    // y en el Lingote de Celestita
    public static ItemStack createColoredIngot(Variant variant) {
        ItemStack item = new ItemStack(Material.COPPER_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(variant.color) + "" + ChatColor.BOLD + "Lingote Profundo " + variant.displayName);
        mark(meta, variant.ingotId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Forjado con los fragmentos");
        lore.add(ChatColor.of(variant.color) + variant.biome + ChatColor.GRAY + ".");
        lore.add("");
        lore.add(ChatColor.GRAY + "Va en la mejora de:");
        lore.add(ChatColor.of(variant.color) + "> " + variant.piece + " de Warden");
        lore.add(ChatColor.GRAY + "Y en el " + ChatColor.WHITE + "Lingote de Celestita" + ChatColor.GRAY + ".");
        lore.add("");
        meta.setLore(lore);
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static Variant ingotVariant(ItemStack item) {
        String id = idOf(item);
        if (id == null) return null;
        for (Variant variant : Variant.values()) {
            if (variant.ingotId().equals(id)) return variant;
        }
        return null;
    }

    // Todos los Lingotes Profundos: los 4 de bioma y el de antes (el de cualquier color), que sigue sirviendo para la
    // Celestita
    public static List<ItemStack> allDeepIngots() {
        List<ItemStack> ingots = new ArrayList<>();
        for (Variant variant : Variant.values()) ingots.add(createColoredIngot(variant));
        ingots.add(createDeepIngot());
        return ingots;
    }

    public static boolean isDeepIngot(ItemStack item) {
        return ingotVariant(item) != null || "lingote_profundo".equals(idOf(item));
    }

    // Lo suelta el 40% de los mobs de la Warden Cave; va en la Energía de Warden
    public static ItemStack createSculkTissue(int amount) {
        ItemStack item = new ItemStack(Material.RABBIT_HIDE, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#1fa3a3") + "" + ChatColor.BOLD + "Tejido Sculk");
        mark(meta, TEJIDO);
        meta.setLore(List.of(
                "",
                ChatColor.of("#147a7a") + "Restos de sculk que dejan",
                ChatColor.of("#147a7a") + "los mobs de la Warden Cave.",
                "",
                ChatColor.GRAY + "Va en la " + ChatColor.of("#00ffff") + "Energía de Warden" + ChatColor.GRAY + ".",
                ""));
        meta.setRarity(ItemRarity.UNCOMMON);
        item.setItemMeta(meta);
        return item;
    }

    // Lo suelta el Ultra Warden (1 o 2 a cada jugador que le pegó): sin él no hay armadura de Warden
    public static ItemStack createSonicCore(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#29DFEB") + "" + ChatColor.BOLD + "Núcleo Sónico");
        mark(meta, NUCLEO);
        meta.setEnchantmentGlintOverride(false);
        meta.setLore(List.of(
                "",
                ChatColor.of("#0BB5B5") + "El eco que todavía late",
                ChatColor.of("#0BB5B5") + "en el pecho del Ultra Warden.",
                "",
                ChatColor.GRAY + "Va en:",
                ChatColor.of("#29DFEB") + "> " + ChatColor.of("#B8F4F7") + "Las mejoras de la armadura de Warden",
                ChatColor.of("#29DFEB") + "> " + ChatColor.of("#B8F4F7") + "La Warden Gun",
                ChatColor.of("#29DFEB") + "> " + ChatColor.of("#B8F4F7") + "El Arco de Sculk",
                ChatColor.of("#29DFEB") + "> " + ChatColor.of("#B8F4F7") + "Las Botas de Warden Escaladoras",
                ""));
        meta.setRarity(ItemRarity.EPIC);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isSonicCore(ItemStack item) {
        return NUCLEO.equals(idOf(item));
    }

    public static ItemStack createDeepIngot() {
        ItemStack item = new ItemStack(Material.COPPER_INGOT);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00e6e6") + "" + ChatColor.BOLD + "Lingote Profundo");
            meta.setCustomModelData(703);
            mark(meta, "lingote_profundo");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Una aleación pesada forjada bajo la");
            lore.add(ChatColor.of("#008b8b") + "presión del vacío y la oscuridad extrema.");
            lore.add(ChatColor.of("#006666") + "Irradia un frío penetrante característico");
            lore.add(ChatColor.of("#006666") + "del subsuelo más profundo.");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createWardenBossHeart() {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Corazón de Warden Boss");
            meta.setCustomModelData(704);
            mark(meta, "corazon_warden_boss");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#008b8b") + "Palpita con una furia incontrolable y");
            lore.add(ChatColor.of("#008b8b") + "una energía oscura inmensurable.");
            lore.add(ChatColor.of("#006666") + "El núcleo vital que alguna vez gobernó");
            lore.add(ChatColor.of("#006666") + "las temibles Warden Caves.");
            lore.add("");
            lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Se obtiene al derrotar al gigantesco");
            lore.add(ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Infested Warden Boss.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Click derecho en el " + ChatColor.of("#29DFEB") + "Altar del Ultra Warden");
            lore.add(ChatColor.GRAY + "para invocar al " + ChatColor.of("#29DFEB") + "Ultra Warden" + ChatColor.GRAY + ".");
            lore.add("");

            meta.setLore(lore);
            meta.setRarity(ItemRarity.EPIC);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Frutas de los árboles de la Warden Cave (las suelta WardenFruits): Baya Sculk de los froglights verdes,
    // Fruta Abisal de los perlados y Baya Regenerativa de las enredaderas del Pantano
    public static ItemStack createSculkBerry(int amount) {
        return fruit(Material.SWEET_BERRIES, amount, "baya_sculk", "#3ad4d4", "Baya Sculk",
                "Visión nocturna por 3 minutos.", "Crece en los árboles de la Caverna Sculk.");
    }

    public static ItemStack createAbyssFruit(int amount) {
        return fruit(Material.BEETROOT, amount, "fruta_abisal", "#b46be0", "Fruta Abisal",
                "Te quita la Oscuridad y no te la", "vuelven a poner por 1 minuto y medio.");
    }

    public static ItemStack createGlowBerry(int amount) {
        return fruit(Material.GLOW_BERRIES, amount, "baya_luminosa", "#ed6b78", "Baya Regenerativa",
                "Regeneración II por 5 segundos.", "Cuelga de los árboles del Pantano Profundo.");
    }

    public static boolean isFruit(ItemStack item) {
        String id = idOf(item);
        return "baya_sculk".equals(id) || "fruta_abisal".equals(id) || "baya_luminosa".equals(id);
    }

    private static ItemStack fruit(Material type, int amount, String id, String color, String name, String... lines) {
        ItemStack item = new ItemStack(type, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(color) + name);
        mark(meta, id);
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
