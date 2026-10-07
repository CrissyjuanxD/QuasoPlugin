package Trabajos;

import Gui.dinocoins.DinoCoinsManager;
import Handlers.ActionBarHandler;
import Handlers.DatabaseManager;
import items.EconomyItemsFunctions;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// El sistema de trabajos: un trabajo a la vez, 100 niveles por trabajo, DinoCoins y experiencia al subir y un bonus
// cada 5 niveles. Los datos van a MySQL (player_jobs y player_job_state)
public final class TrabajosManager implements Listener {

    public static final long ESPERA_CAMBIO = 24L * 60 * 60 * 1000;
    public static final int COSTO_MONEDAS = 5;
    public static final int COSTO_NIVELES = 10;
    public static final int COSTO_DIAMANTES = 5;

    private static TrabajosManager instance;

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final DinoCoinsManager monedas;
    private final Map<UUID, DatosTrabajo> datos = new ConcurrentHashMap<>();
    private final Set<UUID> pagando = new HashSet<>();
    private final Map<UUID, BossBar> barras = new HashMap<>();
    private final Map<UUID, BukkitTask> ocultarBarras = new HashMap<>();
    // Los guardados van en orden por un solo hilo: uno viejo nunca pisa a uno nuevo
    private final ExecutorService guardados = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "QuasoPlugin-Trabajos");
        thread.setDaemon(true);
        return thread;
    });
    private final BukkitTask autoguardado;
    private final TrabajosXp xp;
    private final TrabajosGUI gui;
    // Sube con cada /trabajos reset: una carga que empezó antes del reset se descarta
    private volatile int generacion;

    public TrabajosManager(JavaPlugin plugin, DatabaseManager db, DinoCoinsManager monedas) {
        this.plugin = plugin;
        this.db = db;
        this.monedas = monedas;
        instance = this;

        gui = new TrabajosGUI(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        xp = new TrabajosXp(this);
        plugin.getServer().getPluginManager().registerEvents(xp, plugin);
        plugin.getServer().getPluginManager().registerEvents(gui, plugin);
        TrabajosCommand command = new TrabajosCommand(this, gui);
        Objects.requireNonNull(plugin.getCommand("trabajos")).setExecutor(command);
        Objects.requireNonNull(plugin.getCommand("trabajos")).setTabCompleter(command);

        autoguardado = Bukkit.getScheduler().runTaskTimer(plugin, this::guardarCambios, 20L * 120, 20L * 120);
        for (Player player : Bukkit.getOnlinePlayers()) cargar(player.getUniqueId(), 0);
    }

    // ---------------------------------------------------------------- Para otros sistemas

    // El menú de trabajos, para abrirlo desde /menu
    public void abrirMenu(Player player) {
        gui.abrir(player);
    }

    // La línea "Trabajo:" del scoreboard
    public static String lineaScoreboard(Player player) {
        TrabajosManager manager = instance;
        DatosTrabajo datos = manager == null ? null : manager.datos.get(player.getUniqueId());
        if (datos == null || datos.activo == null) return ChatColor.GRAY + "Ninguno";
        return TrabajosTexto.color(datos.activo) + datos.activo.nombre() + ChatColor.GRAY + " Nv " + datos.nivel(datos.activo);
    }

    // Nivel del jugador en un trabajo (-1 si todavía no se cargaron sus datos); lo usan las misiones de trabajo
    public static int nivel(Player player, Trabajo trabajo) {
        TrabajosManager manager = instance;
        DatosTrabajo datos = manager == null ? null : manager.datos.get(player.getUniqueId());
        return datos == null ? -1 : datos.nivel(trabajo);
    }

    DatosTrabajo datos(Player player) {
        return datos.get(player.getUniqueId());
    }

    JavaPlugin plugin() {
        return plugin;
    }

    // ---------------------------------------------------------------- Carga y guardado

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        cargar(event.getPlayer().getUniqueId(), 0);
    }

    // Si MySQL falla no se inventan datos vacíos (después pisarían los de verdad): se reintenta en un minuto
    private void cargar(UUID uuid, int intento) {
        int deEstaCarga = generacion;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatosTrabajo cargados = DatosTrabajo.desde(db.loadJobs(uuid));
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (Bukkit.getPlayer(uuid) == null) return;
                    if (deEstaCarga != generacion) cargar(uuid, intento);
                    else datos.put(uuid, cargados);
                });
            } catch (SQLException error) {
                plugin.getLogger().warning("No se pudieron cargar los trabajos de " + uuid + ": " + error.getMessage());
                if (intento < 5) Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Bukkit.getPlayer(uuid) != null) cargar(uuid, intento + 1);
                }, 20L * 60);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DatosTrabajo removidos = datos.remove(player.getUniqueId());
        pagando.remove(player.getUniqueId());
        quitarBarra(player.getUniqueId());
        if (removidos != null && removidos.sucio) guardar(player.getUniqueId(), player.getName(), removidos);
    }

    private void guardarCambios() {
        for (Map.Entry<UUID, DatosTrabajo> entry : datos.entrySet()) {
            if (!entry.getValue().sucio) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            guardar(entry.getKey(), player != null ? player.getName() : "?", entry.getValue());
        }
    }

    private void guardar(UUID uuid, String nombre, DatosTrabajo datosJugador) {
        DatabaseManager.JobsData copia = datosJugador.copia();
        datosJugador.sucio = false;
        guardados.execute(() -> {
            try {
                db.saveJobs(uuid, nombre, copia);
            } catch (SQLException error) {
                plugin.getLogger().severe("No se pudieron guardar los trabajos de " + nombre + ": " + error.getMessage());
                datosJugador.sucio = true;
            }
        });
    }

    public void shutdown() {
        autoguardado.cancel();
        for (UUID uuid : new HashSet<>(barras.keySet())) quitarBarra(uuid);
        xp.guardar();
        guardarCambios();
        guardados.shutdown();
        try {
            if (!guardados.awaitTermination(15, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Los trabajos tardaron demasiado en guardarse al apagar.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (instance == this) instance = null;
    }

    // ---------------------------------------------------------------- Experiencia y niveles

    // Solo suma si es el trabajo actual. Pasadas las 1.500 XP de la hora, lo que sigue rinde la cuarta parte
    void ganarXp(Player player, Trabajo trabajo, double cantidad) {
        DatosTrabajo d = datos.get(player.getUniqueId());
        if (d == null || d.activo != trabajo || cantidad <= 0) return;
        int nivel = d.nivel(trabajo);
        if (nivel >= Trabajo.NIVEL_MAXIMO) return;

        long ahora = System.currentTimeMillis();
        if (ahora - d.inicioHora >= 60 * 60 * 1000L) {
            d.inicioHora = ahora;
            d.xpEnLaHora = 0;
        }
        double ganado;
        if (d.xpEnLaHora >= TrabajoNiveles.TOPE_POR_HORA) {
            ganado = cantidad * TrabajoNiveles.RINDE_PASADO_EL_TOPE;
        } else if (d.xpEnLaHora + cantidad > TrabajoNiveles.TOPE_POR_HORA) {
            double dentro = TrabajoNiveles.TOPE_POR_HORA - d.xpEnLaHora;
            ganado = dentro + (cantidad - dentro) * TrabajoNiveles.RINDE_PASADO_EL_TOPE;
        } else {
            ganado = cantidad;
        }
        if (d.xpEnLaHora < TrabajoNiveles.TOPE_POR_HORA && d.xpEnLaHora + cantidad >= TrabajoNiveles.TOPE_POR_HORA) {
            player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.GRIS + "Llegaste a las " + TrabajosTexto.numero(TrabajoNiveles.TOPE_POR_HORA)
                    + " XP de esta hora: lo que ganes hasta que pase rinde la cuarta parte. ¡Descansa un rato!");
        }
        d.xpEnLaHora += cantidad;

        double xp = d.xp(trabajo) + ganado;
        while (nivel < Trabajo.NIVEL_MAXIMO && xp >= TrabajoNiveles.xpParaNivel(nivel + 1)) {
            xp -= TrabajoNiveles.xpParaNivel(nivel + 1);
            nivel++;
            d.niveles.put(trabajo, nivel);
            subirNivel(player, trabajo, nivel);
        }
        if (nivel >= Trabajo.NIVEL_MAXIMO) xp = 0;
        d.niveles.put(trabajo, nivel);
        d.xp.put(trabajo, xp);
        d.sucio = true;

        String progreso = nivel >= Trabajo.NIVEL_MAXIMO ? TrabajosTexto.DORADO + "¡nivel máximo!"
                : TrabajosTexto.BLANCO + TrabajosTexto.numero(xp) + TrabajosTexto.GRIS + "/" + TrabajosTexto.numero(TrabajoNiveles.xpParaNivel(nivel + 1));
        ActionBarHandler.get(plugin).sendProgress(player, "trabajo", TrabajosTexto.nombre(trabajo) + TrabajosTexto.CREMA + " +"
                + formatoXp(ganado) + " XP " + TrabajosTexto.GRIS + "· Nv " + nivel + " · " + progreso);
    }

    private static String formatoXp(double xp) {
        return xp >= 10 || xp == Math.floor(xp) ? String.valueOf((int) Math.round(xp)) : String.format(java.util.Locale.ROOT, "%.1f", xp).replace('.', ',');
    }

    private void subirNivel(Player player, Trabajo trabajo, int nivel) {
        int base = TrabajoNiveles.monedasPorNivel(nivel);
        int bonus = TrabajoNiveles.bonus(nivel);
        darMonedas(player, base + bonus);
        player.giveExp(TrabajoNiveles.experiencia(nivel));

        player.sendMessage(TrabajosTexto.PREFIJO + "¡Subiste a " + TrabajosTexto.nombre(trabajo) + " nivel " + TrabajosTexto.BLANCO + nivel
                + TrabajosTexto.CREMA + "! " + TrabajosTexto.DORADO + "+" + base + " DinoCoins" + TrabajosTexto.CREMA + " y "
                + TrabajoNiveles.experiencia(nivel) + " de experiencia.");
        if (bonus > 0) {
            // Cada 5 niveles: title y el bonus
            player.sendTitle(TrabajosTexto.color(trabajo) + "" + ChatColor.BOLD + trabajo.nombre() + " " + nivel,
                    TrabajosTexto.DORADO + "¡Bonus! " + TrabajosTexto.CREMA + "+" + (base + bonus) + " DinoCoins", 5, 50, 15);
            player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.DORADO + "¡Bonus de nivel " + nivel + "! " + TrabajosTexto.BLANCO + "+"
                    + bonus + " DinoCoins" + TrabajosTexto.CREMA + ".");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
        } else {
            mostrarBarra(player, trabajo, nivel, base);
        }
        if (nivel % 25 == 0) {
            Bukkit.broadcastMessage(TrabajosTexto.PREFIJO + TrabajosTexto.BLANCO + player.getName() + TrabajosTexto.CREMA + " llegó al nivel "
                    + TrabajosTexto.DORADO + nivel + TrabajosTexto.CREMA + " de " + TrabajosTexto.nombre(trabajo) + TrabajosTexto.CREMA + ".");
        }
        Bukkit.getPluginManager().callEvent(new TrabajoSubeNivelEvent(player, trabajo, nivel));
    }

    // Los niveles normales: una bossbar verde unos segundos con un sonidito (si sube otro nivel se reusa la misma)
    private void mostrarBarra(Player player, Trabajo trabajo, int nivel, int monedasGanadas) {
        UUID uuid = player.getUniqueId();
        String titulo = TrabajosTexto.nombre(trabajo) + TrabajosTexto.BLANCO + " · Nivel " + nivel + TrabajosTexto.GRIS + " · "
                + TrabajosTexto.DORADO + "+" + monedasGanadas + (monedasGanadas == 1 ? " DinoCoin" : " DinoCoins");
        BossBar barra = barras.computeIfAbsent(uuid, id -> Bukkit.createBossBar(titulo, BarColor.GREEN, BarStyle.SEGMENTED_10));
        barra.setTitle(titulo);
        barra.setProgress(Math.max(0, Math.min(1, nivel / (double) Trabajo.NIVEL_MAXIMO)));
        barra.addPlayer(player);
        barra.setVisible(true);

        BukkitTask anterior = ocultarBarras.remove(uuid);
        if (anterior != null) anterior.cancel();
        ocultarBarras.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> quitarBarra(uuid), 20L * 4));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
    }

    private void quitarBarra(UUID uuid) {
        BukkitTask tarea = ocultarBarras.remove(uuid);
        if (tarea != null) tarea.cancel();
        BossBar barra = barras.remove(uuid);
        if (barra != null) barra.removeAll();
    }

    // Las DinoCoins van a los monederos; lo que no entra (o si no tiene monedero) va al inventario o al suelo
    void darMonedas(Player player, int cantidad) {
        if (cantidad <= 0) return;
        if (monedas == null) DinoCoinsManager.giveLoose(player, cantidad);
        else monedas.deposit(player, cantidad);
    }

    // ---------------------------------------------------------------- Entrar a un trabajo

    // Lo que falta para cambiarse (0 = ya puede)
    long esperaRestante(DatosTrabajo d) {
        if (d.activo == null) return 0;
        return Math.max(0, d.desde + ESPERA_CAMBIO - System.currentTimeMillis());
    }

    // Cuesta 5 DinoCoins (del inventario o de los monederos), 10 niveles de experiencia y 5 diamantes. Después de
    // entrar hay que esperar 24 horas para cambiarse; el nivel de cada trabajo se guarda
    void unirse(Player player, Trabajo trabajo) {
        UUID uuid = player.getUniqueId();
        DatosTrabajo d = datos.get(uuid);
        if (d == null) {
            player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.ROSA + "Tus trabajos todavía se están cargando, prueba en unos segundos.");
            return;
        }
        if (d.activo == trabajo) {
            player.sendMessage(TrabajosTexto.PREFIJO + "Ya trabajas de " + TrabajosTexto.nombre(trabajo) + TrabajosTexto.CREMA + ".");
            return;
        }
        long espera = esperaRestante(d);
        if (espera > 0) {
            player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.ROSA + "Podrás cambiarte de trabajo en " + TrabajosTexto.tiempo(espera) + ".");
            return;
        }
        if (pagando.contains(uuid)) return;
        String falta = faltaParaEntrar(player);
        if (falta != null) {
            player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.ROSA + falta);
            return;
        }
        int enInventario = monedasEnInventario(player);
        if (enInventario >= COSTO_MONEDAS || monedas == null) {
            if (enInventario < COSTO_MONEDAS) {
                player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.ROSA + "Necesitas " + COSTO_MONEDAS + " DinoCoins.");
                return;
            }
            pagarYEntrar(player, d, trabajo, COSTO_MONEDAS);
            return;
        }
        // Lo que falta sale de los monederos; si no alcanza se devuelve lo que se sacó
        int faltan = COSTO_MONEDAS - enInventario;
        pagando.add(uuid);
        monedas.changeAsyncMoved(player, -faltan, movidas -> {
            pagando.remove(uuid);
            if (!player.isOnline()) {
                if (movidas > 0) plugin.getLogger().warning(player.getName() + " se fue mientras pagaba un trabajo: se le cobraron " + movidas + " DinoCoins de su monedero.");
                return;
            }
            String ahoraFalta = faltaParaEntrar(player);
            if (movidas < faltan || ahoraFalta != null || monedasEnInventario(player) < enInventario || datos.get(uuid) != d) {
                if (movidas > 0) darMonedas(player, movidas);
                player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.ROSA + (ahoraFalta != null ? ahoraFalta
                        : "Necesitas " + COSTO_MONEDAS + " DinoCoins en el inventario o en tus monederos."));
                return;
            }
            pagarYEntrar(player, d, trabajo, enInventario);
        });
    }

    private String faltaParaEntrar(Player player) {
        if (player.getLevel() < COSTO_NIVELES) return "Necesitas " + COSTO_NIVELES + " niveles de experiencia para entrar.";
        if (!player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), COSTO_DIAMANTES)) {
            return "Necesitas " + COSTO_DIAMANTES + " diamantes para entrar.";
        }
        return null;
    }

    private void pagarYEntrar(Player player, DatosTrabajo d, Trabajo trabajo, int monedasDelInventario) {
        quitarMonedasDelInventario(player, monedasDelInventario);
        player.getInventory().removeItem(new ItemStack(Material.DIAMOND, COSTO_DIAMANTES));
        player.giveExpLevels(-COSTO_NIVELES);

        Trabajo anterior = d.activo;
        d.activo = trabajo;
        d.desde = System.currentTimeMillis();
        d.sucio = true;
        guardar(player.getUniqueId(), player.getName(), d);

        player.sendMessage(TrabajosTexto.PREFIJO + "¡Ahora trabajas de " + TrabajosTexto.nombre(trabajo) + TrabajosTexto.CREMA
                + "! Vas por el nivel " + TrabajosTexto.BLANCO + d.nivel(trabajo) + TrabajosTexto.CREMA + "."
                + (anterior != null ? " Tu nivel de " + anterior.nombre() + " queda guardado." : ""));
        player.sendMessage(TrabajosTexto.PREFIJO + TrabajosTexto.GRIS + "Podrás cambiarte de nuevo en 24 horas.");
        Bukkit.broadcastMessage(TrabajosTexto.PREFIJO + TrabajosTexto.BLANCO + player.getName() + TrabajosTexto.CREMA
                + " ha seleccionado el trabajo " + TrabajosTexto.nombre(trabajo) + TrabajosTexto.CREMA + ".");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER, SoundCategory.PLAYERS, 1f, 1f);
    }

    static int monedasEnInventario(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (EconomyItemsFunctions.isDinoCoin(item)) total += item.getAmount();
        }
        return total;
    }

    private static void quitarMonedasDelInventario(Player player, int cantidad) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && cantidad > 0; i++) {
            ItemStack item = contents[i];
            if (!EconomyItemsFunctions.isDinoCoin(item)) continue;
            int quitar = Math.min(cantidad, item.getAmount());
            item.setAmount(item.getAmount() - quitar);
            cantidad -= quitar;
            if (item.getAmount() <= 0) contents[i] = null;
        }
        player.getInventory().setStorageContents(contents);
    }

    // ---------------------------------------------------------------- Admin (/trabajos admin)

    boolean fijarNivel(Player player, Trabajo trabajo, int nivel) {
        DatosTrabajo d = datos.get(player.getUniqueId());
        if (d == null) return false;
        d.niveles.put(trabajo, Math.max(0, Math.min(Trabajo.NIVEL_MAXIMO, nivel)));
        d.xp.put(trabajo, 0.0);
        d.sucio = true;
        Bukkit.getPluginManager().callEvent(new TrabajoSubeNivelEvent(player, trabajo, d.nivel(trabajo)));
        return true;
    }

    // /trabajos ver: los datos de cualquiera; si no está conectado se leen de MySQL
    void datosDe(UUID uuid, java.util.function.Consumer<DatosTrabajo> listo, Runnable error) {
        DatosTrabajo enMemoria = datos.get(uuid);
        if (enMemoria != null) {
            listo.accept(enMemoria);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DatosTrabajo cargados = DatosTrabajo.desde(db.loadJobs(uuid));
                Bukkit.getScheduler().runTask(plugin, () -> listo.accept(cargados));
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, error);
            }
        });
    }

    // /trabajos nivel add|remove: sube o baja niveles del trabajo actual (sin recompensas)
    boolean cambiarNivel(Player player, int cambio) {
        DatosTrabajo d = datos.get(player.getUniqueId());
        if (d == null || d.activo == null) return false;
        return fijarNivel(player, d.activo, d.nivel(d.activo) + cambio);
    }

    // /trabajos reset: borra los trabajos de todos (conectados y desconectados) y empiezan de cero. El borrado va
    // por el mismo hilo que los guardados, así ningún guardado viejo lo pisa
    void resetTodo(Runnable listo) {
        generacion++;
        for (UUID uuid : datos.keySet()) {
            quitarBarra(uuid);
            datos.put(uuid, new DatosTrabajo());
        }
        guardados.execute(() -> {
            try {
                db.deleteAllJobs();
                Bukkit.getScheduler().runTask(plugin, listo);
            } catch (SQLException error) {
                plugin.getLogger().severe("No se pudieron borrar los trabajos: " + error.getMessage());
            }
        });
    }

    boolean quitarEspera(Player player) {
        DatosTrabajo d = datos.get(player.getUniqueId());
        if (d == null) return false;
        d.desde = 0;
        d.sucio = true;
        return true;
    }

    // XP de admin: no cuenta para el tope por hora
    boolean darXpAdmin(Player player, double cantidad) {
        DatosTrabajo d = datos.get(player.getUniqueId());
        if (d == null || d.activo == null) return false;
        double hora = d.xpEnLaHora;
        long inicio = d.inicioHora;
        d.xpEnLaHora = 0;
        d.inicioHora = System.currentTimeMillis();
        ganarXp(player, d.activo, cantidad);
        d.xpEnLaHora = hora;
        d.inicioHora = inicio;
        return true;
    }
}
