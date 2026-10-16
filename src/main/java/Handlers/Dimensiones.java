package Handlers;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// /dimension cerrar|abrir|estado <nether|end|wardencave>: una dimensión cerrada no deja pasar por su portal ni
// llegar con /home o /tpa. El estado se guarda en dimensiones.yml para que siga cerrada tras reiniciar
public class Dimensiones implements CommandExecutor, TabCompleter, Listener {

    public enum Dimension {
        NETHER("nether", "El Nether está cerrado temporalmente"),
        END("end", "El End está cerrado temporalmente"),
        WARDENCAVE("wardencave", "La Warden Cave está cerrada temporalmente");

        final String id;
        final String aviso;

        Dimension(String id, String aviso) {
            this.id = id;
            this.aviso = aviso;
        }

        static Dimension of(String id) {
            for (Dimension dimension : values()) if (dimension.id.equalsIgnoreCase(id)) return dimension;
            return null;
        }
    }

    private static final String PREFIJO = ChatColor.of("#E28B20") + "" + ChatColor.BOLD + "⚙ " + ChatColor.of("#F4D990");
    private static final Set<Dimension> CERRADAS = EnumSet.noneOf(Dimension.class);
    private static final Map<UUID, Long> ULTIMO_AVISO = new HashMap<>();

    private final File archivo;

    public Dimensiones(JavaPlugin plugin) {
        archivo = new File(plugin.getDataFolder(), "dimensiones.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(archivo);
        CERRADAS.clear();
        for (String id : config.getStringList("cerradas")) {
            Dimension dimension = Dimension.of(id);
            if (dimension != null) CERRADAS.add(dimension);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // A qué dimensión lleva un mundo (null si es el mundo normal)
    public static Dimension de(World world) {
        if (world == null) return null;
        if (QuasoPlugin.WORLD_NAME.equals(world.getName())) return Dimension.WARDENCAVE;
        if (world.getEnvironment() == null) return null;
        return switch (world.getEnvironment()) {
            case NETHER -> Dimension.NETHER;
            case THE_END -> Dimension.END;
            default -> null;
        };
    }

    public static boolean cerrada(Dimension dimension) {
        return dimension != null && CERRADAS.contains(dimension);
    }

    // Para /home y /tpa: si el destino está cerrado le avisa con el action bar y devuelve true. Los OPs pasan igual
    public static boolean bloquea(Player player, World destino) {
        return !CERRADAS.isEmpty() && bloquea(player, de(destino));
    }

    private static boolean bloquea(Player player, Dimension dimension) {
        if (CERRADAS.isEmpty()) return false;
        if (!cerrada(dimension) || player.isOp() || dimension == de(player.getWorld())) return false;
        long ahora = System.currentTimeMillis();
        Long antes = ULTIMO_AVISO.get(player.getUniqueId());
        if (antes == null || ahora - antes > 1500) {
            ULTIMO_AVISO.put(player.getUniqueId(), ahora);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ChatColor.of("#DD6110") + "" + ChatColor.BOLD + dimension.aviso));
        }
        return true;
    }

    // Si Paper todavía no sabe el destino del portal, se deduce por el tipo de portal
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        Dimension destino;
        if (event.getTo() != null && event.getTo().getWorld() != null) destino = de(event.getTo().getWorld());
        else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) destino = Dimension.END;
        else if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) destino = Dimension.NETHER;
        else return;
        if (bloquea(event.getPlayer(), destino)) event.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado")) {
            sender.sendMessage(PREFIJO + "Estado de las dimensiones:");
            for (Dimension dimension : Dimension.values()) {
                sender.sendMessage(ChatColor.of("#AAA391") + "  · " + ChatColor.of("#F4D990") + dimension.id + ": "
                        + (cerrada(dimension) ? ChatColor.of("#DD6110") + "cerrada" : ChatColor.of("#C9DC8A") + "abierta"));
            }
            return true;
        }
        String accion = args[0].toLowerCase(Locale.ROOT);
        if (!accion.equals("cerrar") && !accion.equals("abrir") || args.length != 2) {
            sender.sendMessage(PREFIJO + "Uso: /dimension <cerrar|abrir|estado> <nether|end|wardencave>");
            return true;
        }
        Dimension dimension = Dimension.of(args[1]);
        if (dimension == null) {
            sender.sendMessage(PREFIJO + "Dimensión desconocida. Usa nether, end o wardencave.");
            return true;
        }
        boolean cerrar = accion.equals("cerrar");
        if (cerrar == cerrada(dimension)) {
            sender.sendMessage(PREFIJO + "La dimensión " + ChatColor.of("#C9DC8A") + dimension.id
                    + ChatColor.of("#F4D990") + " ya estaba " + (cerrar ? "cerrada." : "abierta."));
            return true;
        }
        if (cerrar) CERRADAS.add(dimension);
        else CERRADAS.remove(dimension);
        guardar();
        sender.sendMessage(PREFIJO + "Dimensión " + ChatColor.of("#C9DC8A") + dimension.id + ChatColor.of("#F4D990")
                + (cerrar ? " cerrada. Nadie puede entrar por portal, /home ni /tpa (los OPs sí)."
                : " abierta de nuevo."));
        return true;
    }

    private void guardar() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        for (Dimension dimension : CERRADAS) ids.add(dimension.id);
        config.set("cerradas", ids);
        try {
            config.save(archivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> opciones = new ArrayList<>();
        if (args.length == 1) StringUtil.copyPartialMatches(args[0], List.of("cerrar", "abrir", "estado"), opciones);
        else if (args.length == 2 && !args[0].equalsIgnoreCase("estado")) {
            List<String> ids = new ArrayList<>();
            for (Dimension dimension : Dimension.values()) ids.add(dimension.id);
            StringUtil.copyPartialMatches(args[1], ids, opciones);
        }
        return opciones;
    }
}
