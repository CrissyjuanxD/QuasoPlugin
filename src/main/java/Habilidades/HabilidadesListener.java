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
    private final Set<UUID> protectNextLanding = new HashSet<>();

    public HabilidadesListener(JavaPlugin plugin, HabilidadesManager manager, HabilidadesEffects effects) {
        this.plugin = plugin;
        this.manager = manager;
        this.effects = effects;
        this.actionBar = new ActionBarHandler(plugin);
    }

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
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.MILK_BUCKET) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                effects.reapplyAllEffects(event.getPlayer(), manager);
            }, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        jumpCount.remove(uuid);
        protectNextLanding.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // --- SISTEMA DOBLE SALTO (SOLO SI SALTA SE LE PERDONA EL DAÑO) ---
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (protectNextLanding.contains(player.getUniqueId())) {
                event.setCancelled(true);
                protectNextLanding.remove(player.getUniqueId());
                return;
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        // --- SISTEMA RESISTENCIA ---
        int resLevel = manager.getHighestLevel(player.getUniqueId(), HabilidadesType.RESISTENCIA);
        if (resLevel == 0) return;

        boolean blocked = false;
        double chance = 0.0;
        boolean canBlock = false;

        boolean isProjectile = (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof org.bukkit.entity.Projectile);
        boolean isMonster = (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Monster);

        if (isProjectile) {
            if (resLevel >= 5) chance = 0.14;
            else chance = 0.08;
            canBlock = true;
        } else if (isMonster) {
            if (resLevel >= 6) chance = 0.14;
            else if (resLevel >= 2) chance = 0.08;
            canBlock = chance > 0;
        } else {
            if (resLevel >= 7) chance = 0.14;
            else if (resLevel >= 3) chance = 0.08;
            canBlock = chance > 0;
        }

        if (canBlock && Math.random() < chance) {
            blocked = true;
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

        if (player.isOnGround()) {
            if (jumpCount.getOrDefault(playerId, 0) > 0) {
                jumpCount.put(playerId, 0);
            }

            if (player.getFallDistance() == 0.0f) {
                protectNextLanding.remove(playerId);
            }

            if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 2)) {
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }

                try {
                    if (player.hasFlyingFallDamage() != net.kyori.adventure.util.TriState.TRUE) {
                        player.setFlyingFallDamage(net.kyori.adventure.util.TriState.TRUE);
                    }
                } catch (NoSuchMethodError ignored) {
                }
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
        if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 8)) maxJumps = 3;
        else if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 4)) maxJumps = 2;
        else if (manager.hasHabilidad(playerId, HabilidadesType.AGILIDAD, 2)) maxJumps = 1;

        if (maxJumps == 0) return;

        if (player.isOnGround()) {
            player.setAllowFlight(true);
            try {
                player.setFlyingFallDamage(net.kyori.adventure.util.TriState.TRUE);
            } catch (NoSuchMethodError ignored) {}
            return;
        }

        int current = jumpCount.getOrDefault(playerId, 0);

        if (current < maxJumps) {
            jumpCount.put(playerId, current + 1);

            protectNextLanding.add(playerId);

            player.setFallDistance(0f);

            Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(0.8);
            player.setVelocity(velocity);

            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1.2f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 13, 0, 0, 0, 0.1);

            if (current + 1 == maxJumps) {
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1);
            }

            if (current + 1 < maxJumps) {
                player.setAllowFlight(true);
                try {
                    player.setFlyingFallDamage(net.kyori.adventure.util.TriState.TRUE);
                } catch (NoSuchMethodError ignored) {}
            }
        }
    }
}