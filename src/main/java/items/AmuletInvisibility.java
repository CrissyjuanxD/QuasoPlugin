package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AmuletInvisibility implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey amuletKey;
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    public AmuletInvisibility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.amuletKey = new NamespacedKey(plugin, "amulet_invisibility");
    }

    public ItemStack createAmulet() {
        ItemStack item = new ItemStack(Material.GLOW_SQUID_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nombre del ítem
            meta.setDisplayName(ChatColor.of("#00ffff") + "" + ChatColor.BOLD + "Amuleto de Invisibilidad");

            // Lore descriptivo
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#66ffff") + "Al consumirse, este amuleto");
            lore.add(ChatColor.of("#66ffff") + "otorga al jugador:");
            lore.add("");
            lore.add(ChatColor.GRAY + "■ " + ChatColor.of("#00cccc") + "Invisibilidad Perfecta " + ChatColor.GRAY + "(3:00)");
            lore.add(ChatColor.GRAY + "■ " + ChatColor.of("#ff99cc") + "Regeneración II " + ChatColor.GRAY + "(3:00)");
            lore.add(ChatColor.GRAY + "■ " + ChatColor.of("#ffcc00") + "Absorción III " + ChatColor.GRAY + "(3:00)");
            lore.add("");
            lore.add(ChatColor.of("#828282") + "Los mobs te ignorarán por completo");
            lore.add(ChatColor.of("#828282") + "a menos que los ataques directamente.");
            lore.add("");
            lore.add(ChatColor.of("#828282") + "Cooldown: 2 minutos.");

            meta.setLore(lore);
            meta.setCustomModelData(600);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(amuletKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isAmulet(ItemStack item) {
        if (item == null || item.getType() != Material.GLOW_SQUID_SPAWN_EGG || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(amuletKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isAmulet(item)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Verificar cooldown global del ítem
        if (player.hasCooldown(Material.GLOW_SQUID_SPAWN_EGG)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir 1 amuleto
        item.setAmount(item.getAmount() - 1);

        // 3 minutos = 180 segundos = 3600 ticks
        int duration = 3600;

        // Otorgar efectos
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration, 2));

        player.setCooldown(Material.GLOW_SQUID_SPAWN_EGG, 2400);

        // Registrar al jugador como "Oculto"
        UUID uuid = player.getUniqueId();
        hiddenPlayers.add(uuid);

        // Remover al jugador de la lista después de los 3 minutos
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            hiddenPlayers.remove(uuid);
        }, duration);

        // Sonidos y Partículas
        player.playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_GLOW_SQUID_SQUIRT, 1.0f, 0.8f);

        player.getWorld().spawnParticle(Particle.GLOW_SQUID_INK, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.05);
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        if (hiddenPlayers.contains(player.getUniqueId())) {

            if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                hiddenPlayers.remove(player.getUniqueId());
                return;
            }

            if (event.getReason() == EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                return;
            }

            event.setCancelled(true);
        }
    }
}