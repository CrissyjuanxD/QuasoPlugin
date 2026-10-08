package EndBiomes;

import Dificultades.CustomMobs.EnderBlaze;
import Dificultades.CustomMobs.EnderCreeper;
import Dificultades.CustomMobs.EnderSpider;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ThreadLocalRandom;

// En las islas de afuera del End el juego solo spawnea endermans (y endermites en el Bosque Prismático, que pasan a
// ser Ender Insects). Parte de esos endermans se cambian ahí mismo por un mob del End según el bioma, uno por uno,
// así la mobcap sigue contando igual (como los ghasts del Abismo en TwoChanges)
public class EndSpawns implements Listener {

    public enum Mob { NINGUNO, ENDER_BLAZE, ENDER_CREEPER, ENDER_SPIDER, SHULKER_NEGRO, WITHER_SKELETON }

    // La isla del dragón queda vanilla
    private static final int MAIN_ISLAND = 500;

    private final JavaPlugin plugin;
    private final EnderBlaze enderBlaze;
    private final EnderCreeper enderCreeper;
    private final EnderSpider enderSpider;
    private final BlackShulker blackShulker;

    public EndSpawns(JavaPlugin plugin, EnderBlaze enderBlaze, EnderCreeper enderCreeper, EnderSpider enderSpider, BlackShulker blackShulker) {
        this.plugin = plugin;
        this.enderBlaze = enderBlaze;
        this.enderCreeper = enderCreeper;
        this.enderSpider = enderSpider;
        this.blackShulker = blackShulker;
    }

    // Qué sale en lugar del enderman según la zona y una tirada de 0 a 99: 25% en el End de siempre y en los Picos
    // Helados, 20% en el Bosque Prismático (que ya tiene los Ender Insects) y 41% en el Páramo, que además tiene
    // Shulkers Negros sueltos y Wither Skeletons (los del datapack casi no salían)
    public static Mob reemplazo(EndBiomeMap.Zone zone, int roll) {
        return switch (zone) {
            case VANILLA, HIELO -> roll < 9 ? Mob.ENDER_BLAZE : roll < 18 ? Mob.ENDER_SPIDER : roll < 25 ? Mob.ENDER_CREEPER : Mob.NINGUNO;
            case PRISMATICO -> roll < 9 ? Mob.ENDER_SPIDER : roll < 15 ? Mob.ENDER_CREEPER : roll < 20 ? Mob.ENDER_BLAZE : Mob.NINGUNO;
            case MARCHITO -> roll < 10 ? Mob.ENDER_BLAZE : roll < 17 ? Mob.ENDER_CREEPER : roll < 24 ? Mob.ENDER_SPIDER
                    : roll < 29 ? Mob.SHULKER_NEGRO : roll < 41 ? Mob.WITHER_SKELETON : Mob.NINGUNO;
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL || !(event.getEntity() instanceof Enderman enderman)) return;
        Location loc = event.getLocation();
        if (loc.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (loc.getX() * loc.getX() + loc.getZ() * loc.getZ() < (double) MAIN_ISLAND * MAIN_ISLAND) return;

        Mob mob = reemplazo(zone(loc.getBlock().getBiome()), ThreadLocalRandom.current().nextInt(100));
        if (mob == Mob.NINGUNO) return;
        Bukkit.getScheduler().runTask(plugin, () -> replace(enderman, mob));
    }

    private static EndBiomeMap.Zone zone(Biome biome) {
        if (EndBiome.isParamo(biome)) return EndBiomeMap.Zone.MARCHITO;
        if (EndBiome.isPrismatic(biome)) return EndBiomeMap.Zone.PRISMATICO;
        if (EndBiome.isHielo(biome)) return EndBiomeMap.Zone.HIELO;
        return EndBiomeMap.Zone.VANILLA;
    }

    private void replace(Enderman enderman, Mob mob) {
        if (!enderman.isValid() || enderman.isDead()) return;
        Location loc = enderman.getLocation();
        enderman.remove();
        switch (mob) {
            case ENDER_BLAZE -> enderBlaze.spawnEnderBlaze(loc.add(0, 1.5, 0));
            case ENDER_CREEPER -> enderCreeper.spawnEnderCreeper(loc);
            case ENDER_SPIDER -> enderSpider.spawnEnderSpider(loc);
            case SHULKER_NEGRO -> {
                // Los de los santuarios no se van; los sueltos desaparecen como cualquier mob
                Shulker shulker = loc.getWorld().spawn(loc, Shulker.class, blackShulker::setup);
                shulker.setPersistent(false);
                shulker.setRemoveWhenFarAway(true);
            }
            case WITHER_SKELETON -> loc.getWorld().spawn(loc, WitherSkeleton.class);
            default -> { }
        }
    }
}
