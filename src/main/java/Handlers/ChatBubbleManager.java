package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatBubbleManager implements Listener, CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NamespacedKey bubbleKey;
    private boolean enabled = true;

    private final Map<UUID, BubbleData> activeBubbles = new HashMap<>();

    public ChatBubbleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bubbleKey = new NamespacedKey(plugin, "is_chat_bubble");

        cleanupOrphanedBubbles();
        startTickTask();
    }

    public void cleanup() {
        for (BubbleData data : activeBubbles.values()) {
            if (data.display != null && !data.display.isDead()) {
                data.display.remove();
            }
        }
        activeBubbles.clear();
    }

    private void cleanupOrphanedBubbles() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getPersistentDataContainer().has(bubbleKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    private String getPlayerColor(Player p) {
        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = board.getEntryTeam(p.getName());

        if (team != null) {
            Handlers.Teams.TeamType type = Handlers.Teams.TeamType.getById(team.getName());
            if (type != null) {
                return type.getBungeeColor().toString();
            }
        }
        return "§f";
    }

    private void startTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeBubbles.isEmpty()) return;

                Iterator<Map.Entry<UUID, BubbleData>> it = activeBubbles.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, BubbleData> entry = it.next();
                    Player p = Bukkit.getPlayer(entry.getKey());
                    BubbleData data = entry.getValue();

                    if (p == null || !p.isOnline() || data.display.isDead()) {
                        if (data.display != null && !data.display.isDead()) data.display.remove();
                        it.remove();
                        continue;
                    }

                    if (data.display.getVehicle() == null || !data.display.getVehicle().equals(p)) {
                        p.addPassenger(data.display);
                    }

                    data.ticks++;

                    if (data.ticks > data.maxTicks) {
                        data.display.remove();
                        it.remove();
                        continue;
                    }

                    Transformation transform = data.display.getTransformation();
                    transform.getTranslation().add(0f, 0.005f, 0f);

                    data.display.setInterpolationDelay(0);
                    data.display.setInterpolationDuration(2);
                    data.display.setTransformation(transform);

                    int fadeStart = data.maxTicks - 20;
                    if (data.ticks > fadeStart) {
                        int ticksLeft = data.maxTicks - data.ticks;
                        float progress = Math.max(0, ticksLeft / 20.0f);

                        byte textOpacity = (byte) (255 * progress);
                        int bgAlpha = (int) (100 * progress);

                        data.display.setTextOpacity(textOpacity);
                        data.display.setBackgroundColor(Color.fromARGB(bgAlpha, 0, 0, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (!enabled) return;

        Player p = e.getPlayer();
        String message = e.getMessage();

        String bubbleText = "§b💬 " + getPlayerColor(p) + p.getName() + "§7: §f" + message;

        int wordCount = message.split("\\s+").length;
        int calculatedTicks = 60 + (wordCount * 15);
        final int finalMaxTicks = Math.min(calculatedTicks, 200);

        Bukkit.getScheduler().runTask(plugin, () -> {
            BubbleData data = activeBubbles.get(p.getUniqueId());

            if (data != null && data.display != null && !data.display.isDead()) {
                data.ticks = 0;
                data.maxTicks = finalMaxTicks;
                data.display.setTextOpacity((byte) 255);
                data.display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
                data.display.setText(bubbleText);

                Transformation transform = data.display.getTransformation();
                transform.getTranslation().set(0f, 0.6f, 0f);
                data.display.setInterpolationDuration(0);
                data.display.setTransformation(transform);
            } else {
                TextDisplay display = (TextDisplay) p.getWorld().spawnEntity(p.getLocation(), EntityType.TEXT_DISPLAY);
                p.addPassenger(display);

                display.getPersistentDataContainer().set(bubbleKey, PersistentDataType.BYTE, (byte) 1);

                display.setBillboard(Display.Billboard.CENTER);
                display.setAlignment(TextDisplay.TextAlignment.CENTER);
                display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
                display.setLineWidth(140);
                display.setShadowed(true);
                display.setText(bubbleText);

                display.setViewRange(24.0f / 64.0f);

                Transformation transform = display.getTransformation();
                transform.getTranslation().set(0f, 0.6f, 0f);
                display.setTransformation(transform);

                activeBubbles.put(p.getUniqueId(), new BubbleData(display, finalMaxTicks));
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("quaso.chatbubble.admin")) {
            sender.sendMessage("§cNo tienes permisos para esto.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUso: /bubble <start|stop>");
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (enabled) {
                sender.sendMessage("§eLas burbujas de chat ya están activadas.");
            } else {
                enabled = true;
                sender.sendMessage("§aBurbujas de chat globales §lACTIVADAS.");
            }
        } else if (args[0].equalsIgnoreCase("stop")) {
            if (!enabled) {
                sender.sendMessage("§eLas burbujas de chat ya están desactivadas.");
            } else {
                enabled = false;
                cleanup();
                sender.sendMessage("§cBurbujas de chat globales §lDESACTIVADAS.");
            }
        } else {
            sender.sendMessage("§cUso: /bubble <start|stop>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("start");
            list.add("stop");
            return list;
        }
        return new ArrayList<>();
    }

    private static class BubbleData {
        TextDisplay display;
        int ticks = 0;
        int maxTicks;

        BubbleData(TextDisplay display, int maxTicks) {
            this.display = display;
            this.maxTicks = maxTicks;
        }
    }
}