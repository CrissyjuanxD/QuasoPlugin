package Pesca;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class FishingMiniGame {

    private static final String COLOR_RED    = "#F02E3B";
    private static final String COLOR_ORANGE = "#EF911C";
    private static final String COLOR_GREEN  = "#78E58A";
    private static final String COLOR_WHITE  = "#F0F0F0";
    private static final String COLOR_GOLD   = "gold";

    private static final int TOTAL_SLOTS = 14;
    private static final int MAX_TICKS = 100;

    private static final int[] BASE_PATTERN = {0, 0, 0, 0, 0, 1, 1, 2, 1, 1, 0, 0, 0, 0};

    private final QuasoPlugin plugin;
    private final Player player;
    private final Runnable onComplete;
    private final ItemStack vanillaLoot;
    private final int colorOffset;

    private int cursorPos;
    private int direction;
    private BukkitTask task;

    private boolean finished = false;
    private boolean failedByTime = false;

    private static int globalOffset = 0;

    public FishingMiniGame(QuasoPlugin plugin, Player player, ItemStack vanillaLoot, Runnable onComplete) {
        this.plugin = plugin;
        this.player = player;
        this.vanillaLoot = vanillaLoot;
        this.onComplete = onComplete;

        this.colorOffset = globalOffset;
        globalOffset = (globalOffset + 1) % TOTAL_SLOTS;

        this.cursorPos = 0;
        this.direction = 1;
    }

    public int getSlotType(int index) {
        int realIndex = (index + colorOffset) % TOTAL_SLOTS;
        return BASE_PATTERN[realIndex];
    }

    public int getCurrentSlotType() {
        return getSlotType(cursorPos);
    }

    public ItemStack getVanillaLoot() {
        return vanillaLoot;
    }

    public boolean isFailedByTime() {
        return failedByTime;
    }

    public void start() {
        int tickSpeed = 1;

        task = new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || finished) {
                    cancel();
                    return;
                }

                ticksElapsed += tickSpeed;

                if (ticksElapsed >= MAX_TICKS) {
                    failedByTime = true;
                    finished = true;
                    cancel();
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                    onComplete.run();
                    return;
                }

                sendActionBar();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.2f, 2.0f);

                cursorPos += direction;
                if (cursorPos <= 0) {
                    cursorPos = 0;
                    direction = 1;
                } else if (cursorPos >= TOTAL_SLOTS - 1) {
                    cursorPos = TOTAL_SLOTS - 1;
                    direction = -1;
                }
            }
        }.runTaskTimer(plugin, 0L, tickSpeed);
    }

    public void playerClick() {
        if (finished) return;
        finished = true;
        if (task != null) task.cancel();

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        onComplete.run();
    }

    public boolean isFinished() {
        return finished;
    }

    private void sendActionBar() {
        ComponentBuilder cb = new ComponentBuilder();

        cb.append("≪").color(net.md_5.bungee.api.ChatColor.of(COLOR_GOLD)).bold(false);
        cb.append("║").color(net.md_5.bungee.api.ChatColor.WHITE).bold(false);

        int i = 0;
        while (i < TOTAL_SLOTS) {
            if (i == cursorPos) {
                cb.append("■").color(net.md_5.bungee.api.ChatColor.of(COLOR_WHITE)).bold(false);
                i++;
            } else {
                int slotType = getSlotType(i);
                String hex = colorForType(slotType);
                StringBuilder sb = new StringBuilder();
                while (i < TOTAL_SLOTS && i != cursorPos && getSlotType(i) == slotType) {
                    sb.append("■");
                    i++;
                }
                cb.append(sb.toString()).color(net.md_5.bungee.api.ChatColor.of(hex)).bold(false);
            }
        }

        cb.append("║").color(net.md_5.bungee.api.ChatColor.WHITE).bold(false);
        cb.append("≫").color(net.md_5.bungee.api.ChatColor.of(COLOR_GOLD)).bold(false);

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, cb.create());
    }

    private String colorForType(int type) {
        return switch (type) {
            case 1  -> COLOR_ORANGE;
            case 2  -> COLOR_GREEN;
            default -> COLOR_RED;
        };
    }
}