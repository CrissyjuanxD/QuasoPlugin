package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class ElementalTotems implements Listener {
    public enum Kind {
        SCULK("sculk_totem", "Sculk Totem", "#79DEE0"),
        ENDER("ender_totem", "Ender Totem", "#C99AEA");

        public final String id;
        private final String name;
        private final String color;

        Kind(String id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }
    }

    private final Plugin plugin;

    public ElementalTotems(Plugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack create(Kind kind) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        String color = ChatColor.of(kind.color).toString();
        meta.setDisplayName(color + ChatColor.BOLD + kind.name);
        meta.setItemModel(NamespacedKey.minecraft(kind.id));
        meta.setRarity(ItemRarity.EPIC);
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, kind.id);
        meta.setLore(kind == Kind.SCULK ? List.of(
                "", ChatColor.GRAY + "Al activarse conserva los efectos del tótem.",
                color + "> Resistencia III " + ChatColor.GRAY + "(0:03)", ""
        ) : List.of(
                "", ChatColor.GRAY + "Al activarse otorga:",
                color + "> Regeneración V " + ChatColor.GRAY + "(1:45)",
                color + "> Absorción V " + ChatColor.GRAY + "(1:05)",
                color + "> Resistencia al fuego IV " + ChatColor.GRAY + "(1:40)",
                color + "> Resistencia V " + ChatColor.GRAY + "(0:05)", ""
        ));
        item.setItemMeta(meta);
        return item;
    }

    static Kind kindOf(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return null;
        String id = EndItems.idOf(item);
        for (Kind kind : Kind.values()) if (kind.id.equals(id)) return kind;
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)) return;
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return;
        // Lee la mano que realmente se consume, no el otro tótem que pueda estar equipado.
        Kind kind = kindOf(player.getInventory().getItem(hand));
        if (kind == null) return;
        // Vanilla limpia los efectos durante la resurrección; se añaden después de esa limpieza.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;
            for (PotionEffect effect : effects(kind)) player.addPotionEffect(effect);
        });
    }

    static List<PotionEffect> effects(Kind kind) {
        if (kind == Kind.SCULK) return List.of(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2));
        // Vanilla: regeneración II 45 s, absorción II 5 s y resistencia al fuego I 40 s.
        // Se suman tres niveles y 60 segundos; el amplificador de Bukkit empieza en cero.
        return List.of(
                new PotionEffect(PotionEffectType.REGENERATION, 105 * 20, 4),
                new PotionEffect(PotionEffectType.ABSORPTION, 65 * 20, 4),
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100 * 20, 3),
                new PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 4)
        );
    }
}
