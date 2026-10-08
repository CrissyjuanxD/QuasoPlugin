package items;

import com.google.common.collect.Multimap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Items del End: Cristal de Celestita (racimos de las geodas del Bosque Prismático), Fragmento Astral (mobs del End),
// Esencia Marchita (shulkers negros y wither skeletons del Páramo), el Ojo del Rey Ender que se craftea con los tres,
// el Lingote y la Plantilla de Celestita, las 6 herramientas de Celestita y la EnderKing Pearl del Rey Ender.
// Las recetas están en ThreeChanges
public final class EndItems {

    // La misma PDC "custom_item" que leen las misiones (MissionUtils.customId)
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("quasoplugin", "custom_item");
    private static final NamespacedKey DAMAGE_KEY = new NamespacedKey("quasoplugin", "celestita_dano");

    private static final String CELESTE = "#8fd3ff";

    // Las herramientas de Celestita: la de Netherite con 1 de daño más, y la espada con 12 (4 más). La espada le pega
    // el doble a los mobs del End y al Rey Ender; el hacha y la lanza, 50% más al Rey Ender
    public enum Tool {
        ESPADA("espada_celestita", "Espada de Celestita", Material.NETHERITE_SWORD, true, 4),
        HACHA("hacha_celestita", "Hacha de Celestita", Material.NETHERITE_AXE, true, 1),
        LANZA("lanza_celestita", "Lanza de Celestita", Material.NETHERITE_SPEAR, true, 1),
        PICO("pico_celestita", "Pico de Celestita", Material.NETHERITE_PICKAXE, false, 1),
        PALA("pala_celestita", "Pala de Celestita", Material.NETHERITE_SHOVEL, false, 1),
        AZADA("azada_celestita", "Azada de Celestita", Material.NETHERITE_HOE, false, 1);

        public final String id;
        public final String name;
        public final Material base;
        public final boolean weapon;
        // Daño de más sobre la de Netherite
        public final int extra;

        Tool(String id, String name, Material base, boolean weapon, int extra) {
            this.id = id;
            this.name = name;
            this.base = base;
            this.weapon = weapon;
            this.extra = extra;
        }
    }

    private static final Set<String> MATERIALS = Set.of("cristal_celestita", "fragmento_astral", "esencia_marchita",
            "ojo_rey_ender", "lingote_celestita", "plantilla_celestita", "enderking_pearl");

    private EndItems() {}

    public static ItemStack createCelestiteCrystal(int amount) {
        return item(Material.PRISMARINE_CRYSTALS, amount, "cristal_celestita", CELESTE, "Cristal de Celestita", ItemRarity.RARE,
                "Sale de los racimos de amatista", "de las geodas del Bosque Prismático.");
    }

    public static ItemStack createAstralFragment(int amount) {
        return item(Material.FIREWORK_STAR, amount, "fragmento_astral", "#c58cff", "Fragmento Astral", ItemRarity.RARE,
                "Lo sueltan los mobs del End.", "Va en la Plantilla de Celestita.");
    }

    public static ItemStack createWitheredEssence(int amount) {
        return item(Material.BLACK_DYE, amount, "esencia_marchita", "#9a8fb0", "Esencia Marchita", ItemRarity.UNCOMMON,
                "La sueltan los Shulkers Negros y los", "Wither Skeletons del Páramo Marchito.");
    }

    public static ItemStack createKingEye() {
        return item(Material.ENDER_EYE, 1, "ojo_rey_ender", "#d36bff", "Ojo del Rey Ender", ItemRarity.EPIC,
                "Úsalo en el altar de un Santuario", "Marchito para invocar al Rey Ender.");
    }

    public static ItemStack createCelestiteIngot() {
        return item(Material.COPPER_INGOT, 1, "lingote_celestita", CELESTE, "Lingote de Celestita", ItemRarity.RARE,
                "Mejora las herramientas de Netherite", "junto con la Plantilla de Celestita.");
    }

    public static ItemStack createCelestiteTemplate() {
        return item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, "plantilla_celestita", CELESTE, "Plantilla de Celestita",
                ItemRarity.EPIC, "En la mesa de herrería: plantilla,", "herramienta de Netherite y", "1 Lingote de Celestita.");
    }

    public static ItemStack createEnderKingPearl() {
        return item(Material.HEART_OF_THE_SEA, 1, "enderking_pearl", "#b55cff", "EnderKing Pearl", ItemRarity.EPIC,
                "La suelta el Rey Ender.", "En la mesa de herrería, con el Peto", "de Warden y unas Elytras, hace el", "Peto de Warden Alado.");
    }

    // La herramienta de Netherite con su daño de siempre más el extra (se cambia el modificador base para que el tooltip
    // lo sume)
    public static ItemStack createTool(Tool tool) {
        ItemStack item = new ItemStack(tool.base);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(CELESTE) + "" + ChatColor.BOLD + tool.name);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, tool.id);
        meta.setRarity(ItemRarity.EPIC);
        meta.setItemModel(NamespacedKey.minecraft(tool.id));

        Multimap<Attribute, AttributeModifier> defaults = tool.base.getDefaultAttributeModifiers(EquipmentSlot.HAND);
        boolean raised = false;
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            AttributeModifier modifier = entry.getValue();
            if (entry.getKey().equals(Attribute.ATTACK_DAMAGE) && !raised) {
                modifier = new AttributeModifier(modifier.getKey(), modifier.getAmount() + tool.extra, modifier.getOperation(), modifier.getSlotGroup());
                raised = true;
            }
            meta.addAttributeModifier(entry.getKey(), modifier);
        }
        if (!raised) {
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(DAMAGE_KEY, tool.extra,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#5fa8d3") + "Forjada con los cristales del End.");
        lore.add("");
        lore.add(ChatColor.of(CELESTE) + "■ " + ChatColor.AQUA + "+" + tool.extra + " de daño sobre Netherite");
        if (tool == Tool.ESPADA) {
            lore.add(ChatColor.of(CELESTE) + "■ " + ChatColor.LIGHT_PURPLE + "Doble de daño a los mobs del End");
            lore.add(ChatColor.of(CELESTE) + "■ " + ChatColor.LIGHT_PURPLE + "Doble de daño al Rey Ender");
        } else if (tool.weapon) {
            lore.add(ChatColor.of(CELESTE) + "■ " + ChatColor.LIGHT_PURPLE + "+50% de daño al Rey Ender");
        }
        if (tool == Tool.PICO) lore.add(ChatColor.of(CELESTE) + "■ " + ChatColor.LIGHT_PURPLE + "18% de Cristal en los racimos");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static Tool toolById(String id) {
        if (id == null) return null;
        for (Tool tool : Tool.values()) {
            if (tool.id.equals(id)) return tool;
        }
        return null;
    }

    private static ItemStack item(Material type, int amount, String id, String color, String name, ItemRarity rarity, String... lines) {
        ItemStack item = new ItemStack(type, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(color) + name);
        meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, id);
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        meta.setLore(lore);
        meta.setRarity(rarity);
        meta.setItemModel(NamespacedKey.minecraft(id));
        item.setItemMeta(meta);
        return item;
    }

    public static String idOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING);
    }

    // Los materiales del End (no las herramientas): no entran en recetas vanilla
    public static boolean isEndItem(ItemStack item) {
        return isMaterial(idOf(item));
    }

    public static boolean isMaterial(String id) {
        return id != null && MATERIALS.contains(id);
    }

    public static boolean isCelestiteSword(ItemStack item) {
        return toolById(idOf(item)) == Tool.ESPADA;
    }

    public static boolean isCelestiteWeapon(ItemStack item) {
        Tool tool = toolById(idOf(item));
        return tool != null && tool.weapon;
    }
}
