package SistemaTumbas;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// Barra blanca arriba de la pantalla que lleva al jugador a su tumba: "Última Muerte: 500mt ↗". Al morir se marca sola la
// tumba nueva; cada marca dura 8 minutos y después queda desmarcada, y desde /muertes se vuelve a marcar cualquiera
public class MarcadorTumba implements Listener {
    static final long DURACION = 8 * 60_000L;
    private static final String[] FLECHAS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private final JavaPlugin plugin;
    private final GravesManager manager;
    private final Map<UUID, Marca> marcas = new HashMap<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();

    private record Marca(UUID tumba, long hasta) {}

    public MarcadorTumba(JavaPlugin plugin, GravesManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::actualizar, 4L, 4L);
    }

    JavaPlugin plugin() { return plugin; }

    // Solo una tumba marcada por jugador: marcar otra reemplaza la anterior
    public void marcar(Player player, Grave grave) {
        marcas.put(player.getUniqueId(), new Marca(grave.getId(), System.currentTimeMillis() + DURACION));
        actualizar();
    }

    public void desmarcar(Player player) {
        marcas.remove(player.getUniqueId());
        quitarBarra(player.getUniqueId());
    }

    public boolean marcada(Player player, Grave grave) {
        Marca marca = marcas.get(player.getUniqueId());
        return marca != null && marca.tumba().equals(grave.getId());
    }

    // "Última Muerte" si es su tumba más nueva; si no, la hora que tiene escrita la lápida
    public String nombre(Grave grave) {
        for (Grave other : manager.getGraves()) {
            if (other.getOwner().equals(grave.getOwner()) && other.getCreationTime() > grave.getCreationTime()) {
                return "Muerte de las " + new SimpleDateFormat("HH:mm").format(new Date(grave.getCreationTime()));
            }
        }
        return "Última Muerte";
    }

    // Cada 4 ticks gira la flecha según hacia dónde mira; se quita al pasar los 8 minutos o si la tumba ya no existe
    private void actualizar() {
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Marca>> it = marcas.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Marca> entry = it.next();
            Grave grave = manager.getGraveById(entry.getValue().tumba());
            Player player = Bukkit.getPlayer(entry.getKey());
            boolean vencida = ahora >= entry.getValue().hasta();
            if (grave == null || vencida) {
                it.remove();
                quitarBarra(entry.getKey());
                if (vencida && grave != null && player != null) {
                    player.sendMessage(TumbaMessages.info("Se desmarcó tu tumba. La puedes volver a marcar con " + TumbaMessages.accent("/muertes")));
                }
                continue;
            }
            if (player == null) continue;
            BossBar barra = barras.computeIfAbsent(entry.getKey(), id -> {
                BossBar nueva = Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SOLID);
                nueva.addPlayer(player);
                return nueva;
            });
            barra.setTitle(titulo(nombre(grave), player.getLocation(), grave.getLocation()));
            barra.setProgress(Math.max(0, Math.min(1, (entry.getValue().hasta() - ahora) / (double) DURACION)));
        }
    }

    private void quitarBarra(UUID player) {
        BossBar barra = barras.remove(player);
        if (barra != null) barra.removeAll();
    }

    // Al volver a entrar se le crea la barra otra vez si la marca sigue
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        quitarBarra(event.getPlayer().getUniqueId());
    }

    static String titulo(String nombre, Location jugador, Location tumba) {
        if (tumba.getWorld() == null || !tumba.getWorld().equals(jugador.getWorld())) {
            return ChatColor.WHITE + nombre + ": en el " + GravesPublicCommand.worldName(tumba.getWorld());
        }
        double dx = tumba.getX() - jugador.getX();
        double dz = tumba.getZ() - jugador.getZ();
        long metros = Math.round(jugador.distance(tumba));
        if (metros <= 2) return ChatColor.WHITE + nombre + ": aquí";
        return ChatColor.WHITE + nombre + ": " + metros + "mt " + ChatColor.BOLD + flecha(jugador.getYaw(), dx, dz);
    }

    // Flecha hacia la tumba según hacia dónde mira el jugador: ↑ adelante, ↓ atrás, → a la derecha. En Minecraft el yaw
    // 0 mira al sur (+Z) y crece al girar a la derecha
    static String flecha(float yaw, double dx, double dz) {
        double objetivo = Math.toDegrees(Math.atan2(-dx, dz));
        double relativo = ((objetivo - yaw) % 360 + 540) % 360 - 180;
        int paso = (int) Math.round(relativo / 45);
        return FLECHAS[(paso + 8) % 8];
    }
}
