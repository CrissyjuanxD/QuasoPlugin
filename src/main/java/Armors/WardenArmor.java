package Armors;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class WardenArmor implements Listener {

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
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#008b8b") + "Forjada en la profunda oscuridad,",
                ChatColor.of("#008b8b") + "esta armadura resuena con las",
                ChatColor.of("#008b8b") + "almas corrompidas del abismo.",
                "",
                ChatColor.GRAY + "Equipar esta pieza otorga: " + ChatColor.WHITE,
                ChatColor.of("#00cccc") + "■ " + ChatColor.AQUA + ChatColor.BOLD + "+2 Corazones",
                ChatColor.of("#00cccc") + "■ " + ChatColor.DARK_AQUA + ChatColor.BOLD + "30% Resistencia al Desgaste",
                ""
        ));

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
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createWardenChestplate() {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Peto de Warden");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#008b8b") + "Forjada en la profunda oscuridad,",
                ChatColor.of("#008b8b") + "esta armadura resuena con las",
                ChatColor.of("#008b8b") + "almas corrompidas del abismo.",
                "",
                ChatColor.GRAY + "Equipar esta pieza otorga: " + ChatColor.WHITE,
                ChatColor.of("#00cccc") + "■ " + ChatColor.AQUA + ChatColor.BOLD + "+2 Corazones",
                ChatColor.of("#00cccc") + "■ " + ChatColor.DARK_AQUA + ChatColor.BOLD + "30% Resistencia al Desgaste",
                ""
        ));

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
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createWardenLeggings() {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Pantalón de Warden");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#008b8b") + "Forjada en la profunda oscuridad,",
                ChatColor.of("#008b8b") + "esta armadura resuena con las",
                ChatColor.of("#008b8b") + "almas corrompidas del abismo.",
                "",
                ChatColor.GRAY + "Equipar esta pieza otorga: " + ChatColor.WHITE,
                ChatColor.of("#00cccc") + "■ " + ChatColor.AQUA + ChatColor.BOLD + "+2 Corazones",
                ChatColor.of("#00cccc") + "■ " + ChatColor.DARK_AQUA + ChatColor.BOLD + "30% Resistencia al Desgaste",
                ""
        ));

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
        item.setItemMeta(meta);

        return item;
    }

    public ItemStack createWardenBoots() {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Botas de Warden");
        meta.setLore(Arrays.asList(
                "",
                ChatColor.of("#008b8b") + "Forjada en la profunda oscuridad,",
                ChatColor.of("#008b8b") + "esta armadura resuena con las",
                ChatColor.of("#008b8b") + "almas corrompidas del abismo.",
                "",
                ChatColor.GRAY + "Equipar esta pieza otorga: " + ChatColor.WHITE,
                ChatColor.of("#00cccc") + "■ " + ChatColor.AQUA + ChatColor.BOLD + "+2 Corazones",
                ChatColor.of("#00cccc") + "■ " + ChatColor.DARK_AQUA + ChatColor.BOLD + "30% Resistencia al Desgaste",
                ""
        ));

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
        item.setItemMeta(meta);

        return item;
    }

    public boolean isWardenArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wardenArmorKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (isWardenArmor(event.getItem())) {
            if (Math.random() < 0.30) {
                event.setCancelled(true);
            }
        }
    }
}