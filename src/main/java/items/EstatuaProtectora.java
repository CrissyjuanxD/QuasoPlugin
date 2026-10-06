package items;

import StatueManager.StatueData;
import StatueManager.StatueManager;
import StatueManager.StatueSchematic;
import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Item "Estatua Protectora".
 *
 * Reutiliza por completo el sistema de estatuas (StatueData / StatueManager):
 * el armor stand que coloca es una estatua en modo ANTI-GRIEF, con el mismo
 * nombre invisible ("Statue Grief") y brillo rojo que las estatuas anti-grief
 * creadas desde /statue.
 *
 * Diferencias con /statue give:
 *   - El item NO lleva la key "viciont:statue_id", así que el StatueListener
 *     no lo intercepta: la colocación y el GUI de configuración no se aplican.
 *   - Radio fijo de 18×18 bloques (9 de radio) y altura de 18 bloques.
 *   - Shift + Click derecho sobre la estatua colocada la devuelve al inventario.
 *   - Al colocarla se dibuja la barrera de partículas durante 40 segundos.
 */
public class EstatuaProtectora implements Listener {

    /** 9 de radio en X/Z => área protegida de 18×18 bloques. */
    public static final double RADIO_XZ = 9.0;
    /** 9 de radio en Y => 18 bloques de alto (9 arriba y 9 abajo de la estatua). */
    public static final double RADIO_Y = 9.0;

    /** 40 segundos de barrera visible al colocarla. */
    private static final int BARRERA_TICKS = 40 * 20;
    private static final long BARRERA_INTERVALO = 10L;

    private static final double DISTANCIA_RENDER_SQ = 30.0 * 30.0;

    private final QuasoPlugin plugin;

    /** Marca del ITEM (no usamos statue_id para no chocar con el StatueListener). */
    private final NamespacedKey itemKey;
    /** Marca del ARMOR STAND colocado, para saber que salió de este item. */
    private final NamespacedKey standKey;

    public EstatuaProtectora(QuasoPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "estatua_protectora");
        this.standKey = new NamespacedKey(plugin, "estatua_protectora_stand");
    }

    // ========================================================================
    //  CREACIÓN DEL ITEM
    // ========================================================================

    public ItemStack createEstatuaProtectora() {
        ItemStack item = new ItemStack(Material.ARMOR_STAND);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ff9e9e") + ChatColor.BOLD.toString() + "Estatua Protectora");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#e88f8f") + "Protege un área de "
                    + ChatColor.of("#ff9e9e") + ChatColor.BOLD + "18×18" + ChatColor.of("#e88f8f") + " bloques");
            lore.add(ChatColor.of("#e88f8f") + "y " + ChatColor.of("#ff9e9e") + ChatColor.BOLD + "18" + ChatColor.of("#e88f8f")
                    + " de altura alrededor de ella.");
            lore.add("");
            lore.add(ChatColor.of("#c9c9c9") + "Dentro de la zona bloquea:");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffb3b3") + "Explosiones de creepers y TNT");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffb3b3") + "Explosiones de ghasts, camas y cristales");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ffb3b3") + "Endermans y mobs que cambian bloques");
            lore.add("");
            lore.add(ChatColor.of("#b4b4b1") + "No impide que los jugadores");
            lore.add(ChatColor.of("#b4b4b1") + "rompan bloques a mano.");
            lore.add("");
            lore.add(ChatColor.of("#a8c8e0") + "Al colocarla verás durante "
                    + ChatColor.of("#a8c8e0") + ChatColor.BOLD + "40 segundos");
            lore.add(ChatColor.of("#a8c8e0") + "la barrera que delimita la zona.");
            lore.add("");
            lore.add(ChatColor.of("#ffd6a8") + "Shift + Click derecho" + ChatColor.of("#b4b4b1") + " sobre la");
            lore.add(ChatColor.of("#b4b4b1") + "estatua para recogerla de vuelta.");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);


            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(itemKey, PersistentDataType.BYTE, (byte) 1);


            ItemModels.apply(meta, "estatua_protectora");
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean esEstatuaProtectora(ItemStack item) {
        if (item == null || item.getType() != Material.ARMOR_STAND || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    private boolean esStandProtector(ArmorStand stand) {
        return stand.getPersistentDataContainer().has(standKey, PersistentDataType.BYTE);
    }

    // ========================================================================
    //  COLOCACIÓN
    // ========================================================================

    @EventHandler
    public void onColocar(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        ItemStack item = e.getItem();
        if (!esEstatuaProtectora(item)) return;

        // Cancelamos para que no se coloque el armor stand vanilla.
        e.setCancelled(true);

        Player p = e.getPlayer();
        Location loc = e.getClickedBlock().getLocation().add(0.5, 1, 0.5);
        loc.setYaw(p.getLocation().getYaw() + 180f);

        colocarEstatua(p, loc);

        if (p.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void colocarEstatua(Player p, Location loc) {
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c&lStatue Grief"));

        stand.setGravity(false);
        stand.setBasePlate(false);
        stand.setArms(true);
        stand.setVisible(true);
        stand.setCustomNameVisible(false);
        stand.setInvulnerable(true);
        stand.setPersistent(true);

        // Marca propia: así sabemos que esta estatua salió del item.
        stand.getPersistentDataContainer().set(standKey, PersistentDataType.BYTE, (byte) 1);

        // Datos de estatua: mismo modo ANTI-GRIEF que /statue, con glow rojo.
        StatueData data = new StatueData(stand);
        data.setDefaults();
        data.setRadiusX(RADIO_XZ);
        data.setRadiusY(RADIO_Y);
        data.setHpMax(10);
        data.setHpCurrent(10);
        data.setVisible(true);
        data.setInvulnerable(true);
        data.setGlowColor(org.bukkit.ChatColor.RED);
        data.setAntiGrief(true); // al final: limpia el efecto de poción por defecto

        // registerStatue aplica el nombre invisible "Statue Grief" y el glow rojo.
        StatueManager statueManager = plugin.getStatueManager();
        if (statueManager != null) {
            statueManager.registerStatue(stand);
        }

        loc.getWorld().playSound(loc, Sound.ENTITY_ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 1.0f, 0.9f);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.7f, 1.6f);

        p.sendMessage(ChatColor.of("#ff9e9e") + "✦ " + ChatColor.of("#e88f8f")
                + "Estatua Protectora colocada. Zona protegida de "
                + ChatColor.of("#ff9e9e") + "18×18" + ChatColor.of("#e88f8f") + " bloques.");

        mostrarBarrera(stand);
    }

    // ========================================================================
    //  RECOGIDA (Shift + Click derecho sobre la estatua)
    // ========================================================================

    @EventHandler
    public void onRecoger(PlayerInteractAtEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof ArmorStand)) return;

        ArmorStand stand = (ArmorStand) e.getRightClicked();
        if (!esStandProtector(stand)) return;

        Player p = e.getPlayer();
        if (!p.isSneaking()) return;

        e.setCancelled(true);
        recogerEstatua(stand, p);
    }

    private void recogerEstatua(ArmorStand stand, Player p) {
        Location loc = stand.getLocation();

        StatueManager statueManager = plugin.getStatueManager();
        if (statueManager != null) statueManager.unregisterStatue(stand);

        StatueSchematic schematic = plugin.getStatueSchematic();
        if (schematic != null) schematic.onStatueRemoved(stand.getUniqueId());

        stand.remove();

        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 0.3, 0.6, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(255, 158, 158), 1.2f));
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.7f, 1.6f);
        loc.getWorld().playSound(loc, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);

        ItemStack devuelta = createEstatuaProtectora();
        if (p.getInventory().firstEmpty() == -1) {
            loc.getWorld().dropItemNaturally(loc, devuelta);
        } else {
            p.getInventory().addItem(devuelta);
        }

        p.sendMessage(ChatColor.of("#ff9e9e") + "✦ " + ChatColor.of("#e88f8f")
                + "Has recogido la Estatua Protectora.");
    }

    // ========================================================================
    //  BARRERA DE PARTÍCULAS (misma lógica que el debug de estatuas)
    // ========================================================================

    private void mostrarBarrera(ArmorStand stand) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= BARRERA_TICKS || !stand.isValid() || !stand.getChunk().isLoaded()) {
                    cancel();
                    return;
                }

                StatueData data = new StatueData(stand);
                Location center = stand.getLocation();

                for (Player viewer : center.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(center) > DISTANCIA_RENDER_SQ) continue;
                    dibujarCaja(viewer, center, data.getRadiusX(), data.getRadiusY());
                }

                ticks += BARRERA_INTERVALO;
            }
        }.runTaskTimer(plugin, 0L, BARRERA_INTERVALO);
    }

    private void dibujarCaja(Player viewer, Location center, double rX, double rY) {
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.5f);
        double step = Math.max(1.5, rX / 8.0);

        Location viewerLoc = viewer.getLocation();

        // Tapa superior e inferior
        for (double x = -rX; x <= rX; x += step) {
            for (double z = -rX; z <= rX; z += step) {
                dibujarPunto(viewer, viewerLoc, center, x, rY, z, dust);
                dibujarPunto(viewer, viewerLoc, center, x, -rY, z, dust);
            }
        }

        // Paredes norte y sur
        for (double x = -rX; x <= rX; x += step) {
            for (double y = -rY; y <= rY; y += step) {
                dibujarPunto(viewer, viewerLoc, center, x, y, rX, dust);
                dibujarPunto(viewer, viewerLoc, center, x, y, -rX, dust);
            }
        }

        // Paredes este y oeste
        for (double z = -rX; z <= rX; z += step) {
            for (double y = -rY; y <= rY; y += step) {
                dibujarPunto(viewer, viewerLoc, center, rX, y, z, dust);
                dibujarPunto(viewer, viewerLoc, center, -rX, y, z, dust);
            }
        }
    }

    private void dibujarPunto(Player viewer, Location viewerLoc, Location center,
                              double dx, double dy, double dz, Particle.DustOptions dust) {
        Location loc = center.clone().add(dx, dy, dz);
        if (loc.distanceSquared(viewerLoc) > DISTANCIA_RENDER_SQ) return;
        viewer.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
    }
}