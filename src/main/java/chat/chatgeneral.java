package chat;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class chatgeneral implements Listener {

    // Unicode del rango en blanco y el nombre en su color; los teams de eventos usan su etiqueta de texto
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Team team = player.getScoreboard().getEntryTeam(player.getName());

        String finalPrefix = "";
        String suffix = "";
        ChatColor nameColor = ChatColor.WHITE;

        if (team != null) {
            finalPrefix = team.getPrefix();
            suffix = team.getSuffix();

            Handlers.Teams.TeamType type = Handlers.Teams.TeamType.getById(team.getName());
            if (type != null) {
                finalPrefix = type.getChatPrefix();
                nameColor = type.getBungeeColor();
            }
        } else {
            finalPrefix = ChatColor.GRAY + "";
        }

        event.setFormat(finalPrefix + nameColor + "%1$s" + ChatColor.RESET + suffix + ChatColor.WHITE + ": %2$s");
    }

    @EventHandler
    public void onInventoryCreative(InventoryCreativeEvent event) {
        Player player = (Player) event.getWhoClicked();
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        ItemStack item = event.getCurrentItem();
        if ((player.isOp() || (team != null && team.getName().equals("mod"))) && item != null && item.getType() != Material.AIR && event.getSlotType() == InventoryType.SlotType.QUICKBAR) {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM, HH:mm:ss");
            Date date = new Date();
            Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_PURPLE + (player.getName() + " HA SACADO " + item.getAmount() + " " + item.getType() + " DEL MODO CREATIVO, FECHA: ").toUpperCase() + ChatColor.YELLOW + formatter.format(date).toUpperCase());
        }
    }

}
