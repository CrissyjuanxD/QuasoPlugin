package items.tienda;

import Encantamientos.RetornoDelVacio;
import Events.MissionSystem.MissionUtils;
import Handlers.ActionBarHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

// Lo que los items de la tienda hacen con el tiempo: los efectos que duran unos minutos (incienso, polvo silencioso,
// tónico, sabiduría y talismán), lo que funciona solo por llevarlo (imán, linterna) y la pluma que salva del vacío
public final class EfectosTienda implements Listener {

    enum Buff {
        INCIENSO("Incienso Ahuyentador", "#e0a35e", 10 * 60),
        SILENCIO("Polvo Silencioso", "#8fb3c9", 3 * 60),
        ANTILEVITACION("Tónico Antilevitación", "#c9a7ff", 4 * 60),
        SABIDURIA("Frasco de Sabiduría", "#b6f06e", 10 * 60),
        BOTIN("Talismán del Botín", "#f2c46b", 10 * 60);

        final String nombre;
        final ChatColor color;
        final int segundos;

        Buff(String nombre, String color, int segundos) {
            this.nombre = nombre;
            this.color = ChatColor.of(color);
            this.segundos = segundos;
        }
    }

    private static final double RADIO_INCIENSO = 24;
    private static final double RADIO_IMAN = 8;

    private final JavaPlugin plugin;
    private final Map<UUID, EnumMap<Buff, Long>> activos = new HashMap<>();
    private final Random random = new Random();

    public EfectosTienda(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::cadaSegundo, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::iman, 5L, 4L);
    }

    void activar(Player player, Buff buff) {
        activos.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(Buff.class))
                .put(buff, System.currentTimeMillis() + buff.segundos * 1000L);
        ActionBarHandler.get(plugin).sendNotification(player, "tienda:" + buff.name(),
                buff.color + "✦ " + buff.nombre + ChatColor.GRAY + " activo por " + ChatColor.WHITE + buff.segundos / 60 + " min");
    }

    boolean tiene(Player player, Buff buff) {
        EnumMap<Buff, Long> buffs = activos.get(player.getUniqueId());
        Long hasta = buffs == null ? null : buffs.get(buff);
        return hasta != null && hasta > System.currentTimeMillis();
    }

    // Avisa cuando se termina un efecto y mantiene la Visión Nocturna de la Linterna de Almas
    private void cadaSegundo() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, EnumMap<Buff, Long>>> jugadores = activos.entrySet().iterator();
        while (jugadores.hasNext()) {
            Map.Entry<UUID, EnumMap<Buff, Long>> entrada = jugadores.next();
            Player player = Bukkit.getPlayer(entrada.getKey());
            Iterator<Map.Entry<Buff, Long>> buffs = entrada.getValue().entrySet().iterator();
            while (buffs.hasNext()) {
                Map.Entry<Buff, Long> buff = buffs.next();
                if (buff.getValue() > ahora) continue;
                buffs.remove();
                if (player != null) {
                    ActionBarHandler.get(plugin).sendNotification(player, "tienda:fin:" + buff.getKey().name(),
                            ChatColor.GRAY + "Se terminó el " + buff.getKey().color + buff.getKey().nombre);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.5f, 1.4f);
                }
            }
            if (entrada.getValue().isEmpty()) jugadores.remove();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if ("linterna_almas".equals(ItemsTienda.idOf(player.getInventory().getItemInOffHand()))) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 15, 0, true, false, true));
            }
        }
    }

    // El Imán de Botín en la mano secundaria trae los items del suelo; no toca lo que tiró otro jugador ni lo que uno
    // mismo acaba de tirar
    private void iman() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!"iman_botin".equals(ItemsTienda.idOf(player.getInventory().getItemInOffHand()))) continue;
            Location destino = player.getLocation().add(0, 0.8, 0);
            for (Entity entity : player.getNearbyEntities(RADIO_IMAN, RADIO_IMAN, RADIO_IMAN)) {
                if (!(entity instanceof Item item) || item.isDead()) continue;
                UUID dueno = item.getOwner();
                if (dueno != null && !dueno.equals(player.getUniqueId())) continue;
                UUID tiro = item.getThrower();
                if (tiro != null && (!tiro.equals(player.getUniqueId()) || item.getTicksLived() < 100)) continue;
                Vector hacia = destino.toVector().subtract(item.getLocation().toVector());
                if (hacia.lengthSquared() < 1.5) continue;
                item.setVelocity(hacia.normalize().multiply(0.45).setY(Math.max(0.05, hacia.getY() * 0.1)));
            }
        }
    }

    // ---------------------------------------------------------------- Lo que cambia cada efecto

    // Incienso: nada hostil aparece solo cerca del jugador (las hordas y los jefes sí)
    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(event.getEntity() instanceof Enemy)) return;
        Location lugar = event.getLocation();
        for (Map.Entry<UUID, EnumMap<Buff, Long>> entrada : activos.entrySet()) {
            Long hasta = entrada.getValue().get(Buff.INCIENSO);
            if (hasta == null || hasta <= System.currentTimeMillis()) continue;
            Player player = Bukkit.getPlayer(entrada.getKey());
            if (player != null && player.getWorld().equals(lugar.getWorld())
                    && player.getLocation().distanceSquared(lugar) <= RADIO_INCIENSO * RADIO_INCIENSO) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Polvo Silencioso: sus pasos, golpes y bloques no le llegan a los sensores, chilladores ni Wardens
    @EventHandler(ignoreCancelled = true)
    public void onVibracion(GenericGameEvent event) {
        if (event.getEntity() instanceof Player player && tiene(player, Buff.SILENCIO)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLevitacion(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PotionEffect nuevo = event.getNewEffect();
        if (nuevo != null && nuevo.getType().equals(PotionEffectType.LEVITATION) && tiene(player, Buff.ANTILEVITACION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExperiencia(PlayerExpChangeEvent event) {
        if (event.getAmount() > 0 && tiene(event.getPlayer(), Buff.SABIDURIA)) event.setAmount(event.getAmount() * 2);
    }

    // Talismán: cada objeto normal que suelta un monstruo tiene 50% de salir doble, y 50% más de experiencia. No cuenta
    // jefes, mobs de spawner ni los items custom (almas, fragmentos, esencias...)
    @EventHandler(priority = EventPriority.HIGH)
    public void onMuerte(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null || !(entity instanceof Enemy) || !tiene(killer, Buff.BOTIN) || MissionUtils.bossId(entity) != null) return;
        CreatureSpawnEvent.SpawnReason razon = entity.getEntitySpawnReason();
        if (razon == CreatureSpawnEvent.SpawnReason.SPAWNER || razon == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER
                || razon == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG || entity.fromMobSpawner()) return;
        int cuantos = event.getDrops().size();
        for (int i = 0; i < cuantos; i++) {
            ItemStack drop = event.getDrops().get(i);
            if (esNormal(drop) && random.nextBoolean()) event.getDrops().add(drop.clone());
        }
        event.setDroppedExp((int) Math.round(event.getDroppedExp() * 1.5));
    }

    private static boolean esNormal(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.hasItemMeta()) return true;
        ItemMeta meta = item.getItemMeta();
        return !meta.hasCustomModelData() && !meta.hasDisplayName() && meta.getPersistentDataContainer().isEmpty();
    }

    // Pluma del Vacío: si cae al vacío del End la gasta y lo devuelve al último suelo firme (el encantamiento Retorno
    // del Vacío va antes; si ese lo salvó, la pluma no se gasta)
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVacio(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) return;
        ItemStack[] contenido = player.getInventory().getContents();
        for (int slot = 0; slot < contenido.length; slot++) {
            if (!"pluma_vacio".equals(ItemsTienda.idOf(contenido[slot]))) continue;
            ItemStack pluma = contenido[slot];
            pluma.setAmount(pluma.getAmount() - 1);
            player.getInventory().setItem(slot, pluma.getAmount() > 0 ? pluma : null);

            Location destino = RetornoDelVacio.ultimoSuelo(player);
            if (destino == null || !world.equals(destino.getWorld())) destino = world.getSpawnLocation();
            Location desde = player.getLocation();
            event.setCancelled(true);
            player.setFallDistance(0);
            player.setVelocity(new Vector());
            player.teleport(destino);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
            world.spawnParticle(Particle.REVERSE_PORTAL, desde, 50, 0.5, 1, 0.5, 0.1);
            world.spawnParticle(Particle.END_ROD, destino.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.05);
            world.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 0.8f);
            ActionBarHandler.get(plugin).sendNotification(player, "tienda:pluma", ChatColor.of("#b57edc") + "✦ ¡La Pluma del Vacío te salvó!");
            return;
        }
    }
}
