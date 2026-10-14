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
            lore.add(ChatColor.GRAY + "> Lanza flechas espectrales");
            lore.add(ChatColor.RED + "> Nivel de Explosión: " + ChatColor.WHITE + "2");
            lore.addAll(costoLore(1));
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 1);
            meta.setItemModel(NamespacedKey.minecraft("arco_nivel1"));
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
            lore.add(ChatColor.GRAY + "> Lanza flechas espectrales");
            lore.add(ChatColor.RED + "> Nivel de Explosión: " + ChatColor.WHITE + "3");
            lore.addAll(costoLore(2));
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 2);
            meta.setItemModel(NamespacedKey.minecraft("arco_nivel2"));
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
            lore.add(ChatColor.GRAY + "> Lanza flechas espectrales");
            lore.add(ChatColor.RED + "> Nivel de Explosión: " + ChatColor.WHITE + "4");
            lore.addAll(costoLore(3));
            lore.add("");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(explosiveBowLevelKey, PersistentDataType.INTEGER, 3);
            meta.setItemModel(NamespacedKey.minecraft("arco_nivel3"));
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

        actualizarLore(event.getBow(), level);

        if (player.getGameMode() != GameMode.CREATIVE) {
            boolean hasInfinity = event.getBow().containsEnchantment(Enchantment.INFINITY);
            ItemStack flecha = event.getConsumable();
            // Con Infinidad y una flecha normal el juego no gasta la del disparo; si no, ya la sacó del inventario
            boolean disparoGratis = hasInfinity && flecha != null && flecha.getType() == Material.ARROW;

            int amountToConsume = Math.max(0, costo(level, hasInfinity) - (disparoGratis ? 0 : 1));

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

    // Flechas por disparo: 3, 6 y 10; con Infinidad la mitad (1, 3 y 5)
    static int costo(int level, boolean infinidad) {
        int base = switch (level) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 10;
            default -> 1;
        };
        return infinidad ? Math.max(1, base / 2) : base;
    }

    static List<String> costoLore(int level) {
        return List.of(ChatColor.GOLD + "⚠ Consume " + costo(level, false) + " flechas por disparo",
                ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "(con Infinidad, " + costo(level, true) + ")");
    }

    // Los arcos de antes decían "Infinidad reduce el coste en 1": se les cambia el lore al disparar
    private void actualizarLore(ItemStack bow, int level) {
        ItemMeta meta = bow.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) return;
        List<String> nuevo = new ArrayList<>();
        boolean cambio = false;
        for (String line : lore) {
            String plano = ChatColor.stripColor(line);
            if (plano.startsWith("⚠ Consume")) {
                cambio |= !line.equals(costoLore(level).get(0));
                nuevo.add(costoLore(level).get(0));
            } else if (plano.contains("Infinidad")) {
                cambio |= !line.equals(costoLore(level).get(1));
                nuevo.add(costoLore(level).get(1));
            } else {
                nuevo.add(line);
            }
        }
        if (!cambio) return;
        meta.setLore(nuevo);
        bow.setItemMeta(meta);
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