package Dificultades;

import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedBee;
import Dificultades.CustomMobs.CorruptedSpider;
import Dificultades.CustomMobs.CorruptedZombies;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ThreadLocalRandom;

// Cambio extra (día 14, entre el uno y el dos): el Zombie Floral, la Spider Floral, el Creeper Floral y la Mini Abeja
// Floral salen solos en el Overworld. Lo que hacen lo pone el cambio uno (sus listeners están desde el día 1); acá solo
// se convierten
public class ExtraChanges implements Listener, Change {

    public enum Floral { NINGUNO, ZOMBIE, SPIDER, BOMBITA, ABEJA }

    // De cada 100 que spawnean solos, cuántos salen florales
    static final int ZOMBIES = 25;
    static final int SPIDERS = 25;
    static final int BOMBITAS = 20;
    // De los zombies, arañas, creepers y esqueletos que no salen florales, cuántos se cambian por una Mini Abeja Floral
    static final int ABEJAS = 10;
    // Las abejas naturales se van si no hay jugadores a esta distancia, como un monstruo
    private static final double LEJOS = 128;

    private final JavaPlugin plugin;
    private final CorruptedZombies zombies;
    private final CorruptedSpider spiders;
    private final Bombita bombitas;
    private final CorruptedBee abejas;
    private final NamespacedKey abejaNatural;
    private BukkitTask limpieza;
    private boolean isApplied = false;

    public ExtraChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.zombies = new CorruptedZombies(plugin);
        this.spiders = new CorruptedSpider(plugin);
        this.bombitas = new Bombita(plugin);
        this.abejas = new CorruptedBee(plugin);
        this.abejaNatural = new NamespacedKey(plugin, "abeja_floral_natural");
    }

    @Override
    public String id() {
        return "extra";
    }

    @Override
    public String description() {
        return "Día 14: Zombie Floral, Spider Floral, Creepers Florales y Mini Abejas Florales spawnean solos en el Overworld";
    }

    @Override
    public boolean isApplied() {
        return isApplied;
    }



    @Override
    public void apply() {
        if (isApplied) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        limpieza = Bukkit.getScheduler().runTaskTimer(plugin, this::despawnAbejas, 200L, 200L);
        isApplied = true;
    }

    // Los que ya salieron se quedan, como los de las raids; solo dejan de salir nuevos
    @Override
    public void revert() {
        if (!isApplied) return;
        HandlerList.unregisterAll(this);
        if (limpieza != null) limpieza.cancel();
        isApplied = false;
    }

    // En qué se convierte un mob que spawnea solo, con una tirada de 0 a 99. Los zombies bebé no salen florales. Después
    // de los florales viene la franja de la Mini Abeja (también desde los esqueletos)
    public static Floral floral(EntityType type, boolean adulto, int roll) {
        int florales = switch (type) {
            case ZOMBIE -> adulto ? ZOMBIES : 0;
            case SPIDER -> SPIDERS;
            case CREEPER -> BOMBITAS;
            case SKELETON -> 0;
            default -> -1;
        };
        if (florales < 0) return Floral.NINGUNO;
        if (roll < florales) {
            return switch (type) {
                case ZOMBIE -> Floral.ZOMBIE;
                case SPIDER -> Floral.SPIDER;
                default -> Floral.BOMBITA;
            };
        }
        return roll < florales + ABEJAS ? Floral.ABEJA : Floral.NINGUNO;
    }

    // Se convierten ahí mismo, sin cancelar el spawn, así la mobcap cuenta igual. Solo en el Overworld: la Warden
    // Cave tiene sus propios mobs
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        World world = event.getLocation().getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL || world.getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (convertir(event.getEntity(), ThreadLocalRandom.current().nextInt(100)) == Floral.ABEJA) event.setCancelled(true);
    }

    // La abeja no se puede convertir desde otro mob: se cancela ese spawn y sale ella en su lugar
    Floral convertir(LivingEntity entity, int roll) {
        boolean adulto = !(entity instanceof Zombie zombie) || zombie.isAdult();
        Floral floral = floral(entity.getType(), adulto, roll);
        switch (floral) {
            case ZOMBIE -> zombies.transformToCorruptedZombie((Zombie) entity);
            case SPIDER -> spiders.transformspawnCorruptedSpider((Spider) entity);
            case BOMBITA -> bombitas.transformToBombita((Creeper) entity);
            case ABEJA -> spawnAbeja(entity.getLocation().add(0, 1, 0));
            default -> { }
        }
        return floral;
    }

    // No se guarda al descargarse el chunk y se va si no hay nadie cerca, así no se juntan como animales
    private void spawnAbeja(Location location) {
        Bee abeja = abejas.spawnCorruptedBee(location);
        abeja.setPersistent(false);
        abeja.setRemoveWhenFarAway(true);
        abeja.getPersistentDataContainer().set(abejaNatural, PersistentDataType.BYTE, (byte) 1);
    }

    private void despawnAbejas() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            for (Bee abeja : world.getEntitiesByClass(Bee.class)) {
                if (!abeja.getPersistentDataContainer().has(abejaNatural, PersistentDataType.BYTE)) continue;
                boolean cerca = false;
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(abeja.getLocation()) < LEJOS * LEJOS) {
                        cerca = true;
                        break;
                    }
                }
                if (!cerca) abeja.remove();
            }
        }
    }
}
