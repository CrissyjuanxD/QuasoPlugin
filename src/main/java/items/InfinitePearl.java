package items;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InfinitePearl implements Listener {
    private static final NamespacedKey KEY = new NamespacedKey("quasoplugin", "perla_infinita");
    private static final double EFFECT_CHANCE = 0.35;
    private static final int EFFECT_TICKS = 30 * 20;
    private static final List<PotionEffect> EFFECTS = List.of(
            new PotionEffect(PotionEffectType.SPEED, EFFECT_TICKS, 1),
            new PotionEffect(PotionEffectType.JUMP_BOOST, EFFECT_TICKS, 1),
            new PotionEffect(PotionEffectType.REGENERATION, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.NIGHT_VISION, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.SLOW_FALLING, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.NAUSEA, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.WEAKNESS, EFFECT_TICKS, 0),
            new PotionEffect(PotionEffectType.HUNGER, EFFECT_TICKS, 1),
            new PotionEffect(PotionEffectType.GLOWING, EFFECT_TICKS, 0)
    );

    public static ItemStack createPearl() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#B57EDC") + "" + ChatColor.BOLD + "Perla Infinita");
        meta.setLore(List.of(
                "",
                ChatColor.of("#D8B4F8") + "Una perla que nunca se gasta.",
                ChatColor.of("#D8B4F8") + "Cada uso puede darte un efecto",
                ChatColor.of("#D8B4F8") + "al azar por 30 segundos.",
                "",
                ChatColor.GRAY + "Uso:",
                ChatColor.GRAY + "> " + ChatColor.WHITE + "Click derecho",
                ""
        ));
        meta.setRarity(ItemRarity.EPIC);
        meta.setMaxStackSize(1);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        ItemModels.apply(meta, "perla_infinita");
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPearl(ItemStack item) {
        return item != null && item.getType() == Material.ENDER_PEARL && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    // La perla no se gasta; el cooldown es el normal de las ender pearls
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onThrow(PlayerLaunchProjectileEvent event) {
        if (!(event.getProjectile() instanceof EnderPearl) || !isPearl(event.getItemStack())) return;

        event.setShouldConsume(false);

        Player player = event.getPlayer();
        if (ThreadLocalRandom.current().nextDouble() < EFFECT_CHANCE) {
            PotionEffect effect = EFFECTS.get(ThreadLocalRandom.current().nextInt(EFFECTS.size()));
            player.addPotionEffect(effect);
        }
    }
}
