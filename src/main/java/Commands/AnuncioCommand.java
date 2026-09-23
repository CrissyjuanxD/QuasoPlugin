package Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnuncioCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ismanu.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar esto.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso correcto: /anuncio <mensaje>");
            return true;
        }

        String mensaje = String.join(" ", args);

        mensaje = mensaje.replace("\"", "\\\"");

        String jsonMessage = "[\"\"," +
                "{\"text\":\"\\n\"}," +
                "{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"#E95B1E\"}," +
                "{\"text\":\" Anuncio\",\"bold\":true,\"color\":\"#DB7C26\"}," +
                "{\"text\":\" \\u25ba\",\"bold\":true,\"color\":\"gray\"}," +
                "{\"text\":\"\\n\\n\"}," +
                "{\"text\":\"" + mensaje + "\",\"color\":\"#9AE47C\"}," +
                "{\"text\":\"\\n \"}" +
                "]";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + jsonMessage);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.3f);
        }

        return true;
    }
}