package Events.MissionSystem;

import Handlers.ActionBarHandler;
import Handlers.Teams.TeamType;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class Mission30 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    public Mission30(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (missionHandler.isMissionActive(player, 30)) {
                    MissionData data = missionHandler.getData(player, 30);

                    if (!data.isCompleted()) {
                        int completedMissions = 0;

                        for (int i = 1; i <= 29; i++) {
                            if (missionHandler.isMissionCompleted(player, i)) {
                                completedMissions++;
                            }
                        }

                        data.setProgressValue("missions_completed", completedMissions);
                        missionHandler.saveData(player, 30, data);

                        if (completedMissions >= 29) {
                            successNotification.showSuccess(player);
                            missionHandler.completeMission(player, 30);

                            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                            Team team = board.getTeam(TeamType.Y_MIEMBRO.getId());
                            if (team != null) {
                                team.addEntry(player.getName());
                            } else {
                                // Por si el equipo no está creado en el mundo aún
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "team join YMiembro " + player.getName());
                            }
                        }
                    }
                }
            }
        }, 600L, 1200L);
    }

    @Override
    public String getName() {
        return "Jugador Experto";
    }

    @Override
    public String getDescription() {
        return "Completa las 29 misiones anteriores.";
    }

    @Override
    public int getMissionNumber() {
        return 30;
    }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();

        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(30);

        ItemStack tokens = EconomyItems.createVithiumToken();
        tokens.setAmount(50);

        ItemStack enchantedApples = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 15);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);

        for (int i = 0; i < 27; i++) {
            if (i == 11) {
                rewards.add(tokens);
            } else if (i == 13) {
                rewards.add(coins);
            } else if (i == 15) {
                rewards.add(enchantedApples);
            } else {
                rewards.add(xpFill.clone());
            }
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}
}