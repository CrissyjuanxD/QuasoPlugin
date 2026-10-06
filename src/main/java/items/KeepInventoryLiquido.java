package items;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class KeepInventoryLiquido implements Listener {

    private static final int DURACION_TICKS = 480 * 20;

    private static final String CELESTE_CLARO = "#BFE9FF";
    private static final String CELESTE = "#A8DFFF";
    private static final String AZUL_PASTEL = "#9EC5FF";
    private static final String AZUL_SUAVE = "#7E9DC4";

    private final NamespacedKey liquidoKey;

    public KeepInventoryLiquido(JavaPlugin plugin) {
        this.liquidoKey = new NamespacedKey(plugin, "keep_inventory_liquido");
    }

    public ItemStack createKeepInventoryLiquido() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of(CELESTE) + ChatColor.BOLD.toString() + "Keep Inventory Líquido");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of(AZUL_PASTEL) + "Mientras dure su efecto, la muerte no");
            lore.add(ChatColor.of(AZUL_PASTEL) + "podrá arrebatarte lo que llevas encima:");
            lore.add("");
            lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + ChatColor.BOLD.toString() + "Keep Inventory "
                    + ChatColor.of(CELESTE_CLARO) + "durante 8 minutos.");
            lore.add("");
            lore.add(ChatColor.of(AZUL_SUAVE) + "El efecto perdura incluso");
            lore.add(ChatColor.of(AZUL_SUAVE) + "después de haber muerto.");

            meta.setLore(lore);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);


            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(liquidoKey, PersistentDataType.BYTE, (byte) 1);


            ItemModels.apply(meta, "keep_inventory_liquido");
            item.setItemMeta(meta);
        }

        item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(1.6f)
                .animation(ItemUseAnimation.DRINK)
                .sound(Key.key("minecraft:entity.generic.drink"))
                .hasConsumeParticles(false)
                .build());

        return item;
    }

    public boolean isKeepInventoryLiquido(ItemStack item) {
        if (item == null || item.getType() != Material.GOLDEN_APPLE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(liquidoKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isKeepInventoryLiquido(event.getItem())) return;

        Player player = event.getPlayer();

        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, DURACION_TICKS, 0, true, true, true));

        player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 1.0f, 1.3f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.8f);

        Particle.DustOptions celeste = new Particle.DustOptions(Color.fromRGB(168, 223, 255), 1.4f);
        Particle.DustOptions azul = new Particle.DustOptions(Color.fromRGB(158, 197, 255), 1.2f);

        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.45, 0.7, 0.45, 0, celeste);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 20, 0.55, 0.8, 0.55, 0, azul);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.02);
    }
}