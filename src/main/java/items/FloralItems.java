package items;

import com.google.common.collect.Multimap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// El equipo floral (etapa uno): la Abeja Floral suelta la Esencia Floral, con 64 de cada drop floral, la esencia y una
// plantilla de Netherite sale la Mejora Floral, y con la mejora en la herrería las herramientas y la armadura de
// Netherite pasan a florales. Es mejor que el Netherite y peor que la armadura de Warden; con el pico floral se pueden
// romper los minerales de la Warden Cave. Las recetas están en FloralForja
public final class FloralItems {

    public static final String ROSA = "#FFB3D9";
    private static final String VERDE = "#A8E6A1";
    private static final NamespacedKey MINADO_KEY = new NamespacedKey("quasoplugin", "floral_minado");
    private static final NamespacedKey ARMADURA_KEY = new NamespacedKey("quasoplugin", "floral_armadura");

    // Las 8 piezas. Daño y velocidad son los totales que muestra el juego (null = los del Netherite); minado es lo que
    // pica más rápido que el Netherite. La armadura: armadura, dureza y vida de más de cada pieza
    public enum Pieza {
        ESPADA("espada_floral", "Espada Floral", Material.NETHERITE_SWORD, 9.0, null, 0),
        HACHA("hacha_floral", "Hacha Floral", Material.NETHERITE_AXE, 8.0, 1.2, 0.2),
        PICO("pico_floral", "Pico Floral", Material.NETHERITE_PICKAXE, null, 1.3, 0.2),
        PALA("pala_floral", "Pala Floral", Material.NETHERITE_SHOVEL, null, 1.1, 0.2),
        CASCO("casco_floral", "Casco Floral", Material.NETHERITE_HELMET, 3),
        PETO("peto_floral", "Peto Floral", Material.NETHERITE_CHESTPLATE, 8),
        PANTALON("pantalon_floral", "Pantalón Floral", Material.NETHERITE_LEGGINGS, 6),
        BOTAS("botas_floral", "Botas Florales", Material.NETHERITE_BOOTS, 3);

        public static final double DUREZA = 3.5;
        public static final double VIDA = 2;
        public static final double SIN_DESGASTE = 0.15;

        public final String id;
        public final String nombre;
        public final Material base;
        public final Double dano;
        public final Double velocidad;
        public final double minado;
        public final int armadura;

        Pieza(String id, String nombre, Material base, Double dano, Double velocidad, double minado) {
            this(id, nombre, base, dano, velocidad, minado, 0);
        }

        Pieza(String id, String nombre, Material base, int armadura) {
            this(id, nombre, base, null, null, 0, armadura);
        }

        Pieza(String id, String nombre, Material base, Double dano, Double velocidad, double minado, int armadura) {
            this.id = id;
            this.nombre = nombre;
            this.base = base;
            this.dano = dano;
            this.velocidad = velocidad;
            this.minado = minado;
            this.armadura = armadura;
        }

        public boolean esArmadura() {
            return armadura > 0;
        }

        public EquipmentSlot slot() {
            return switch (this) {
                case CASCO -> EquipmentSlot.HEAD;
                case PETO -> EquipmentSlot.CHEST;
                case PANTALON -> EquipmentSlot.LEGS;
                case BOTAS -> EquipmentSlot.FEET;
                default -> EquipmentSlot.HAND;
            };
        }
    }

    private static final Set<String> MATERIALES = Set.of("esencia_floral", "mejora_floral");

    private FloralItems() {}

    public static ItemStack createEssence() {
        return material(Material.FIREWORK_STAR, "esencia_floral", "Esencia Floral", ItemRarity.EPIC,
                "La suelta la Abeja Floral.", "Va en la Mejora Floral.");
    }

    public static ItemStack createUpgrade() {
        return material(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "mejora_floral", "Mejora Floral", ItemRarity.EPIC,
                "En la herrería: la mejora, una pieza", "de Netherite y un Bloque de Miel.",
                "Sirve para la espada, el hacha, el pico,", "la pala y las 4 piezas de armadura.");
    }

    private static ItemStack material(Material type, String id, String name, ItemRarity rarity, String... lines) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(ROSA) + "" + ChatColor.BOLD + name);
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, id);
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : lines) lore.add(ChatColor.GRAY + line);
        meta.setLore(lore);
        meta.setRarity(rarity);
        meta.setItemModel(NamespacedKey.minecraft(id));
        item.setItemMeta(meta);
        return item;
    }

    // La pieza de Netherite con los atributos florales. En la herrería se quedan sus encantamientos y su durabilidad
    public static ItemStack create(Pieza pieza) {
        ItemStack item = new ItemStack(pieza.base);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(ROSA) + "" + ChatColor.BOLD + pieza.nombre);
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, pieza.id);
        meta.setRarity(ItemRarity.EPIC);
        meta.setItemModel(NamespacedKey.minecraft(pieza.id));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#d98cb3") + "Forjada con la esencia de la Abeja Floral.");
        lore.add("");
        if (pieza.esArmadura()) armadura(meta, pieza, lore);
        else herramienta(meta, pieza, lore);
        lore.add("");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Los atributos del Netherite cambiados: el daño y la velocidad de ataque nuevos, y el minado más rápido
    private static void herramienta(ItemMeta meta, Pieza pieza, List<String> lore) {
        Multimap<Attribute, AttributeModifier> defaults = pieza.base.getDefaultAttributeModifiers(EquipmentSlot.HAND);
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            AttributeModifier modifier = entry.getValue();
            // El jugador pega 1 y ataca 4 veces por segundo sin nada en la mano: el modificador es el total menos eso
            if (entry.getKey().equals(Attribute.ATTACK_DAMAGE) && pieza.dano != null) {
                modifier = new AttributeModifier(modifier.getKey(), pieza.dano - 1, modifier.getOperation(), modifier.getSlotGroup());
            } else if (entry.getKey().equals(Attribute.ATTACK_SPEED) && pieza.velocidad != null) {
                modifier = new AttributeModifier(modifier.getKey(), pieza.velocidad - 4, modifier.getOperation(), modifier.getSlotGroup());
            }
            meta.addAttributeModifier(entry.getKey(), modifier);
        }
        if (pieza.minado > 0) {
            meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(MINADO_KEY, pieza.minado,
                    AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.MAINHAND));
        }

        if (pieza == Pieza.ESPADA) lore.add(punto() + ChatColor.of(VERDE) + "9 de daño (1 más que Netherite)");
        if (pieza == Pieza.HACHA) lore.add(punto() + ChatColor.of(VERDE) + "8 de daño y ataca más rápido");
        if (pieza.minado > 0) lore.add(punto() + ChatColor.of(VERDE) + "Pica " + Math.round(pieza.minado * 100) + "% más rápido que Netherite");
        if (pieza == Pieza.PICO) lore.add(punto() + ChatColor.of(VERDE) + "Rompe los minerales de la Warden Cave");
    }

    private static void armadura(ItemMeta meta, Pieza pieza, List<String> lore) {
        EquipmentSlotGroup grupo = pieza.slot().getGroup();
        String nombre = pieza.name().toLowerCase(java.util.Locale.ROOT);
        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(new NamespacedKey("quasoplugin", "floral_" + nombre + "_armor"),
                pieza.armadura, AttributeModifier.Operation.ADD_NUMBER, grupo));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(new NamespacedKey("quasoplugin", "floral_" + nombre + "_toughness"),
                Pieza.DUREZA, AttributeModifier.Operation.ADD_NUMBER, grupo));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(new NamespacedKey("quasoplugin", "floral_" + nombre + "_knockback"),
                0.1, AttributeModifier.Operation.ADD_NUMBER, grupo));
        meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(new NamespacedKey("quasoplugin", "floral_" + nombre + "_health"),
                Pieza.VIDA, AttributeModifier.Operation.ADD_NUMBER, grupo));
        meta.getPersistentDataContainer().set(ARMADURA_KEY, PersistentDataType.BYTE, (byte) 1);

        EquippableComponent equipo = meta.getEquippable();
        equipo.setSlot(pieza.slot());
        equipo.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);
        equipo.setModel(NamespacedKey.minecraft("floral"));
        meta.setEquippable(equipo);

        lore.add(ChatColor.GRAY + "Equipar esta pieza otorga:");
        lore.add(punto() + ChatColor.of(VERDE) + ChatColor.BOLD + "+1 Corazón");
        lore.add(punto() + ChatColor.of(VERDE) + ChatColor.BOLD + "15% Resistencia al Desgaste");
    }

    private static String punto() {
        return ChatColor.of(ROSA) + "■ ";
    }

    public static Pieza piezaPorId(String id) {
        if (id == null) return null;
        for (Pieza pieza : Pieza.values()) {
            if (pieza.id.equals(id)) return pieza;
        }
        return null;
    }

    public static Pieza pieza(ItemStack item) {
        return piezaPorId(EndItems.idOf(item));
    }

    public static boolean isArmor(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ARMADURA_KEY, PersistentDataType.BYTE);
    }

    // La esencia, la mejora y los drops florales: no entran en recetas vanilla (WardenCaveItemGuard)
    public static boolean isMaterial(ItemStack item) {
        String id = EndItems.idOf(item);
        return (id != null && MATERIALES.contains(id)) || FloralMobItems.dropId(item) != null;
    }
}
