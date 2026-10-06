package imp.crissyjuanxd;

import Armors.WardenArmor;
import Bosses.BossChunkListener;
import Bosses.BossRewards;
import Bosses.InfestedWardenLairs;
import Casino.CasinoCommands;
import Casino.CasinoManager;
import Commands.*;
import EffectListener.ImmunityEffect;
import EffectListener.KeepInventoryEffect;
import EffectListener.TotemEffectRestorer;
import EffectListener.CorruptureEffect;
import EffectListener.CustomEffectManager;
import EffectListener.EffectPreventionListener;
import EndBiomes.BlackShulker;
import EndBiomes.EndDrops;
import EndBiomes.EndPopulator;
import EndBiomes.EnderInsect;
import Encantamientos.*;
import Events.BuildBattle.BuildBattleCommand;
import Events.BuildBattle.BuildBattleHandler;
import Events.HotPotato.HotPotatoCommand;
import Events.HotPotato.HotPotatoHandler;
import Events.ItemParty.ItemPartyCommand;
import Events.Skybattle.LavaClashCommand;
import InfestedCaves.*;
import Managers.ItemManager;
import Managers.QuasoDatapack;
import Managers.MobManager;
import ShopSystem.*;
import StatueManager.*;
import SistemaTumbas.*;
import items.MochilaCommand;
import Gui.dinocoins.DinoCoinsManager;
import Gui.dinocoins.DinoCoinsCommand;
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
import org.bukkit.entity.SpawnCategory;
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

    private ChangesHandler changesHandler;
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
    private DinoCoinsManager dinoCoinsManager;
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
    private StatueDebugManager statueDebugManager;
    private StatueSchematic statueSchematic;

    public static final String WORLD_NAME = "wardencave";
    private WardenGenerator generator;
    private PortalManager portalManager;
    private WardenCaveListeners listeners;
    private WardenCaveAmbient wardenAmbient;
    private PasoIgneo pasoIgneo;
    private StructureManager structureManager;

    // Inicia todos los sistemas, el orden importa porque varios dependen de otros
    @Override
    public void onEnable() {
        instance = this;
        this.Version = getPluginMeta().getVersion();

        logStartup();
        registerBaseListeners();
        saveDefaultConfig();
        ItemModels.load(this);

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
        initEnchantmentSystem();
        initEndSystem();

        getLogger().info("DinoNuggetsSMP habilitado completamente.");
    }

    // Guarda los datos y apaga los sistemas que tienen tareas o entidades activas
    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&',
                        Prefix + "&aha sido deshabilitado!, &eVersion: " + Version));

        if (economyItemsFunctions != null) economyItemsFunctions.shutdown();
        if (dinoCoinsManager != null) dinoCoinsManager.shutdown();

        if (nightmareMechanic != null) {
            nightmareMechanic.onDisableNightmare();
        } else {
            Bukkit.getLogger().severe("nightmareMechanic is null, cannot disable nightmare.");
        }

        if (config != null) {
            MobCapManager.getInstance(this, config).shutdown();
        }

        if (pasoIgneo != null) {
            pasoIgneo.restoreAll();
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

        if (effectManager != null) effectManager.cleanupAllEffects();
        if (statueDebugManager != null) statueDebugManager.cleanup();
        if (statueSchematic != null) statueSchematic.cleanup();

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

    // Cada etapa del server (uno, dos...) se prende o se apaga con /changes y queda guardada en cambios.yml
    private void initCoreDayAndDeathStormSystem() {
        changesHandler = new ChangesHandler(this);

        PluginCommand changesCommand = getCommand("changes");
        if (changesCommand != null) {
            ChangesCommand executor = new ChangesCommand(changesHandler);
            changesCommand.setExecutor(executor);
            changesCommand.setTabCompleter(executor);
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
        infestedBeeHandler = new InfestedBeeHandler(this);
        mobManager = new MobManager(this, infestedBeeHandler);
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
        Bukkit.getPluginManager().registerEvents(new KeepInventoryLiquido(this), this);
        Bukkit.getPluginManager().registerEvents(new EstatuaProtectora(this), this);
        Bukkit.getPluginManager().registerEvents(new AmuletoUltimaEsperanza(this), this);
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
        Bukkit.getPluginManager().registerEvents(new InfinitePearl(), this);
        Bukkit.getPluginManager().registerEvents(new WardenReturnItem(), this);

        getCommand("mochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
        getCommand("delmochilas").setExecutor(new MochilaCommand(economyItemsFunctions));
        dinoCoinsManager = new DinoCoinsManager(this, databaseManager, economyItemsFunctions);
        DinoCoinsCommand coinsCommand = new DinoCoinsCommand(dinoCoinsManager);
        getCommand("dinocoins").setExecutor(coinsCommand);
        getCommand("dinocoins").setTabCompleter(coinsCommand);
    }

    private void initMissionSystem() {
        this.missionHandler = new MissionHandler(this, databaseManager);

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

        customSpawnerHandler = new CustomSpawnerHandler(this, infestedBeeHandler);
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

        CorruptureEffect corruptureEffect = new CorruptureEffect(this);
        ImmunityEffect immunityEffect = new ImmunityEffect(this);
        KeepInventoryEffect keepInventoryEffect = new KeepInventoryEffect(this);

        effectManager.registerEffect(corruptureEffect);
        effectManager.registerEffect(immunityEffect);
        effectManager.registerEffect(keepInventoryEffect);

        getServer().getPluginManager().registerEvents(effectManager, this);
        getServer().getPluginManager().registerEvents(corruptureEffect, this);
        getServer().getPluginManager().registerEvents(immunityEffect, this);
        getServer().getPluginManager().registerEvents(keepInventoryEffect, this);
        getServer().getPluginManager().registerEvents(new TotemEffectRestorer(this), this);

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
        habilidadesGUI = new HabilidadesGUI(this, habilidadesManager);
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

        getServer().getPluginManager().registerEvents(new BossChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new BossRewards(this), this);
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
        this.statueDebugManager = new StatueDebugManager(this, statueManager);
        this.statueSchematic = new StatueSchematic(this, statueManager);
        this.statueGUI = new StatueGUI(this, statueManager);
        StatueCommand statueCommand = new StatueCommand(statueDebugManager);
        Objects.requireNonNull(getCommand("givestatue")).setExecutor(statueCommand);
        Objects.requireNonNull(getCommand("givestatue")).setTabCompleter(statueCommand);
        getServer().getPluginManager().registerEvents(
                new StatueListener(this, statueManager, statueGUI, statueSchematic), this);
        getServer().getPluginManager().registerEvents(statueGUI, this);
        getServer().getPluginManager().registerEvents(statueSchematic, this);
        statueSchematic.scanAllWorlds();
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
        getServer().getPluginManager().registerEvents(new NaturalWardens(this), this);
        getServer().getPluginManager().registerEvents(new WardenFruits(this), this);
        getServer().getPluginManager().registerEvents(new DarknessShield(), this);

        InfestedWardenLairs wardenLairs = new InfestedWardenLairs(this);
        WardenCaveCommand wardenCommand = new WardenCaveCommand(this, portalManager, wardenLairs);
        getCommand("wardencave").setExecutor(wardenCommand);
        getCommand("wardencave").setTabCompleter(wardenCommand);

        this.wardenAmbient = new WardenCaveAmbient(this);
        getServer().getPluginManager().registerEvents(this.wardenAmbient, this);
        getServer().getPluginManager().registerEvents(new WardenCaveItemGuard(), this);

        boolean datapackUpdated = QuasoDatapack.install(this);
        if (!QuasoDatapack.biomesLoaded()) {
            getLogger().warning("El datapack de QuasoPlugin (biomas de la Warden Cave y del End) se acaba de instalar o no está cargado. "
                    + "Reinicia el server; si la carpeta del mundo " + WORLD_NAME + " ya existía sin los biomas, bórrala para que se genere bien.");
        } else if (datapackUpdated) {
            getLogger().warning("Se actualizó el datapack de QuasoPlugin (biomas y encantamientos). Reinicia el server para que se cargue.");
        }

        createInfestedWorld();
        Bukkit.getOnlinePlayers().forEach(WardenCaveListeners::applyFakeDay);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            structureManager.loadSchematics();
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world != null) listeners.pasteTempleIfNeeded(world);
        }, 20L);
        getLogger().info("WardenCave ha sido habilitado correctamente.");
    }

    // Los 5 encantamientos del datapack: Paso Ígneo, Purificación y Visión Abisal (Warden Cave), Anclaje y Retorno del Vacío (End)
    private void initEnchantmentSystem() {
        this.pasoIgneo = new PasoIgneo(this);
        getServer().getPluginManager().registerEvents(pasoIgneo, this);
        new VisionAbisal(this);
        getServer().getPluginManager().registerEvents(new Anclaje(this), this);
        getServer().getPluginManager().registerEvents(new RetornoDelVacio(this), this);
        getServer().getPluginManager().registerEvents(new EnchantDrops(this), this);
        if (!QuasoEnchant.allLoaded()) {
            getLogger().warning("Los encantamientos del datapack no están cargados todavía: reinicia el server.");
        }
    }

    // Los biomas nuevos del End (Bosque Prismático y Páramo Marchito) se ponen con un populator en las islas de afuera
    // que se generen desde ahora; los chunks que ya existían no cambian
    private void initEndSystem() {
        BlackShulker blackShulker = new BlackShulker(this);
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.THE_END) world.getPopulators().add(new EndPopulator(blackShulker));
        }
        getServer().getPluginManager().registerEvents(blackShulker, this);
        getServer().getPluginManager().registerEvents(new EnderInsect(this), this);
        getServer().getPluginManager().registerEvents(new EndDrops(this), this);
        EndItems.registerRecipes(this);
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

    // Crea o carga la dimensión WardenCave con su generador. Para el server siempre es medianoche, así a cielo abierto
    // los mobs spawnean igual que de noche; a los jugadores se les manda el mediodía (WardenCaveListeners)
    public void createInfestedWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.generator(generator);
            world = creator.createWorld();
            if (world != null) getLogger().info("Dimensión " + WORLD_NAME + " cargada/creada.");
        }
        if (world == null) return;

        world.setGameRule(org.bukkit.GameRules.ADVANCE_TIME, false);
        if (sharedClock()) {
            getLogger().warning("time.affects-all-worlds está en true en paper-global.yml: la " + WORLD_NAME
                    + " comparte la hora con el mundo normal y no se puede dejar de noche.");
        } else {
            world.setTime(WardenCaveListeners.SERVER_TIME);
        }
        world.setGameRule(org.bukkit.GameRules.ADVANCE_WEATHER, false);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_PHANTOMS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_PATROLS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_WANDERING_TRADERS, false);

        // Mobcap de monstruos por jugador en la dimensión (la vanilla es 70); se cambia en config.yml.
        // El MobCapManager no la toca
        if (!getConfig().isInt("wardencave.limite_mobs")) {
            getConfig().set("wardencave.limite_mobs", 35);
            saveConfig();
        }
        world.setSpawnLimit(SpawnCategory.MONSTER, getConfig().getInt("wardencave.limite_mobs"));
    }

    // Con time.affects-all-worlds en true todos los mundos usan el mismo reloj
    private boolean sharedClock() {
        java.io.File file = new java.io.File("config", "paper-global.yml");
        return file.isFile() && org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file)
                .getBoolean("time.affects-all-worlds");
    }

    public static QuasoPlugin getInstance() {
        return instance;
    }

    public ChangesHandler getChangesHandler() {
        return changesHandler;
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

    public StatueManager getStatueManager() { return statueManager; }

    public StatueSchematic getStatueSchematic() { return statueSchematic; }

    public ItemManager getItemManager() { return itemManager; }

    public HabilidadesManager getHabilidadesManager() { return habilidadesManager; }

    public PortalManager getPortalManager() { return portalManager; }

    public StructureManager getStructureManager() { return structureManager; }

}
