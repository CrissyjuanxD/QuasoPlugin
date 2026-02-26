package TitleListener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MuerteHandler implements Listener {
    private final JavaPlugin plugin;

    public MuerteHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        event.setDeathMessage(null); // Cancelar mensaje por defecto

        // Obtener la última causa de daño
        if (victim.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) victim.getLastDamageCause();
            Entity damager = damageEvent.getDamager();

            // Si el atacante es un jugador
            if (damager instanceof Player) {
                Player killer = (Player) damager;
                sendPlayerKilledMessage(victim, killer);
                return;
            }

            // Si el atacante es un mob
            if (damager instanceof LivingEntity) {
                LivingEntity mob = (LivingEntity) damager;
                sendMobKilledMessage(victim, mob);
                return;
            }

            // Si el atacante es un proyectil (flecha, etc.)
            if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof LivingEntity) {
                    LivingEntity shooter = (LivingEntity) projectile.getShooter();

                    if (shooter instanceof Player) {
                        sendPlayerKilledMessage(victim, (Player) shooter);
                    } else {
                        sendMobKilledMessage(victim, shooter);
                    }
                    return;
                }
            }
        }

        // Si no fue asesinado por otra entidad
        sendEnvironmentalDeathMessage(victim);
    }

    private void sendPlayerKilledMessage(Player victim, Player killer) {
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        String weaponName = weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()
                ? weapon.getItemMeta().getDisplayName()
                : formatItemName(weapon.getType().toString());

        String message;
        if (weapon.getType().isAir()) {
            message = String.format(
                    "[§7☠§7] §c%s §6ha muerto a manos de §c%s",
                    victim.getName(), killer.getName()
            );
        } else {
            message = String.format(
                    "[§7☠§7] §c%s §6ha muerto a manos de §c%s §6usando §d%s",
                    victim.getName(), killer.getName(), weaponName
            );
        }

        // Enviar a la consola
        Bukkit.getConsoleSender().sendMessage(message);

        // Enviar a los jugadores (formato JSON)
        String jsonMessage;
        if (weapon.getType().isAir()) {
            jsonMessage = String.format(
                    "[\"\",{\"text\":\"[\",\"bold\":true,\"color\":\"gray\"}," +
                            "{\"text\":\"\u2620\",\"bold\":true,\"color\":\"#AD3C3C\"}," +
                            "{\"text\":\"]\",\"bold\":true,\"color\":\"gray\"}," +
                            "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}," +
                            "{\"text\":\" ha muerto a manos de\",\"color\":\"#D9632B\"}," +
                            "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}]",
                    victim.getName(), killer.getName()
            );
        } else {
            jsonMessage = String.format(
                    "[\"\",{\"text\":\"[\",\"bold\":true,\"color\":\"gray\"}," +
                            "{\"text\":\"\u2620\",\"bold\":true,\"color\":\"#AD3C3C\"}," +
                            "{\"text\":\"]\",\"bold\":true,\"color\":\"gray\"}," +
                            "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}," +
                            "{\"text\":\" ha muerto a manos de\",\"color\":\"#D9632B\"}," +
                            "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}," +
                            "{\"text\":\" usando\",\"color\":\"#D9632B\"}," +
                            "{\"text\":\" \",\"bold\":true}," +
                            "{\"text\":\"%s\",\"italic\":true,\"color\":\"#F25396\"}," +
                            "{\"text\":\"\",\"bold\":true}]",
                    victim.getName(), killer.getName(), weaponName
            );
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
    }

    private void sendMobKilledMessage(Player victim, LivingEntity mob) {
        String mobName = mob.getCustomName() != null ? mob.getCustomName() : formatMobName(mob.getType().toString());

        // Mensaje para la consola
        String consoleMessage = String.format(
                "[§7☠§7] §c%s §6ha sido asesinado por §9%s",
                victim.getName(), mobName
        );
        Bukkit.getConsoleSender().sendMessage(consoleMessage);

        // Mensaje para los jugadores (formato JSON)
        String jsonMessage = String.format(
                "[\"\",{\"text\":\"[\",\"bold\":true,\"color\":\"gray\"}," +
                        "{\"text\":\"\u2620\",\"bold\":true,\"color\":\"#AD3C3C\"}," +
                        "{\"text\":\"]\",\"bold\":true,\"color\":\"gray\"}," +
                        "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}," +
                        "{\"text\":\" ha sido asesinado por\",\"color\":\"#D9632B\"}," +
                        "{\"text\":\" \",\"bold\":true}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#2B95CC\"}," +
                        "{\"text\":\"\",\"bold\":true}]",
                victim.getName(), mobName
        );

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
    }

    private void sendEnvironmentalDeathMessage(Player victim) {
        String deathCause = "desconocida";

        if (victim.getLastDamageCause() != null) {
            deathCause = translateCause(victim.getLastDamageCause().getCause());
        }

        // Mensaje para la consola
        String consoleMessage = String.format(
                "[§7☠§7] §c%s §6ha muerto por §9%s",
                victim.getName(), deathCause
        );
        Bukkit.getConsoleSender().sendMessage(consoleMessage);

        // Mensaje para los jugadores (formato JSON)
        String jsonMessage = String.format(
                "[\"\",{\"text\":\"[\",\"bold\":true,\"color\":\"gray\"}," +
                        "{\"text\":\"\u2620\",\"bold\":true,\"color\":\"#AD3C3C\"}," +
                        "{\"text\":\"]\",\"bold\":true,\"color\":\"gray\"}," +
                        "{\"text\":\" %s\",\"bold\":true,\"color\":\"#F63C69\"}," +
                        "{\"text\":\" ha muerto por\",\"color\":\"#D9632B\"}," +
                        "{\"text\":\" \",\"bold\":true}," +
                        "{\"text\":\"%s\",\"bold\":true,\"color\":\"#2B95CC\"}," +
                        "{\"text\":\"\",\"bold\":true}]",
                victim.getName(), deathCause
        );

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);
    }

    private String translateCause(org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
        switch (cause) {
            case FALL: return "caída";
            case FIRE: return "fuego";
            case FIRE_TICK: return "fuego";
            case LAVA: return "lava";
            case DROWNING: return "ahogamiento";
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION: return "explosión";
            case VOID: return "vacío";
            case LIGHTNING: return "rayo";
            case SUICIDE: return "suicidio";
            case STARVATION: return "inanición";
            case POISON: return "veneno";
            case MAGIC: return "magia";
            case WITHER: return "efecto Wither";
            case FALLING_BLOCK: return "bloque que cae";
            case DRAGON_BREATH: return "aliento de dragón";
            case DRYOUT: return "desecación";
            case HOT_FLOOR: return "suelo caliente";
            case FLY_INTO_WALL: return "choque con elytra";
            case CRAMMING: return "aplastamiento";
            case CONTACT: return "Estalactita";
            case CAMPFIRE: return "Fogsta";
            case FREEZE: return "Congelado";
            case THORNS: return "Espinas";
            case SUFFOCATION: return "sofocaciòn";
            case WORLD_BORDER: return "Limite del Mundo";
            case SONIC_BOOM: return "Sonic Boom";

            default: return cause.toString().toLowerCase().replace("_", " ");
        }
    }

    private String formatMobName(String mobType) {
        // Formatear nombres de mobs (ej. "ZOMBIE" -> "Zombie")
        return mobType.toLowerCase().replace("_", " ");
    }

    private String formatItemName(String itemType) {
        // Formatear nombres de items (ej. "DIAMOND_SWORD" -> "diamond sword")
        return itemType.toLowerCase().replace("_", " ");
    }
}