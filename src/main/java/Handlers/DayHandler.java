package Handlers;

import Dificultades.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

    public class DayHandler {
        private final JavaPlugin plugin;
        private int currentDay = 1;
        private BukkitRunnable dayTask;
        private DayOneChanges dayOneChanges;
    
        public DayHandler(JavaPlugin plugin) {
            this.plugin = plugin;
            dayOneChanges = new DayOneChanges(plugin, this);
            loadDayData();
            applyCurrentDayChanges();
        }

        // Iniciar o reiniciar el temporizador de día

        public void advanceDay() {
            currentDay++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(ChatColor.GOLD + "¡Es el día " + currentDay + "!");
            }
            applyCurrentDayChanges();
            saveDayData();
        }

        public void changeDay(int day) {
            revertCurrentDayChanges();
            currentDay = day;

            applyCurrentDayChanges();
            saveDayData();
        }


        private void applyCurrentDayChanges() {
            if (currentDay >= 1) {
                dayOneChanges.apply();
            }
        }

        private void revertCurrentDayChanges() {
            if (currentDay < 1) {
                dayOneChanges.revert();
            }
        }

        public int getCurrentDay() {
            return currentDay;
        }

        private void saveDayData() {
            try {
                File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                config.set("DiaActual", currentDay);

                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void loadDayData() {
            File file = new File(plugin.getDataFolder(), "DayandStorm.yml");
            if (file.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                currentDay = config.getInt("DiaActual", 1);
            }
        }
    }
