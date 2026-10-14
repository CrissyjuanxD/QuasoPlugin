package Encantamientos;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

// Vuelo Rápido (arnés del Happy Ghast) ahora es un encantamiento del datapack: el arnés le sube la velocidad de vuelo al
// ghast que lo lleva, sin tareas del plugin. Acá solo se pasan los libros y arneses del sistema viejo (el nivel guardado
// en el item) al encantamiento, y se le quita a los ghasts el modificador que les ponía la tarea vieja
public class VueloRapido implements Listener {

    private static final String LORE_VIEJO = "Vuelo Rápido";
    private static final NamespacedKey MODIFICADOR_LEGADO = NamespacedKey.minecraft("11a11a11-22b2-33c3-44d4-55e55e55e55e");

    private final NamespacedKey nivelViejo;
    private final NamespacedKey modificadorViejo;

    public VueloRapido(JavaPlugin plugin) {
        this.nivelViejo = new NamespacedKey(plugin, "fast_flight_level");
        this.modificadorViejo = new NamespacedKey(plugin, "fast_flight_bonus");
        for (World world : Bukkit.getWorlds()) {
            for (HappyGhast ghast : world.getEntitiesByClass(HappyGhast.class)) migrar(ghast);
        }
        for (Player player : Bukkit.getOnlinePlayers()) migrar(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        migrar(event.getPlayer());
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof HappyGhast ghast) migrar(ghast);
        }
    }

    private void migrar(Player player) {
        migrar(player.getInventory().getContents(), player.getInventory()::setItem);
        migrar(player.getEnderChest().getContents(), player.getEnderChest()::setItem);
    }

    private void migrar(ItemStack[] items, java.util.function.BiConsumer<Integer, ItemStack> poner) {
        for (int i = 0; i < items.length; i++) {
            ItemStack nuevo = migrar(items[i]);
            if (nuevo != null) poner.accept(i, nuevo);
        }
    }

    // El arnés que lleva puesto y el modificador de velocidad que quedó guardado en el ghast
    private void migrar(HappyGhast ghast) {
        if (ghast.getEquipment() != null) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.BODY, EquipmentSlot.SADDLE}) {
                ItemStack nuevo = migrar(ghast.getEquipment().getItem(slot));
                if (nuevo != null) ghast.getEquipment().setItem(slot, nuevo);
            }
        }
        for (Attribute attribute : new Attribute[]{Attribute.FLYING_SPEED, Attribute.MOVEMENT_SPEED}) {
            AttributeInstance instance = ghast.getAttribute(attribute);
            if (instance == null) continue;
            instance.getModifiers().stream()
                    .filter(m -> m.getKey().equals(modificadorViejo) || m.getKey().equals(MODIFICADOR_LEGADO))
                    .toList().forEach(instance::removeModifier);
        }
    }

    // Libro viejo → libro del encantamiento (misma cantidad); arnés viejo → arnés encantado. null si no es del sistema
    // viejo
    ItemStack migrar(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        Integer nivel = item.getItemMeta().getPersistentDataContainer().get(nivelViejo, PersistentDataType.INTEGER);
        if (nivel == null || nivel <= 0) return null;
        Enchantment vuelo = QuasoEnchant.VUELO_RAPIDO.get();
        if (vuelo == null) return null;
        nivel = Math.min(nivel, QuasoEnchant.VUELO_RAPIDO.maxLevel());

        if (item.getType() == Material.ENCHANTED_BOOK) {
            ItemStack libro = QuasoEnchant.VUELO_RAPIDO.book(nivel);
            libro.setAmount(item.getAmount());
            return libro;
        }
        ItemStack arnes = item.clone();
        ItemMeta meta = arnes.getItemMeta();
        meta.getPersistentDataContainer().remove(nivelViejo);
        // El brillo viejo era un Irrompibilidad escondido; el encantamiento de verdad ya brilla y se ve
        if (meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            meta.removeEnchant(Enchantment.UNBREAKING);
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (meta.hasLore()) {
            List<String> lore = new ArrayList<>(meta.getLore());
            lore.removeIf(line -> ChatColor.stripColor(line).startsWith(LORE_VIEJO));
            meta.setLore(lore.isEmpty() ? null : lore);
        }
        meta.addEnchant(vuelo, nivel, true);
        arnes.setItemMeta(meta);
        return arnes;
    }
}
