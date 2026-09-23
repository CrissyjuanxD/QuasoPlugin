package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ExplosiveBow implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey explosiveBowLevelKey;
    private final NamespacedKey arrowLevelKey;

    public ExplosiveBow(JavaPlugin plugin) {
        this.plugin = plugin;
        this.explosiveBowLevelKey = new NamespacedKey(plugin, "explosive_bow_level");
        this.arrowLevelKey = new NamespacedKey(plugin, "explosive_arrow_level");
    }

    public ItemStack createExplosiveBowLevel1() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ff6666") + "" + ChatColor.BOLD + "Arco Explosivo Nvl 1");
            meta.setCustomModelData(500);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ff9999") + "Un arma inestable y destructiva");
            lore.add(ChatColor.of("#ff9999") + "capaz de detonar en el impacto.");
            lore.add("");
            lore.add(ChatColor.GRAY + "■ Lanza flechas espectrales");
            lore.add(ChatColor.RED + "■ Nivel de Explosión: " + ChatColor.WHITE + "2");
            lore.add(ChatColor.GOLD + "⚠ Consume 3 flechas por disparo");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "(Infinidad reduce el coste en 1)");
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 1);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    public ItemStack createExplosiveBowLevel2() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ff3333") + "" + ChatColor.BOLD + "Arco Explosivo Nvl 2");
            meta.setCustomModelData(501);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ff6666") + "Un arma inestable y destructiva");
            lore.add(ChatColor.of("#ff6666") + "capaz de detonar en el impacto.");
            lore.add("");
            lore.add(ChatColor.GRAY + "■ Lanza flechas espectrales");
            lore.add(ChatColor.RED + "■ Nivel de Explosión: " + ChatColor.WHITE + "3");
            lore.add(ChatColor.GOLD + "⚠ Consume 6 flechas por disparo");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "(Infinidad reduce el coste en 1)");
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 2);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    public ItemStack createExplosiveBowLevel3() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#cc0000") + "" + ChatColor.BOLD + "Arco Explosivo Nvl 3");
            meta.setCustomModelData(502);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#ff3333") + "Un arma inestable y destructiva");
            lore.add(ChatColor.of("#ff3333") + "capaz de detonar en el impacto.");
            lore.add("");
            lore.add(ChatColor.GRAY + "■ Lanza flechas espectrales");
            lore.add(ChatColor.RED + "■ Nivel de Explosión: " + ChatColor.WHITE + "4");
            lore.add(ChatColor.GOLD + "⚠ Consume 10 flechas por disparo");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "(Infinidad reduce el coste en 1)");
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 3);
            bow.setItemMeta(meta);
        }
        return bow;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getBow() == null || !event.getBow().hasItemMeta()) return;

        ItemMeta meta = event.getBow().getItemMeta();
        if (!meta.getPersistentDataContainer().has(explosiveBowLevelKey, PersistentDataType.INTEGER)) return;

        int level = meta.getPersistentDataContainer().get(explosiveBowLevelKey, PersistentDataType.INTEGER);

        if (player.getGameMode() != GameMode.CREATIVE) {
            int requiredArrows = switch (level) {
                case 1 -> 3;
                case 2 -> 6;
                case 3 -> 10;
                default -> 1;
            };

            boolean hasInfinity = event.getBow().containsEnchantment(Enchantment.INFINITY);
            ItemStack flecha = event.getConsumable();
            boolean disparoGratis = hasInfinity && flecha != null && flecha.getType() == Material.ARROW;

            int amountToConsume = hasInfinity ? (requiredArrows - 1) : requiredArrows;
            if (!disparoGratis) amountToConsume = Math.max(0, amountToConsume - 1);

            if (!consumeArrows(player, amountToConsume)) {
                event.setCancelled(true);
                if (!disparoGratis && flecha != null) player.getInventory().addItem(flecha.asOne());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }
        }

        if (event.getProjectile() instanceof Arrow) {
            event.getProjectile().remove();

            SpectralArrow spectral = player.launchProjectile(SpectralArrow.class);
            spectral.setVelocity(event.getProjectile().getVelocity());
            spectral.setShooter(player);
            spectral.getPersistentDataContainer().set(arrowLevelKey, PersistentDataType.INTEGER, level);

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SpectralArrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(arrowLevelKey, PersistentDataType.INTEGER)) return;

        if (event.getHitEntity() == null) return;
        if (event.getHitEntity() instanceof Player) return;

        int level = arrow.getPersistentDataContainer().get(arrowLevelKey, PersistentDataType.INTEGER);
        Location hitLoc = arrow.getLocation();
        World world = arrow.getWorld();

        float explosionPower = switch (level) {
            case 1 -> 2.0f;
            case 2 -> 3.0f;
            case 3 -> 4.0f;
            default -> 2.0f;
        };

        world.createExplosion(hitLoc, explosionPower, false, false, arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null);

        arrow.remove();
    }

    private boolean consumeArrows(Player player, int amountToConsume) {
        int totalArrows = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                totalArrows += item.getAmount();
            }
        }

        if (totalArrows < amountToConsume) {
            return false;
        }

        int remainingToConsume = amountToConsume;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                if (item.getAmount() <= remainingToConsume) {
                    remainingToConsume -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - remainingToConsume);
                    remainingToConsume = 0;
                }

                if (remainingToConsume <= 0) break;
            }
        }
        return true;
    }
}