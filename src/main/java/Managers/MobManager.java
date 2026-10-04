package Managers;

import Bosses.InfestedWardenBoss;
import EndBiomes.BlackShulker;
import EndBiomes.EnderInsect;
import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MobManager {

    private final QuasoPlugin plugin;

    private final Bombita bombitaSpawner;
    private final Iceologer iceologerSpawner;
    private final CorruptedZombies corruptedZombieSpawner;
    private final CorruptedSpider corruptedSpider;
    private final GuardianBlaze guardianBlaze;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final InfestedBeeHandler infestedBeeHandler;
    private final CorruptedBee corruptedBee;
    private final InfestedCreeper infestedCreeper;
    private final InfestedGhast infestedGhast;
    private final InfestedSkeleton infestedSkeleton;
    private final InfestedCaveSpider infestedCaveSpider;
    private final WardenZombie wardenZombie;
    private final EnderBlaze enderBlaze;
    private final EnderCreeper enderCreeper;
    private final EnderSpider enderSpider;

    private final List<String> registeredMobs;

    @FunctionalInterface
    public interface SpawnCallback {
        void onSpawned(Entity entity);
    }

    private volatile SpawnCallback pendingCallback = null;
    private volatile Location expectedSpawnLocation = null;

    public void notifyEntitySpawned(Entity entity) {
        if (pendingCallback == null || expectedSpawnLocation == null) return;
        Location eLoc = entity.getLocation();
        Location expLoc = expectedSpawnLocation;
        if (eLoc.getWorld() == null || !eLoc.getWorld().equals(expLoc.getWorld())) return;
        if (eLoc.distanceSquared(expLoc) > 36) return;

        SpawnCallback cb = pendingCallback;
        pendingCallback = null;
        expectedSpawnLocation = null;
        cb.onSpawned(entity);
    }

    public MobManager(QuasoPlugin plugin, InfestedBeeHandler infestedBeeHandler) {
        this.plugin = plugin;

        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.corruptedZombieSpawner = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin);
        this.guardianBlaze = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.infestedBeeHandler = infestedBeeHandler;
        this.corruptedBee = new CorruptedBee(plugin);
        this.infestedCreeper = new InfestedCreeper(plugin);
        this.infestedGhast = new InfestedGhast(plugin);
        this.infestedSkeleton = new InfestedSkeleton(plugin);
        this.infestedCaveSpider = new InfestedCaveSpider(plugin);
        this.wardenZombie = new WardenZombie(plugin);
        this.enderBlaze = new EnderBlaze(plugin);
        this.enderCreeper = new EnderCreeper(plugin);
        this.enderSpider = new EnderSpider(plugin);

        this.registeredMobs = new ArrayList<>();
        cargarNombresDeMobs();
    }

    private void cargarNombresDeMobs() {
        String[] mobs = {
                "bombita", "iceologer", "corruptedzombie", "corruptedspider", "queenbee",
                "guardianblaze", "guardiancorruptedskeleton", "corruptedinfernalspider",
                "infestedbee", "estatuarecompensa", "corruptedbee", "infestedcreeper",
                "infestedghast", "infestedskeleton", "infestedcavespider", "wardenzombie", "enderblaze",
                "endercreeper", "enderspider", "infestedwarden", "enderinsect", "shulkernegro",
        };
        for (String mob : mobs) {
            registeredMobs.add(mob);
        }
    }

    // Spawnea cualquier mob custom por su nombre (lo usa /spawnqp)
    public boolean spawnMob(String mobType, Location location, Player targetPlayer, String variantArgs) {
        switch (mobType.toLowerCase()) {
            case "bombita": bombitaSpawner.spawnBombita(location); return true;
            case "iceologer": iceologerSpawner.spawnIceologer(location); return true;
            case "corruptedzombie": corruptedZombieSpawner.spawnCorruptedZombie(location); return true;
            case "corruptedspider": corruptedSpider.spawnCorruptedSpider(location); return true;
            case "queenbee": QueenBeeHandler.spawn(plugin, location); return true;
            case "guardianblaze": guardianBlaze.spawnGuardianBlaze(location); return true;
            case "guardiancorruptedskeleton": guardianCorruptedSkeleton.spawnGuardianCorruptedSkeleton(location); return true;
            case "corruptedinfernalspider": corruptedInfernalSpider.spawnCorruptedInfernalSpider(location); return true;
            case "infestedbee": infestedBeeHandler.spawnInfestedBee(location); return true;
            case "estatuarecompensa": Estatua_Reward.spawn(location); return true;
            case "corruptedbee": corruptedBee.spawnCorruptedBee(location); return true;
            case "infestedcreeper": infestedCreeper.spawnInfestedCreeper(location); return true;
            case "infestedghast": infestedGhast.spawnInfestedGhast(location); return true;
            case "infestedskeleton": infestedSkeleton.spawnInfestedSkeleton(location); return true;
            case "infestedcavespider": infestedCaveSpider.spawnInfestedCaveSpider(location); return true;
            case "wardenzombie": wardenZombie.spawnWardenZombie(location); return true;
            case "enderblaze": enderBlaze.spawnEnderBlaze(location); return true;
            case "endercreeper": enderCreeper.spawnEnderCreeper(location); return true;
            case "enderspider": enderSpider.spawnEnderSpider(location); return true;
            // Uno de prueba: no queda atado a ninguna Ancient City, así que no reaparece
            case "infestedwarden": InfestedWardenBoss.spawn(plugin, location, "comando"); return true;
            case "enderinsect": EnderInsect.spawn(plugin, location); return true;
            case "shulkernegro": BlackShulker.spawn(plugin, location); return true;
            default: return false;
        }
    }

    // Todavía no se usa: nadie llama a notifyEntitySpawned, así que siempre devuelve null
    public Entity spawnMobAndReturn(String mobType, Location location, Player targetPlayer, String variantArgs) {
        final Entity[] captured = {null};

        pendingCallback = entity -> captured[0] = entity;
        expectedSpawnLocation = location.clone();

        spawnMob(mobType, location, targetPlayer, variantArgs);

        pendingCallback = null;
        expectedSpawnLocation = null;

        return captured[0];
    }

    public List<String> getRegisteredMobs() {
        return registeredMobs;
    }
}