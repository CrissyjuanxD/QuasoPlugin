package Trabajos;

import Events.MissionSystem.BossDefeatedEvent;
import Events.MissionSystem.MissionUtils;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Cómo se gana XP en cada trabajo. Contra las granjas: los bloques puestos no dan XP al romperlos, los mobs de spawner
// no cuentan, si estás AFK (5 minutos sin moverte ni mover la cámara) no ganas nada y en Constructor volver a poner
// un bloque en el mismo lugar antes de 30 minutos no da XP. Lo nuevo del plugin da más: los minerales y troncos de la
// Warden Cave y del End, construir allá, sus frutas, los mobs del plugin y los jefes
public final class TrabajosXp implements Listener {

    // Dónde pasa: la Warden Cave y el End pagan más
    enum Mundo {
        NORMAL, WARDEN_CAVE, END;

        static Mundo de(World world) {
            if (world.getName().equals(QuasoPlugin.WORLD_NAME)) return WARDEN_CAVE;
            return world.getEnvironment() == World.Environment.THE_END ? END : NORMAL;
        }

        boolean nuevo() {
            return this != NORMAL;
        }
    }

    private static final long AFK = 5 * 60 * 1000L;
    private static final long MISMO_LUGAR = 30 * 60 * 1000L;

    private static TrabajosXp actual;

    private record Lugar(UUID mundo, long bloque) {}

    private final TrabajosManager manager;
    private final BloquesColocados colocados;
    private final Map<UUID, Long> ultimoMovimiento = new HashMap<>();
    private final Map<UUID, Map<Lugar, Long>> construidos = new HashMap<>();
    // Los élites que están muriendo y su nivel (EliteMobs los suelta antes de MONITOR)
    private final Map<UUID, Integer> elites = new HashMap<>();

    TrabajosXp(TrabajosManager manager) {
        this.manager = manager;
        this.colocados = new BloquesColocados(manager.plugin());
        Bukkit.getPluginManager().registerEvents(colocados, manager.plugin());
        Bukkit.getScheduler().runTaskTimer(manager.plugin(), this::limpiarConstruidos, 20L * 60 * 10, 20L * 60 * 10);
        long ahora = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) ultimoMovimiento.put(player.getUniqueId(), ahora);
        actual = this;
    }

    void guardar() {
        colocados.guardarTodo(Bukkit.getWorlds());
    }

    // Si el bloque lo puso un jugador (lo usa Talador para no tirar casas de troncos)
    public static boolean puesto(Block block) {
        TrabajosXp xp = actual;
        return xp != null && xp.colocados.esColocado(block);
    }

    private void dar(Player player, Trabajo trabajo, double xp) {
        if (xp <= 0 || !MissionUtils.isSurvival(player)) return;
        Long movido = ultimoMovimiento.get(player.getUniqueId());
        if (movido != null && System.currentTimeMillis() - movido > AFK) return;
        manager.ganarXp(player, trabajo, xp);
    }

    // ---------------------------------------------------------------- AFK

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock() || event.hasChangedOrientation()) {
            ultimoMovimiento.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ultimoMovimiento.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ultimoMovimiento.remove(event.getPlayer().getUniqueId());
        construidos.remove(event.getPlayer().getUniqueId());
    }

    // ---------------------------------------------------------------- Minería, Leñador, Granjero y Constructor

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material tipo = block.getType();
        boolean puesto = colocados.esColocado(block);
        colocados.quitar(block);
        if (puesto) return;

        Player player = event.getPlayer();
        Mundo mundo = Mundo.de(block.getWorld());
        double mina = xpMineria(tipo, mundo);
        if (mina > 0) {
            dar(player, Trabajo.MINERIA, mina);
            return;
        }
        double tala = xpLenador(tipo, mundo);
        if (tala > 0) {
            dar(player, Trabajo.LENADOR, tala);
            return;
        }
        double fruta = xpFruta(tipo, mundo);
        if (fruta > 0) {
            dar(player, Trabajo.GRANJERO, fruta);
            return;
        }
        double cultivo = xpCultivo(tipo);
        if (cultivo <= 0) return;
        // Los cultivos solo cuentan maduros (la caña de azúcar también tiene edad, pero es de su crecimiento)
        if (tipo != Material.SUGAR_CANE && block.getBlockData() instanceof Ageable ageable
                && ageable.getAge() < ageable.getMaximumAge()) return;
        dar(player, Trabajo.GRANJERO, cultivo);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Material tipo = block.getType();
        if (seRastrea(tipo)) colocados.marcar(block);

        double xp = xpConstructor(tipo) * (Mundo.de(block.getWorld()).nuevo() ? 1.5 : 1);
        if (xp <= 0) return;
        Player player = event.getPlayer();
        Map<Lugar, Long> lugares = construidos.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        long ahora = System.currentTimeMillis();
        Long antes = lugares.put(new Lugar(block.getWorld().getUID(), block.getBlockKey()), ahora);
        if (antes != null && ahora - antes < MISMO_LUGAR) return;
        dar(player, Trabajo.CONSTRUCTOR, xp);
    }

    private void limpiarConstruidos() {
        long limite = System.currentTimeMillis() - MISMO_LUGAR;
        construidos.values().forEach(lugares -> lugares.values().removeIf(tiempo -> tiempo < limite));
        construidos.values().removeIf(Map::isEmpty);
    }

    // Bayas dulces y luminosas (se cosechan con clic derecho)
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Material tipo = event.getHarvestedBlock().getType();
        if (tipo == Material.SWEET_BERRY_BUSH || tipo == Material.CAVE_VINES || tipo == Material.CAVE_VINES_PLANT) {
            // Las enredaderas del Pantano Profundo dan Bayas Regenerativas
            boolean wardenCave = Mundo.de(event.getHarvestedBlock().getWorld()) == Mundo.WARDEN_CAVE;
            dar(event.getPlayer(), Trabajo.GRANJERO, wardenCave ? 1.5 : 0.5);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) dar(player, Trabajo.GRANJERO, 4);
    }

    // ---------------------------------------------------------------- Guerrero

    // En LOWEST para ver si es élite antes de que EliteMobs lo suelte
    @EventHandler(priority = EventPriority.LOWEST)
    public void marcarElite(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() != null && MissionUtils.isElite(entity)) elites.put(entity.getUniqueId(), nivelElite(entity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Integer elite = elites.remove(entity.getUniqueId());
        Player killer = entity.getKiller();
        if (event.isCancelled() || killer == null || !(entity instanceof Enemy) || MissionUtils.bossId(entity) != null) return;
        CreatureSpawnEvent.SpawnReason razon = entity.getEntitySpawnReason();
        if (razon == CreatureSpawnEvent.SpawnReason.SPAWNER || razon == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER
                || razon == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG || entity.fromMobSpawner()) return;
        if (elite != null) {
            dar(killer, Trabajo.GUERRERO, xpElite(elite));
            return;
        }
        if (entity.getType() == EntityType.WARDEN) {
            dar(killer, Trabajo.GUERRERO, XP_WARDEN);
            return;
        }
        AttributeInstance vida = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxima = vida != null ? vida.getValue() : 20;
        dar(killer, Trabajo.GUERRERO, delPlugin(entity) ? xpMobPlugin(maxima) : xpMonstruo(maxima));
    }

    // Los mobs del plugin (florales, infestados, del End...) llevan alguna marca del plugin en su PDC
    private boolean delPlugin(LivingEntity entity) {
        String namespace = manager.plugin().getName().toLowerCase(java.util.Locale.ROOT);
        for (NamespacedKey key : entity.getPersistentDataContainer().getKeys()) {
            if (key.getNamespace().equals(namespace)) return true;
        }
        return false;
    }

    private static int nivelElite(LivingEntity entity) {
        try {
            EliteEntity elite = EntityTracker.getEliteMobEntity(entity);
            if (elite != null) return elite.getLevel();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // Los jefes le dan XP a todos los que pelearon: entera si son Guerreros y la mitad para el trabajo que tengan si no
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBoss(BossDefeatedEvent event) {
        double xp = xpJefe(event.getBossId());
        for (Player player : event.getPlayers()) {
            if (!player.isOnline()) continue;
            Trabajo trabajo = TrabajosManager.activo(player);
            if (trabajo != null) dar(player, trabajo, trabajo == Trabajo.GUERRERO ? xp : xp / 2);
        }
    }

    // Por dificultad: Wither, Abeja Floral, Ender Dragon, Infested Warden, Ultra Warden y Rey Ender
    static double xpJefe(String bossId) {
        return switch (bossId) {
            case "wither" -> 400;
            case "abeja_reina" -> 600;
            case "ender_dragon" -> 900;
            case "infested_warden_boss" -> 1200;
            case "ultra_warden" -> 1600;
            case "rey_ender" -> 2200;
            default -> 250;
        };
    }

    // ---------------------------------------------------------------- Pescador

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) return;
        dar(event.getPlayer(), Trabajo.PESCADOR, xpPesca(item.getItemStack()));
    }

    // En las zonas de pesca la pesca normal se cancela y el premio lo da el minijuego (FishingListener avisa aquí)
    public static void pescoEnZona(Player player, ItemStack premio, boolean especial) {
        TrabajosXp xp = actual;
        if (xp == null || premio == null) return;
        xp.dar(player, Trabajo.PESCADOR, especial ? 12 : xpPesca(premio));
    }

    // ---------------------------------------------------------------- Tablas de XP

    static final double XP_WARDEN = 40;

    // Los Minerales Profundos de la Warden Cave (terracotas) y los racimos de amatista del End (de ahí sale la Celestita)
    static double xpMineria(Material tipo, Mundo mundo) {
        if (mundo == Mundo.WARDEN_CAVE && InfestedCaves.WardenBiome.fromOre(tipo) != null) return 25;
        if (mundo == Mundo.END && tipo == Material.AMETHYST_CLUSTER) return 5;
        return xpMineria(tipo);
    }

    static double xpMineria(Material tipo) {
        return switch (tipo) {
            case STONE, DEEPSLATE, TUFF, GRANITE, DIORITE, ANDESITE, CALCITE, BLACKSTONE, BASALT, END_STONE -> 0.05;
            case COAL_ORE, COPPER_ORE, NETHER_GOLD_ORE -> 2;
            case DEEPSLATE_COAL_ORE, DEEPSLATE_COPPER_ORE, NETHER_QUARTZ_ORE -> 3;
            case REDSTONE_ORE -> 4;
            case DEEPSLATE_REDSTONE_ORE, IRON_ORE -> 5;
            case DEEPSLATE_IRON_ORE -> 6;
            case GOLD_ORE, LAPIS_ORE -> 7;
            case DEEPSLATE_GOLD_ORE, DEEPSLATE_LAPIS_ORE -> 8;
            case DIAMOND_ORE -> 15;
            case DEEPSLATE_DIAMOND_ORE -> 18;
            case EMERALD_ORE -> 20;
            case DEEPSLATE_EMERALD_ORE -> 22;
            case ANCIENT_DEBRIS -> 30;
            default -> 0;
        };
    }

    // En la Warden Cave y el End cuentan también los troncos pelados y la madera (los árboles del End son así)
    static double xpLenador(Material tipo, Mundo mundo) {
        String nombre = tipo.name();
        if (mundo.nuevo() && (nombre.endsWith("_LOG") || nombre.endsWith("_WOOD") || nombre.endsWith("_STEM")
                || nombre.endsWith("_HYPHAE"))) return 1.8;
        return xpLenador(tipo);
    }

    static double xpLenador(Material tipo) {
        String nombre = tipo.name();
        if (tipo == Material.CRIMSON_STEM || tipo == Material.WARPED_STEM) return 1.4;
        if (tipo == Material.MANGROVE_ROOTS) return 0.5;
        if (nombre.endsWith("_LOG") && !nombre.startsWith("STRIPPED_")) return 1.2;
        return 0;
    }

    // Cultivos (maduros) y lo que crece solo
    static double xpCultivo(Material tipo) {
        return switch (tipo) {
            case WHEAT, CARROTS, POTATOES -> 0.6;
            case BEETROOTS, NETHER_WART -> 0.8;
            case COCOA, MELON, PUMPKIN -> 1;
            case PITCHER_CROP -> 1.5;
            case SUGAR_CANE -> 0.3;
            default -> 0;
        };
    }

    // Los froglights de los árboles de la Warden Cave se rompen en frutas
    static double xpFruta(Material tipo, Mundo mundo) {
        return mundo == Mundo.WARDEN_CAVE && (tipo == Material.VERDANT_FROGLIGHT || tipo == Material.PEARLESCENT_FROGLIGHT) ? 2 : 0;
    }

    // Los bloques que dan XP al romperlos y que, si los pone un jugador, dejan de darla
    static boolean seRastrea(Material tipo) {
        for (Mundo mundo : Mundo.values()) {
            if (xpMineria(tipo, mundo) > 0 || xpLenador(tipo, mundo) > 0 || xpFruta(tipo, mundo) > 0) return true;
        }
        return tipo == Material.MELON || tipo == Material.PUMPKIN || tipo == Material.SUGAR_CANE;
    }

    // Básicos 0,1 · comunes 0,3 · trabajados (ladrillos, cuarzo, concreto, terracota...) 0,5. Lo que no es de
    // construir (antorchas, redstone, cofres, plantas...) no da nada
    static double xpConstructor(Material tipo) {
        String n = tipo.name();
        if (n.endsWith("_ORE") || n.startsWith("RAW_") || n.contains("INFESTED") || n.contains("SHULKER")
                || n.contains("SPAWNER") || n.contains("TORCH") || n.contains("BUTTON") || n.contains("PRESSURE_PLATE")
                || n.contains("RAIL") || n.contains("CHEST") || n.contains("BULB") || n.equals("SCAFFOLDING")) return 0;

        if ((n.contains("CONCRETE") && !n.contains("POWDER")) || n.contains("BRICK") || n.contains("QUARTZ")
                || n.contains("TERRACOTTA") || n.contains("PRISMARINE") || n.contains("PURPUR") || n.contains("COPPER")
                || n.startsWith("POLISHED_") || n.startsWith("CHISELED_") || n.startsWith("SMOOTH_") || n.startsWith("CUT_")
                || n.contains("TILES") || n.equals("AMETHYST_BLOCK")) return 0.5;

        if (n.contains("PLANKS") || n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE")
                || n.endsWith("_STAIRS") || n.endsWith("_SLAB") || n.endsWith("_WALL") || n.contains("FENCE")
                || n.contains("GLASS") || n.endsWith("_WOOL") || n.contains("SANDSTONE") || n.endsWith("_DOOR")
                || n.endsWith("_TRAPDOOR") || n.endsWith("LANTERN") || n.contains("BOOKSHELF") || n.startsWith("MOSSY_")
                || n.contains("CONCRETE_POWDER") || n.equals("PACKED_MUD")) return 0.3;

        return switch (tipo) {
            case DIRT, COARSE_DIRT, GRASS_BLOCK, COBBLESTONE, COBBLED_DEEPSLATE, STONE, DEEPSLATE, ANDESITE, DIORITE,
                 GRANITE, TUFF, SAND, RED_SAND, GRAVEL, NETHERRACK, BLACKSTONE, BASALT, END_STONE, CLAY, MUD, CALCITE,
                 SNOW_BLOCK, MOSS_BLOCK -> 0.1;
            default -> 0;
        };
    }

    // De 4 (zombi, araña) a 15 XP según la vida del monstruo
    static double xpMonstruo(double vidaMaxima) {
        return Math.max(4, Math.min(15, Math.round(2 + vidaMaxima / 5)));
    }

    // Los mobs del plugin pagan más: de 10 (los florales) a 30 XP (los infestados de la Warden Cave)
    static double xpMobPlugin(double vidaMaxima) {
        return Math.max(10, Math.min(30, Math.round(4 + vidaMaxima / 5)));
    }

    // Los élites dan de 20 a 60 XP según su nivel de EliteMobs (nivel 0 si no se pudo leer)
    static double xpElite(int nivel) {
        return Math.max(20, Math.min(60, Math.round(20 + nivel / 2.0)));
    }

    static double xpPesca(ItemStack item) {
        return switch (item.getType()) {
            case COD, SALMON -> 6;
            case TROPICAL_FISH, PUFFERFISH -> 8;
            case BOW, ENCHANTED_BOOK, NAME_TAG, NAUTILUS_SHELL, SADDLE -> 12;
            case FISHING_ROD -> item.getEnchantments().isEmpty() ? 3 : 12;
            default -> 3;
        };
    }
}
