package imp.crissyjuanxd;

import Armors.WardenArmor;
import Bosses.BossChunkListener;
import Casino.CasinoCommands;
import Casino.CasinoManager;
import Commands.*;
import EffectListener.ConfusionEffect;
import EffectListener.CorruptureEffect;
import EffectListener.CustomEffectManager;
import EffectListener.EffectPreventionListener;
import Events.BuildBattle.BuildBattleCommand;
import Events.BuildBattle.BuildBattleHandler;
import Events.HotPotato.HotPotatoCommand;
import Events.HotPotato.HotPotatoHandler;
import Events.ItemParty.ItemPartyCommand;
import Events.Skybattle.LavaClashCommand;
import InfestedCaves.*;
import Managers.ItemManager;
import Managers.MobManager;
import ShopSystem.*;
import StatueManager.*;
import SistemaTumbas.*;
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
import Pesca.*;
import mobcap.MobCapManager;
import mobcap.commands.MobCapCommand;
import mobcap.commands.MobCapTabCompleter;
import mobcap.config.MobCapConfig;
import mobcap.spawn.CustomSpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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

    public String Prefix = "§9§lQua§c§lso§7§lPlugin &7➤ &f";
    public String Version;
    public static boolean shuttingDown = false;

    private static QuasoPlugin instance;

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

    private ChatBubbleManager chatBubbleManager;

    private MobManager mobManager;
    private ItemManager itemManager;

    private FishingZoneManager fishingZoneManager;
    private FishingWandListener fishingWandListener;

    private GravesManager gravesManager;

    private MissionHandler missionHandler;
    private MissionRewardHandler missionRewardHandler;

    private AltarFunctions altarFunctions;

    private CasinoManager casinoManager;

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
    private ItemsEventos itemsEventos;
    private InvulnerableItemProtection invulnerableItemProtection;
    private AmuletInvisibility amuletInvisibility;
    private ExplosiveBow explosiveBow;

    private MobSoundManager mobSoundManager;
    private CustomSpawnerHandler customSpawnerHandler;

    private MobCapManager mobCapManager;
    private MobCapConfig config;
    private CustomSpawnManager spawnManager;

    private HabilidadesManager habilidadesManager;
    private HabilidadesGUI habilidadesGUI;
    private HabilidadesListener habilidadesListener;
    private HabilidadesEffects habilidadesEffects;
    private CustomItemRegistry customItemRegistry;

    private EventoHandler eventoHandler;
    private AchievementPartyHandler achievementPartyHandler;
    private AchievementCommands achievementCommands;
    private AchievementGUI achievementGUI;
    private ItemPartyHandler itemPartyHandler;
    private HotPotatoHandler hotPotatoHandler;
    private BuildBattleHandler buildBattleHandler;

    private RemoveParticlesCreeper removeParticlesCreeper;
    private CorruptedZombies corruptedZombies;
    private CorruptedInfernalSpider corruptedinfernalSpider;
    private CustomBoat customBoat;

    private InfestedBeeHandler infestedBeeHandler;

    private StatueManager statueManager;
    private StatueGUI statueGUI;

    public static final String WORLD_NAME = "wardencave";
    private WardenGenerator generator;
    private PortalManager portalManager;
    private WardenCaveListeners listeners;
    private WardenCaveAmbient wardenAmbient;
    private StructureManager structureManager;

    // Inicia todos los sistemas, el orden importa porque varios dependen de otros
    @Override
    public void onEnable() {
        instance = this;
        this.Version = getPluginMeta().getVersion();

        logStartup();
        registerBaseListeners();
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.teamsHandler = new TeamsHandler();
        this.teamsHandler.loadTeams();

        initMobSoundSystem();
        initCoreDayAndDeathStormSystem();
        initTiempoSystem();
        itemandmobManager();
        initItemsSystem();
        initMissionSystem();
        initChatTeamsAndFirstJoinSystem();
        initAltarSystem();
        initGeneralCommandsAndCustomSpawners();
        initAsyncAndUtilitySystems();
        initAnimationAndTitleSystem();
        initHabilidadesSystem();
        initGameplaySystem();
        initFishingSystem();
        initGraveSystem();
        initEventsSystem();
        initEventCommandsSystem();
        initShopSystem();
        initMobsAndBossesSystem();
        initMobCapSystem();
        statueEffectSystem();
        initCasinoSystem();
        initInfestedCavesDimension();

        getLogger().info("DinoNuggetsSMP habilitado completamente.");
    }

    // Guarda los datos y apaga los sistemas que tienen tareas o entidades activas
    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        if (nightmareMechanic != null) {
            nightmareMechanic.onDisableNightmare();
        } else {
            Bukkit.getLogger().severe("nightmareMechanic is null, cannot disable nightmare.");
        }

        if (config != null) {
            MobCapManager.getInstance(this, config).shutdown();
        }

        if (mobSoundManager != null) {
            mobSoundManager.shutdown();
        }

        if (missionHandler != null) {
            missionHandler.forceSaveAllOnShutdown();
        }

        if (chatBubbleManager != null) {
            chatBubbleManager.cleanup();
        }

        if (gravesManager != null) {
            gravesManager.saveData();
        }

        cleanupBossHandlers();

        Handlers.ToastHandler.cleanupToasts();

        shuttingDown = true;
    }

    private void logStartup() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido habilitado!, &eVersion: " + Version));
    }

    private void registerBaseListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    private void initCoreDayAndDeathStormSystem() {
        dayHandler = new DayHandler(this);

        PluginCommand changeDayCommand = getCommand("cambiardia");
        if (changeDayCommand != null) {
            changeDayCommand.setExecutor(new DayCommandHandler(dayHandler));
        }
    }

    private void initMobSoundSystem() {
        mobSoundManager = new MobSoundManager(this);
    }

    private void initTiempoSystem() {
        tiempoCommand = new TiempoCommand(this);

        Objects.requireNonNull(getCommand("timers")).setExecutor(tiempoCommand);
        Objects.requireNonNull(getCommand("timers")).setTabCompleter(tiempoCommand);

    }

    private void itemandmobManager() {
        itemManager = new ItemManager(this);
        mobManager = new MobManager(this, dayHandler);
    }

    private void initItemsSystem() {
        invulnerableItemProtection = new InvulnerableItemProtection(this);
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
        itemsEventos = new ItemsEventos(this);
        amuletInvisibility = new AmuletInvisibility(this);
        explosiveBow = new ExplosiveBow(this);
        WardenArmor wardenArmor = new WardenArmor(this);

        Bukkit.getPluginManager().registerEvents(invulnerableItemProtection, this);
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
        Bukkit.getPluginManager().registerEvents(itemsEventos, this);
        Bukkit.getPluginManager().registerEvents(amuletInvisibility, this);
        Bukkit.getPluginManager().registerEvents(explosiveBow, this);
        Bukkit.getPluginManager().registerEvents(wardenArmor, this);

        getCommand("mochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
        getCommand("delmochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
    }

    private void initMissionSystem() {
        this.missionHandler = new MissionHandler(this, databaseManager, dayHandler);

        MissionGUI missionGUI = new MissionGUI(this, missionHandler);

        MissionCommands missionCommands = new MissionCommands(missionHandler, missionGUI);

        Objects.requireNonNull(getCommand("missions")).setExecutor(missionCommands);
        Objects.requireNonNull(getCommand("misiones")).setExecutor(missionCommands);

        Objects.requireNonNull(getCommand("missions")).setTabCompleter(missionCommands);

        this.missionRewardHandler = new MissionRewardHandler(this, missionHandler);

        this.missionHandler.registerAllMissionListeners();

    }

    private void initChatTeamsAndFirstJoinSystem() {
        chatgeneral chatGeneralHandler = new chatgeneral();

        FirstJoinHandler firstJoinHandler = new FirstJoinHandler(this, missionHandler, databaseManager, teamsHandler);

        Bukkit.getPluginManager().registerEvents(chatGeneralHandler, this);
        Bukkit.getPluginManager().registerEvents(firstJoinHandler, this);
    }

    private void initGeneralCommandsAndCustomSpawners() {
        Objects.requireNonNull(this.getCommand("spawnqp"))
                .setExecutor(new SpawnMobs(this, mobManager));

        ItemsCommands itemsCommands = new ItemsCommands(this, itemManager);

        Objects.requireNonNull(this.getCommand("giveqp")).setExecutor(itemsCommands);
        Objects.requireNonNull(this.getCommand("giveqp")).setTabCompleter(itemsCommands);

        Objects.requireNonNull(this.getCommand("ping")).setExecutor(new PingCommand(this));

        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("anuncio").setExecutor(new AnuncioCommand());

        customSpawnerHandler = new CustomSpawnerHandler(this, dayHandler);
        new GiveSpawnerCommand(this);

        Objects.requireNonNull(this.getCommand("reloadcustomspawn"))
                .setExecutor(new ReloadCustomSpawnCommand(customSpawnerHandler));

        Bukkit.getPluginManager().registerEvents(customSpawnerHandler, this);

        Homes homesCmd = new Homes(this);
        getCommand("sethome").setExecutor(homesCmd);
        getCommand("home").setExecutor(homesCmd);
        getCommand("delhome").setExecutor(homesCmd);

        getCommand("home").setTabCompleter(homesCmd);
        getCommand("delhome").setTabCompleter(homesCmd);

        getCommand("bosstp").setExecutor(new BossTPCommand(this, missionHandler));
        getCommand("setbossspawn").setExecutor(new SetBossSpawnCommand(this));
        getCommand("quasoreload").setExecutor(new QuasoReloadCommand(this, databaseManager));

        getCommand("settiendas").setExecutor(new SetTiendasCommand(this));
        getCommand("tiendas").setExecutor(new TiendasCommand(this));
    }

    private void initAsyncAndUtilitySystems() {
        new VHList(this);
        getServer().getPluginManager().registerEvents(new AnvilOverEnchantHandler(this), this);
    }

    private void initAltarSystem() {
        this.altarFunctions = new AltarFunctions(this);
        getLogger().info("Sistema de Altares y Cooldowns persistentes cargado.");
    }

    private void initAnimationAndTitleSystem() {
        ruletaAnimation = new RuletaAnimation(this);
        misionAnimation = new MisionAnimation(this);
        eventoAnimation = new EventoAnimation(this);
        successNotif = new SuccessNotification(this);

        MuerteHandler muertehandler = new MuerteHandler(this);

        Objects.requireNonNull(this.getCommand("magictp")).setExecutor(new MagicTP(this));

        Bukkit.getPluginManager().registerEvents(muertehandler, this);

        Objects.requireNonNull(this.getCommand("ruletaqp"))
                .setExecutor(new RuletaCommand(ruletaAnimation));

        SnowballDamage snowballDamage1 = new SnowballDamage(this);
        Bukkit.getPluginManager().registerEvents(snowballDamage1, this);

        this.getCommand("proteccion").setExecutor(new ComandoProteccion());
    }

    private void initGameplaySystem() {
        this.nightmareMechanic = new NightmareMechanic(this, tiempoCommand, successNotif);

        this.effectManager = new CustomEffectManager();

        ConfusionEffect confusionEffect = new ConfusionEffect(this);
        CorruptureEffect corruptureEffect = new CorruptureEffect(this);

        effectManager.registerEffect(confusionEffect);
        effectManager.registerEffect(corruptureEffect);

        getServer().getPluginManager().registerEvents(effectManager, this);
        getServer().getPluginManager().registerEvents(corruptureEffect, this);

        this.effectPreventionListener = new EffectPreventionListener();
        getServer().getPluginManager().registerEvents(effectPreventionListener, this);

        NightmareCommand nightmareCommand = new NightmareCommand(this, nightmareMechanic);
        Objects.requireNonNull(this.getCommand("addnightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("removenightmare")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("resetnightmarecooldown")).setExecutor(nightmareCommand);
        Objects.requireNonNull(this.getCommand("levelnightmare")).setExecutor(nightmareCommand);

        chatBubbleManager = new ChatBubbleManager(this);

        getServer().getPluginManager().registerEvents(chatBubbleManager, this);

        getCommand("bubble").setExecutor(chatBubbleManager);
        getCommand("bubble").setTabCompleter(chatBubbleManager);
    }

    private void initFishingSystem() {
        fishingZoneManager = new FishingZoneManager(this);
        fishingWandListener = new FishingWandListener(this);

        FishingListener fishingListener = new FishingListener(this, fishingZoneManager, itemManager);

        getServer().getPluginManager().registerEvents(fishingWandListener, this);
        getServer().getPluginManager().registerEvents(fishingListener, this);

        FishingCommand fishingCommand = new FishingCommand(this, fishingZoneManager, fishingWandListener);
        getCommand("pesca").setExecutor(fishingCommand);
        getCommand("pesca").setTabCompleter(fishingCommand);

        getLogger().info("Sistema de Pesca habilitado correctamente.");
    }

    private void initGraveSystem() {
        this.gravesManager = new GravesManager(this);
        getServer().getPluginManager().registerEvents(new GravesListener(gravesManager), this);

        GravesPublicCommand tumbaCmd = new GravesPublicCommand(gravesManager);
        getCommand("tumba").setExecutor(tumbaCmd);
        getCommand("tumba").setTabCompleter(tumbaCmd);

        GravesCommand tumbasAdminCmd = new GravesCommand(gravesManager);
        getCommand("tumbas").setExecutor(tumbasAdminCmd);
        getCommand("tumbas").setTabCompleter(tumbasAdminCmd);
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

    // Eventos del server y el guardado de inventarios mientras alguien está en un evento
    private void initEventsSystem() {
        eventoHandler = new EventoHandler(this, habilidadesManager, habilidadesEffects);
        achievementPartyHandler = new AchievementPartyHandler(this);
        achievementGUI = new AchievementGUI(this, achievementPartyHandler);
        achievementCommands = new AchievementCommands(achievementPartyHandler);
        itemPartyHandler = new ItemPartyHandler(this, tiempoCommand);
        hotPotatoHandler = new HotPotatoHandler(this, tiempoCommand, habilidadesManager, habilidadesEffects);
        buildBattleHandler = new BuildBattleHandler(this, tiempoCommand);

        EventInventoryManager invManager = new EventInventoryManager(this, databaseManager);

        eventoHandler.setEventInventoryManager(invManager);
        buildBattleHandler.setEventInventoryManager(invManager);

        invManager.setIsInEventCondition(nombre ->
                eventoHandler.isParticipante(nombre) ||
                        buildBattleHandler.isParticipante(nombre)
        );

        Bukkit.getPluginManager().registerEvents(eventoHandler, this);
        Bukkit.getPluginManager().registerEvents(achievementPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(itemPartyHandler, this);
        Bukkit.getPluginManager().registerEvents(hotPotatoHandler, this);
        Bukkit.getPluginManager().registerEvents(buildBattleHandler, this);

        Objects.requireNonNull(this.getCommand("addlogro")).setExecutor(achievementCommands);
        Objects.requireNonNull(this.getCommand("addlogro")).setTabCompleter(achievementCommands);
        Objects.requireNonNull(this.getCommand("removelogro")).setExecutor(achievementCommands);
        Objects.requireNonNull(this.getCommand("removelogro")).setTabCompleter(achievementCommands);
    }

    private void initEventCommandsSystem() {
        LavaClashCommand lavaCmd = new LavaClashCommand(eventoHandler);
        getCommand("lavaclash").setExecutor(lavaCmd);
        getCommand("lavaclash").setTabCompleter(lavaCmd);

        ItemPartyCommand itemPartyCmd = new ItemPartyCommand(itemPartyHandler);
        getCommand("itemparty").setExecutor(itemPartyCmd);
        getCommand("itemparty").setTabCompleter(itemPartyCmd);

        HotPotatoCommand hotPotatoCmd = new HotPotatoCommand(hotPotatoHandler);
        getCommand("hotpotato").setExecutor(hotPotatoCmd);
        getCommand("hotpotato").setTabCompleter(hotPotatoCmd);

        BuildBattleCommand bbCmd = new BuildBattleCommand(buildBattleHandler);
        getCommand("buildbattle").setExecutor(bbCmd);
        getCommand("buildbattle").setTabCompleter(bbCmd);
    }

    private void initShopSystem() {

        CustomItemRegistry.init(this, itemManager);

        ShopManager shopManager = new ShopManager(this);
        ShopGUI shopGUI = new ShopGUI(shopManager);
        ShopCommands shopCommands = new ShopCommands(shopManager, shopGUI);
        ShopListeners shopListeners = new ShopListeners(shopManager, shopGUI);

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
        infestedBeeHandler = new InfestedBeeHandler(this);

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

        getCommand("givestatue").setExecutor(new StatueCommand());

        getServer().getPluginManager().registerEvents(new StatueListener(statueManager, statueGUI), this);
        getServer().getPluginManager().registerEvents(statueGUI, this);

        statueManager.loadStatues();
    }

    private void initCasinoSystem() {
        casinoManager = new CasinoManager(this, itemManager);
        getCommand("casino").setExecutor(new CasinoCommands(casinoManager));
    }

    private void initInfestedCavesDimension() {
        this.generator = new WardenGenerator(this);
        this.structureManager = new StructureManager(this);
        this.portalManager = new PortalManager(this);
        this.listeners = new WardenCaveListeners(this, portalManager, structureManager);
        getServer().getPluginManager().registerEvents(listeners, this);
        getServer().getPluginManager().registerEvents(portalManager, this);

        WardenCaveCommand wardenCommand = new WardenCaveCommand(this, portalManager);
        getCommand("wardencave").setExecutor(wardenCommand);
        getCommand("wardencave").setTabCompleter(wardenCommand);

        this.wardenAmbient = new WardenCaveAmbient(this);
        getServer().getPluginManager().registerEvents(this.wardenAmbient, this);

        createInfestedWorld();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            structureManager.loadSchematics();
        }, 20L);
        getLogger().info("WardenCave ha sido habilitado correctamente.");
    }

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
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = ChatColor.of("#FFD700") + "\uD83E\uDD50 " + ChatColor.RESET + ChatColor.of("#B0E0E6") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#B0E0E6") + " se ha conectado a " + ChatColor.of("#FCE68D") + ChatColor.BOLD + "Croissants";
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = ChatColor.of("#7C7981") + "\uD83E\uDD50 " + ChatColor.RESET + ChatColor.of("#B8B8B8") + ChatColor.BOLD + event.getPlayer().getName() + ChatColor.RESET + ChatColor.of("#7C7981") + " se ha desconectado.";
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

    // Crea o carga la dimensión WardenCave con su generador, siempre de noche
    public void createInfestedWorld() {
        if (Bukkit.getWorld(WORLD_NAME) == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.generator(generator);
            World world = creator.createWorld();
            if (world != null) {
                world.setGameRule(org.bukkit.GameRules.ADVANCE_TIME, false);
                world.setTime(18000);
                getLogger().info("Dimensión " + WORLD_NAME + " cargada/creada.");
            }
        }
    }

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

    public PortalManager getPortalManager() { return portalManager; }

    public StructureManager getStructureManager() { return structureManager; }

}


