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

import java.util.List;

public class AutoAnnouncer implements CommandExecutor {

    // Lo importante desde el día 1, en el orden en que se le va a ir necesitando a un jugador nuevo
    public static final List<String> MENSAJES = List.of(
            "Usa /menu para abrir el menú principal: desde ahí entras a tus Misiones, Trabajos, Habilidades, Protecciones y Homes.",
            "Usa /menu, /misiones o el Libro de Misiones para ver la misión del día. Cada día se abre una nueva y algunas traen una misión extra.",
            "Al completar una misión recibes una Ficha de Misión: llévala a la Estatua de Recompensas del spawn para abrir tu cofre con DinoCoins y objetos. Las misiones extra pagan directo a tu monedero.",
            "Las DinoCoins se guardan en el Monedero, que se compra en el Mercado por 5 DinoCoins. Para comprar en la tienda o subir habilidades llévalas en el inventario.",
            "Usa el Libro de Habilidades para desbloquear tu árbol en /menu: ahí subes Vitalidad, Resistencia y Agilidad con DinoCoins, experiencia y bloques. La Biblioteca vende el libro por 1 DinoCoin.",
            "Si mueres, tus cosas quedan en una tumba a tus pies. Los primeros 20 minutos solo tú puedes abrirla y después cualquiera durante 10 minutos. Usa /muertes para ver dónde quedaron.",
            "Usa /menu o /trabajos para elegir uno de los 6 trabajos y ganar DinoCoins al subir de nivel. Entrar cuesta 5 DinoCoins, 10 niveles y 5 diamantes, y te puedes cambiar cada 24 horas sin perder tu nivel.",
            "En las zonas de pesca, cuando algo pica vuelve a usar la caña justo cuando el marcador esté en el verde. Los premios especiales los compra la Pescadería.",
            "Durante la BloodMoon los monstruos sueltan Fragmentos de BloodMoon. En la tienda de Cambios, 6 fragmentos valen 1 DinoCoin.",
            "En el casino se juega con DinoFichas. En la tienda de Cambios 1 DinoCoin son 5 DinoFichas, y 6 DinoFichas vuelven a ser 1 DinoCoin.",
            "Las raids están cambiadas: los raiders son Bombitas, desde la segunda oleada salen Iceologers y a veces llega una horda de corruptos.",
            "/sethome <nombre> guarda una base (hasta 10), /home <nombre> te lleva y /delhome <nombre> la borra; en /menu, Homes te las muestra todas. /spawn y /tiendas te llevan al spawn y a las tiendas.",
            "Usa /proteccion o Protecciones en /menu para proteger tu base.",
            "La tienda del spawn va creciendo durante la temporada: salen items nuevos y lo que ya estaba sube un poco de precio. Lo más fuerte nunca se vende, sale de misiones, bosses y biomas.",
            "Con 30 misiones completas pasas a DinoNugget+ y la misión 100 te da el rol DinoLeyenda.",
            "Usa /twitch para vincular tu cuenta de Twitch: los subs y VIPs del canal reclaman su kit cada mes, y los subs pueden usar /fly en el Overworld y el Nether."
    );

    private final JavaPlugin plugin;
    private final List<String> mensajes;

    private int indiceActual = 0;
    private BukkitTask mainTask;
    private BukkitTask manualBurstTask; // Tarea para la ráfaga de 30s
    private boolean isBursting = false; // Bloquea si ya se está haciendo una ráfaga

    public AutoAnnouncer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mensajes = MENSAJES;

        // Inicia el loop automático normal
        startNormalLoop();
    }

    // --- BUCLE NORMAL (Cada 10 min) ---
    private void startNormalLoop() {
        if (mainTask != null) mainTask.cancel();

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                enviarMensajeActual();
            }
        }.runTaskTimer(plugin, 12000L, 12000L); // 10 minutos
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

        sender.sendMessage(ChatColor.GREEN + "Iniciando ráfaga de anuncios (cada 30 seg). El timer de 10 minutos se pausó.");

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
                    sender.sendMessage(ChatColor.GREEN + "Ráfaga de anuncios completada. Retomando timer normal de 10 mins.");
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
