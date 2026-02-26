package Habilidades;

import Handlers.ActionBarHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
    // Set para saber si el jugador está actualmente en una secuencia de saltos permitidos
    private final Set<UUID> isDoubleJumping = new HashSet<>();

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
            if (item != null && item.getType() == Material.BOOK && item.hasItemMeta()) {
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

    // --- DOBLE SALTO Y CAÍDA ---

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Si está marcado como que usó su habilidad de salto, cancelamos daño
            if (isDoubleJumping.contains(player.getUniqueId())) {
                event.setCancelled(true);
                // No lo removemos aquí todavía, esperamos a que toque suelo en onMove
            }
            // Si NO está en la lista (se le quitó el allowFlight por caerse normal), recibe daño normal.
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        // LÓGICA DE SUELO
        if (player.isOnGround()) {
            // Reiniciamos todo al tocar suelo
            jumpCount.put(player.getUniqueId(), 0);
            isDoubleJumping.remove(player.getUniqueId()); // Ya no está saltando, aterrizó

            // Si tiene la habilidad, le permitimos volar para preparar el salto
            if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 2)) {
                player.setAllowFlight(true);
            }
        }
        // LÓGICA DE AIRE (Anti-Exploit de Caída)
        else {
            // Si el jugador está cayendo (fallDistance > 3), NO ha usado doble salto, y tiene allowFlight activado...
            // Significa que se cayó caminando. Hay que quitarle el vuelo para que se haga daño.
            if (!isDoubleJumping.contains(player.getUniqueId())
                    && player.getFallDistance() > 3.0
                    && player.getAllowFlight()) {

                player.setAllowFlight(false); // Le cortamos las alas para que la gravedad haga su trabajo
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        event.setCancelled(true);
        player.setAllowFlight(false); // Desactivar vuelo real
        player.setFlying(false);

        // Verificar niveles
        int maxJumps = 0;
        if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 4)) maxJumps = 2; // Triple salto (salto base + 2 aires)
        else if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.AGILIDAD, 2)) maxJumps = 1; // Doble salto

        int current = jumpCount.getOrDefault(player.getUniqueId(), 0);

        if (current < maxJumps) {
            jumpCount.put(player.getUniqueId(), current + 1);

            // MARCAR COMO SALTO SEGURO
            isDoubleJumping.add(player.getUniqueId());
            player.setFallDistance(0f); // Resetear daño de caída acumulado

            // Impulso
            Vector velocity = player.getLocation().getDirection().multiply(0.5).setY(0.8);
            player.setVelocity(velocity);

            // Efectos
            player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1.2f);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 5, 0, 0, 0, 0.1);

            // Sonido extra si es el último salto
            if (current + 1 == maxJumps) {
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1);
            }

            // Si le quedan saltos, permitir volar de nuevo para el siguiente input
            if (current + 1 < maxJumps) {
                player.setAllowFlight(true);
            } else {
                // Si ya no le quedan saltos, desactivamos allowFlight
                // PERO seguimos en isDoubleJumping para no hacernos daño al caer
                player.setAllowFlight(false);
            }
        }
    }


    // --- RESISTENCIA ---
    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Arrow || event.getDamager() instanceof org.bukkit.entity.Projectile)) return;

        if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.RESISTENCIA, 1)) {
            if (Math.random() < 0.10) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                actionBar.sendActionBar(player, ChatColor.AQUA + "¡Proyectil Bloqueado!");
            }
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (manager.hasHabilidad(victim.getUniqueId(), HabilidadesType.RESISTENCIA, 2)) {
            if (Math.random() < 0.05) {
                event.setCancelled(true);
                attacker.damage(event.getDamage());
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0,1,0), 10);
                victim.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 2f);
                actionBar.sendActionBar(victim, ChatColor.GOLD + "¡PARRY REALIZADO!");
                actionBar.sendActionBar(attacker, ChatColor.RED + "¡TE HICIERON PARRY!");
            }
        }
    }

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Monster)) return;

        if (manager.hasHabilidad(player.getUniqueId(), HabilidadesType.RESISTENCIA, 3)) {
            if (Math.random() < 0.10) {
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.5f);
                actionBar.sendActionBar(player, ChatColor.GREEN + "¡Golpe Bloqueado!");
            }
        }
    }
}