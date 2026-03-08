package Habilidades;

import Handlers.ActionBarHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class HabilidadesListener implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    private final HabilidadesEffects effects;
    private final ActionBarHandler actionBar;

    private final Map<UUID, Integer> jumpCount = new HashMap<>();

    // --- NUEVAS VARIABLES BASADAS EN EL SISTEMA ROBUSTO ---
    private final Set<UUID> protectNextLanding = new HashSet<>();
    private final Map<UUID, Double> storedFallDistance = new HashMap<>();

    public HabilidadesListener(JavaPlugin plugin, HabilidadesManager manager, HabilidadesEffects effects) {
        this.plugin = plugin;
        this.manager = manager;
        this.effects = effects;
        this.actionBar = new ActionBarHandler(plugin);
    }

    // --- GUI ---
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.KNOWLEDGE_BOOK && item.hasItemMeta()) {
                if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999) {
                    event.setCancelled(true);
                    HabilidadesGUI gui = new HabilidadesGUI(plugin, manager, null);
                    gui.openHabilidadesGUI(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { effects.reapplyAllEffects(event.getPlayer(), manager); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> effects.reapplyAllEffects(event.getPlayer(), manager), 5L);
    }

    @EventHandler
    public void onTotem(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player player) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> effects.reapplyAllEffects(player, manager), 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        jumpCount.remove(uuid);
        protectNextLanding.remove(uuid);
        storedFallDistance.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (protectNextLanding.contains(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        int resLevel = manager.getHighestLevel(player.getUniqueId(), HabilidadesType.RESISTENCIA);
        if (resLevel == 0) return;

        boolean blocked = false;

        if (resLevel >= 3) {
            // Nivel 3: 10% de probabilidad de bloquear CUALQUIER daño
            if (Math.random() < 0.10) blocked = true;
        } else if (resLevel == 2) {
            // Nivel 2: 10% de probabilidad de bloquear daño de MONSTRUOS
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent byEntity = (EntityDamageByEntityEvent) event;
                if (byEntity.getDamager() instanceof Monster) {
                    if (Math.random() < 0.10) blocked = true;
                }
            }
        } else if (resLevel == 1) {
            // Nivel 1: 10% de probabilidad de bloquear PROYECTILES
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent byEntity = (EntityDamageByEntityEvent) event;
                if (byEntity.getDamager() instanceof org.bukkit.entity.Projectile) {
                    if (Math.random() < 0.10) blocked = true;
                }
            }
        }

        if (blocked) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
            actionBar.sendActionBar(player, ChatColor.AQUA + "¡Daño Bloqueado!");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        double fallDistance;

        if (!player.isOnGround() && player.getFallDistance() > 0.0F) {
            fallDistance = storedFallDistance.getOrDefault(playerId, 0.0);
            storedFallDistance.put(playerId, Math.max(fallDistance, player.getFallDistance()));
        }

        if (player.isOnGround()) {
            jumpCount.put(playerId, 0);

            if (storedFallDistance.containsKey(playerId)) {
                fallDistance = storedFallDistance.get(playerId);
                storedFallDistance.remove(playerId);

                boolean isProtected = protectNextLanding.contains(playerId);

                if (!isProtected && fallDistance > 3.0) {
                    double damage = fallDistance - 3.0;
                    if (damage > 0.0) {
                        player.damage(damage);
                    }
                }
                protectNextLanding.remove(playerId);
            }

            if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 2)) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            }
        }
        else {
            if (!protectNextLanding.contains(playerId)
                    && player.getFallDistance() > 3.0
                    && player.getAllowFlight()) {

                player.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        int maxJumps = 0;
        if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 4)) maxJumps = 2;
        else if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 2)) maxJumps = 1;

        if (maxJumps == 0) return;

        int current = jumpCount.getOrDefault(playerId, 0);

        if (current < maxJumps) {
            jumpCount.put(playerId, current + 1);

            protectNextLanding.add(playerId);
            player.setFallDistance(0f);
            storedFallDistance.put(playerId, 0.0);

            Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(0.8);
            player.setVelocity(velocity);

            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1.2f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 13, 0, 0, 0, 0.1);

            if (current + 1 == maxJumps) {
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1);
            }

            if (current + 1 < maxJumps) {
                player.setAllowFlight(true);
            }
        }
    }
}