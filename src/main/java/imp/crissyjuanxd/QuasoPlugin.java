package imp.crissyjuanxd;

import Bosses.BossChunkListener;
import Casino.CasinoCommands;
import Casino.CasinoManager;
import Commands.*;
import EffectListener.ConfusionEffect;
import EffectListener.CorruptureEffect;
import EffectListener.CustomEffectManager;
import EffectListener.EffectPreventionListener;
import Events.HotPotato.HotPotatoCommand;
import Events.HotPotato.HotPotatoHandler;
import Events.ItemParty.ItemPartyCommand;
import Events.Skybattle.LavaClashCommand;
import ShopSystem.*;
import StatueManager.*;
import items.MochilaCommand;
import Dificultades.CustomMobs.*;
import Dificultades.Features.*;
import Events.AchievementParty.AchievementCommands;
import Events.AchievementParty.AchievementGUI;
import Events.AchievementParty.AchievementPartyHandler;
import Events.ItemParty.ItemPartyHandler;
import Events.MissionSystem.MissionCommands;
import Events.MissionSystem.MissionGUI;
import Events.MissionSystem.MissionHandler;
import Events.MissionSystem.MissionRewardHandler;
import Events.Skybattle.EventoHandler;
import Habilidades.*;
import Handlers.*;
import TitleListener.*;
import items.*;
import list.VHList;
import mobcap.MobCapManager;
import mobcap.commands.MobCapCommand;
import mobcap.commands.MobCapTabCompleter;
import mobcap.config.MobCapConfig;
import mobcap.spawn.CustomSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import chat.chatgeneral;

import java.util.Objects;

public class QuasoPlugin extends JavaPlugin implements Listener {

    // ------------------------------------------------------------------------
    //  Constantes y estado básico
    // ------------------------------------------------------------------------

    public String Prefix = "§9§lQua§c§lso§7§lPlugin &7➤ &f";
    public String Version;
    public static boolean shuttingDown = false;

    private static QuasoPlugin instance;

    // ------------------------------------------------------------------------
    //  Core / Dificultades principales / Juagdores
    // ------------------------------------------------------------------------

    private DayHandler dayHandler;
    private NightmareMechanic nightmareMechanic;

    private DatabaseManager databaseManager;
    private TeamsHandler teamsHandler;

    private TiempoCommand tiempoCommand;
    private RuletaAnimation ruletaAnimation;
    private MisionAnimation misionAnimation;
    private EventoAnimation eventoAnimation;

    private SuccessNotification successNotif;
    private CustomEffectManager effectManager;
    private EffectPreventionListener effectPreventionListener;

    // ------------------------------------------------------------------------
    //  Sistemas de Misiones / Tiendas / Linterna
    // ------------------------------------------------------------------------

    private MissionHandler missionHandler;
    private MissionSystemCommands missionSystemCommands;
    private MissionRewardHandler missionRewardHandler;

/*    private ShopHandler shopHandler;
    private ShopCommand shopCommand;*/

    private AltarFunctions altarFunctions;

    private CasinoManager casinoManager;

    // ------------------------------------------------------------------------
    //  Corrupción Ansiosa / Tótems / Items
    // ------------------------------------------------------------------------
    private DoubleLifeTotem doubleLifeTotemHandler;
    private NormalTotemHandler normalTotemHandler;
    private EconomyItemsFunctions economyItemsFunctions;
    private EconomyIceTotem economyIceTotem;
    private EconomyFlyTotem economyFlyTotem;
    private excavatorItem ExcavatorItem;
    private AmuletBloodM amuletBloodM;
    private AmuletInmortal amuletInmortal;
    private LifeCampfire lifeCampfire;
    private HappyGhastEnchant happyGhastEnchant;

    // ------------------------------------------------------------------------
    //  Ping / Sonidos / Spawners
    // ------------------------------------------------------------------------

    private MobSoundManager mobSoundManager;
    private CustomSpawnerHandler customSpawnerHandler;

    // ------------------------------------------------------------------------
    //  Mobcap
    // ------------------------------------------------------------------------

    private MobCapManager mobCapManager;
    private MobCapConfig config;
    private CustomSpawnManager spawnManager;

    // ------------------------------------------------------------------------
    //  Habilidades
    // ------------------------------------------------------------------------

    private HabilidadesManager habilidadesManager;
    private HabilidadesGUI habilidadesGUI;
    private HabilidadesListener habilidadesListener;
    private HabilidadesEffects habilidadesEffects;
    private CustomItemRegistry customItemRegistry;

    // ------------------------------------------------------------------------
    //  Eventos
    // ------------------------------------------------------------------------

    private EventoHandler eventoHandler;
    private AchievementPartyHandler achievementPartyHandler;
    private AchievementCommands achievementCommands;
    private AchievementGUI achievementGUI;
    private ItemPartyHandler itemPartyHandler;
    private HotPotatoHandler hotPotatoHandler;

    // ------------------------------------------------------------------------
    //  Mobs / Bosses / Entidades
    // ------------------------------------------------------------------------
    private RemoveParticlesCreeper removeParticlesCreeper;
    private CorruptedZombies corruptedZombies;
    private CorruptedInfernalSpider corruptedinfernalSpider;
    private CustomBoat customBoat;

    /*private HellishBeeHandler hellishBeeHandler;*/
    private InfestedBeeHandler infestedBeeHandler;
    /*private QueenBeeHandler queenBeeHandler;*/

    private StatueManager statueManager;
    private StatueGUI statueGUI;

    // ------------------------------------------------------------------------
    //  Inicipialización y apagado
    // ------------------------------------------------------------------------

    @Override
    public void onEnable() {
        instance = this;
        this.Version = getDescription().getVersion();

        logStartup();
        registerBaseListeners();
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.teamsHandler = new TeamsHandler();
        this.teamsHandler.loadTeams();

        initMobSoundSystem();
        initCoreDayAndDeathStormSystem();
        initTiempoSystem();
        initItemsSystem();
        initMissionSystem();
        initChatTeamsAndFirstJoinSystem();
        initAltarSystem();
        initGeneralCommandsAndCustomSpawners();
        initAsyncAndUtilitySystems();
        initAnimationAndTitleSystem();
        initGameplaySystem();
        initHabilidadesSystem();
        initEventsSystem();
        initEventCommandsSystem();
        initShopSystem();
        initMobsAndBossesSystem();
        initMobCapSystem();
        statueEffectSystem();
        initCasinoSystem();

        getLogger().info("DinoNuggetsSMP habilitado completamente.");
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        // Apagar Nightmare
        if (nightmareMechanic != null) {
            nightmareMechanic.onDisableNightmare();
        } else {
            Bukkit.getLogger().severe("nightmareMechanic is null, cannot disable nightmare.");
        }

        // MobCap
        if (config != null) {
            MobCapManager.getInstance(this, config).shutdown();
        }

        // Sonidos
        if (mobSoundManager != null) {
            mobSoundManager.shutdown();
        }

        if (missionHandler != null) {
            missionHandler.forceSaveAllOnShutdown();
        }

        // Limpieza de bosses/abejas
        cleanupBossHandlers();

        Handlers.ToastHandler.cleanupToasts();

        // if (corruptedEnd != null) corruptedEnd.shutdown();

        shuttingDown = true;
    }

    // ------------------------------------------------------------------------
    //  Inicialización por sistemas (onEnable)
    // ------------------------------------------------------------------------

    private void logStartup() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido habilitado!, &eVersion: " + Version));
    }

    private void registerBaseListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    private void initCoreDayAndDeathStormSystem() {
        // DayHandler
        dayHandler = new DayHandler(this);

        // Comando cambio de día
        PluginCommand changeDayCommand = getCommand("cambiardia");
        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }
    }

    private void initMobSoundSystem() {
        mobSoundManager = new MobSoundManager(this);
    }

    private void initTiempoSystem() {
        // Registrar el comando para el temporizador
        tiempoCommand = new TiempoCommand(this);

        Objects.requireNonNull(getCommand("timers")).setExecutor(tiempoCommand);
        Objects.requireNonNull(getCommand("timers")).setTabCompleter(tiempoCommand);

    }

    private void initItemsSystem() {
        // Tótems protección de ítems Armor y Herramientas
        normalTotemHandler = new NormalTotemHandler(this);
        doubleLifeTotemHandler = new DoubleLifeTotem(this);
        economyItemsFunctions = new EconomyItemsFunctions(this, databaseManager);
        economyIceTotem = new EconomyIceTotem(this);
        economyFlyTotem = new EconomyFlyTotem(this);
        ExcavatorItem = new excavatorItem(this);
        amuletBloodM = new AmuletBloodM(this);
        amuletInmortal = new AmuletInmortal(this);
        lifeCampfire = new LifeCampfire(this);
        happyGhastEnchant = new HappyGhastEnchant(this);

        Bukkit.getPluginManager().registerEvents(normalTotemHandler, this);
        Bukkit.getPluginManager().registerEvents(economyItemsFunctions, this);
        Bukkit.getPluginManager().registerEvents(doubleLifeTotemHandler, this);
        Bukkit.getPluginManager().registerEvents(economyIceTotem, this);
        Bukkit.getPluginManager().registerEvents(economyFlyTotem, this);
        Bukkit.getPluginManager().registerEvents(ExcavatorItem, this);
        Bukkit.getPluginManager().registerEvents(amuletBloodM, this);
        Bukkit.getPluginManager().registerEvents(amuletInmortal, this);
        Bukkit.getPluginManager().registerEvents(lifeCampfire, this);
        Bukkit.getPluginManager().registerEvents(happyGhastEnchant, this);

        getCommand("mochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
        getCommand("delmochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
    }

    private void initMissionSystem() {
        // Este maneja la lógica y los datos.
        this.missionHandler = new MissionHandler(this, databaseManager, dayHandler);

        MissionGUI missionGUI = new MissionGUI(this, missionHandler);

        MissionCommands missionCommands = new MissionCommands(missionHandler, missionGUI);

        Objects.requireNonNull(getCommand("missions")).setExecutor(missionCommands);
        Objects.requireNonNull(getCommand("misiones")).setExecutor(missionCommands);

        Objects.requireNonNull(getCommand("missions")).setTabCompleter(missionCommands);

        // 4. Registrar la recompensa (usando el MISMO handler)
        this.missionRewardHandler = new MissionRewardHandler(this, missionHandler);

        // 5. Registrar los eventos (Listeners) DEL HANDLER ORIGINAL
        this.missionHandler.registerAllMissionListeners();

    }

    private void initChatTeamsAndFirstJoinSystem() {
        chatgeneral chatGeneralHandler = new chatgeneral();

        FirstJoinHandler firstJoinHandler = new FirstJoinHandler(this, missionHandler, databaseManager, teamsHandler);

        Bukkit.getPluginManager().registerEvents(chatGeneralHandler, this);
        Bukkit.getPluginManager().registerEvents(firstJoinHandler, this);
    }

    private void initGeneralCommandsAndCustomSpawners() {
        // spawnvct
        Objects.requireNonNull(this.getCommand("spawnqp"))
                .setExecutor(new SpawnMobs(this, dayHandler));

        // Items generales
        ItemsCommands itemsCommands = new ItemsCommands(this);

        Objects.requireNonNull(this.getCommand("giveqp")).setExecutor(itemsCommands);
        Objects.requireNonNull(this.getCommand("giveqp")).setTabCompleter(itemsCommands);

        Objects.requireNonNull(this.getCommand("ping")).setExecutor(new PingCommand(this));

        getCommand("anuncio").setExecutor(new AnuncioCommand());

        // Spawners custom
        customSpawnerHandler = new CustomSpawnerHandler(this, dayHandler);
        new GiveSpawnerCommand(this);

        Objects.requireNonNull(this.getCommand("reloadcustomspawn"))
                .setExecutor(new ReloadCustomSpawnCommand(customSpawnerHandler));

        Bukkit.getPluginManager().registerEvents(customSpawnerHandler, this);

        getCommand("bosstp").setExecutor(new BossTPCommand(this, missionHandler));
        getCommand("setbossspawn").setExecutor(new SetBossSpawnCommand(this));
        getCommand("quasoreload").setExecutor(new QuasoReloadCommand(this, databaseManager));
    }

    private void initAsyncAndUtilitySystems() {
        // Lista VHList
        new VHList(this).runTaskTimer(this, 0, 20);
        getServer().getPluginManager().registerEvents(new AnvilOverEnchantHandler(this), this);
    }

    private void initAltarSystem() {
        this.altarFunctions = new AltarFunctions(this);
        getLogger().info("Sistema de Altares y Cooldowns persistentes cargado.");
    }

    private void initAnimationAndTitleSystem() {
        // Animaciones
        ruletaAnimation = new RuletaAnimation(this);
        misionAnimation = new MisionAnimation(this);
        eventoAnimation = new EventoAnimation(this);
        successNotif = new SuccessNotification(this);

        // MuerteHandler
        MuerteHandler muertehandler = new MuerteHandler(this);

        // Comandos de teleport / disco
        Objects.requireNonNull(this.getCommand("magictp")).setExecutor(new MagicTP(this));

        Bukkit.getPluginManager().registerEvents(muertehandler, this);

        // Comandos de ruleta / muerte / bonus
        Objects.requireNonNull(this.getCommand("ruletaqp"))
                .setExecutor(new RuletaCommand(ruletaAnimation));

        // Listeners adicionales
        SnowballDamage snowballDamage1 = new SnowballDamage(this);
        Bukkit.getPluginManager().registerEvents(snowballDamage1, this);

        this.getCommand("proteccion").setExecutor(new ComandoProteccion());
    }

    private void initGameplaySystem() {
        //Nightmare
        this.nightmareMechanic = new NightmareMechanic(this, tiempoCommand, successNotif);

        //Efectos Custom
        this.effectManager = new CustomEffectManager();

        ConfusionEffect confusionEffect = new ConfusionEffect(this);
        CorruptureEffect corruptureEffect = new CorruptureEffect(this);

        effectManager.registerEffect(confusionEffect);
        effectManager.registerEffect(corruptureEffect);

        getServer().getPluginManager().registerEvents(effectManager, this);
        getServer().getPluginManager().registerEvents(corruptureEffect, this);

        this.effectPreventionListener = new EffectPreventionListener();
        getServer().getPluginManager().registerEvents(effectPreventionListener, this);

        // Comandos
        NightmareCommand nightmareCommand = new NightmareCommand(this, nightmareMechanic);
        Objects.requireNonNull(this.getCommand("addnightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("removenightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("resetnightmarecooldown")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("levelnightmare")).setExecutor(nightmareCommand);
    }

    private void initHabilidadesSystem() {
        habilidadesManager = new HabilidadesManager(this);
        habilidadesEffects = new HabilidadesEffects(this);
        habilidadesGUI = new HabilidadesGUI(this, habilidadesManager, dayHandler);
        habilidadesListener = new HabilidadesListener(this, habilidadesManager, habilidadesEffects);

        Bukkit.getPluginManager().registerEvents(habilidadesGUI, this);
        Bukkit.getPluginManager().registerEvents(habilidadesListener, this);

        HabilidadesCommand habilidadesCommand = new HabilidadesCommand(habilidadesManager, habilidadesEffects);
        Objects.requireNonNull(getCommand("habilidades")).setExecutor(habilidadesCommand);
        Objects.requireNonNull(getCommand("habilidades")).setTabCompleter(habilidadesCommand);

        getLogger().info("Sistema de Habilidades habilitado correctamente!");
    }

    private void initEventsSystem() {
        eventoHandler = new EventoHandler(this, habilidadesManager, habilidadesEffects);
        achievementPartyHandler = new AchievementPartyHandler(this);
        achievementGUI = new AchievementGUI(this, achievementPartyHandler);
        achievementCommands = new AchievementCommands(achievementPartyHandler);
        itemPartyHandler = new ItemPartyHandler(this, tiempoCommand);
        hotPotatoHandler = new HotPotatoHandler(this, tiempoCommand, habilidadesManager, habilidadesEffects);

        Bukkit.getPluginManager().registerEvents(eventoHandler, this);
        Bukkit.getPluginManager().registerEvents(achievementPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(itemPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(hotPotatoHandler, this);

        // Comandos de logros
        Objects.requireNonNull(this.getCommand("addlogro")).setExecutor(achievementCommands);
        Objects.requireNonNull(this.getCommand("addlogro")).setTabCompleter(achievementCommands);
        Objects.requireNonNull(this.getCommand("removelogro")).setExecutor(achievementCommands);
        Objects.requireNonNull(this.getCommand("removelogro")).setTabCompleter(achievementCommands);
    }

    private void initEventCommandsSystem() {
        LavaClashCommand lavaCmd = new LavaClashCommand(eventoHandler);
        getCommand("lavaclash").setExecutor(lavaCmd);
        getCommand("lavaclash").setTabCompleter(lavaCmd);

        // ItemParty
        ItemPartyCommand itemPartyCmd = new ItemPartyCommand(itemPartyHandler);
        getCommand("itemparty").setExecutor(itemPartyCmd);
        getCommand("itemparty").setTabCompleter(itemPartyCmd);

        HotPotatoCommand hotPotatoCmd = new HotPotatoCommand(hotPotatoHandler);
        getCommand("hotpotato").setExecutor(hotPotatoCmd);
        getCommand("hotpotato").setTabCompleter(hotPotatoCmd);
    }

    private void initShopSystem() {

        CustomItemRegistry.init(this);

        //Inicializar Shop System
        ShopManager shopManager = new ShopManager(this);
        ShopGUI shopGUI = new ShopGUI(shopManager);
        ShopCommands shopCommands = new ShopCommands(shopManager, shopGUI);
        ShopListeners shopListeners = new ShopListeners(shopManager, shopGUI);

        //Registrar Eventos y Comandos
        getServer().getPluginManager().registerEvents(shopListeners, this);
        getCommand("spawnshop").setExecutor(shopCommands);
        getCommand("removeshop").setExecutor(shopCommands);
        getCommand("trade").setExecutor(shopCommands);
        getCommand("trade").setTabCompleter(shopCommands);
    }

    private void initMobsAndBossesSystem() {
        corruptedZombies = new CorruptedZombies(this);
        customBoat = new CustomBoat(this);
        corruptedinfernalSpider = new CorruptedInfernalSpider(this);

        Bukkit.getPluginManager().registerEvents(customBoat, this);

        removeParticlesCreeper = new RemoveParticlesCreeper(this);
        Bukkit.getPluginManager().registerEvents(removeParticlesCreeper, this);
        //Booses
        infestedBeeHandler = new InfestedBeeHandler(this);
        /*hellishBeeHandler = new HellishBeeHandler(this);*/
        /*queenBeeHandler = new QueenBeeHandler(this);*/

        getServer().getPluginManager().registerEvents(new BossChunkListener(this), this);
        Objects.requireNonNull(getCommand("debugarena")).setExecutor(new DebugArenaCommand());
    }

    private void initMobCapSystem() {
        config = new MobCapConfig(this);
        mobCapManager = MobCapManager.getInstance(this, config);
        spawnManager = new CustomSpawnManager(this, config);

        MobCapCommand commandExecutor = new MobCapCommand(mobCapManager, config);
        MobCapTabCompleter tabCompleter = new MobCapTabCompleter();

        Objects.requireNonNull(getCommand("mobcap")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("mobcap")).setTabCompleter(tabCompleter);
        Objects.requireNonNull(getCommand("mobcapinfo")).setExecutor(commandExecutor);

        Bukkit.getPluginManager().registerEvents(spawnManager, this);
        getLogger().info("Lógica de MobCap habilitada correctamente!");
    }

    private void statueEffectSystem() {
        this.statueManager = new StatueManager(this);
        this.statueGUI = new StatueGUI(this);

        // Registrar Comandos
        getCommand("givestatue").setExecutor(new StatueCommand()); // Ajustar import

        // Registrar Eventos
        getServer().getPluginManager().registerEvents(new StatueListener(statueManager, statueGUI), this);
        getServer().getPluginManager().registerEvents(statueGUI, this);

        // Cargar estatuas ya existentes en el mundo (por si hubo reload)
        statueManager.loadStatues();
    }

    private void initCasinoSystem() {
        casinoManager = new CasinoManager(this);
        getCommand("casino").setExecutor(new CasinoCommands(casinoManager));
    }

    // ------------------------------------------------------------------------
    //  Metodos
    // ------------------------------------------------------------------------

    private void cleanupBossHandlers() {
        if (infestedBeeHandler != null) {
            try {
                infestedBeeHandler.shutdown();
                getLogger().info("InfestedBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar InfestedBeeHandler: " + e.getMessage());
            }
            infestedBeeHandler = null;
        }

/*        if (hellishBeeHandler != null) {
            try {
                hellishBeeHandler.shutdown();
                getLogger().info("HellishBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar HellishBeeHandler: " + e.getMessage());
            }
            hellishBeeHandler = null;
        }*/

/*        if (queenBeeHandler != null) {
            try {
                queenBeeHandler.shutdown();
                getLogger().info("QueenBeeHandler limpiado correctamente");
            } catch (Exception e) {
                getLogger().warning("Error al limpiar QueenBeeHandler: " + e.getMessage());
            }
            queenBeeHandler = null;
        }*/
    }
    // ------------------------------------------------------------------------
    //  Eventos básicos (join / quit / world load)
    // ------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.of("#FF009F") + "۞ " + ChatColor.RESET + ChatColor.of("#B83EFF") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#FF009F") + " se ha conectado.";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.of("#7C7981") + "۞ " + ChatColor.RESET + ChatColor.of("#B8B8B8") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#7C7981") + " se ha desconectado.";
        event.setQuitMessage(message);
    }
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (mobCapManager != null && mobCapManager.isInitialized()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                mobCapManager.handleNewWorld(event.getWorld());
            }, 20L);
        }
    }

    // ------------------------------------------------------------------------
    //  Getters útiles
    // ------------------------------------------------------------------------

    public static QuasoPlugin getInstance() {
        return instance;
    }

    public DayHandler getDayHandler() {
        return dayHandler;
    }

    public DoubleLifeTotem getDoubleLifeTotemHandler() {
        return doubleLifeTotemHandler;
    }

    public MobCapManager getMobCapManager() {
        return mobCapManager;
    }

    public MobCapConfig getMobCapConfig() {
        return config;
    }

    public SuccessNotification getSuccessNotifier() {
        return successNotif;
    }
}


