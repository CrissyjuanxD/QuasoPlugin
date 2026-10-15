package BaseDatos;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

// /basedatos (o /bd): abre el diálogo de la base de datos. Solo OPs
public class BaseDatosCommand implements CommandExecutor, TabCompleter {

    private final BaseDatosDialogos dialogos;

    public BaseDatosCommand(BaseDatosDialogos dialogos) {
        this.dialogos = dialogos;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(BaseDatosDialogos.PERMISO)) {
            sender.sendMessage(ChatColor.RED + "Solo los OPs pueden ver la base de datos.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "La base de datos se ve en un diálogo: usa /basedatos dentro del juego.");
            return true;
        }
        dialogos.abrir(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
