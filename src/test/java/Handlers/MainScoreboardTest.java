package Handlers;

import Events.MissionSystem.MissionHandler;
import Gui.dinocoins.DinoCoinsManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import support.PaperTestRegistry;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MainScoreboardTest {
    private MainScoreboard scoreboard;
    private Player player;
    private Scoreboard main;
    private Scoreboard own;
    private ScoreboardManager boards;
    private YamlConfiguration config;
    private final Map<String, Team> teams = new HashMap<>();
    private final Map<String, Set<String>> entries = new HashMap<>();
    private final Set<String> originalMembers = new HashSet<>(Set.of("Crosszy"));
    private AtomicReference<Scoreboard> current;
    private AtomicReference<Objective> sidebar;
    private Objective objective;
    private MockedStatic<Bukkit> bukkit;

    @BeforeAll
    static void initializePaper() { PaperTestRegistry.initialize(); }

    @BeforeEach
    void setup() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        when(plugin.getServer()).thenReturn(server);
        config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        main = mock(Scoreboard.class);
        own = mock(Scoreboard.class);
        Team mainTeam = mock(Team.class);
        when(mainTeam.getName()).thenReturn("ZMiembro");
        when(mainTeam.getEntries()).thenAnswer(call -> originalMembers);
        when(mainTeam.hasEntry(anyString())).thenAnswer(call -> originalMembers.contains(call.getArgument(0)));
        when(main.getTeams()).thenReturn(Set.of(mainTeam));
        when(main.getEntryTeam("Crosszy")).thenReturn(mainTeam);
        objective = mock(Objective.class);
        when(objective.getName()).thenReturn("QuasoBoard");
        when(objective.getScore(anyString())).thenAnswer(call -> mock(Score.class));
        sidebar = new AtomicReference<>();
        doAnswer(call -> { sidebar.set(objective); return null; }).when(objective).setDisplaySlot(DisplaySlot.SIDEBAR);
        when(own.getObjective(DisplaySlot.SIDEBAR)).thenAnswer(call -> sidebar.get());
        AtomicReference<Objective> registered = new AtomicReference<>();
        when(own.getObjective("QuasoBoard")).thenAnswer(call -> registered.get());
        when(own.registerNewObjective(eq("QuasoBoard"), any(Criteria.class), anyString())).thenAnswer(call -> {
            registered.set(objective);
            return objective;
        });
        when(own.getTeam(anyString())).thenAnswer(call -> teams.get(call.getArgument(0)));
        when(own.registerNewTeam(anyString())).thenAnswer(call -> {
            String name = call.getArgument(0);
            Team team = mock(Team.class);
            Set<String> members = new HashSet<>();
            AtomicReference<String> prefix = new AtomicReference<>("");
            when(team.getPrefix()).thenAnswer(read -> prefix.get());
            doAnswer(write -> { prefix.set(write.getArgument(0)); return null; }).when(team).setPrefix(nullable(String.class));
            when(team.getEntries()).thenAnswer(read -> members);
            when(team.hasEntry(anyString())).thenAnswer(read -> members.contains(read.getArgument(0)));
            doAnswer(write -> members.add(write.getArgument(0))).when(team).addEntry(anyString());
            doAnswer(write -> members.remove(write.getArgument(0))).when(team).removeEntry(anyString());
            teams.put(name, team);
            entries.put(name, members);
            return team;
        });
        player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getName()).thenReturn("Crosszy");
        when(player.getUniqueId()).thenReturn(playerId);
        current = new AtomicReference<>(main);
        when(player.getScoreboard()).thenAnswer(call -> current.get());
        doAnswer(call -> { current.set(call.getArgument(0)); return null; }).when(player).setScoreboard(any());
        MissionHandler missions = mock(MissionHandler.class);
        when(missions.getCompletedMissionCount(player)).thenReturn(12);
        when(missions.getTotalMissionCount()).thenReturn(100);
        DinoCoinsManager coins = mock(DinoCoinsManager.class);
        when(coins.getCachedDinoCoins(playerId)).thenReturn(320);
        boards = mock(ScoreboardManager.class);
        when(boards.getMainScoreboard()).thenReturn(main);
        when(boards.getNewScoreboard()).thenReturn(own);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(task.getTaskId()).thenReturn(1);
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), eq(0L), eq(20L))).thenReturn(task);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(boards);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
        bukkit.when(() -> Bukkit.getScoreboardCriteria(anyString())).thenAnswer(call -> mock(Criteria.class));
        scoreboard = new MainScoreboard(plugin, missions, coins);
    }

    @AfterEach
    void cleanup() { bukkit.close(); }

    private String line(int number) {
        return net.md_5.bungee.api.ChatColor.stripColor(teams.get("sb_line_" + number).getPrefix());
    }

    @Test
    void displaysQuasoDataAndReloadsConfiguredUnicodeTitleAndIcons() {
        scoreboard.run();
        assertSame(own, current.get());
        assertEquals("\uE901 Usuario: Crosszy", line(11));
        assertEquals("\uE902 Rango: DinoNugget", line(9));
        assertEquals("\uE903 Misiones: 12/100", line(7));
        assertEquals("\uE904 DinoCoins: 320", line(5));
        assertEquals("\uE905 Trabajo: Pendiente", line(3));
        assertEquals("\uE906 croissant.holy.gg", line(1));
        assertTrue(entries.get("ZMiembro").contains("Crosszy"));
        config.set("main-scoreboard.titulo", "\uE950");
        config.set("main-scoreboard.iconos.usuario", "\uE951");
        scoreboard.run();
        verify(objective).setDisplayName("\uE950");
        assertEquals("\uE951 Usuario: Crosszy", line(11));
        originalMembers.clear();
        scoreboard.run();
        assertFalse(entries.get("ZMiembro").contains("Crosszy"));
    }

    @Test
    void yieldsToEventSidebarAndRestoresThePersonalBoardAfterTheEvent() {
        scoreboard.run();
        Scoreboard eventBoard = mock(Scoreboard.class);
        Objective eventObjective = mock(Objective.class);
        when(eventObjective.getName()).thenReturn("itemparty");
        when(eventBoard.getObjective(DisplaySlot.SIDEBAR)).thenReturn(eventObjective);
        current.set(eventBoard);
        scoreboard.run();
        assertSame(eventBoard, current.get());
        verify(eventBoard, never()).registerNewTeam(anyString());
        current.set(main);
        scoreboard.run();
        assertSame(own, current.get());
        verify(boards, times(1)).getNewScoreboard();
        scoreboard.shutdown();
        assertSame(main, current.get());
    }
}
