package Commands;

import Events.ItemParty.ItemPartyHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemPartyCastigoCommand implements CommandExecutor, TabCompleter {

    private final ItemPartyHandler handler;

    public ItemPartyCastigoCommand(ItemPartyHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. Permisos (Opcional, pero recomendado)
        if (!sender.hasPermission("itemparty.admin")) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }

        // 2. Validación de argumentos
        if (args.length < 2 || !args[0].equalsIgnoreCase("remove")) {
            sender.sendMessage("§cUso correcto: /itempartycastigo remove <jugador>");
            return true;
        }

        // 3. Ejecución
        String targetName = args[1];
        boolean exito = handler.quitarCastigoManualmente(targetName);

        if (exito) {
            sender.sendMessage("§a[ItemParty] §fSe ha retirado el castigo al jugador §e" + targetName + "§f.");
            // Mensaje opcional al admin indicando que se guardó en config
        } else {
            sender.sendMessage("§c[ItemParty] §fEl jugador §e" + targetName + " §fno tiene un castigo activo o no existe en los registros.");
        }

        return true;
    }

    // Autocompletado (Tab)
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Sugerir "remove"
            return Collections.singletonList("remove");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Sugerir nombres de jugadores online
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}