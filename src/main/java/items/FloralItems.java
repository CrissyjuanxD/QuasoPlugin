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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// El equipo floral (etapa uno): la Abeja Floral suelta la Esencia Floral, con 16 de cada drop floral, la esencia y una
// plantilla de Netherite sale la Mejora Floral, y con la mejora en la herrería las herramientas, la armadura de
// Netherite y el escudo pasan a florales. Es el paso entre el Netherite y la Celestita o la armadura de Warden (esas solo
// salen de una pieza floral); con el pico floral se pueden romper los minerales de la Warden Cave. Las recetas están en
// FloralForja
public final class FloralItems {

    public static final String ROSA = "#FFB3D9";
    private static final NamespacedKey MINADO_KEY = new NamespacedKey("quasoplugin", "floral_minado");
    private static final NamespacedKey ARMADURA_KEY = new NamespacedKey("quasoplugin", "floral_armadura");

    // Las 11 piezas. Daño y velocidad son los totales que muestra el juego (null = los del Netherite); minado es lo que
    // pica más rápido que el Netherite. La armadura: armadura, dureza y vida de más de cada pieza
    public enum Pieza {
        ESPADA("espada_floral", "Espada Floral", Material.NETHERITE_SWORD, 9.0, null, 0),
        HACHA("hacha_floral", "Hacha Floral", Material.NETHERITE_AXE, 8.0, 1.2, 0.2),
        LANZA("lanza_floral", "Lanza Floral", Material.NETHERITE_SPEAR, 5.5, null, 0),
        PICO("pico_floral", "Pico Floral", Material.NETHERITE_PICKAXE, null, 1.3, 0.2),
        PALA("pala_floral", "Pala Floral", Material.NETHERITE_SHOVEL, null, 1.1, 0.2),
        AZADA("azada_floral", "Azada Floral", Material.NETHERITE_HOE, null, null, 0.2),
        ESCUDO("escudo_floral", "Escudo Floral", Material.SHIELD, null, null, 0),
        CASCO("casco_floral", "Casco Floral", Material.NETHERITE_HELMET, 3),
        PETO("peto_floral", "Peto Floral", Material.NETHERITE_CHESTPLATE, 8),
        PANTALON("pantalon_floral", "Pantalón Floral", Material.NETHERITE_LEGGINGS, 6),
        BOTAS("botas_floral", "Botas Florales", Material.NETHERITE_BOOTS, 3);

        public static final double DUREZA = 4.0;
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
                "En la herrería: la mejora, una pieza", "de Netherite (o un escudo) y un", "Bloque de Miel. Sirve para una pieza.");
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

    // La pieza de Netherite (o el escudo) con los atributos florales. En la herrería se quedan sus encantamientos y su
    // durabilidad
    public static ItemStack create(Pieza pieza) {
        ItemStack item = new ItemStack(pieza.base);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(ROSA) + "" + ChatColor.BOLD + pieza.nombre);
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, pieza.id);
        meta.setRarity(ItemRarity.EPIC);
        meta.setItemModel(NamespacedKey.minecraft(pieza.id));

        List<EquipoLore.Atributo> atributos;
        if (pieza.esArmadura()) atributos = armadura(meta, pieza);
        else if (pieza == Pieza.ESCUDO) atributos = escudo(meta);
        else atributos = herramienta(meta, pieza);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setLore(EquipoLore.lore(ROSA, List.of("Forjada con la esencia de", "la Abeja Floral."), pieza.esArmadura(),
                atributos, EquipoLore.Rango.FLORAL));
        item.setItemMeta(meta);
        return item;
    }

    // Los atributos del Netherite cambiados: el daño y la velocidad de ataque nuevos, y el minado más rápido
    private static List<EquipoLore.Atributo> herramienta(ItemMeta meta, Pieza pieza) {
        double dano = 1, velocidad = 4;
        Multimap<Attribute, AttributeModifier> defaults = pieza.base.getDefaultAttributeModifiers(EquipmentSlot.HAND);
        for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
            AttributeModifier modifier = entry.getValue();
            // El jugador pega 1 y ataca 4 veces por segundo sin nada en la mano: el modificador es el total menos eso
            if (entry.getKey().equals(Attribute.ATTACK_DAMAGE) && pieza.dano != null) {
                modifier = new AttributeModifier(modifier.getKey(), pieza.dano - 1, modifier.getOperation(), modifier.getSlotGroup());
            } else if (entry.getKey().equals(Attribute.ATTACK_SPEED) && pieza.velocidad != null) {
                modifier = new AttributeModifier(modifier.getKey(), pieza.velocidad - 4, modifier.getOperation(), modifier.getSlotGroup());
            }
            if (entry.getKey().equals(Attribute.ATTACK_DAMAGE)) dano += modifier.getAmount();
            if (entry.getKey().equals(Attribute.ATTACK_SPEED)) velocidad += modifier.getAmount();
            meta.addAttributeModifier(entry.getKey(), modifier);
        }
        if (pieza.minado > 0) {
            meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(MINADO_KEY, pieza.minado,
                    AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.MAINHAND));
        }

        List<EquipoLore.Atributo> atributos = new ArrayList<>();
        atributos.add(new EquipoLore.Atributo(EquipoLore.DANO, EquipoLore.numero(dano) + " de daño"));
        atributos.add(new EquipoLore.Atributo(EquipoLore.VELOCIDAD, EquipoLore.numero(velocidad) + " golpes por segundo"));
        if (pieza.minado > 0) {
            atributos.add(new EquipoLore.Atributo(EquipoLore.MINADO, "Pica " + Math.round(pieza.minado * 100) + "% más rápido que Netherite"));
        }
        if (pieza == Pieza.PICO) atributos.add(new EquipoLore.Atributo(EquipoLore.MINADO, "Rompe los minerales de la Warden Cave"));
        if (pieza == Pieza.HACHA) atributos.add(new EquipoLore.Atributo(EquipoLore.VELOCIDAD, "Ataca más rápido que la de Netherite"));
        return atributos;
    }

    // El escudo floral no se rompe nunca
    private static List<EquipoLore.Atributo> escudo(ItemMeta meta) {
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        return List.of(new EquipoLore.Atributo(EquipoLore.IRROMPIBLE, "Irrompible"),
                new EquipoLore.Atributo(EquipoLore.BLOQUEO, "Bloquea como un escudo normal"));
    }

    private static List<EquipoLore.Atributo> armadura(ItemMeta meta, Pieza pieza) {
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

        return List.of(
                new EquipoLore.Atributo(EquipoLore.ARMADURA, pieza.armadura + " de armadura y " + EquipoLore.numero(Pieza.DUREZA) + " de dureza"),
                new EquipoLore.Atributo(EquipoLore.VIDA, "+1 corazón"),
                new EquipoLore.Atributo(EquipoLore.DESGASTE, Math.round(Pieza.SIN_DESGASTE * 100) + "% de no gastarse"),
                new EquipoLore.Atributo(EquipoLore.EMPUJE, "10% menos empuje"));
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
