package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class AmuletInmortal implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey amuletKey;
    private final Set<UUID> invulnerablePlayers = new HashSet<>();

    public AmuletInmortal(JavaPlugin plugin) {
        this.plugin = plugin;
        this.amuletKey = new NamespacedKey(plugin, "amulet_inmortal");
    }

    public ItemStack createAmulet() {
        ItemStack item = new ItemStack(Material.ALLAY_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#3c9dcd") + ChatColor.BOLD.toString() + "Amuleto de la Inmortalidad");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#8bb6e5") + "Al consumirse, este amuleto");
            lore.add(ChatColor.of("#8bb6e5") + "otorga al jugador:");
            lore.add("");
            lore.add(ChatColor.GRAY + "> " + ChatColor.GOLD + ChatColor.BOLD.toString() + "Resistencia X " + ChatColor.of("#7095bd") + "durante 20 segundos.");
            lore.add("");
            lore.add(ChatColor.of("#828282") + "Solo puede usarse una vez");
            lore.add(ChatColor.of("#828282") + "cada 60 segundos.");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(amuletKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isAmulet(ItemStack item) {
        if (item == null || item.getType() != Material.ALLAY_SPAWN_EGG || !item.hasItemMeta()) return false;
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

        if (player.hasCooldown(Material.ALLAY_SPAWN_EGG)) {
            return;
        }

        item.setAmount(item.getAmount() - 1);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 9));

        player.setCooldown(Material.ALLAY_SPAWN_EGG, 1200);

        UUID uuid = player.getUniqueId();
        invulnerablePlayers.add(uuid);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            invulnerablePlayers.remove(uuid);
        }, 400L);

        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);

        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.1);

        Particle.DustOptions lightBlueDust = new Particle.DustOptions(Color.fromRGB(60, 157, 205), 1.5f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0, lightBlueDust);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (invulnerablePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}