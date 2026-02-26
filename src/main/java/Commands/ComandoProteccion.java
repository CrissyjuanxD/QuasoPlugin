package Commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.ChatColor;

import java.util.Arrays;

public class ComandoProteccion implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player jugador = (Player) sender;
        ItemStack libro = crearLibroGuia();

        // Intentar agregar al inventario
        if (jugador.getInventory().firstEmpty() == -1) {
            // Inventario lleno, dropear el libro
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), libro);
            jugador.sendMessage(ChatColor.GOLD + "Se ha dropeado la guía de protecciones ya que tu inventario está lleno.");
        } else {
            jugador.getInventory().addItem(libro);
            jugador.sendMessage(ChatColor.GOLD + "Has recibido la guía de protecciones.");
        }

        return true;
    }

    private ItemStack crearLibroGuia() {
        ItemStack libro = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) libro.getItemMeta();

        meta.setTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Guía de Protecciones");
        meta.setAuthor("Servidor");
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Guía de Protecciones");

        // Contenido del libro
        String[] paginas = {
                ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/addnamepr <nombre>\n" +
                        ChatColor.BLACK + "Descripción: Nombra tu protección. Este comando es indispensable para usar el resto\n" +
                        ChatColor.DARK_GRAY + "------------\n\n" +
                        ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/addmember <jugador>\n" +
                        ChatColor.BLACK + "Descripción: Añade un jugador a tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------",

                ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/delmember <jugador>\n" +
                        ChatColor.BLACK + "Descripción: Elimina un jugador de tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------\n\n" +
                        ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/addowner <jugador>\n" +
                        ChatColor.BLACK + "Descripción: Añade un propietario a tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------",

                ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/delowner <jugador>\n" +
                        ChatColor.BLACK + "Descripción: Elimina un propietario de tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------\n\n" +
                        ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/ownerlist\n" +
                        ChatColor.BLACK + "Descripción: Lista todos los propietarios de tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------",

                ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/memberlist\n" +
                        ChatColor.BLACK + "Descripción: Lista todos los miembros de tu protección.\n" +
                        ChatColor.DARK_GRAY + "------------\n\n" +
                        ChatColor.BLACK + "▸ " + ChatColor.BOLD + "/prlist\n" +
                        ChatColor.BLACK + "Descripción: Lista todas las protecciones.\n" +
                        ChatColor.DARK_GRAY + "------------"
        };

        meta.setPages(Arrays.asList(paginas));
        libro.setItemMeta(meta);

        return libro;
    }
}
