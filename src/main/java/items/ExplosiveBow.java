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

    // ==========================================
    // CREACIÓN DE ITEMS
    // ==========================================

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

    // ==========================================
    // LÓGICA DE DISPARO
    // ==========================================

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getBow() == null || !event.getBow().hasItemMeta()) return;

        ItemMeta meta = event.getBow().getItemMeta();
        if (!meta.getPersistentDataContainer().has(explosiveBowLevelKey, PersistentDataType.INTEGER)) return;

        int level = meta.getPersistentDataContainer().get(explosiveBowLevelKey, PersistentDataType.INTEGER);

        if (player.getGameMode() != GameMode.CREATIVE) {
            // Determinar consumo base
            int requiredArrows = switch (level) {
                case 1 -> 3;
                case 2 -> 6;
                case 3 -> 10;
                default -> 1;
            };

            // Verificar si el arco tiene infinidad
            boolean hasInfinity = event.getBow().containsEnchantment(Enchantment.INFINITY);

            // Si tiene infinidad, perdonamos 1 flecha del coste total
            int amountToConsume = hasInfinity ? (requiredArrows - 1) : requiredArrows;

            if (!consumeArrows(player, amountToConsume)) {
                // No tiene suficientes flechas
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            // Cancelar el consumo automático del juego para tener control manual perfecto
            event.setConsumeItem(false);
        }

        // Reemplazar la flecha normal por una espectral
        if (event.getProjectile() instanceof Arrow) {
            event.getProjectile().remove(); // Borra la flecha original

            SpectralArrow spectral = player.launchProjectile(SpectralArrow.class);
            spectral.setVelocity(event.getProjectile().getVelocity()); // Copia la fuerza del disparo
            spectral.setShooter(player);
            spectral.getPersistentDataContainer().set(arrowLevelKey, PersistentDataType.INTEGER, level);

            // Sonido de amatista al disparar
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
        }
    }

    // ==========================================
    // LÓGICA DE IMPACTO / EXPLOSIÓN
    // ==========================================

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof SpectralArrow arrow)) return;
        if (!arrow.getPersistentDataContainer().has(arrowLevelKey, PersistentDataType.INTEGER)) return;

        // CONDICIÓN: Solo explota si le da a una entidad, pero que NO sea jugador
        if (event.getHitEntity() == null) return; // Le dio a un bloque, no hace nada
        if (event.getHitEntity() instanceof Player) return; // Le dio a un jugador, no hace nada

        int level = arrow.getPersistentDataContainer().get(arrowLevelKey, PersistentDataType.INTEGER);
        Location hitLoc = arrow.getLocation();
        World world = arrow.getWorld();

        float explosionPower = switch (level) {
            case 1 -> 2.0f;
            case 2 -> 3.0f;
            case 3 -> 4.0f;
            default -> 2.0f;
        };

        // Generar la explosión (false = NO rompe bloques, solo hace daño)
        world.createExplosion(hitLoc, explosionPower, false, false, arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null);

        arrow.remove(); // Borrar la flecha tras detonar
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    /**
     * Consume una cantidad específica de flechas del inventario del jugador.
     * @return true si se consumieron, false si no tiene suficientes.
     */
    private boolean consumeArrows(Player player, int amountToConsume) {
        int totalArrows = 0;
        // Contar flechas en el inventario
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                totalArrows += item.getAmount();
            }
        }

        if (totalArrows < amountToConsume) {
            return false; // No hay suficientes
        }

        // Restar flechas
        int remainingToConsume = amountToConsume;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) {
                if (item.getAmount() <= remainingToConsume) {
                    remainingToConsume -= item.getAmount();
                    item.setAmount(0); // Vacía este stack
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