package EffectListener;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class KeepInventoryEffect implements CustomEffect, Listener {

    private static final boolean EVITAR_SLIMES = true;
    private static final double RADIO_BLOQUEO_SLIMES = 8.0;

    private final Plugin plugin;
    private final NamespacedKey duracionKey;
    private final NamespacedKey amplificadorKey;

    private final List<Location> muertesRecientes = new ArrayList<>();
    private final Set<UUID> silenciados = new HashSet<>();

    public KeepInventoryEffect(Plugin plugin) {
        this.plugin = plugin;
        this.duracionKey = new NamespacedKey(plugin, "keepinv_duracion_restante");
        this.amplificadorKey = new NamespacedKey(plugin, "keepinv_amplificador");
    }

    // =========================================================================
    // AVISOS (solo al beber el líquido y al caducar de verdad)
    // =========================================================================

    @Override
    public void applyEffect(Player player, int durationSeconds, int amplifier) {
        if (silenciados.contains(player.getUniqueId())) return;

        Location loc = player.getLocation();

        player.playSound(loc, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 0.8f, 1.6f);
        player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);

        Particle.DustOptions celeste = new Particle.DustOptions(Color.fromRGB(168, 223, 255), 1.3f);
        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 35, 0.45, 0.7, 0.45, 0, celeste);

        enviarActionBar(player, ChatColor.of("#A8DFFF") + "✦ Tu inventario está protegido.");
    }

    @Override
    public void removeEffect(Player player) {
        if (player.isDead() || !player.isOnline()) return;
        if (silenciados.contains(player.getUniqueId())) return;

        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.5f);
        enviarActionBar(player, ChatColor.of("#7E9DC4") + "✧ La protección de inventario se ha disipado.");
    }

    @Override
    public PotionEffectType getTriggerEffectType() {
        return PotionEffectType.LUCK;
    }

    @Override
    public boolean isEffectActive(Player player) {
        return player.hasPotionEffect(PotionEffectType.LUCK);
    }

    @Override
    public void cleanup() {
        muertesRecientes.clear();
        silenciados.clear();
    }

    private void desilenciar(Player player, long ticks) {
        UUID id = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> silenciados.remove(id), ticks);
    }

    // =========================================================================
    // 1. MUERTE: conservar inventario + guardar el efecto
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        silenciados.add(player.getUniqueId());

        PotionEffect efecto = player.getPotionEffect(PotionEffectType.LUCK);
        if (efecto == null) return;

        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(duracionKey, PersistentDataType.INTEGER, efecto.getDuration());
        data.set(amplificadorKey, PersistentDataType.INTEGER, efecto.getAmplifier());

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        if (EVITAR_SLIMES) {
            Location loc = player.getLocation().clone();
            muertesRecientes.add(loc);
            Bukkit.getScheduler().runTaskLater(plugin, () -> muertesRecientes.remove(loc), 3L);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1.0f, 1.5f);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSlimeSpawn(CreatureSpawnEvent event) {
        if (!EVITAR_SLIMES) return;
        if (muertesRecientes.isEmpty()) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.POTION_EFFECT) return;
        if (!(event.getEntity() instanceof Slime)) return;

        Location spawn = event.getLocation();
        for (Location muerte : muertesRecientes) {
            if (!muerte.getWorld().equals(spawn.getWorld())) continue;
            if (muerte.distanceSquared(spawn) <= RADIO_BLOQUEO_SLIMES * RADIO_BLOQUEO_SLIMES) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // =========================================================================
    // 2. REVIVIR: devolver el efecto, sin avisos
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        restaurar(event.getPlayer());
        desilenciar(event.getPlayer(), 5L);
    }

    /** Al usar un tótem vanilla borra todo y TotemEffectRestorer lo devuelve 1 tick después. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        silenciados.add(player.getUniqueId());
        desilenciar(player, 5L);
    }

    /** LOWEST para marcar el silencio ANTES de que CustomEffectManager reaplique el efecto. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        silenciados.add(player.getUniqueId());
        restaurar(player);
        desilenciar(player, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        silenciados.remove(event.getPlayer().getUniqueId());
    }

    // El efecto guardado solo se borra cuando se devuelve: si entra todavía muerto (se fue en la pantalla de muerte)
    // queda guardado y se le da al revivir
    private void restaurar(Player player) {
        if (!player.getPersistentDataContainer().has(duracionKey, PersistentDataType.INTEGER)) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || player.isDead()) return;

            PersistentDataContainer data = player.getPersistentDataContainer();
            Integer duracion = data.get(duracionKey, PersistentDataType.INTEGER);
            if (duracion == null) return;
            Integer guardado = data.get(amplificadorKey, PersistentDataType.INTEGER);
            int amplificador = guardado == null ? 0 : guardado;
            data.remove(duracionKey);
            data.remove(amplificadorKey);

            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, duracion, amplificador, true, true, true));
        }, 1L);
    }

    private void enviarActionBar(Player player, String mensaje) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(mensaje));
    }
}