package Managers;

import Bosses.InfestedWardenBoss;
import Bosses.ReyEnderBoss;
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
                "endercreeper", "enderspider", "infestedwarden", "enderinsect", "shulkernegro", "reyender",
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
            // De prueba: la arena queda donde se spawnea
            case "reyender": ReyEnderBoss.spawn(plugin, location); return true;
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

    // ---------------------------------------------------------------- Lo que la web muestra de cada mob

    // Ficha de un mob custom para la web: id (el de /spawnqp), nombre, color, etapa (uno, extra, dos o tres), vida,
    // dónde sale, qué hace y el huevo de spawn que se usa de ícono. Si agregas, cambias o quitas un mob, hazlo aquí
    // también y la web se actualiza sola la próxima vez que prenda el servidor (o con /web subir)
    public record InfoMob(String id, String nombre, String color, String etapa, String vida, String donde, String texto, String huevo) {}

    private static final List<InfoMob> INFO_MOBS = List.of(
            new InfoMob("corruptedzombie", "Zombie Floral", "#FF8CC6", "uno", "20", "Raids · solo desde el día 14",
                    "Dispara una wind charge con estela floral que da veneno y debilidad. Tiene fuerza, velocidad y 30% de soltar Carne Corrupta.", "zombie_spawn_egg"),
            new InfoMob("corruptedspider", "Spider Floral", "#F6C945", "uno", "16", "Raids · sola desde el día 14",
                    "Araña con velocidad y fuerza permanentes. Si te pega y no te cubres con el escudo, te deja una telaraña en los pies.", "spider_spawn_egg"),
            new InfoMob("bombita", "Bombita", "#FF5555", "uno", "20", "Raids · sola desde el día 14",
                    "Creeper chiquito y rápido que explota casi al instante. Las Bombitas no se dañan entre ellas.", "creeper_spawn_egg"),
            new InfoMob("iceologer", "Iceologer", "#55FFFF", "uno", "48", "Raids, desde la 2ª oleada",
                    "Usa su arco de hielo, colmillos de hielo, una esfera que te congela y una lluvia de bloques de hielo. Sus vex son Ángeles de Hielo. Suelta un Cristal de Hielo y 10% el arco.", "evoker_spawn_egg"),
            new InfoMob("corruptedbee", "Corrupted Bee", "#AA00AA", "uno", "15", "Dungeon de la Abeja Floral",
                    "Abeja siempre enojada que va por el jugador más cercano. Su picadura da Veneno III y no muere al picar.", "bee_spawn_egg"),
            new InfoMob("guardianblaze", "Guardian Blaze", "#FFAA00", "uno", "40", "Dungeons y estructuras del server",
                    "Tira bolas de fuego en fila y un anillo de fuego. Suelta netherite scrap.", "blaze_spawn_egg"),
            new InfoMob("guardiancorruptedskeleton", "Guardian Corrupted Skeleton", "#AA00AA", "uno", "25", "Dungeons y estructuras del server",
                    "Lanza cráneos que dan Wither III si no te cubres con el escudo.", "wither_skeleton_spawn_egg"),
            new InfoMob("corruptedinfernalspider", "Corrupted Infernal Spider", "#FF5555", "uno", "16", "Dungeons y estructuras del server",
                    "Araña de fuego que deja telarañas.", "spider_spawn_egg"),
            new InfoMob("wardenzombie", "Warden Zombie", "#1f7a86", "dos", "60", "Los 4 biomas de la Warden Cave",
                    "Lento pero con mucha vida y armadura. Te escucha si te mueves sin agacharte y su golpe da Oscuridad 4 segundos. 15% de soltar un fragmento resonante.", "zombie_spawn_egg"),
            new InfoMob("infestedskeleton", "Infested Skeleton", "#00AAAA", "dos", "100", "Caverna Sculk (cian)",
                    "Arco con Poder VII y flechas que hacen daño instantáneo; a veces lanza un sonic boom. 7% de soltar su alma (casco de Warden).", "skeleton_spawn_egg"),
            new InfoMob("infestedcavespider", "Infested Cave Spider", "#00AAAA", "dos", "100", "Pantano Profundo (verde)",
                    "Araña de cueva más grande con efectos al azar. 20% de lanzar un sonic boom al pegarte y los proyectiles no le hacen daño. 7% de soltar su alma (botas).", "cave_spider_spawn_egg"),
            new InfoMob("infestedghast", "Infested Ghast", "#00AAAA", "dos", "100", "Abismo Flotante (morado)",
                    "Bolas de fuego más fuertes con rastro de sonic boom; al explotar dan Oscuridad a 25 bloques. 7% de soltar su alma (peto).", "ghast_spawn_egg"),
            new InfoMob("infestedcreeper", "Infested Creeper", "#00AAAA", "dos", "100", "Ruinas de Ceniza (gris)",
                    "Cargado; su explosión no rompe bloques pero da Oscuridad y Veneno a 15 bloques. Con proyectiles no baja de 18 de vida: remátalo cuerpo a cuerpo. 7% de soltar su alma (pantalón).", "creeper_spawn_egg"),
            new InfoMob("enderblaze", "Ender Blaze", "#FF55FF", "tres", "50", "Islas de afuera del End",
                    "Sus bolas de fuego explotan al pegar (sin romper bloques) y se tepea al recibir daño o cada cierto tiempo.", "blaze_spawn_egg"),
            new InfoMob("endercreeper", "Ender Creeper", "#FF55FF", "tres", "40", "Islas de afuera del End",
                    "Cargado, invisible y con Velocidad I. Explota como un creeper cargado normal: con armadura de Warden no mata de un golpe.", "creeper_spawn_egg"),
            new InfoMob("enderspider", "Ender Spider", "#FF55FF", "tres", "40", "Islas de afuera del End",
                    "Velocidad I, el triple de daño de una araña y un proyectil morado que te tepea a 15 bloques de ella.", "spider_spawn_egg"),
            new InfoMob("enderinsect", "Ender Insect", "#c58cff", "tres", "16", "Bosque Prismático",
                    "Endermite grande que los Enderman no atacan. Escupe proyectiles que dan Veneno II y dejan una nube de esporas 30 segundos. Suelta Fragmento Astral (10%).", "endermite_spawn_egg"),
            new InfoMob("shulkernegro", "Shulker Negro", "#9a8fb0", "tres", "60", "Santuarios del Páramo Marchito",
                    "Sus balas pegan el doble y dan Wither II y Ceguera; cada 10 a 14 segundos tira una ráfaga de 6 balas. Suelta 1 o 2 Esencias Marchitas y Fragmento Astral (25%).", "shulker_spawn_egg")
    );

    public static List<InfoMob> infoMobs() {
        return INFO_MOBS;
    }
}