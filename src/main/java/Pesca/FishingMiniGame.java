package Pesca;

import imp.crissyjuanxd.QuasoPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.Random;
import java.util.function.Consumer;

// El minijuego de las zonas de pesca: el pez tira del anzuelo y un marcador recorre la barra. Para tirar hay que volver
// a usar la caña: verde = pesca perfecta, naranja = buena y rojo = normal. Si no tira en 6 segundos el pez se escapa
public class FishingMiniGame {

    public enum Resultado { NORMAL, BUENA, PERFECTA, ESCAPO, SOLTO }

    static final int CASILLAS = 24;
    static final int VERDE = 2;
    static final int NARANJA = 4;
    static final int DURACION = 120;
    // El clic con el que picó no cuenta como tirar
    static final int GRACIA = 4;
    static final int MAX_RETRASO = 6;

    private static final String SEGMENTO = "▬";
    private static final TextColor AGUA = TextColor.color(0x61B1F2);
    private static final TextColor AGUA_CLARA = TextColor.color(0x9FD8F5);
    private static final TextColor ROJO = TextColor.color(0xC9545D);
    private static final TextColor NARANJA_COLOR = TextColor.color(0xEFA94A);
    private static final TextColor VERDE_COLOR = TextColor.color(0x7BE38E);
    private static final TextColor MARCADOR = TextColor.color(0xFFFFFF);
    private static final Title.Times TIEMPOS = Title.Times.times(Duration.ZERO, Duration.ofMillis(400), Duration.ZERO);

    private final QuasoPlugin plugin;
    private final Player player;
    private final FishHook hook;
    private final ItemStack vanillaLoot;
    private final Consumer<Resultado> alTerminar;
    private final Random random = new Random();
    private final BossBar tiempo;
    private final int verdeDesde;
    private final int[] historial = new int[MAX_RETRASO + 1];

    private Location anzuelo;
    private double posicion;
    private int direccion;
    private double velocidad;
    private double velocidadObjetivo;
    private int casilla;
    private int ticks;
    private int proximoTiron;
    private BukkitTask task;
    private boolean terminado;

    public FishingMiniGame(QuasoPlugin plugin, Player player, FishHook hook, ItemStack vanillaLoot, Consumer<Resultado> alTerminar) {
        this.plugin = plugin;
        this.player = player;
        this.hook = hook;
        this.vanillaLoot = vanillaLoot;
        this.alTerminar = alTerminar;
        this.anzuelo = hook.getLocation();
        this.verdeDesde = verdeAleatorio(random);
        // Arranca en un borde al azar y va hacia el otro
        boolean izquierda = random.nextBoolean();
        this.posicion = izquierda ? 0 : CASILLAS - 1;
        this.direccion = izquierda ? 1 : -1;
        this.velocidad = 0.6;
        this.velocidadObjetivo = 0.6;
        this.casilla = (int) posicion;
        this.proximoTiron = 6;
        this.tiempo = BossBar.bossBar(Component.text("≈ ", AGUA).append(Component.text("¡Algo picó! Vuelve a usar la caña para tirar", AGUA_CLARA))
                .append(Component.text(" ≈", AGUA)), 1f, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_12);
    }

    public ItemStack getVanillaLoot() {
        return vanillaLoot;
    }

    // Dónde estaba el anzuelo la última vez (ahí sale el premio)
    public Location getAnzuelo() {
        return anzuelo.clone();
    }

    public boolean isFinished() {
        return terminado;
    }

    public void start() {
        // Sin otra picada mientras dura el minijuego: el segundo clic recoge la caña en vez de pescar otra vez
        hook.resetFishingState();
        hook.setWaitTime(DURACION + 200);
        player.showBossBar(tiempo);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 1f, 1f);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.6f, 1.6f);
        dibujar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    // Volver a usar la caña. Con el ping se mira dónde estaba el marcador cuando el jugador lo vio
    public void tirar() {
        if (terminado || ticks < GRACIA) return;
        terminar(zona(historial[(ticks - retraso(player.getPing(), ticks)) % historial.length], verdeDesde));
    }

    // Se fue del server: se corta sin premio
    public void cancelar() {
        if (terminado) return;
        terminado = true;
        if (task != null) task.cancel();
        player.hideBossBar(tiempo);
        if (hook.isValid()) hook.remove();
    }

    private void tick() {
        if (terminado) return;
        if (!player.isOnline()) {
            cancelar();
            return;
        }
        if (!hook.isValid()) {
            terminar(Resultado.SOLTO);
            return;
        }
        ticks++;
        anzuelo = hook.getLocation();
        if (ticks >= DURACION) {
            terminar(Resultado.ESCAPO);
            return;
        }

        mover();
        historial[ticks % historial.length] = casilla;
        dibujar();
        animar();

        float restante = 1f - (float) ticks / DURACION;
        tiempo.progress(restante);
        tiempo.color(restante > 0.5f ? BossBar.Color.BLUE : restante > 0.25f ? BossBar.Color.YELLOW : BossBar.Color.RED);
    }

    // El pez cambia de fuerza cada medio segundo: el marcador acelera y frena en vez de ir siempre igual
    private void mover() {
        if (ticks % 10 == 0) velocidadObjetivo = 0.45 + random.nextDouble() * 0.5;
        velocidad += (velocidadObjetivo - velocidad) * 0.3;
        posicion += direccion * velocidad;
        if (posicion <= 0) {
            posicion = -posicion;
            direccion = 1;
        } else if (posicion >= CASILLAS - 1) {
            posicion = 2 * (CASILLAS - 1) - posicion;
            direccion = -1;
        }
        int anterior = casilla;
        casilla = (int) Math.round(posicion);
        if (zona(casilla, verdeDesde) == Resultado.PERFECTA && zona(anterior, verdeDesde) != Resultado.PERFECTA) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.35f, 1.8f);
        }
    }

    // Estela en el agua y, cada tanto, un tirón que hunde el corcho
    private void animar() {
        if (ticks % 2 == 0) anzuelo.getWorld().spawnParticle(Particle.FISHING, anzuelo, 2, 0.15, 0.02, 0.15, 0.01);
        if (ticks % 4 == 0) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 0.12f, 1.6f);
        if (ticks < proximoTiron) return;
        proximoTiron = ticks + 14 + random.nextInt(11);
        hook.setVelocity(new Vector((random.nextDouble() - 0.5) * 0.12, -0.25, (random.nextDouble() - 0.5) * 0.12));
        anzuelo.getWorld().spawnParticle(Particle.SPLASH, anzuelo, 14, 0.2, 0.05, 0.2, 0);
        anzuelo.getWorld().spawnParticle(Particle.BUBBLE_POP, anzuelo, 4, 0.15, 0.05, 0.15, 0.02);
        anzuelo.getWorld().playSound(anzuelo, Sound.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 0.35f, 1.2f + random.nextFloat() * 0.3f);
    }

    // ≈ ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ≈ en el subtítulo: el marcador va en blanco y las casillas de al lado brillan un poco
    private void dibujar() {
        TextComponent.Builder barra = Component.text().append(Component.text("≈ ", AGUA));
        StringBuilder tramo = new StringBuilder();
        TextColor colorTramo = null;
        for (int i = 0; i < CASILLAS; i++) {
            if (i == casilla) {
                if (!tramo.isEmpty()) barra.append(Component.text(tramo.toString(), colorTramo));
                tramo.setLength(0);
                colorTramo = null;
                barra.append(Component.text(SEGMENTO, MARCADOR, TextDecoration.BOLD));
                continue;
            }
            TextColor color = color(zona(i, verdeDesde));
            if (Math.abs(i - casilla) == 1) color = TextColor.lerp(0.45f, color, MARCADOR);
            if (colorTramo != null && !color.equals(colorTramo)) {
                barra.append(Component.text(tramo.toString(), colorTramo));
                tramo.setLength(0);
            }
            colorTramo = color;
            tramo.append(SEGMENTO);
        }
        if (!tramo.isEmpty()) barra.append(Component.text(tramo.toString(), colorTramo));
        barra.append(Component.text(" ≈", AGUA));
        // El primer momento avisa en grande que picó
        Component arriba = ticks < 16 ? Component.text("¡Pica!", AGUA, TextDecoration.BOLD) : Component.empty();
        player.showTitle(Title.title(arriba, barra.build(), TIEMPOS));
    }

    private void terminar(Resultado resultado) {
        terminado = true;
        if (task != null) task.cancel();
        player.hideBossBar(tiempo);
        // Al tick siguiente: el clic llega dentro del evento de pesca y la caña todavía está usando el anzuelo
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (hook.isValid()) {
                anzuelo = hook.getLocation();
                hook.remove();
            }
            if (player.isOnline()) alTerminar.accept(resultado);
        });
    }

    private static TextColor color(Resultado zona) {
        return switch (zona) {
            case PERFECTA -> VERDE_COLOR;
            case BUENA -> NARANJA_COLOR;
            default -> ROJO;
        };
    }

    // ---------------------------------------------------------------- Reglas (sin Bukkit, para los tests)

    static Resultado zona(int casilla, int verdeDesde) {
        if (casilla >= verdeDesde && casilla < verdeDesde + VERDE) return Resultado.PERFECTA;
        if (casilla >= verdeDesde - NARANJA && casilla < verdeDesde + VERDE + NARANJA) return Resultado.BUENA;
        return Resultado.NORMAL;
    }

    // El verde cae en cualquier parte, pero siempre con su naranja completo y algo de rojo a cada lado
    static int verdeAleatorio(Random random) {
        int desde = NARANJA + 1;
        int hasta = CASILLAS - VERDE - NARANJA - 1;
        return desde + random.nextInt(hasta - desde + 1);
    }

    // Cuántos ticks atrás mirar: lo que tarda en llegar la barra y volver el clic (como mucho 300 ms)
    static int retraso(int ping, int ticks) {
        int atras = (int) Math.round(Math.max(0, ping) / 50.0);
        return Math.max(0, Math.min(Math.min(MAX_RETRASO, ticks - 1), atras));
    }
}
