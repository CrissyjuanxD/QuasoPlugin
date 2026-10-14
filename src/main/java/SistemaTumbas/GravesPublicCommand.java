package SistemaTumbas;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GravesPublicCommand implements CommandExecutor, TabCompleter {
    private static final int MAX_DIALOGO = 10;
    private static final TextColor LILA = TextColor.color(0xCDB4DB);
    private static final TextColor CREMA = TextColor.color(0xFDE2E4);
    private static final TextColor AZUL = TextColor.color(0xB8C0FF);
    private static final TextColor VERDE = TextColor.color(0xA8E6CF);
    private static final TextColor NARANJA = TextColor.color(0xFFD6A5);
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final GravesManager manager;
    private final MarcadorTumba marcador;

    public GravesPublicCommand(GravesManager manager, MarcadorTumba marcador) {
        this.manager = manager;
        this.marcador = marcador;
    }

    // /muertes abre un diálogo con tus tumbas para marcarlas o desmarcarlas (en el chat si el cliente no tiene diálogos).
    // "/muertes marcar #id" marca o desmarca, y "/muertes #id" te tepea si la config lo permite o te dice dónde está
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        List<Grave> myGraves = new ArrayList<>();
        for (Grave g : manager.getGraves()) {
            if (g.getOwner().equals(player.getUniqueId())) myGraves.add(g);
        }
        myGraves.sort(Comparator.comparingLong(Grave::getCreationTime).reversed());

        if (myGraves.isEmpty()) {
            player.sendMessage(TumbaMessages.info("No tienes ninguna tumba activa."));
            return true;
        }

        if (args.length == 0) {
            if (Commands.Homes.soportaDialogos(marcador.plugin(), player)) {
                player.showDialog(dialogo(player, myGraves));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.5f);
            } else {
                listaEnChat(player, myGraves, label);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("marcar")) {
            Grave grave = args.length > 1 ? buscar(myGraves, args[1]) : myGraves.get(0);
            if (grave == null) {
                player.sendMessage(TumbaMessages.error("Tumba no encontrada o ya expiró."));
            } else if (marcador.marcada(player, grave)) {
                marcador.desmarcar(player);
                player.sendMessage(TumbaMessages.info("Desmarcaste " + marcador.nombre(grave) + "."));
            } else {
                marcador.marcar(player, grave);
                player.sendMessage(TumbaMessages.success("Marcaste " + marcador.nombre(grave) + ". La barra de arriba te lleva a la tumba por 8 minutos."));
            }
            return true;
        }

        Grave g = buscar(myGraves, args[0]);
        if (g == null) {
            player.sendMessage(TumbaMessages.error("Tumba no encontrada o ya expiró."));
            return true;
        }
        Location loc = g.getLocation();
        if (manager.teleportsToGrave() && loc.getWorld() != null) {
            player.teleport(loc);
            player.sendMessage(TumbaMessages.success("Teletransportado a tu tumba."));
        } else {
            player.sendMessage(TumbaMessages.info("Tu tumba está en " + worldName(loc.getWorld()) + " X:" + loc.getBlockX() + " Y:"
                    + loc.getBlockY() + " Z:" + loc.getBlockZ() + ". Hay que ir caminando."));
        }
        return true;
    }

    // Acepta "#ab12", "#ab12,X:..,Y:..,Z:.." (el tab) o el id completo que manda el diálogo
    private static Grave buscar(List<Grave> graves, String texto) {
        String shortId = texto.split(",")[0].replace("#", "");
        if (shortId.isEmpty()) return null;
        for (Grave g : graves) {
            if (g.getId().toString().startsWith(shortId)) return g;
        }
        return null;
    }

    // Un botón por tumba que dice Marcar o Desmarcar; con el teleport activado, al lado va el botón Ir
    private Dialog dialogo(Player player, List<Grave> graves) {
        boolean tepea = manager.teleportsToGrave();
        List<ActionButton> buttons = new ArrayList<>();
        for (Grave g : graves.subList(0, Math.min(MAX_DIALOGO, graves.size()))) {
            boolean marcada = marcador.marcada(player, g);
            Location loc = g.getLocation();
            Component label = Component.text(marcada ? "Desmarcar" : "Marcar", marcada ? NARANJA : VERDE).decorate(TextDecoration.BOLD)
                    .append(Component.text(" · " + marcador.nombre(g), CREMA).decoration(TextDecoration.BOLD, false))
                    .append(Component.text(" · " + worldName(loc.getWorld()) + distancia(player, loc), AZUL).decoration(TextDecoration.BOLD, false));
            Component tooltip = Component.text("X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ(), AZUL)
                    .append(Component.newline())
                    .append(Component.text("Hace " + ModoTumba.reloj(System.currentTimeMillis() - g.getCreationTime()) + " · ", CREMA))
                    .append(LEGACY.deserialize(state(g, System.currentTimeMillis())))
                    .append(Component.newline())
                    .append(Component.text(marcada ? "Clic para quitar la barra." : "Clic para que una barra arriba te lleve a la tumba por 8 minutos.", LILA));
            buttons.add(ActionButton.create(label, tooltip, tepea ? 250 : 310,
                    DialogAction.staticAction(net.kyori.adventure.text.event.ClickEvent.runCommand("/muertes marcar " + g.getId()))));
            if (tepea) {
                buttons.add(ActionButton.create(Component.text("Ir", VERDE).decorate(TextDecoration.BOLD), Component.text("Teletransportarte a esta tumba.", CREMA), 50,
                        DialogAction.staticAction(net.kyori.adventure.text.event.ClickEvent.runCommand("/muertes " + g.getId()))));
            }
        }
        Component title = Component.text("☠ ", LILA).decorate(TextDecoration.BOLD)
                .append(Component.text("Tus muertes", CREMA).decorate(TextDecoration.BOLD));
        Component intro = Component.text(graves.size() == 1 ? "Tienes 1 tumba." : "Tienes " + graves.size() + " tumbas.", VERDE)
                .append(Component.newline())
                .append(Component.text("Marca una y una barra blanca arriba de la pantalla te dirá a cuántos metros está y hacia dónde ir. Dura 8 minutos.", CREMA));
        DialogBase base = DialogBase.builder(title).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(List.of(DialogBody.plainMessage(intro, 310))).build();
        ActionButton close = ActionButton.create(Component.text("Cerrar", NARANJA), null, 150, null);
        DialogType type = DialogType.multiAction(buttons, close, tepea ? 2 : 1);
        return Dialog.create(builder -> builder.empty().base(base).type(type));
    }

    private static String distancia(Player player, Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) return "";
        return " · " + Math.round(player.getLocation().distance(loc)) + "mt";
    }

    // La lista de antes, para clientes sin diálogos, con [Marcar] o [Desmarcar] al final de cada línea
    private void listaEnChat(Player player, List<Grave> myGraves, String label) {
        player.sendMessage(TumbaMessages.info("Tus tumbas (" + myGraves.size() + "):"));
        long now = System.currentTimeMillis();
        for (Grave g : myGraves) {
            Location loc = g.getLocation();
            String shortId = "#" + g.getId().toString().substring(0, 4);
            TextComponent line = new TextComponent(ChatColor.of("#CDB4DB") + " • " + ChatColor.of("#FDE2E4") + worldName(loc.getWorld())
                    + ChatColor.of("#B8C0FF") + " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ()
                    + ChatColor.GRAY + " · hace " + ChatColor.WHITE + ModoTumba.reloj(now - g.getCreationTime())
                    + ChatColor.GRAY + " · " + state(g, now));
            String hover = manager.teleportsToGrave() ? "Clic para ir a la tumba " + shortId : "Ve caminando a X:" + loc.getBlockX()
                    + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " " + shortId));
            boolean marcada = marcador.marcada(player, g);
            TextComponent marcar = new TextComponent(ChatColor.of(marcada ? "#FFD6A5" : "#A8E6CF") + (marcada ? " [Desmarcar]" : " [Marcar]"));
            marcar.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(marcada ? "Quitar la barra" : "Una barra arriba te lleva a la tumba por 8 minutos")));
            marcar.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/muertes marcar " + shortId));
            player.spigot().sendMessage(line, marcar);
        }
    }

    // Privada (y cuánto falta para que se abra), abierta, y cuánto le queda antes de soltar las cosas
    private String state(Grave grave, long now) {
        long untilOpen = manager.millisUntilOpen(grave);
        String left = ModoTumba.reloj(grave.getExpiryTime() - now);
        if (untilOpen > 0) return ChatColor.of("#B8C0FF") + "privada " + ModoTumba.reloj(untilOpen);
        if (untilOpen < 0) return ChatColor.of("#B8C0FF") + "privada" + ChatColor.GRAY + " · quedan " + left;
        return ChatColor.of("#E3B778") + "abierta" + ChatColor.GRAY + " · quedan " + left;
    }

    static String worldName(World world) {
        if (world == null) return "?";
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> world.getName().equalsIgnoreCase("wardencave") ? "Warden Cave" : world.getName().equals("world") ? "Overworld" : world.getName();
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if (!(sender instanceof Player player) || args.length == 0 || args.length > 2) return result;
        if (args.length == 2 && !args[0].equalsIgnoreCase("marcar")) return result;
        if (args.length == 1 && "marcar".startsWith(args[0].toLowerCase())) result.add("marcar");
        for (Grave g : manager.getGraves()) {
            if (!g.getOwner().equals(player.getUniqueId())) continue;
            Location loc = g.getLocation();
            String option = "#" + g.getId().toString().substring(0, 4) + ",X:" + loc.getBlockX() + ",Y:" + loc.getBlockY() + ",Z:" + loc.getBlockZ();
            if (option.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) result.add(option);
        }
        return result;
    }
}
