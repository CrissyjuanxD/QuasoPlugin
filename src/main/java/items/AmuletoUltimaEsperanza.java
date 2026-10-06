package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * AMULETO DE LA ÚLTIMA ESPERANZA.
 *
 * Te saca de donde estés y te manda a tu último spawn, pero el viaje se paga:
 * llegas con Debilidad II, Veneno II, Ceguera II y Confusión II.
 *
 * Al usarlo se forma una esfera de partículas alrededor del jugador durante
 * 2.5 segundos; al completarse, teletransporta (misma logica que el Life Totem),
 * salta la animacion de totem con el propio amuleto y la esfera se expande
 * difuminándose en el destino.
 */
public class AmuletoUltimaEsperanza implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey amuletoKey;
    private final Random random = new Random();

    /** Material del item (tambien se usa para el cooldown). */
    private static final Material MATERIAL = Material.ECHO_SHARD;

    /** Tiempos de la animacion. */
    private static final int TICKS_FORMACION = 50; // 2.5 s
    private static final int TICKS_EXPANSION = 20; // 1 s de difuminado
    private static final int COOLDOWN_TICKS = 20 * 10;

    /** Efectos de castigo al llegar (duracion en ticks, amplificador 1 = nivel II). */
    private static final int DUR_DEBILIDAD = 20 * 30;
    private static final int DUR_VENENO = 20 * 30;
    private static final int DUR_CEGUERA = 20 * 30;
    private static final int DUR_CONFUSION = 20 * 30;

    /** Azules pastel, blancos y dorados. */
    private static final Color[] COLORES = {
            Color.fromRGB(168, 223, 255), // azul celeste pastel
            Color.fromRGB(158, 197, 255), // azul pastel
            Color.fromRGB(205, 232, 255), // azul muy claro
            Color.WHITE,
            Color.fromRGB(245, 245, 255), // blanco azulado
            Color.fromRGB(255, 214, 102), // dorado
            Color.fromRGB(255, 236, 170)  // dorado pastel
    };

    /** Jugadores con el ritual en curso (evita usarlo dos veces a la vez). */
    private final Set<UUID> enRitual = new HashSet<>();

    /** Tiempo tras el teleport en el que no se recibe daño de caida. */
    private static final long PROTECCION_CAIDA_MS = 5000L;

    /** Hasta cuando (millis) cada jugador esta protegido de la caida tras el teleport. */
    private final Map<UUID, Long> sinDanoCaidaHasta = new HashMap<>();

    public AmuletoUltimaEsperanza(JavaPlugin plugin) {
        this.plugin = plugin;
        this.amuletoKey = new NamespacedKey(plugin, "amuleto_ultima_esperanza");
    }

    // ========================================================================
    //  ITEM
    // ========================================================================

    public ItemStack createAmuleto() {
        ItemStack item = new ItemStack(MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#A8DFFF") + ChatColor.BOLD.toString() + "Amuleto de la Última Esperanza");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#CDE8FF") + "Click derecho para invocarlo.");
            lore.add("");
            lore.add(ChatColor.of("#CDE8FF") + "Te devuelve a tu " + ChatColor.of("#FFD666") + ChatColor.BOLD + "último spawn"
                    + ChatColor.of("#CDE8FF") + ",");
            lore.add(ChatColor.of("#CDE8FF") + "pero el viaje te deja tocado:");
            lore.add("");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#B9C7E0") + "Debilidad II");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#A9D9A0") + "Veneno II");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#8F8F8F") + "Ceguera II");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#C9A0D9") + "Confusión II");
            lore.add("");
            lore.add(ChatColor.of("#8A8A8A") + "El ritual tarda 2.5 segundos.");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);


            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(amuletoKey, PersistentDataType.BYTE, (byte) 1);


            ItemModels.apply(meta, "amuleto_ultima_esperanza");
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isAmuleto(ItemStack item) {
        if (item == null || item.getType() != MATERIAL || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(amuletoKey, PersistentDataType.BYTE);
    }

    // ========================================================================
    //  USO
    // ========================================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isAmuleto(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.hasCooldown(MATERIAL)) return;
        if (enRitual.contains(player.getUniqueId())) return;

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.setAmount(item.getAmount() - 1);
        }
        player.setCooldown(MATERIAL, COOLDOWN_TICKS);

        iniciarRitual(player);
    }

    // ========================================================================
    //  SPAWN (misma logica que el Life Totem)
    // ========================================================================

    /** Último spawn del jugador: cama/ancla y, si no tiene, el spawn del mundo principal. */
    private Location getUltimoSpawn(Player player) {
        Location respawn = player.getRespawnLocation();
        if (respawn != null && respawn.getWorld() != null) {
            return respawn;
        }

        World overworld = Bukkit.getWorlds().get(0);
        return overworld.getSpawnLocation();
    }

    /** Busca un sitio donde quepa el jugador (ni en el aire ni dentro de bloques). */
    private Location findSafeLocation(Location location) {
        location = location.clone();

        // Buscar el bloque sólido más alto
        while (location.getBlock().getType().isAir() && location.getY() > location.getWorld().getMinHeight()) {
            location.subtract(0, 1, 0);
        }

        // Ajustar a 1 bloque arriba del suelo
        location.add(0, 1, 0);

        // Verificar que la cabeza y los pies no estén en bloques sólidos
        if (!location.getBlock().getType().isSolid()
                && !location.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
            return location;
        }

        // Si no es seguro, buscar una ubicación cercana
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location testLoc = location.clone().add(x, 0, z);
                if (!testLoc.getBlock().getType().isSolid()
                        && !testLoc.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    return testLoc;
                }
            }
        }

        // Si no se encuentra ubicación segura, encima del bloque más alto
        return location.getWorld().getHighestBlockAt(location).getLocation().add(0.5, 1, 0.5);
    }

    // ========================================================================
    //  RITUAL
    // ========================================================================

    private void iniciarRitual(Player player) {
        UUID id = player.getUniqueId();
        enRitual.add(id);

        World world = player.getWorld();
        Location inicio = player.getLocation();

        world.playSound(inicio, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.4f);
        world.playSound(inicio, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
        world.playSound(inicio, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.2f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(id);

                if (p == null || !p.isOnline() || p.isDead()) {
                    enRitual.remove(id);
                    cancel();
                    return;
                }

                try {
                    if (tick < TICKS_FORMACION) {
                        formarEsfera(p);
                    } else if (tick == TICKS_FORMACION) {
                        teletransportar(p);
                    } else {
                        int pasoExpansion = tick - TICKS_FORMACION;
                        expandirEsfera(p, pasoExpansion);

                        if (pasoExpansion >= TICKS_EXPANSION) {
                            enRitual.remove(id);
                            cancel();
                            return;
                        }
                    }
                } catch (Exception e) {
                    // Un fallo aqui no puede dejar la tarea repitiendose cada tick.
                    plugin.getLogger().warning("[Amuleto] Error en el ritual de " + p.getName() + ": " + e.getMessage());
                    enRitual.remove(id);
                    cancel();
                    return;
                }

                tick++;
            }

            /** La esfera se va cerrando alrededor del jugador. */
            private void formarEsfera(Player p) {
                Location centro = p.getLocation().clone().add(0, 1.0, 0);
                double progreso = (double) tick / TICKS_FORMACION;
                double radio = 0.4 + progreso * 1.8;

                dibujarEsfera(centro, radio, 1.1f, 26);

                // Anillo girando a los pies, para que se note el ritual.
                double angulo = tick * 0.5;
                for (int i = 0; i < 3; i++) {
                    double a = angulo + (i * (2 * Math.PI / 3));
                    Location l = p.getLocation().clone().add(Math.cos(a) * 1.4, 0.1, Math.sin(a) * 1.4);
                    dust(l, colorAleatorio(), 1.3f);
                }

                if (tick % 10 == 0) {
                    float pitch = 0.8f + (float) progreso * 1.0f;
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, pitch);
                    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5f, pitch);
                }
                if (tick == TICKS_FORMACION - 10) {
                    p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.8f);
                }
            }

            /** La esfera se expande y se difumina en el destino. */
            private void expandirEsfera(Player p, int pasoExpansion) {
                Location centro = p.getLocation().clone().add(0, 1.0, 0);
                double progreso = (double) pasoExpansion / TICKS_EXPANSION;
                double radio = 2.2 + progreso * 4.0;
                float tamano = (float) Math.max(0.3, 1.4 - progreso * 1.1);

                dibujarEsfera(centro, radio, tamano, 34);

                if (pasoExpansion == 1) {
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.5f);
                }
                if (pasoExpansion % 6 == 0) {
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.7f);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void teletransportar(Player p) {
        Location origen = p.getLocation().clone();
        World mundoOrigen = origen.getWorld();

        // Efectos antes del teleport
        mundoOrigen.playSound(origen, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.8f);
        // En 1.21.11 FLASH necesita color: sin el lanza una excepcion.
        mundoOrigen.spawnParticle(Particle.FLASH, origen.clone().add(0, 1, 0), 1, 0, 0, 0, 0, Color.AQUA);
        mundoOrigen.spawnParticle(Particle.PORTAL, origen, 50);
        dibujarEsfera(origen.clone().add(0, 1, 0), 2.2, 1.4f, 40);

        Location destino = findSafeLocation(getUltimoSpawn(p));

        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }

        // Si venia cayendo (por ejemplo al vacio) llega sin la caida acumulada
        // ni la velocidad, y el aterrizaje no le hace daño durante un momento.
        p.setFallDistance(0);
        p.setVelocity(new Vector(0, 0, 0));
        sinDanoCaidaHasta.put(p.getUniqueId(), System.currentTimeMillis() + PROTECCION_CAIDA_MS);

        if (!p.teleport(destino)) {
            sinDanoCaidaHasta.remove(p.getUniqueId());
            p.sendMessage(ChatColor.of("#FF9E9E") + "۞ El amuleto no ha podido llevarte a tu último spawn.");
            return;
        }

        p.setFallDistance(0);
        p.setVelocity(new Vector(0, 0, 0));

        // Efectos despues del teleport
        p.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.3f);
        p.playSound(destino, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.2f);
        p.getWorld().spawnParticle(Particle.PORTAL, destino, 50);

        mostrarAnimacionTotem(p);
        aplicarCastigo(p);
    }

    /**
     * Animacion de totem (la de la pantalla, con su sonido y particulas) pero
     * mostrando el amuleto.
     *
     * El cliente pinta el item de totem que tenga en la mano, asi que durante un
     * instante le ponemos un totem con el modelo del amuleto, lanzamos el efecto
     * y le devolvemos su item. Todo pasa en el mismo tick y en orden, asi que el
     * jugador no llega a ver el cambio.
     */
    private void mostrarAnimacionTotem(Player p) {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        if (meta != null) {
            ItemModels.apply(meta, "amuleto_ultima_esperanza");
            totem.setItemMeta(meta);
        }

        ItemStack enMano = p.getInventory().getItemInMainHand();

        p.getInventory().setItemInMainHand(totem);
        p.updateInventory();

        p.playEffect(EntityEffect.PROTECTED_FROM_DEATH);

        p.getInventory().setItemInMainHand(enMano);
        p.updateInventory();
    }

    private void aplicarCastigo(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, DUR_DEBILIDAD, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, DUR_VENENO, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, DUR_CEGUERA, 1, false, true, true));

        // Confusión = efecto custom del plugin, montado sobre UNLUCK.
        player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, DUR_CONFUSION, 1, false, false, true));

        player.sendMessage(ChatColor.of("#A8DFFF") + "" + ChatColor.BOLD + "۞ "
                + ChatColor.of("#CDE8FF") + "El amuleto te ha devuelto a tu último spawn"
                + ChatColor.of("#8A8A8A") + ", pero el viaje te ha pasado factura.");
    }

    // ========================================================================
    //  PARTÍCULAS
    // ========================================================================

    private Color colorAleatorio() {
        return COLORES[random.nextInt(COLORES.length)];
    }

    private void dust(Location loc, Color color, float size) {
        World w = loc.getWorld();
        if (w == null) return;

        w.spawnParticle(Particle.DUST, loc.getX(), loc.getY(), loc.getZ(),
                1, 0.0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, size));
    }

    /** Esfera de puntos repartidos por la superficie (espiral de Fibonacci). */
    private void dibujarEsfera(Location centro, double radio, float tamano, int puntos) {
        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        double desfase = random.nextDouble() * Math.PI * 2;

        for (int i = 0; i < puntos; i++) {
            double y = 1 - (i / (double) (puntos - 1)) * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double theta = phi * i + desfase;

            double x = Math.cos(theta) * r;
            double z = Math.sin(theta) * r;

            dust(centro.clone().add(x * radio, y * radio, z * radio), colorAleatorio(), tamano);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        enRitual.remove(event.getPlayer().getUniqueId());
        sinDanoCaidaHasta.remove(event.getPlayer().getUniqueId());
    }

    /** Anula el daño de caida justo despues del teleport (por si venia cayendo). */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Long hasta = sinDanoCaidaHasta.get(player.getUniqueId());
        if (hasta == null) return;

        if (System.currentTimeMillis() > hasta) {
            sinDanoCaidaHasta.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);
    }
}