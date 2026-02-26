package TitleListener;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SuccessNotification {

    private final JavaPlugin plugin;

    public SuccessNotification(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void showSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);

        String titleMessage = ChatColor.GREEN + "" + ChatColor.BOLD + "✔";

        player.sendTitle(titleMessage, "", 5, 20, 10);
    }
}