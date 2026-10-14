package Armors;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import items.EquipoLore;
import items.WardenCaveItems;

import java.util.ArrayList;
import java.util.List;

public class WardenArmor implements Listener {

    private static final NamespacedKey WARDEN_MODEL = NamespacedKey.minecraft("warden");
    private static final NamespacedKey WINGED_MODEL = NamespacedKey.minecraft("warden_alado");
    private final JavaPlugin plugin;
    private final NamespacedKey wardenArmorKey;

    public WardenArmor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.wardenArmorKey = new NamespacedKey(plugin, "warden_armor");
    }

    public ItemStack createWardenHelmet() {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Casco de Warden");
        meta.setLore(lore(WardenCaveItems.Variant.CIAN, 4, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "warden_helmet_armor"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD
        ));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey(plugin, "warden_helmet_toughness"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD
        ));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                new NamespacedKey(plugin, "warden_helmet_knockback"), 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD
        ));
        meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                new NamespacedKey(plugin, "warden_helmet_health"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD
        ));

        meta.setCustomModelData(800);
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(wardenArmorKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("casco_warden"));
        setEquipmentModel(meta, EquipmentSlot.HEAD, WARDEN_MODEL);
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createWardenChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Peto de Warden");
        meta.setLore(lore(WardenCaveItems.Variant.MORADO, 9, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "warden_chestplate_armor"), 9, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST
        ));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey(plugin, "warden_chestplate_toughness"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST
        ));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                new NamespacedKey(plugin, "warden_chestplate_knockback"), 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST
        ));
        meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                new NamespacedKey(plugin, "warden_chestplate_health"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST
        ));

        meta.setCustomModelData(801);
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(wardenArmorKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("peto_warden"));
        setEquipmentModel(meta, EquipmentSlot.CHEST, WARDEN_MODEL);
        item.setItemMeta(meta);

        return item;
    }

    // Peto de Warden Alado: el mismo peto pero planea como unas Elytras (EnderKing Pearl + peto + Elytras en la herrería)
    public ItemStack createWingedWardenChestplate() {
        ItemStack item = createWardenChestplate();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of("#b55cff") + "" + ChatColor.BOLD + "Peto de Warden Alado");
        meta.setLore(lore(WardenCaveItems.Variant.MORADO, 9, true));
        meta.setGlider(true);
        meta.getPersistentDataContainer().set(items.EndItems.ITEM_KEY, PersistentDataType.STRING, "peto_warden_alado");
        meta.setItemModel(NamespacedKey.minecraft("peto_warden_alado"));
        setEquipmentModel(meta, EquipmentSlot.CHEST, WINGED_MODEL);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWinged(ItemStack item) {
        return "peto_warden_alado".equals(items.EndItems.idOf(item));
    }

    public ItemStack createWardenLeggings() {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Pantalón de Warden");
        meta.setLore(lore(WardenCaveItems.Variant.GRIS, 7, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "warden_leggings_armor"), 7, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS
        ));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey(plugin, "warden_leggings_toughness"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS
        ));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                new NamespacedKey(plugin, "warden_leggings_knockback"), 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS
        ));
        meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                new NamespacedKey(plugin, "warden_leggings_health"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS
        ));

        meta.setCustomModelData(802);
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(wardenArmorKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("pantalon_warden"));
        setEquipmentModel(meta, EquipmentSlot.LEGS, WARDEN_MODEL);
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createWardenBoots() {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Botas de Warden");
        meta.setLore(lore(WardenCaveItems.Variant.VERDE, 4, false));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "warden_boots_armor"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET
        ));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                new NamespacedKey(plugin, "warden_boots_toughness"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET
        ));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                new NamespacedKey(plugin, "warden_boots_knockback"), 0.1, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET
        ));
        meta.addAttributeModifier(Attribute.MAX_HEALTH, new AttributeModifier(
                new NamespacedKey(plugin, "warden_boots_health"), 4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET
        ));

        meta.setCustomModelData(803);
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(wardenArmorKey, PersistentDataType.BYTE, (byte) 1);
        meta.setItemModel(NamespacedKey.minecraft("bota_warden"));
        setEquipmentModel(meta, EquipmentSlot.FEET, WARDEN_MODEL);
        item.setItemMeta(meta);

        return item;
    }

    // El lore va con el color del bioma de la pieza, el mismo de su alma y su mineral: Casco cian (Caverna Sculk),
    // Peto morado (Abismo Flotante), Pantalón gris (Ruinas de Ceniza) y Botas verde (Pantano Profundo). Tiene el formato
    // de todo el equipo (EquipoLore) y el tooltip de atributos del juego se oculta
    static List<String> lore(WardenCaveItems.Variant bioma, int armadura, boolean alado) {
        List<EquipoLore.Atributo> atributos = new ArrayList<>(List.of(
                new EquipoLore.Atributo(EquipoLore.ARMADURA, armadura + " de armadura y 4 de dureza"),
                new EquipoLore.Atributo(EquipoLore.VIDA, "+2 corazones"),
                new EquipoLore.Atributo(EquipoLore.DESGASTE, "30% de no gastarse"),
                new EquipoLore.Atributo(EquipoLore.EMPUJE, "10% menos empuje")));
        if (alado) atributos.add(new EquipoLore.Atributo("✈", "Planea como unas Elytras"));
        return EquipoLore.lore(bioma.color(), List.of("Forjada en la profunda oscuridad,", "resuena con las almas del abismo.",
                "Pieza " + bioma.biome() + "."), true, atributos, EquipoLore.Rango.WARDEN);
    }

    public boolean isWardenArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wardenArmorKey, PersistentDataType.BYTE);
    }

    private static void setEquipmentModel(ItemMeta meta, EquipmentSlot slot, NamespacedKey model) {
        EquippableComponent equipment = meta.getEquippable();
        if (!meta.hasEquippable()) equipment.setEquipSound(Sound.ITEM_ARMOR_EQUIP_NETHERITE);
        equipment.setSlot(slot);
        equipment.setModel(model);
        meta.setEquippable(equipment);
    }

    // Actualiza también las piezas anteriores al pack, sin reemplazar sus atributos,
    // encantamientos, durabilidad, nombre ni capacidad de planeo.
    boolean refreshEquipmentModel(ItemStack item) {
        if (!isWardenArmor(item)) return false;
        EquipmentSlot slot = switch (item.getType()) {
            case NETHERITE_HELMET -> EquipmentSlot.HEAD;
            case NETHERITE_CHESTPLATE -> EquipmentSlot.CHEST;
            case NETHERITE_LEGGINGS -> EquipmentSlot.LEGS;
            case NETHERITE_BOOTS -> EquipmentSlot.FEET;
            default -> null;
        };
        if (slot == null) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey model = isWinged(item) ? WINGED_MODEL : WARDEN_MODEL;
        EquippableComponent equipment = meta.getEquippable();
        if (model.equals(equipment.getModel()) && slot == equipment.getSlot()) return false;
        setEquipmentModel(meta, slot, model);
        item.setItemMeta(meta);
        return true;
    }

    @EventHandler
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        var equipment = event.getEntity().getEquipment();
        if (equipment == null) return;
        for (EquipmentSlot slot : event.getEquipmentChanges().keySet()) {
            if (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.CHEST
                    && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) continue;
            ItemStack item = equipment.getItem(slot);
            if (refreshEquipmentModel(item)) equipment.setItem(slot, item);
        }
    }

    // La armadura del Warden tiene 30% de probabilidad de no gastarse
    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isWardenArmor(event.getItem())) {
            if (Math.random() < 0.30) {
                event.setCancelled(true);
            }
        }
    }
}
