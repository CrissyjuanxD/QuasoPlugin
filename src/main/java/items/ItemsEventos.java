package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ItemsEventos implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey manzanaKey;
    private final NamespacedKey plumaKey;
    private final NamespacedKey plumaMejoradaKey;

    /** Duracion de la levitacion de cada pluma. */
    private static final int LEVITACION_NORMAL_TICKS = 21;   // ~1 segundo
    private static final int LEVITACION_MEJORADA_TICKS = 60; // 3 segundos

    /**
     * Ticks SEGUIDOS que hay que estar en el suelo para dar la caida por
     * terminada. Asi rozar un bloque a mitad de la caida no quita la proteccion.
     */
    private static final int TICKS_ATERRIZAJE = 10;

    /** Red de seguridad por si se queda en el aire (elytra, vuelo...). */
    private static final int MAX_TICKS_PROTECCION = 20 * 60;

    /**
     * Jugadores con la caida protegida por la Pluma Mejorada.
     * Se quitan de la lista cuando aterrizan de verdad, asi la proteccion vale
     * SOLO para esa caida.
     */
    private final Set<UUID> caidaProtegida = new HashSet<>();

    public ItemsEventos(JavaPlugin plugin) {
        this.plugin = plugin;
        this.manzanaKey = new NamespacedKey(plugin, "manzana_vida");
        this.plumaKey = new NamespacedKey(plugin, "pluma_levitacion");
        this.plumaMejoradaKey = new NamespacedKey(plugin, "pluma_levitacion_mejorada");
    }

    // --------------------------------------------------------
    // CREACIÓN DE ITEMS
    // --------------------------------------------------------

    public ItemStack createManzanaVida() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#77DD77") + ChatColor.BOLD.toString() + "Manzana de la Vida");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#A8E6CF") + "Consúmela para obtener:");
            lore.add(ChatColor.of("#77DD77") + "• " + ChatColor.of("#A8E6CF") + "Curación Instantánea III");
            meta.setLore(lore);


            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            FoodComponent food = meta.getFood();
            food.setCanAlwaysEat(true);
            meta.setFood(food);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(manzanaKey, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("manzana_vida"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPlumaLevitacion() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#F8DE7E") + ChatColor.BOLD.toString() + "Pluma de Levitación");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#FDFD96") + "Click derecho para usar.");
            lore.add(ChatColor.of("#FDFD96") + "Te dará la habilidad de:");
            lore.add(ChatColor.of("#F8DE7E") + "• " + ChatColor.of("#FDFD96") + "Levitación XI " + ChatColor.GRAY + "(1 segundo)");
            meta.setLore(lore);


            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(plumaKey, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("pluma_levi"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPlumaLevitacionMejorada() {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#C9F0FF") + ChatColor.BOLD.toString() + "Pluma de Levitación Mejorada");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#EAF9FF") + "Click derecho para usar.");
            lore.add(ChatColor.of("#EAF9FF") + "Te dará la habilidad de:");
            lore.add(ChatColor.of("#C9F0FF") + "• " + ChatColor.of("#EAF9FF") + "Levitación XI " + ChatColor.GRAY + "(3 segundos)");
            lore.add(ChatColor.of("#C9F0FF") + "• " + ChatColor.of("#EAF9FF") + "Sin daño de caída " + ChatColor.GRAY + "(solo en esa caída)");
            lore.add("");
            lore.add(ChatColor.of("#8A8A8A") + "La protección se pierde en cuanto");
            lore.add(ChatColor.of("#8A8A8A") + "tocas el suelo.");
            meta.setLore(lore);

            // Si añades un modelo propio al pack, cámbialo aquí.


            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(plumaMejoradaKey, PersistentDataType.BYTE, (byte) 1);

            meta.setItemModel(NamespacedKey.minecraft("pluma_levi_mejorada"));
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isManzanaVida(ItemStack item) {
        if (item == null || item.getType() != Material.APPLE) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(manzanaKey, PersistentDataType.BYTE);
    }

    public boolean isPlumaLevitacion(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plumaKey, PersistentDataType.BYTE);
    }

    public boolean isPlumaLevitacionMejorada(ItemStack item) {
        if (item == null || item.getType() != Material.FEATHER) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plumaMejoradaKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onConsumeManzana(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isManzanaVida(item)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2, true, true));

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ARMADILLO_EAT, 1.0f, 2.0f);
        }
    }

    @EventHandler
    public void onInteractPluma(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        boolean mejorada = isPlumaLevitacionMejorada(item);
        if (!mejorada && !isPlumaLevitacion(item)) return;

        event.setCancelled(true);

        if (player.hasCooldown(Material.FEATHER)) {
            return;
        }

        int duracion = mejorada ? LEVITACION_MEJORADA_TICKS : LEVITACION_NORMAL_TICKS;
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duracion, 10, true, true));

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 0.5f, 2.0f);

        if (mejorada) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 0.4, 0.1, 0.4, 0.02);

            protegerCaida(player);
        }

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }

        player.setCooldown(Material.FEATHER, mejorada ? 60 : 20);
    }

    // --------------------------------------------------------
    //  PROTECCIÓN DE CAÍDA DE LA PLUMA MEJORADA
    // --------------------------------------------------------

    /**
     * Protege la caida del jugador hasta que aterrice de verdad.
     *
     * Mientras dure, la distancia de caida se pone a 0 cada tick: al tocar el
     * suelo no hay daño que calcular. El evento de daño queda como respaldo por
     * si el aterrizaje llega entre dos ticks.
     */
    private void protegerCaida(Player player) {
        UUID id = player.getUniqueId();

        // Si ya estaba protegido (por ejemplo, otra pluma en el aire) la tarea
        // anterior sigue viva y vale para los dos usos.
        if (!caidaProtegida.add(id)) return;

        player.setFallDistance(0);

        new BukkitRunnable() {
            int ticks = 0;
            int ticksEnSuelo = 0;
            boolean haEstadoEnElAire = false;

            @Override
            public void run() {
                ticks++;

                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.isOnline() || p.isDead() || !caidaProtegida.contains(id)) {
                    caidaProtegida.remove(id);
                    cancel();
                    return;
                }

                p.setFallDistance(0);

                // Mientras siga subiendo con la levitación no comprobamos nada.
                if (p.hasPotionEffect(PotionEffectType.LEVITATION)) {
                    ticksEnSuelo = 0;
                    haEstadoEnElAire = true;
                    return;
                }

                boolean apoyado = p.isOnGround() || p.isInWater() || p.isClimbing();

                if (!apoyado) {
                    ticksEnSuelo = 0;
                    haEstadoEnElAire = true;
                } else {
                    ticksEnSuelo++;

                    if (ticksEnSuelo == 1 && haEstadoEnElAire) {
                        efectoAterrizaje(p);
                        haEstadoEnElAire = false;
                    }

                    // Ya aterrizó y se ha quedado en el suelo: se acabó la protección.
                    if (ticksEnSuelo >= TICKS_ATERRIZAJE) {
                        caidaProtegida.remove(id);
                        cancel();
                        return;
                    }
                }

                if (ticks > MAX_TICKS_PROTECCION) {
                    caidaProtegida.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void efectoAterrizaje(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.9f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.4, 0.1, 0.4, 0.03);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!caidaProtegida.contains(player.getUniqueId())) return;

        // Las perlas de ender tambien cuentan como FALL. Antes gastaban la
        // proteccion y la caida de verdad luego hacia daño.
        if (event.getDamageSource().getDamageType() == DamageType.ENDER_PEARL) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        caidaProtegida.remove(event.getPlayer().getUniqueId());
    }
}
