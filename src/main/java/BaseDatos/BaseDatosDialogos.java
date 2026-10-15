package BaseDatos;

import BaseDatos.BaseDatosSQL.Columna;
import Commands.QuasoReloadCommand;
import Events.MissionSystem.MissionHandler;
import Handlers.DatabaseManager;
import Trabajos.TrabajosManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

// /basedatos: toda la base de datos en diálogos, solo para OPs. Ver las tablas y sus filas, buscar, editar, añadir y
// borrar filas, el estado de la conexión, vaciar una tabla o hacer el reset total (con confirmación escrita y copia de
// seguridad que se puede restaurar) y recargar la config y la conexión como /quasoreload
public final class BaseDatosDialogos {

    public static final String PERMISO = "quaso.basedatos";
    public static final String CARPETA_COPIAS = "basedatos-copias";
    // Lo que hay que escribir para el reset total
    static final String PALABRA_RESET = "RESET";
    private static final int COPIAS_POR_TABLA = 10;
    private static final Set<String> MISIONES = Set.of("player_missions", "global_missions");
    private static final Set<String> TRABAJOS = Set.of("player_jobs", "player_job_state");

    private static final LegacyComponentSerializer TEXTO = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final ClickCallback.Options BOTONES = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(30)).build();
    private static final DateTimeFormatter ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private static final String AZUL = ChatColor.of("#8EC5FF").toString();
    private static final String CREMA = ChatColor.of("#F2E6D0").toString();
    private static final String VERDE = ChatColor.of("#7BD88F").toString();
    private static final String ROJO = ChatColor.of("#FF6B6B").toString();
    private static final String AMARILLO = ChatColor.of("#FFD166").toString();
    private static final String GRIS = ChatColor.GRAY.toString();
    private static final String OSCURO = ChatColor.DARK_GRAY.toString();
    private static final String BLANCO = ChatColor.WHITE.toString();
    private static final String NEGRITA = ChatColor.BOLD.toString();

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final BaseDatosSQL sql;
    private final MissionHandler misiones;
    private final TrabajosManager trabajos;
    private final File carpetaCopias;

    // Lo que se está mirando: la tabla, la página y lo que se buscó (null si nada)
    record Vista(String tabla, int pagina, String filtro) {}

    private record DatosTabla(List<Columna> columnas, BaseDatosSQL.Pagina pagina) {}

    record DatosFila(List<Columna> columnas, Map<String, String> fila) {}

    // Cómo se guarda una copia de seguridad en el archivo
    private static final class Copia {
        String tabla;
        String fecha;
        List<String> columnas;
        List<List<String>> filas;
    }

    @FunctionalInterface
    private interface Tarea<T> {
        T hacer() throws Exception;
    }

    // misiones y trabajos pueden ser null (entonces sus tablas se cambian directo)
    public BaseDatosDialogos(JavaPlugin plugin, DatabaseManager db, MissionHandler misiones, TrabajosManager trabajos) {
        this.plugin = plugin;
        this.db = db;
        this.sql = new BaseDatosSQL(db);
        this.misiones = misiones;
        this.trabajos = trabajos;
        this.carpetaCopias = new File(plugin.getDataFolder(), CARPETA_COPIAS);
    }

    public void abrir(Player player) {
        abrirPrincipal(player, null);
    }

    // ---------------------------------------------------------------- Menú principal y estado

    // Si no hay conexión sale la ventana para recargar en vez del error de siempre
    void abrirPrincipal(Player player, String aviso) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            BaseDatosSQL.Estado estado = null;
            Exception error = null;
            try {
                estado = sql.estado();
            } catch (Exception e) {
                error = e;
            }
            BaseDatosSQL.Estado leido = estado;
            Exception fallo = error;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) player.showDialog(fallo == null ? principal(leido, aviso) : sinConexion(fallo, aviso));
            });
        });
    }

    private Dialog principal(BaseDatosSQL.Estado estado, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(VERDE + "● Conectado" + GRIS + " · responde en " + BLANCO + estado.respuestaMs() + " ms" + GRIS + " · "
                + BLANCO + estado.version()));
        cuerpo.add(mensaje(BLANCO + estado.tablas().size() + " tablas" + GRIS + " · " + BLANCO + estado.filas() + " filas" + GRIS
                + " · " + BLANCO + BaseDatosSQL.tamano(estado.bytes())));
        if (!estado.faltan().isEmpty())
            cuerpo.add(mensaje(ROJO + "Faltan tablas del plugin: " + String.join(", ", estado.faltan()) + GRIS + " (Recargar las vuelve a crear)."));
        cuerpo.add(mensaje(GRIS + "Las de color crema son de QuasoPlugin; las grises, de otro plugin o de la web. Elige una:"));

        List<ActionButton> botones = new ArrayList<>();
        for (BaseDatosSQL.Tabla tabla : estado.tablas()) {
            botones.add(boton((tabla.propia() ? CREMA : GRIS) + tabla.nombre() + OSCURO + " (" + tabla.filas() + ")",
                    GRIS + (tabla.propia() ? "Tabla de QuasoPlugin" : "Tabla de otro plugin o de la web") + "\n" + BLANCO + tabla.filas()
                            + " filas" + GRIS + " · " + BaseDatosSQL.tamano(tabla.bytes()) + "\n" + GRIS + efecto(tabla.nombre()),
                    150, p -> abrirTabla(p, new Vista(tabla.nombre(), 0, null), null)));
        }
        botones.add(boton(AZUL + "Estado", GRIS + "La conexión, el servidor de MySQL y el tamaño de cada tabla.", 150,
                p -> abrirEstado(p)));
        botones.add(boton(VERDE + "Recargar", GRIS + "Lo mismo que /quasoreload: el config.yml y la conexión a MySQL.", 150,
                this::recargar));
        botones.add(boton(ROJO + NEGRITA + "Reset total", GRIS + "Vacía todas las tablas de QuasoPlugin (con dos confirmaciones y copia).",
                150, this::abrirReset));
        return dialogo(AZUL + NEGRITA + "☰ Base de datos", cuerpo, List.of(),
                DialogType.multiAction(botones, cerrar(), 2));
    }

    private Dialog sinConexion(Exception error, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(ROJO + "✖ No se pudo conectar a MySQL."));
        cuerpo.add(mensaje(GRIS + motivo(error)));
        cuerpo.add(mensaje(CREMA + "Revisa " + BLANCO + "Database1" + CREMA + " en el config.yml y usa " + VERDE + "Recargar" + CREMA + "."));
        List<ActionButton> botones = List.of(
                boton(VERDE + "Recargar", GRIS + "Vuelve a leer el config.yml y a conectar.", 150, this::recargar),
                boton(AZUL + "Reintentar", null, 150, p -> abrirPrincipal(p, null)));
        return dialogo(ROJO + NEGRITA + "☰ Base de datos", cuerpo, List.of(), DialogType.multiAction(botones, cerrar(), 2));
    }

    void abrirEstado(Player player) {
        cargar(player, sql::estado, estado -> {
            long propias = estado.tablas().stream().filter(BaseDatosSQL.Tabla::propia).count();
            StringBuilder datos = new StringBuilder();
            datos.append(GRIS).append("Servidor: ").append(BLANCO).append(db.getHost()).append(':').append(db.getPort()).append('\n')
                    .append(GRIS).append("Base: ").append(BLANCO).append(db.getDatabaseName()).append(GRIS).append(" · usuario ")
                    .append(BLANCO).append(db.getUsername()).append('\n')
                    .append(GRIS).append("Versión: ").append(BLANCO).append(estado.version()).append('\n')
                    .append(GRIS).append("Responde en: ").append(BLANCO).append(estado.respuestaMs()).append(" ms").append('\n')
                    .append(GRIS).append("Encendido hace: ").append(BLANCO).append(duracion(estado.uptimeSegundos())).append('\n')
                    .append(GRIS).append("Conexiones abiertas: ").append(BLANCO)
                    .append(estado.conexiones() < 0 ? "sin permiso para verlo" : String.valueOf(estado.conexiones())).append('\n')
                    .append(GRIS).append("Tablas: ").append(BLANCO).append(estado.tablas().size()).append(GRIS).append(" (")
                    .append(propias).append(" de ").append(BaseDatosSQL.PROPIAS.size()).append(" de QuasoPlugin)").append('\n')
                    .append(GRIS).append("Filas: ").append(BLANCO).append(estado.filas()).append(GRIS).append(" · tamaño ")
                    .append(BLANCO).append(BaseDatosSQL.tamano(estado.bytes()));
            StringBuilder tablas = new StringBuilder(CREMA + "Cada tabla:");
            for (BaseDatosSQL.Tabla tabla : estado.tablas()) {
                tablas.append('\n').append(GRIS).append(" · ").append(tabla.propia() ? CREMA : GRIS).append(tabla.nombre())
                        .append(GRIS).append(": ").append(BLANCO).append(tabla.filas()).append(" filas").append(GRIS).append(", ")
                        .append(BaseDatosSQL.tamano(tabla.bytes()));
            }
            List<DialogBody> cuerpo = new ArrayList<>(List.of(mensaje(datos.toString()), mensaje(tablas.toString())));
            if (!estado.faltan().isEmpty())
                cuerpo.add(mensaje(ROJO + "Faltan: " + String.join(", ", estado.faltan()) + GRIS + ". Recargar las crea de nuevo."));
            List<ActionButton> botones = List.of(boton(VERDE + "Recargar", GRIS + "Lo mismo que /quasoreload.", 150, this::recargar));
            return dialogo(AZUL + NEGRITA + "Estado de la base de datos", cuerpo, List.of(),
                    DialogType.multiAction(botones, volver(p -> abrirPrincipal(p, null)), 1));
        }, p -> abrirPrincipal(p, null));
    }

    // El config.yml en el hilo principal y la conexión (que puede tardar si MySQL no responde) en otro
    void recargar(Player player) {
        QuasoReloadCommand.recargarConfig(plugin);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            db.reload();
            Bukkit.getScheduler().runTask(plugin, () -> abrirPrincipal(player, VERDE + "✔ Se recargaron el config.yml y la conexión a MySQL."));
        });
    }

    // ---------------------------------------------------------------- Una tabla

    void abrirTabla(Player player, Vista vista, String aviso) {
        cargar(player, () -> {
            List<Columna> columnas = sql.columnas(vista.tabla());
            BaseDatosSQL.Pagina pagina = sql.filas(vista.tabla(), columnas, vista.pagina(), vista.filtro());
            // Si se borraron filas y la página quedó vacía, se muestra la última que tiene algo
            int ultima = paginas(pagina.total()) - 1;
            if (pagina.filas().isEmpty() && vista.pagina() > ultima && ultima >= 0) {
                pagina = sql.filas(vista.tabla(), columnas, ultima, vista.filtro());
            }
            return new DatosTabla(columnas, pagina);
        }, datos -> tabla(fijarPagina(vista, datos.pagina().total()), datos, aviso), p -> abrirPrincipal(p, null));
    }

    private static Vista fijarPagina(Vista vista, long total) {
        int ultima = Math.max(0, paginas(total) - 1);
        return vista.pagina() > ultima ? new Vista(vista.tabla(), ultima, vista.filtro()) : vista;
    }

    static int paginas(long total) {
        return (int) Math.max(1, (total + BaseDatosSQL.POR_PAGINA - 1) / BaseDatosSQL.POR_PAGINA);
    }

    private Dialog tabla(Vista vista, DatosTabla datos, String aviso) {
        List<Columna> columnas = datos.columnas();
        long total = datos.pagina().total();
        int paginas = paginas(total);

        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        String cuantas = BLANCO + total + (total == 1 ? " fila" : " filas") + (vista.filtro() != null
                ? GRIS + " con «" + BLANCO + vista.filtro() + GRIS + "»" : "") + GRIS + " · página " + BLANCO + (vista.pagina() + 1)
                + GRIS + " de " + BLANCO + paginas;
        cuerpo.add(mensaje(cuantas));
        StringBuilder cols = new StringBuilder(GRIS + "Columnas: ");
        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            if (i > 0) cols.append(GRIS).append(", ");
            cols.append(CREMA).append(c.nombre()).append(c.clave() ? AMARILLO + " 🔑" : "").append(OSCURO).append(" ").append(c.tipo());
        }
        cuerpo.add(mensaje(cols.toString()));
        cuerpo.add(mensaje(GRIS + efecto(vista.tabla())));
        if (datos.pagina().filas().isEmpty())
            cuerpo.add(mensaje(GRIS + (vista.filtro() != null ? "Nada coincide con la búsqueda." : "La tabla está vacía.")));

        List<ActionButton> botones = new ArrayList<>();
        for (Map<String, String> fila : datos.pagina().filas()) {
            Map<String, String> clave = BaseDatosSQL.clave(columnas, fila);
            botones.add(boton(CREMA + BaseDatosSQL.resumen(columnas, fila), vistaPrevia(columnas, fila), 200,
                    p -> abrirFila(p, vista, clave, null)));
        }
        if (vista.pagina() > 0)
            botones.add(boton(AZUL + "« Anterior", null, 200, p -> abrirTabla(p, new Vista(vista.tabla(), vista.pagina() - 1, vista.filtro()), null)));
        if (vista.pagina() < paginas - 1)
            botones.add(boton(AZUL + "Siguiente »", null, 200, p -> abrirTabla(p, new Vista(vista.tabla(), vista.pagina() + 1, vista.filtro()), null)));
        botones.add(boton(AZUL + "Buscar", GRIS + "Busca un texto en todas las columnas (por ejemplo el nombre o el UUID de un jugador).",
                200, p -> p.showDialog(buscar(vista))));
        if (vista.filtro() != null)
            botones.add(boton(GRIS + "Quitar búsqueda", null, 200, p -> abrirTabla(p, new Vista(vista.tabla(), 0, null), null)));
        botones.add(boton(VERDE + "Añadir fila", GRIS + "Agrega una fila nueva escribiendo cada columna.", 200,
                p -> p.showDialog(anadir(vista, columnas))));
        botones.add(boton(AMARILLO + "Copias de seguridad", GRIS + "Las copias que se guardan al vaciar la tabla, para restaurarlas.",
                200, p -> abrirCopias(p, vista, null)));
        botones.add(boton(ROJO + "Vaciar tabla", GRIS + "Borra todas las filas (con confirmación escrita y copia).", 200,
                p -> abrirVaciar(p, vista)));
        return dialogo(AZUL + NEGRITA + "☰ " + vista.tabla(), cuerpo, List.of(),
                DialogType.multiAction(botones, volver(p -> abrirPrincipal(p, null)), 2));
    }

    // Lo que se ve al pasar el mouse por una fila: sus primeras columnas
    private static Component vistaPrevia(List<Columna> columnas, Map<String, String> fila) {
        StringBuilder texto = new StringBuilder();
        int mostradas = 0;
        for (Columna c : columnas) {
            if (mostradas++ == 6) break;
            if (texto.length() > 0) texto.append('\n');
            texto.append(GRIS).append(c.nombre()).append(": ").append(valor(fila.get(c.nombre()), 40));
        }
        return texto(texto.toString());
    }

    private Dialog buscar(Vista vista) {
        DialogInput entrada = DialogInput.text("texto", texto(CREMA + "Texto a buscar")).width(300).maxLength(64)
                .initial(vista.filtro() == null ? "" : vista.filtro()).build();
        return dialogo(AZUL + NEGRITA + "Buscar en " + vista.tabla(),
                List.of(mensaje(GRIS + "Se busca en todas las columnas menos los inventarios guardados.")), List.of(entrada),
                DialogType.confirmation(
                        formulario(AZUL + "Buscar", (p, respuesta) -> {
                            String texto = respuesta.getText("texto");
                            String filtro = texto == null || texto.isBlank() ? null : texto.trim();
                            abrirTabla(p, new Vista(vista.tabla(), 0, filtro), null);
                        }),
                        volver(p -> abrirTabla(p, vista, null))));
    }

    // ---------------------------------------------------------------- Una fila

    void abrirFila(Player player, Vista vista, Map<String, String> clave, String aviso) {
        cargar(player, () -> {
            List<Columna> columnas = sql.columnas(vista.tabla());
            return new DatosFila(columnas, sql.fila(vista.tabla(), columnas, clave));
        }, datos -> {
            if (datos.fila() == null) return null;
            return fila(vista, datos, aviso);
        }, p -> abrirTabla(p, vista, null), p -> abrirTabla(p, vista, ROJO + "✖ Esa fila ya no existe (la borró alguien o cambió su clave)."));
    }

    private Dialog fila(Vista vista, DatosFila datos, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        StringBuilder texto = new StringBuilder();
        for (Columna c : datos.columnas()) {
            if (texto.length() > 0) texto.append('\n');
            texto.append(c.clave() ? AMARILLO : CREMA).append(c.nombre()).append(OSCURO).append(" ").append(c.tipo())
                    .append(GRIS).append(": ").append(valor(datos.fila().get(c.nombre()), 160));
        }
        cuerpo.add(mensaje(texto.toString()));
        Map<String, String> clave = BaseDatosSQL.clave(datos.columnas(), datos.fila());
        List<ActionButton> botones = List.of(
                boton(AMARILLO + "Editar", GRIS + "Cambia los valores de esta fila.", 150, p -> p.showDialog(editar(vista, datos))),
                boton(ROJO + "Borrar fila", GRIS + "Pide confirmación.", 150, p -> p.showDialog(borrarFila(vista, datos))));
        return dialogo(AZUL + NEGRITA + vista.tabla() + GRIS + " · " + CREMA + BaseDatosSQL.resumen(datos.columnas(), datos.fila()),
                cuerpo, List.of(), DialogType.multiAction(botones, volver(p -> abrirTabla(p, vista, null)), 2));
    }

    private Dialog editar(Vista vista, DatosFila datos) {
        List<Columna> columnas = datos.columnas();
        Map<String, String> original = datos.fila();
        List<DialogInput> entradas = new ArrayList<>();
        List<String> largas = new ArrayList<>();
        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            String valor = original.get(c.nombre());
            if (valor != null && valor.length() > BaseDatosSQL.MAX_EDITABLE) {
                largas.add(c.nombre() + " (" + valor.length() + " caracteres)");
                continue;
            }
            entradas.add(entrada("c" + i, c, valor));
        }
        List<DialogBody> cuerpo = new ArrayList<>();
        cuerpo.add(mensaje(CREMA + "Cambia lo que quieras y guarda. Escribe " + BLANCO + BaseDatosSQL.NULO + CREMA
                + " para dejar un campo nulo. Solo se guardan las columnas que cambiaste."));
        if (!largas.isEmpty())
            cuerpo.add(mensaje(GRIS + "No se editan aquí por lo largas (se quedan como están): " + String.join(", ", largas) + "."));
        if (MISIONES.contains(vista.tabla()) || TRABAJOS.contains(vista.tabla()))
            cuerpo.add(mensaje(GRIS + "Antes se guarda lo pendiente y después los conectados se recargan con lo que pongas."));

        return dialogo(AMARILLO + NEGRITA + "Editar " + vista.tabla(), cuerpo, entradas, DialogType.confirmation(
                formulario(VERDE + "Guardar", (p, respuesta) -> guardarEdicion(p, vista, datos, respuesta)),
                volver(p -> abrirFila(p, vista, BaseDatosSQL.clave(columnas, original), null))));
    }

    // Botón Guardar de editar: solo las columnas que cambiaron, ya validadas
    void guardarEdicion(Player p, Vista vista, DatosFila datos, DialogResponseView respuesta) {
        List<Columna> columnas = datos.columnas();
        Map<String, String> original = datos.fila();
        Map<String, String> nuevos = new LinkedHashMap<>();
        try {
            for (int i = 0; i < columnas.size(); i++) {
                Columna c = columnas.get(i);
                String antes = original.get(c.nombre());
                if (antes != null && antes.length() > BaseDatosSQL.MAX_EDITABLE) continue;
                String escrito = leer(respuesta, "c" + i, c);
                if (escrito == null || sinCambio(c, antes, escrito)) continue;
                nuevos.put(c.nombre(), BaseDatosSQL.convertir(c, escrito));
            }
        } catch (IllegalArgumentException invalido) {
            p.showDialog(aviso(ROJO + "✖ " + invalido.getMessage(), q -> q.showDialog(editar(vista, datos))));
            return;
        }
        Map<String, String> clave = BaseDatosSQL.clave(columnas, original);
        if (nuevos.isEmpty()) {
            abrirFila(p, vista, clave, GRIS + "No cambiaste nada.");
            return;
        }
        // Si cambió una columna de la clave, la fila se vuelve a buscar con la clave nueva
        Map<String, String> claveNueva = new LinkedHashMap<>(clave);
        for (String columna : clave.keySet()) if (nuevos.containsKey(columna)) claveNueva.put(columna, nuevos.get(columna));
        AtomicInteger cambiadas = new AtomicInteger();
        aplicar(vista.tabla(), () -> cambiadas.set(sql.actualizar(vista.tabla(), columnas, clave, nuevos)), error -> {
            if (error != null) p.showDialog(aviso(ROJO + "✖ MySQL no guardó el cambio: " + motivo(error),
                    q -> abrirFila(q, vista, clave, null)));
            else if (cambiadas.get() == 0) abrirTabla(p, vista, ROJO + "✖ Esa fila ya no existía: no se guardó nada.");
            else {
                registrar(p, "editó " + nuevos.keySet() + " en " + vista.tabla() + " " + clave);
                abrirFila(p, vista, claveNueva, VERDE + "✔ Guardado (" + String.join(", ", nuevos.keySet()) + ").");
            }
        });
    }

    private Dialog anadir(Vista vista, List<Columna> columnas) {
        List<DialogInput> entradas = new ArrayList<>();
        for (int i = 0; i < columnas.size(); i++) {
            Columna c = columnas.get(i);
            if (!c.auto()) entradas.add(entrada("c" + i, c, null));
        }
        List<DialogBody> cuerpo = List.of(mensaje(CREMA + "Lo que dejes vacío toma el valor por defecto de la columna. "
                + BLANCO + BaseDatosSQL.NULO + CREMA + " lo deja nulo. Las columnas con 🔑 no se pueden repetir."));
        return dialogo(VERDE + NEGRITA + "Añadir a " + vista.tabla(), cuerpo, entradas, DialogType.confirmation(
                formulario(VERDE + "Añadir", (p, respuesta) -> guardarNueva(p, vista, columnas, respuesta)),
                volver(p -> abrirTabla(p, vista, null))));
    }

    // Botón Añadir: lo vacío no se manda (toma el valor por defecto)
    void guardarNueva(Player p, Vista vista, List<Columna> columnas, DialogResponseView respuesta) {
        Map<String, String> valores = new LinkedHashMap<>();
        try {
            for (int i = 0; i < columnas.size(); i++) {
                Columna c = columnas.get(i);
                if (c.auto()) continue;
                String escrito = leer(respuesta, "c" + i, c);
                if (escrito == null || escrito.isEmpty()) continue;
                valores.put(c.nombre(), BaseDatosSQL.convertir(c, escrito));
            }
        } catch (IllegalArgumentException invalido) {
            p.showDialog(aviso(ROJO + "✖ " + invalido.getMessage(), q -> q.showDialog(anadir(vista, columnas))));
            return;
        }
        aplicar(vista.tabla(), () -> sql.insertar(vista.tabla(), valores), error -> {
            if (error != null) p.showDialog(aviso(ROJO + "✖ MySQL no añadió la fila: " + motivo(error),
                    q -> q.showDialog(anadir(vista, columnas))));
            else {
                registrar(p, "añadió una fila a " + vista.tabla() + " " + valores.keySet());
                abrirTabla(p, vista, VERDE + "✔ Fila añadida.");
            }
        });
    }

    private Dialog borrarFila(Vista vista, DatosFila datos) {
        Map<String, String> clave = BaseDatosSQL.clave(datos.columnas(), datos.fila());
        List<DialogBody> cuerpo = List.of(
                mensaje(CREMA + "¿Borrar esta fila de " + BLANCO + vista.tabla() + CREMA + "?"),
                mensaje(BLANCO + BaseDatosSQL.resumen(datos.columnas(), datos.fila())),
                mensaje(GRIS + efecto(vista.tabla())));
        return dialogo(ROJO + NEGRITA + "Borrar fila", cuerpo, List.of(), DialogType.confirmation(
                boton(ROJO + "Borrar", null, 150, p -> borrarFilaConfirmada(p, vista, datos)),
                volver(p -> abrirFila(p, vista, clave, null))));
    }

    void borrarFilaConfirmada(Player p, Vista vista, DatosFila datos) {
        Map<String, String> clave = BaseDatosSQL.clave(datos.columnas(), datos.fila());
        AtomicInteger borradas = new AtomicInteger();
        aplicar(vista.tabla(), () -> borradas.set(sql.borrar(vista.tabla(), datos.columnas(), clave)), error -> {
            if (error != null) p.showDialog(aviso(ROJO + "✖ MySQL no la borró: " + motivo(error), q -> abrirTabla(q, vista, null)));
            else {
                if (borradas.get() > 0) registrar(p, "borró una fila de " + vista.tabla() + " " + clave);
                abrirTabla(p, vista, borradas.get() > 0 ? VERDE + "✔ Fila borrada." : GRIS + "Esa fila ya no existía.");
            }
        });
    }

    // Un campo del formulario: interruptor para las columnas sí/no y texto para lo demás
    private static DialogInput entrada(String llave, Columna c, String valor) {
        String etiqueta = (c.clave() ? AMARILLO : CREMA) + c.nombre() + OSCURO + " " + c.tipo() + (c.clave() ? AMARILLO + " 🔑" : "")
                + (valor == null && c.defecto() != null ? GRIS + " (por defecto " + c.defecto() + ")" : "");
        if (c.booleana()) {
            return DialogInput.bool(llave, texto(etiqueta)).initial("1".equals(valor)).build();
        }
        String inicial = valor == null ? "" : valor;
        return DialogInput.text(llave, texto(etiqueta)).width(300)
                .maxLength(Math.max(Math.max(c.largoMaximo(), inicial.length()), BaseDatosSQL.NULO.length())).initial(inicial).build();
    }

    // Lo que dejó el OP en el campo, con los interruptores pasados a 1 o 0
    private static String leer(DialogResponseView respuesta, String llave, Columna c) {
        if (c.booleana()) {
            Boolean marcado = respuesta.getBoolean(llave);
            return marcado == null ? null : marcado ? "1" : "0";
        }
        return respuesta.getText(llave);
    }

    // En el formulario de editar los nulos se muestran vacíos: dejarlo vacío o apagado no los cambia
    static boolean sinCambio(Columna c, String antes, String escrito) {
        if (antes == null) return escrito.isEmpty() || (c.booleana() && escrito.equals("0"));
        if (c.booleana()) return escrito.equals("1") == "1".equals(antes);
        return escrito.equals(antes);
    }

    // ---------------------------------------------------------------- Vaciar, reset y copias

    void abrirVaciar(Player player, Vista vista) {
        cargar(player, sql::tablas, tablas -> {
            long filas = tablas.stream().filter(t -> t.nombre().equals(vista.tabla())).mapToLong(BaseDatosSQL.Tabla::filas).sum();
            List<DialogBody> cuerpo = List.of(
                    mensaje(CREMA + "Vas a borrar " + ROJO + "las " + filas + " filas" + CREMA + " de " + BLANCO + vista.tabla() + CREMA + "."),
                    mensaje(GRIS + efecto(vista.tabla())),
                    mensaje(GRIS + "Antes se guarda una copia en " + BLANCO + "plugins/" + plugin.getName() + "/" + CARPETA_COPIAS + "/"
                            + vista.tabla() + GRIS + " y la puedes restaurar desde Copias de seguridad."),
                    mensaje(CREMA + "Para confirmar escribe el nombre de la tabla: " + BLANCO + vista.tabla()));
            DialogInput confirmar = DialogInput.text("confirmar", texto(CREMA + "Nombre de la tabla")).width(300).maxLength(64).build();
            return dialogo(ROJO + NEGRITA + "Vaciar " + vista.tabla(), cuerpo, List.of(confirmar), DialogType.confirmation(
                    formulario(ROJO + "Vaciar", (p, respuesta) -> confirmarVaciar(p, vista, respuesta.getText("confirmar"))),
                    volver(p -> abrirTabla(p, vista, null))));
        }, p -> abrirTabla(p, vista, null));
    }

    // Solo se vacía si escribió el nombre exacto de la tabla
    void confirmarVaciar(Player p, Vista vista, String escrito) {
        if (escrito == null || !escrito.trim().equals(vista.tabla())) {
            abrirTabla(p, vista, ROJO + "✖ No escribiste «" + vista.tabla() + "»: no se borró nada.");
            return;
        }
        vaciar(p, List.of(vista.tabla()), (borradas, copias) -> abrirTabla(p, new Vista(vista.tabla(), 0, null),
                VERDE + "✔ Se vació " + vista.tabla() + ": " + borradas + " filas borradas. Copia: " + String.join(", ", copias)));
    }

    void abrirReset(Player player) {
        cargar(player, sql::tablas, tablas -> {
            List<BaseDatosSQL.Tabla> propias = tablas.stream().filter(BaseDatosSQL.Tabla::propia).toList();
            long filas = propias.stream().mapToLong(BaseDatosSQL.Tabla::filas).sum();
            StringBuilder lista = new StringBuilder(CREMA + "Se vacían las " + propias.size() + " tablas de QuasoPlugin (" + ROJO
                    + filas + " filas" + CREMA + "):");
            for (BaseDatosSQL.Tabla tabla : propias) {
                lista.append('\n').append(GRIS).append(" · ").append(BLANCO).append(tabla.nombre()).append(GRIS).append(" (")
                        .append(tabla.filas()).append(") ").append(efecto(tabla.nombre()));
            }
            List<DialogBody> cuerpo = List.of(
                    mensaje(lista.toString()),
                    mensaje(GRIS + "Se guarda una copia de cada tabla antes de borrar. Las tablas de otros plugins no se tocan."),
                    mensaje(CREMA + "Para seguir escribe " + ROJO + PALABRA_RESET + CREMA + ":"));
            DialogInput confirmar = DialogInput.text("confirmar", texto(CREMA + "Confirmación")).width(300).maxLength(16).build();
            return dialogo(ROJO + NEGRITA + "Reset total (1 de 2)", cuerpo, List.of(confirmar), DialogType.confirmation(
                    formulario(ROJO + "Continuar", (p, respuesta) -> confirmarReset(p, propias, filas, respuesta.getText("confirmar"))),
                    volver(p -> abrirPrincipal(p, null))));
        }, p -> abrirPrincipal(p, null));
    }

    // Primera confirmación: tiene que escribir RESET; después sale la segunda
    void confirmarReset(Player p, List<BaseDatosSQL.Tabla> propias, long filas, String escrito) {
        if (escrito == null || !escrito.trim().equals(PALABRA_RESET)) {
            abrirPrincipal(p, ROJO + "✖ No escribiste " + PALABRA_RESET + ": no se borró nada.");
            return;
        }
        p.showDialog(resetFinal(propias, filas));
    }

    private Dialog resetFinal(List<BaseDatosSQL.Tabla> propias, long filas) {
        List<DialogBody> cuerpo = List.of(
                mensaje(ROJO + NEGRITA + "Última confirmación."),
                mensaje(CREMA + "Se borran " + ROJO + filas + " filas" + CREMA + " de " + propias.size() + " tablas. Solo se pueden"
                        + " recuperar desde las copias de seguridad."),
                mensaje(GRIS + "Los conectados se recargan vacíos y cada jugador cuenta como nuevo al volver a entrar (rango"
                        + " DinoNugget y kit de bienvenida)."));
        return dialogo(ROJO + NEGRITA + "Reset total (2 de 2)", cuerpo, List.of(), DialogType.confirmation(
                boton(ROJO + NEGRITA + "Sí, borrar todo", null, 150,
                        p -> resetear(p, propias.stream().map(BaseDatosSQL.Tabla::nombre).toList())),
                volver(p -> abrirPrincipal(p, GRIS + "Reset cancelado: no se borró nada."))));
    }

    void resetear(Player p, List<String> tablas) {
        vaciar(p, tablas, (borradas, copias) -> {
            registrar(p, "hizo el reset total (" + borradas + " filas)");
            abrirPrincipal(p, VERDE + "✔ Reset hecho: " + borradas + " filas borradas de " + copias.size()
                    + " tablas. Las copias están en " + CARPETA_COPIAS + ".");
        });
    }

    // Vacía las tablas una por una (cada una guarda su copia antes); si una falla, las siguientes no se tocan
    private void vaciar(Player player, List<String> tablas, BiConsumer<Integer, List<String>> listo) {
        vaciar(player, tablas, 0, 0, new ArrayList<>(), listo);
    }

    private void vaciar(Player player, List<String> tablas, int indice, int borradas, List<String> copias,
                        BiConsumer<Integer, List<String>> listo) {
        if (indice >= tablas.size()) {
            listo.accept(borradas, copias);
            return;
        }
        String tabla = tablas.get(indice);
        AtomicInteger filas = new AtomicInteger();
        AtomicReference<String> copia = new AtomicReference<>();
        aplicar(tabla, () -> {
            copia.set(copiar(tabla));
            filas.set(sql.vaciar(tabla));
        }, error -> {
            if (error != null) {
                player.showDialog(aviso(ROJO + "✖ No se pudo vaciar " + tabla + ": " + motivo(error)
                        + (indice > 0 ? GRIS + " (ya se vaciaron: " + String.join(", ", tablas.subList(0, indice)) + ")" : ""),
                        p -> abrirPrincipal(p, null)));
                return;
            }
            registrar(player, "vació " + tabla + " (" + filas.get() + " filas, copia " + copia.get() + ")");
            copias.add(copia.get());
            vaciar(player, tablas, indice + 1, borradas + filas.get(), copias, listo);
        });
    }

    void abrirCopias(Player player, Vista vista, String aviso) {
        File[] archivos = new File(carpetaCopias, carpeta(vista.tabla())).listFiles((dir, nombre) -> nombre.endsWith(".json"));
        List<File> copias = archivos == null ? List.of()
                : Arrays.stream(archivos).sorted(Comparator.comparing(File::getName).reversed()).toList();
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(copias.isEmpty() ? GRIS + "Todavía no hay copias de esta tabla. Se guardan solas al vaciarla o en el reset,"
                + " o con «Guardar copia ahora»." : CREMA + "Elige una copia para restaurarla. Se guardan las últimas "
                + COPIAS_POR_TABLA + " de cada tabla."));
        List<ActionButton> botones = new ArrayList<>();
        for (File archivo : copias) {
            botones.add(boton(CREMA + nombreCopia(archivo), GRIS + archivo.getName(), 200, p -> p.showDialog(restaurar(vista, archivo))));
        }
        botones.add(boton(VERDE + "Guardar copia ahora", GRIS + "Guarda cómo está la tabla en este momento.", 200,
                p -> guardarCopia(p, vista)));
        player.showDialog(dialogo(AMARILLO + NEGRITA + "Copias de " + vista.tabla(), cuerpo, List.of(),
                DialogType.multiAction(botones, volver(p -> abrirTabla(p, vista, null)), 1)));
    }

    // Pasa por aplicar para que en misiones y trabajos se guarde antes lo pendiente
    void guardarCopia(Player p, Vista vista) {
        aplicar(vista.tabla(), () -> registrar(p, "guardó una copia de " + vista.tabla() + ": " + copiar(vista.tabla())),
                error -> abrirCopias(p, vista, error == null ? VERDE + "✔ Copia guardada." : ROJO + "✖ " + motivo(error)));
    }

    private Dialog restaurar(Vista vista, File archivo) {
        List<DialogBody> cuerpo = List.of(
                mensaje(CREMA + "¿Restaurar la copia del " + BLANCO + nombreCopia(archivo) + CREMA + " en " + BLANCO + vista.tabla() + CREMA + "?"),
                mensaje(GRIS + "Las filas de la copia se vuelven a poner: las que tienen la misma clave se reemplazan y las demás filas"
                        + " de la tabla se quedan como están."));
        return dialogo(AMARILLO + NEGRITA + "Restaurar copia", cuerpo, List.of(), DialogType.confirmation(
                boton(VERDE + "Restaurar", null, 150, p -> restaurarCopia(p, vista, archivo)),
                volver(p -> abrirCopias(p, vista, null))));
    }

    void restaurarCopia(Player p, Vista vista, File archivo) {
        AtomicInteger filas = new AtomicInteger();
        aplicar(vista.tabla(), () -> filas.set(sql.restaurar(vista.tabla(), leerCopia(archivo))), error -> {
            if (error != null) p.showDialog(aviso(ROJO + "✖ No se pudo restaurar: " + motivo(error), q -> abrirCopias(q, vista, null)));
            else {
                registrar(p, "restauró " + archivo.getName() + " en " + vista.tabla() + " (" + filas.get() + " filas)");
                abrirTabla(p, new Vista(vista.tabla(), 0, null), VERDE + "✔ Se restauraron " + filas.get() + " filas.");
            }
        });
    }

    // Guarda la tabla entera en un JSON y deja solo las últimas copias. Devuelve tabla/archivo
    private String copiar(String tabla) throws SQLException {
        BaseDatosSQL.Contenido contenido = sql.todo(tabla);
        File carpeta = new File(carpetaCopias, carpeta(tabla));
        LocalDateTime ahora = LocalDateTime.now();
        String nombre = ahora.format(ARCHIVO) + "_" + contenido.filas().size() + "filas.json";
        Copia copia = new Copia();
        copia.tabla = tabla;
        copia.fecha = ahora.toString();
        copia.columnas = contenido.columnas();
        copia.filas = contenido.filas();
        try {
            Files.createDirectories(carpeta.toPath());
            Files.writeString(new File(carpeta, nombre).toPath(), GSON.toJson(copia), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("no se pudo guardar la copia, así que no se borró nada (" + e.getMessage() + ")", e);
        }
        File[] viejas = carpeta.listFiles((dir, n) -> n.endsWith(".json"));
        if (viejas != null && viejas.length > COPIAS_POR_TABLA) {
            Arrays.sort(viejas, Comparator.comparing(File::getName));
            for (int i = 0; i < viejas.length - COPIAS_POR_TABLA; i++) viejas[i].delete();
        }
        return carpeta(tabla) + "/" + nombre;
    }

    private static BaseDatosSQL.Contenido leerCopia(File archivo) throws SQLException {
        try {
            Copia copia = GSON.fromJson(Files.readString(archivo.toPath(), StandardCharsets.UTF_8), Copia.class);
            if (copia == null || copia.columnas == null || copia.filas == null) throw new SQLException("la copia está vacía o dañada");
            for (List<String> fila : copia.filas) {
                if (fila.size() != copia.columnas.size()) throw new SQLException("la copia está dañada (una fila no tiene todas las columnas)");
            }
            return new BaseDatosSQL.Contenido(copia.columnas, copia.filas);
        } catch (IOException | RuntimeException e) {
            throw new SQLException("no se pudo leer la copia (" + e.getMessage() + ")", e);
        }
    }

    // El nombre de la carpeta sale del de la tabla, sin caracteres raros
    static String carpeta(String tabla) {
        return tabla.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    // "2026-10-15_14-03-22_12filas.json" -> "2026-10-15 14:03:22 · 12 filas"
    static String nombreCopia(File archivo) {
        String[] partes = archivo.getName().replace(".json", "").split("_");
        if (partes.length < 3) return archivo.getName();
        return partes[0] + " " + partes[1].replace('-', ':') + " · " + partes[2].replace("filas", " filas");
    }

    // ---------------------------------------------------------------- Cambios con caché

    // Las tablas de misiones y de trabajos pasan por su sistema (guarda lo pendiente, cambia y recarga a los
    // conectados) para que su caché no pise lo editado; las demás se cambian directo. "listo" corre en el hilo principal
    private void aplicar(String tabla, DatabaseManager.Cambio cambio, Consumer<SQLException> listo) {
        if (MISIONES.contains(tabla) && misiones != null) {
            misiones.editarBaseDeDatos(cambio, listo);
        } else if (TRABAJOS.contains(tabla) && trabajos != null) {
            trabajos.editarBaseDeDatos(cambio, listo);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                SQLException error = null;
                try {
                    cambio.aplicar();
                } catch (SQLException e) {
                    error = e;
                }
                SQLException fin = error;
                Bukkit.getScheduler().runTask(plugin, () -> listo.accept(fin));
            });
        }
    }

    // Todo cambio queda en la consola
    private void registrar(Player player, String que) {
        plugin.getLogger().warning("[BaseDatos] " + player.getName() + " " + que + ".");
    }

    // Qué pasa en el juego si se borran filas de esta tabla
    static String efecto(String tabla) {
        return switch (tabla) {
            case "players" -> "Los jugadores borrados cuentan como nuevos al entrar (DinoNugget y kit de bienvenida).";
            case "player_missions" -> "Se pierde el progreso de misiones de esos jugadores.";
            case "global_missions" -> "Las misiones borradas quedan desactivadas.";
            case "player_jobs" -> "Se pierden los niveles y la XP de trabajo.";
            case "player_job_state" -> "Se pierde el trabajo actual y la espera para cambiar.";
            case "player_backpacks" -> "Esas mochilas y monederos quedan vacíos.";
            case "event_inventories" -> "Se pierden los inventarios guardados de los eventos.";
            default -> "No es de QuasoPlugin: el plugin no la usa.";
        };
    }

    // ---------------------------------------------------------------- Ayudas de los diálogos

    // La consulta va en otro hilo y el diálogo se arma en el principal; si el diálogo sale null se usa "siNulo" y si
    // MySQL falla sale el error con Volver
    private <T> void cargar(Player player, Tarea<T> tarea, Function<T, Dialog> mostrar, Consumer<Player> volver) {
        cargar(player, tarea, mostrar, volver, volver);
    }

    private <T> void cargar(Player player, Tarea<T> tarea, Function<T, Dialog> dialogo, Consumer<Player> volver, Consumer<Player> siNulo) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                T resultado = tarea.hacer();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;
                    Dialog listo = dialogo.apply(resultado);
                    if (listo == null) siNulo.accept(player);
                    else player.showDialog(listo);
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) player.showDialog(aviso(ROJO + "✖ MySQL dio un error: " + motivo(e), volver));
                });
            }
        });
    }

    private static Dialog aviso(String texto, Consumer<Player> volver) {
        return dialogo(AZUL + NEGRITA + "☰ Base de datos", List.of(mensaje(texto)), List.of(), DialogType.notice(volver(volver)));
    }

    private static String motivo(Exception error) {
        String texto = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return texto.length() > 300 ? texto.substring(0, 300) + "…" : texto;
    }

    static String valor(String valor, int maximo) {
        if (valor == null) return OSCURO + BaseDatosSQL.NULO;
        if (valor.isEmpty()) return OSCURO + "(vacío)";
        if (valor.length() <= maximo) return BLANCO + valor;
        return BLANCO + valor.substring(0, maximo) + OSCURO + "… (" + valor.length() + " caracteres)";
    }

    static String duracion(long segundos) {
        if (segundos < 0) return "sin permiso para verlo";
        long dias = segundos / 86400, horas = segundos % 86400 / 3600, minutos = segundos % 3600 / 60;
        if (dias > 0) return dias + " d " + horas + " h";
        if (horas > 0) return horas + " h " + minutos + " min";
        return minutos + " min";
    }

    private static Dialog dialogo(String titulo, List<DialogBody> cuerpo, List<DialogInput> entradas, DialogType tipo) {
        DialogBase base = DialogBase.builder(texto(titulo)).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(cuerpo).inputs(entradas).build();
        return Dialog.create(builder -> builder.empty().base(base).type(tipo));
    }

    private static DialogBody mensaje(String texto) {
        return DialogBody.plainMessage(texto(texto), 360);
    }

    // Cada botón vuelve a mirar que siga siendo OP antes de hacer algo
    private static ActionButton boton(String etiqueta, Object ayuda, int ancho, Consumer<Player> accion) {
        Component tooltip = ayuda == null ? null : ayuda instanceof Component c ? c : texto(ayuda.toString());
        return ActionButton.create(texto(etiqueta), tooltip, ancho, DialogAction.customClick((respuesta, audiencia) -> {
            if (audiencia instanceof Player p && p.hasPermission(PERMISO)) accion.accept(p);
        }, BOTONES));
    }

    private static ActionButton formulario(String etiqueta, BiConsumer<Player, DialogResponseView> accion) {
        return ActionButton.create(texto(etiqueta), null, 150, DialogAction.customClick((respuesta, audiencia) -> {
            if (audiencia instanceof Player p && p.hasPermission(PERMISO)) accion.accept(p, respuesta);
        }, BOTONES));
    }

    private static ActionButton volver(Consumer<Player> accion) {
        return boton(GRIS + "« Volver", null, 150, accion);
    }

    private static ActionButton cerrar() {
        return ActionButton.create(texto(ROJO + "Cerrar"), null, 150, null);
    }

    private static Component texto(String legacy) {
        return TEXTO.deserialize(legacy);
    }
}
