package Handlers;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.List;

public class AutoAnnouncer implements CommandExecutor {

    private final JavaPlugin plugin;
    private final List<String> mensajes;

    private int indiceActual = 0;
    private BukkitTask mainTask;
    private BukkitTask manualBurstTask; // Tarea para la ráfaga de 30s
    private boolean isBursting = false; // Bloquea si ya se está haciendo una ráfaga

    public AutoAnnouncer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mensajes = Arrays.asList(
                "Para ganar ManuCoins, puedes derrotar bosses, completar misiones o probar suerte en el casino.",
                "Usa el comando /misiones para ver las misiones activas. ¡Al completarlas obtendrás ManuCoins!",
                "Usa los comandos /spawn o /tiendas para teletransportarte a la zona de spawn o tiendas.",
                "Puedes usar /sethome <nombre> para establecer una base. Luego, usa /home <nombre> para teletransportarte a ella y /delhome <nombre> para eliminarla. El límite es de 10 bases.",
                "Durante una BloodMoon, al eliminar mobs podrás obtener Fragmento de BloodMoon, las cuales podrás intercambiar por ManuCoins en la tienda.",
                "Si mueres de forma normal, no perderás tu inventario. Sin embargo, si mueres en PvP, sí lo perderás y además soltarás 15 ManuCoins. (Un jugador solo puede soltar ManuCoins una vez cada 30 minutos).",
                "Recuerda que puedes cambiar tu skin usando el comando /skin set <nick>."
        );

        // Inicia el loop automático normal
        startNormalLoop();
    }

    // --- BUCLE NORMAL (Cada 8 min) ---
    private void startNormalLoop() {
        if (mainTask != null) mainTask.cancel();

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                enviarMensajeActual();
            }
        }.runTaskTimer(plugin, 9600L, 9600L); // 8 minutos
    }

    // --- COMANDO /autoanuncio ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("viciont_hardcore3.command.autoanuncio")) {
            sender.sendMessage(ChatColor.RED + "No tienes permisos.");
            return true;
        }

        if (isBursting) {
            sender.sendMessage(ChatColor.RED + "Ya hay una ráfaga de anuncios en progreso.");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Iniciando ráfaga de anuncios (cada 30 seg). El timer de 8 minutos se pausó.");

        // Pausar timer normal
        if (mainTask != null) mainTask.cancel();
        isBursting = true;
        indiceActual = 0; // Reiniciar índice para que los mande desde el primero

        // Iniciar ráfaga (Cada 30s = 600 ticks)
        manualBurstTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (indiceActual >= mensajes.size()) {
                    // Terminaron de enviarse todos
                    sender.sendMessage(ChatColor.GREEN + "Ráfaga de anuncios completada. Retomando timer normal de 8 mins.");
                    isBursting = false;
                    indiceActual = 0;
                    startNormalLoop(); // Retomar ciclo normal
                    cancel();
                    return;
                }

                enviarMensajeActual();
            }
        }.runTaskTimer(plugin, 0L, 600L); // 0 de delay inicial, 30s de periodo

        return true;
    }

    public void shutdown() {
        if (mainTask != null) mainTask.cancel();
        if (manualBurstTask != null) manualBurstTask.cancel();
        isBursting = false;
    }

    // --- LÓGICA DE ENVÍO Y FORMATO ---
    private void enviarMensajeActual() {
        String mensajeTexto = mensajes.get(indiceActual);

        String header = "\n" +
                ChatColor.of("#00e6e6") + ChatColor.BOLD + "۞ " +
                ChatColor.of("#00bfff") + ChatColor.BOLD + "Tip " +
                ChatColor.GRAY + ChatColor.BOLD + "►\n\n";

        String body = ChatColor.of("#80dfff") + mensajeTexto + "\n ";

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(header + body);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.5f);
        }

        indiceActual++;
        if (!isBursting && indiceActual >= mensajes.size()) {
            indiceActual = 0; // Reset normal si no está en ráfaga
        }
    }
}
