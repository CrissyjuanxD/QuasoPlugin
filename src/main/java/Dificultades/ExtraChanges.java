package Dificultades;

import Dificultades.CustomMobs.Bombita;
import Dificultades.CustomMobs.CorruptedSpider;
import Dificultades.CustomMobs.CorruptedZombies;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

// Cambio extra (día 14, entre el uno y el dos): el Zombie Floral, la Spider Floral y las Bombitas salen solos en el
// Overworld. Lo que hacen lo pone el cambio uno (sus listeners están desde el día 1); acá solo se convierten
public class ExtraChanges implements Listener, Change {

    public enum Floral { NINGUNO, ZOMBIE, SPIDER, BOMBITA }

    // De cada 100 que spawnean solos, cuántos salen florales
    static final int ZOMBIES = 25;
    static final int SPIDERS = 25;
    static final int BOMBITAS = 20;

    private final JavaPlugin plugin;
    private final CorruptedZombies zombies;
    private final CorruptedSpider spiders;
    private final Bombita bombitas;
    private boolean isApplied = false;

    public ExtraChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.zombies = new CorruptedZombies(plugin);
        this.spiders = new CorruptedSpider(plugin);
        this.bombitas = new Bombita(plugin);
    }

    @Override
    public String id() {
        return "extra";
    }

    @Override
    public String description() {
        return "Día 14: Zombie Floral, Spider Floral y Bombitas spawnean solos en el Overworld";
    }

    @Override
    public boolean isApplied() {
        return isApplied;
    }

    @Override
    public void apply() {
        if (isApplied) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        isApplied = true;
    }

    // Los que ya salieron se quedan, como los de las raids; solo dejan de salir nuevos
    @Override
    public void revert() {
        if (!isApplied) return;
        HandlerList.unregisterAll(this);
        isApplied = false;
    }

    // En qué se convierte un mob que spawnea solo, con una tirada de 0 a 99. Los zombies bebé quedan normales
    public static Floral floral(EntityType type, boolean adulto, int roll) {
        return switch (type) {
            case ZOMBIE -> adulto && roll < ZOMBIES ? Floral.ZOMBIE : Floral.NINGUNO;
            case SPIDER -> roll < SPIDERS ? Floral.SPIDER : Floral.NINGUNO;
            case CREEPER -> roll < BOMBITAS ? Floral.BOMBITA : Floral.NINGUNO;
            default -> Floral.NINGUNO;
        };
    }

    // Se convierten ahí mismo, sin cancelar el spawn, así la mobcap cuenta igual. Solo en el Overworld: la Warden
    // Cave tiene sus propios mobs
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        World world = event.getLocation().getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL || world.getName().equals(QuasoPlugin.WORLD_NAME)) return;
        convertir(event.getEntity(), ThreadLocalRandom.current().nextInt(100));
    }

    void convertir(LivingEntity entity, int roll) {
        boolean adulto = !(entity instanceof Zombie zombie) || zombie.isAdult();
        switch (floral(entity.getType(), adulto, roll)) {
            case ZOMBIE -> zombies.transformToCorruptedZombie((Zombie) entity);
            case SPIDER -> spiders.transformspawnCorruptedSpider((Spider) entity);
            case BOMBITA -> bombitas.transformToBombita((Creeper) entity);
            default -> { }
        }
    }
}
