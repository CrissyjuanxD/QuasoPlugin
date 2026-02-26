package TitleListener;

import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RuletaAnimation {
    private final JavaPlugin plugin;
    private static int ongoingAnimations = 0;

    public RuletaAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void playAnimation(Player player, String jsonMessage) {
        if (ongoingAnimations == 0) {
            Bukkit.getLogger().info("RuletaAnimation empezada para todos los jugadores.");
        }
        ongoingAnimations++;

        // Efectos iniciales
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0, true, false, false));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 3.0f, 1.0f);

        // Etapas de la animación
        String[] stages = {
                ChatColor.GRAY + "C_",
                ChatColor.WHITE + "Ca",
                ChatColor.GRAY + "Cam_",
                ChatColor.WHITE + "Camb",
                ChatColor.GRAY + "Cambi_",
                ChatColor.WHITE + "Cambio",
                ChatColor.DARK_RED + "\u26a0 " + ChatColor.of("#E96034") + "Cambio " + ChatColor.of("#EA8A2B") + "Aplicado" + ChatColor.DARK_RED + " \u26a0",
        };

        Sound[] stageSounds = {
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.BLOCK_NOTE_BLOCK_HAT,
                Sound.ENTITY_ENDER_DRAGON_GROWL
        };

        new BukkitRunnable() {
            int currentStage = 0;
            int tickCounter = 0;
            boolean showingUnderscore = false;
            BukkitRunnable blinkTask = null;
            boolean showingFinalMessage = false;
            int finalMessageTicks = 0;

            @Override
            public void run() {
                if (showingFinalMessage) {
                    finalMessageTicks++;
                    if (finalMessageTicks >= 60) { // 3 segundos (60 ticks)
                        showJsonMessageAndFinish();
                    }
                    return;
                }

                if (currentStage >= stages.length - 1) {
                    showFinalStage();
                    return;
                }

                tickCounter++;

                String display = stages[currentStage];
                if (showingUnderscore && stages[currentStage].endsWith("_")) {
                    display = stages[currentStage].substring(0, stages[currentStage].length() - 1) + "_";
                }

                player.sendTitle(display, "", 0, 10, 0);

                if (tickCounter == 1) {
                    player.playSound(player.getLocation(), stageSounds[currentStage], 1.0f, 0.8f + (currentStage * 0.1f));
                }

                if (tickCounter >= 5) {
                    advanceStage();
                }
            }

            private void advanceStage() {
                currentStage++;
                tickCounter = 0;
                showingUnderscore = false;

                if (blinkTask != null) blinkTask.cancel();

                if (currentStage < stages.length - 1 && stages[currentStage].endsWith("_")) {
                    blinkTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (currentStage >= stages.length - 1) return;

                            String blinkDisplay = showingUnderscore ?
                                    stages[currentStage].substring(0, stages[currentStage].length() - 1) + "_" :
                                    stages[currentStage];

                            player.sendTitle(blinkDisplay, "", 0, 5, 0);
                            showingUnderscore = !showingUnderscore;
                        }
                    };
                    blinkTask.runTaskTimer(plugin, 0, 10);
                }
            }

            private void showFinalStage() {
                player.sendTitle(stages[stages.length - 1], "", 10, 70, 20);
                player.playSound(player.getLocation(), stageSounds[stageSounds.length - 1], 1.0f, 0.7f);
                showingFinalMessage = true;
            }

            private void showJsonMessageAndFinish() {
                if (jsonMessage != null && !jsonMessage.isEmpty()) {
                    // ENVIAR EL JSON EXACTAMENTE COMO EN EL CÓDIGO ORIGINAL
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + jsonMessage);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
                }

                synchronized (RuletaAnimation.this) {
                    if (--ongoingAnimations == 0) {
                        Bukkit.getLogger().info("RuletaAnimation finalizada para todos los jugadores.");
                    }
                }

                if (blinkTask != null) blinkTask.cancel();
                cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}