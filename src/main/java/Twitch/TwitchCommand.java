package Twitch;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// /twitch: ver tu estado, vincular la cuenta, reclamar el kit del mes o desvincular
final class TwitchCommand implements CommandExecutor, TabCompleter {

    private static final List<String> OPTIONS = List.of("vincular", "kit", "desvincular");

    private final TwitchManager manager;

    TwitchCommand(TwitchManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden usar /twitch. Para administrar usa /twitchadmin.");
            return true;
        }
        String option = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (option) {
            case "vincular" -> manager.startLink(player);
            case "kit", "reclamar" -> manager.claim(player);
            case "desvincular" -> unlink(player);
            default -> {
                manager.sendStatus(player);
                player.sendMessage(TwitchText.GRAY + " /twitch vincular · /twitch kit · /twitch desvincular");
            }
        }
        return true;
    }

    // La cuenta queda registrada con los kits que ya cobró: volver a vincularla no da otro kit en el mismo mes
    private void unlink(Player player) {
        TwitchAccount account = manager.store().linkedTo(player.getUniqueId());
        if (account == null) {
            player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "No tienes una cuenta de Twitch vinculada.");
            return;
        }
        manager.unlink(account);
        player.sendMessage(TwitchText.PREFIX + TwitchText.TEXT + "Desvinculaste la cuenta " + TwitchText.HIGHLIGHT + account.login + TwitchText.TEXT + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> result = new ArrayList<>();
        for (String option : OPTIONS) {
            if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) result.add(option);
        }
        return result;
    }
}
