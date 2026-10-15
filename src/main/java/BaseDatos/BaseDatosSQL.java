package BaseDatos;

import Handlers.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

// Lo que /basedatos le pide a MySQL: tablas, columnas, filas por página y los cambios a mano. Los nombres de tabla y de
// columna salen siempre de information_schema (nunca de lo que escribe el jugador) y van entre comillas invertidas;
// los valores van siempre como parámetros
public final class BaseDatosSQL {

    // Las tablas que crea QuasoPlugin (en este orden salen en el menú). El reset total solo vacía estas
    public static final List<String> PROPIAS = List.of("players", "player_missions", "global_missions", "player_jobs",
            "player_job_state", "player_backpacks", "event_inventories");
    public static final int POR_PAGINA = 8;
    // Más largo que esto no se edita desde el diálogo (los inventarios y mochilas guardados ocupan miles de caracteres)
    public static final int MAX_EDITABLE = 512;
    // Lo que se escribe en un campo para dejarlo nulo
    public static final String NULO = "NULL";

    public record Columna(String nombre, String tipo, boolean nula, boolean auto, String defecto, boolean clave) {

        // Los BOOLEAN de MySQL son tinyint(1)
        public boolean booleana() {
            return tipo.startsWith("tinyint(1)") || tipo.equals("boolean") || tipo.equals("bool");
        }

        public boolean entera() {
            return !booleana() && (tipo.startsWith("int") || tipo.startsWith("bigint") || tipo.startsWith("smallint")
                    || tipo.startsWith("mediumint") || tipo.startsWith("tinyint"));
        }

        public boolean decimal() {
            return tipo.startsWith("double") || tipo.startsWith("float") || tipo.startsWith("decimal") || tipo.startsWith("real");
        }

        // Los textos largos no entran en la búsqueda (son inventarios en base64)
        public boolean larga() {
            return tipo.contains("longtext") || tipo.contains("blob") || tipo.contains("mediumtext");
        }

        // El largo máximo de un varchar(N) o char(N); para lo demás, el del diálogo
        public int largoMaximo() {
            int abre = tipo.indexOf('('), cierra = tipo.indexOf(')');
            if ((tipo.startsWith("varchar") || tipo.startsWith("char")) && abre > 0 && cierra > abre) {
                try {
                    return Math.min(MAX_EDITABLE, Integer.parseInt(tipo.substring(abre + 1, cierra)));
                } catch (NumberFormatException ignored) {
                }
            }
            return MAX_EDITABLE;
        }
    }

    public record Tabla(String nombre, long filas, long bytes) {
        public boolean propia() {
            return PROPIAS.contains(nombre);
        }
    }

    public record Pagina(List<Map<String, String>> filas, long total) {}

    public record Estado(long respuestaMs, String version, long uptimeSegundos, long conexiones, List<Tabla> tablas,
                         List<String> faltan) {
        public long filas() {
            return tablas.stream().mapToLong(Tabla::filas).sum();
        }

        public long bytes() {
            return tablas.stream().mapToLong(Tabla::bytes).sum();
        }
    }

    // Una tabla completa (para las copias de seguridad)
    public record Contenido(List<String> columnas, List<List<String>> filas) {}

    private final DatabaseManager db;

    public BaseDatosSQL(DatabaseManager db) {
        this.db = db;
    }

    // ---------------------------------------------------------------- Lectura

    public Estado estado() throws SQLException {
        long inicio = System.nanoTime();
        return db.consultar(conn -> {
            String version;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT VERSION()")) {
                rs.next();
                version = rs.getString(1);
            }
            long respuesta = (System.nanoTime() - inicio) / 1_000_000;
            long uptime = variable(conn, "Uptime"), conexiones = variable(conn, "Threads_connected");
            List<Tabla> tablas = tablas(conn);
            List<String> faltan = new ArrayList<>(PROPIAS);
            for (Tabla tabla : tablas) faltan.remove(tabla.nombre());
            return new Estado(respuesta, version, uptime, conexiones, tablas, faltan);
        });
    }

    // Un valor de SHOW GLOBAL STATUS (-1 si el usuario no tiene permiso para verlo)
    private static long variable(Connection conn, String nombre) {
        try (PreparedStatement st = conn.prepareStatement("SHOW GLOBAL STATUS LIKE ?")) {
            st.setString(1, nombre);
            ResultSet rs = st.executeQuery();
            return rs.next() ? Long.parseLong(rs.getString(2)) : -1;
        } catch (SQLException | NumberFormatException e) {
            return -1;
        }
    }

    public List<Tabla> tablas() throws SQLException {
        return db.consultar(BaseDatosSQL::tablas);
    }

    // Las filas se cuentan con COUNT(*): el TABLE_ROWS de InnoDB es aproximado
    private static List<Tabla> tablas(Connection conn) throws SQLException {
        Map<String, Long> bytes = new LinkedHashMap<>();
        try (PreparedStatement st = conn.prepareStatement("SELECT TABLE_NAME, COALESCE(DATA_LENGTH, 0) + COALESCE(INDEX_LENGTH, 0) "
                + "FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'")) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) bytes.put(rs.getString(1), rs.getLong(2));
        }
        List<Tabla> tablas = new ArrayList<>();
        try (Statement st = conn.createStatement()) {
            for (Map.Entry<String, Long> entry : bytes.entrySet()) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + id(entry.getKey()))) {
                    rs.next();
                    tablas.add(new Tabla(entry.getKey(), rs.getLong(1), entry.getValue()));
                }
            }
        }
        tablas.sort(Comparator.comparingInt((Tabla t) -> t.propia() ? PROPIAS.indexOf(t.nombre()) : PROPIAS.size())
                .thenComparing(Tabla::nombre));
        return tablas;
    }

    public List<Columna> columnas(String tabla) throws SQLException {
        return db.consultar(conn -> {
            List<Columna> columnas = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement("SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, EXTRA, COLUMN_DEFAULT, "
                    + "COLUMN_KEY FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
                st.setString(1, tabla);
                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    String extra = rs.getString(4);
                    columnas.add(new Columna(rs.getString(1), rs.getString(2).toLowerCase(Locale.ROOT), "YES".equals(rs.getString(3)),
                            extra != null && extra.toLowerCase(Locale.ROOT).contains("auto_increment"), rs.getString(5),
                            "PRI".equals(rs.getString(6))));
                }
            }
            if (columnas.isEmpty()) throw new SQLException("La tabla " + tabla + " ya no existe.");
            return columnas;
        });
    }

    // Una página de filas, ordenada por la clave. El filtro busca el texto en todas las columnas que no son largas
    public Pagina filas(String tabla, List<Columna> columnas, int pagina, String filtro) throws SQLException {
        return db.consultar(conn -> {
            List<Columna> buscables = columnas.stream().filter(c -> !c.larga()).toList();
            boolean filtra = filtro != null && !filtro.isBlank() && !buscables.isEmpty();
            String donde = filtra ? " WHERE CONCAT_WS(' ', " + buscables.stream().map(c -> id(c.nombre()))
                    .collect(Collectors.joining(", ")) + ") LIKE ?" : "";
            String patron = filtra ? "%" + escaparLike(filtro.trim()) + "%" : null;

            long total;
            try (PreparedStatement st = conn.prepareStatement("SELECT COUNT(*) FROM " + id(tabla) + donde)) {
                if (filtra) st.setString(1, patron);
                ResultSet rs = st.executeQuery();
                rs.next();
                total = rs.getLong(1);
            }
            List<Map<String, String>> filas = new ArrayList<>();
            try (PreparedStatement st = conn.prepareStatement("SELECT * FROM " + id(tabla) + donde + ordenPor(columnas) + " LIMIT ? OFFSET ?")) {
                int i = 1;
                if (filtra) st.setString(i++, patron);
                st.setInt(i++, POR_PAGINA);
                st.setInt(i, Math.max(0, pagina) * POR_PAGINA);
                ResultSet rs = st.executeQuery();
                while (rs.next()) filas.add(leer(rs, columnas));
            }
            return new Pagina(filas, total);
        });
    }

    // La fila tal como está ahora (null si ya no existe)
    public Map<String, String> fila(String tabla, List<Columna> columnas, Map<String, String> clave) throws SQLException {
        return db.consultar(conn -> {
            List<Columna> claves = claves(columnas);
            try (PreparedStatement st = conn.prepareStatement("SELECT * FROM " + id(tabla) + donde(claves) + " LIMIT 1")) {
                ponerClave(st, 1, claves, clave);
                ResultSet rs = st.executeQuery();
                return rs.next() ? leer(rs, columnas) : null;
            }
        });
    }

    public Contenido todo(String tabla) throws SQLException {
        return db.consultar(conn -> {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + id(tabla))) {
                int n = rs.getMetaData().getColumnCount();
                List<String> columnas = new ArrayList<>();
                for (int i = 1; i <= n; i++) columnas.add(rs.getMetaData().getColumnName(i));
                List<List<String>> filas = new ArrayList<>();
                while (rs.next()) {
                    List<String> fila = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) fila.add(rs.getString(i));
                    filas.add(fila);
                }
                return new Contenido(columnas, filas);
            }
        });
    }

    // ---------------------------------------------------------------- Cambios

    // Solo cambia las columnas que vienen en "nuevos" (ya convertidas con convertir)
    public int actualizar(String tabla, List<Columna> columnas, Map<String, String> clave, Map<String, String> nuevos) throws SQLException {
        if (nuevos.isEmpty()) return 0;
        return db.consultar(conn -> {
            List<Columna> claves = claves(columnas);
            String set = nuevos.keySet().stream().map(c -> id(c) + " = ?").collect(Collectors.joining(", "));
            try (PreparedStatement st = conn.prepareStatement("UPDATE " + id(tabla) + " SET " + set + donde(claves) + " LIMIT 1")) {
                int i = 1;
                for (String valor : nuevos.values()) st.setString(i++, valor);
                ponerClave(st, i, claves, clave);
                return st.executeUpdate();
            }
        });
    }

    public void insertar(String tabla, Map<String, String> valores) throws SQLException {
        db.consultar(conn -> {
            String sql = valores.isEmpty() ? "INSERT INTO " + id(tabla) + " () VALUES ()"
                    : "INSERT INTO " + id(tabla) + " (" + valores.keySet().stream().map(BaseDatosSQL::id).collect(Collectors.joining(", "))
                    + ") VALUES (" + String.join(", ", java.util.Collections.nCopies(valores.size(), "?")) + ")";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                int i = 1;
                for (String valor : valores.values()) st.setString(i++, valor);
                return st.executeUpdate();
            }
        });
    }

    public int borrar(String tabla, List<Columna> columnas, Map<String, String> clave) throws SQLException {
        return db.consultar(conn -> {
            List<Columna> claves = claves(columnas);
            try (PreparedStatement st = conn.prepareStatement("DELETE FROM " + id(tabla) + donde(claves) + " LIMIT 1")) {
                ponerClave(st, 1, claves, clave);
                return st.executeUpdate();
            }
        });
    }

    // DELETE en vez de TRUNCATE: el usuario del panel a veces no tiene permiso de DROP
    public int vaciar(String tabla) throws SQLException {
        return db.consultar(conn -> {
            try (Statement st = conn.createStatement()) {
                return st.executeUpdate("DELETE FROM " + id(tabla));
            }
        });
    }

    // Vuelve a poner las filas de una copia: las que tienen la misma clave se reemplazan, las demás no se tocan
    public int restaurar(String tabla, Contenido contenido) throws SQLException {
        if (contenido.filas().isEmpty()) return 0;
        return db.consultar(conn -> {
            String sql = "REPLACE INTO " + id(tabla) + " (" + contenido.columnas().stream().map(BaseDatosSQL::id)
                    .collect(Collectors.joining(", ")) + ") VALUES (" + String.join(", ",
                    java.util.Collections.nCopies(contenido.columnas().size(), "?")) + ")";
            conn.setAutoCommit(false);
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                for (List<String> fila : contenido.filas()) {
                    for (int i = 0; i < fila.size(); i++) st.setString(i + 1, fila.get(i));
                    st.addBatch();
                }
                st.executeBatch();
                conn.commit();
                return contenido.filas().size();
            } catch (SQLException error) {
                conn.rollback();
                throw error;
            } finally {
                conn.setAutoCommit(true);
            }
        });
    }

    // ---------------------------------------------------------------- Ayudas (sin base de datos, con tests)

    public static String id(String nombre) {
        return "`" + nombre.replace("`", "``") + "`";
    }

    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // Las columnas que identifican una fila: la clave primaria, o todas si la tabla no tiene
    public static List<Columna> claves(List<Columna> columnas) {
        List<Columna> claves = columnas.stream().filter(Columna::clave).toList();
        return claves.isEmpty() ? columnas : claves;
    }

    public static Map<String, String> clave(List<Columna> columnas, Map<String, String> fila) {
        Map<String, String> clave = new LinkedHashMap<>();
        for (Columna c : claves(columnas)) clave.put(c.nombre(), fila.get(c.nombre()));
        return clave;
    }

    // <=> compara también los NULL
    static String donde(List<Columna> claves) {
        return " WHERE " + claves.stream().map(c -> id(c.nombre()) + " <=> ?").collect(Collectors.joining(" AND "));
    }

    private static void ponerClave(PreparedStatement st, int desde, List<Columna> claves, Map<String, String> clave) throws SQLException {
        for (Columna c : claves) st.setString(desde++, clave.get(c.nombre()));
    }

    static String ordenPor(List<Columna> columnas) {
        List<Columna> claves = columnas.stream().filter(Columna::clave).toList();
        List<Columna> orden = claves.isEmpty() ? columnas.subList(0, 1) : claves;
        return " ORDER BY " + orden.stream().map(c -> id(c.nombre())).collect(Collectors.joining(", "));
    }

    private static Map<String, String> leer(ResultSet rs, List<Columna> columnas) throws SQLException {
        Map<String, String> fila = new LinkedHashMap<>();
        for (Columna c : columnas) fila.put(c.nombre(), rs.getString(c.nombre()));
        return fila;
    }

    // Lo que escribió el OP pasado al valor que se guarda; null es NULL. Lanza IllegalArgumentException con el motivo
    public static String convertir(Columna columna, String texto) {
        if (texto == null || texto.equals(NULO)) {
            if (!columna.nula()) throw new IllegalArgumentException(columna.nombre() + " no puede quedar en NULL.");
            return null;
        }
        String limpio = texto.trim();
        if (columna.booleana()) {
            return switch (limpio.toLowerCase(Locale.ROOT)) {
                case "1", "true", "si", "sí", "verdadero" -> "1";
                case "0", "false", "no", "falso" -> "0";
                default -> throw new IllegalArgumentException(columna.nombre() + " tiene que ser 1 o 0 (sí o no).");
            };
        }
        if (columna.entera() || columna.decimal()) {
            if (limpio.isEmpty() && columna.nula()) return null;
            try {
                BigDecimal numero = new BigDecimal(limpio.replace(',', '.'));
                if (columna.entera()) return numero.toBigIntegerExact().toString();
                return numero.toPlainString();
            } catch (NumberFormatException | ArithmeticException e) {
                throw new IllegalArgumentException(columna.nombre() + " tiene que ser un número" + (columna.entera() ? " entero." : "."));
            }
        }
        if (texto.length() > columna.largoMaximo())
            throw new IllegalArgumentException(columna.nombre() + " admite hasta " + columna.largoMaximo() + " caracteres.");
        return texto;
    }

    // Un texto corto para el botón de cada fila: el nombre del jugador (si la tabla tiene) y la clave
    public static String resumen(List<Columna> columnas, Map<String, String> fila) {
        List<String> partes = new ArrayList<>();
        for (Columna c : columnas) {
            if (c.nombre().endsWith("name") && fila.get(c.nombre()) != null) {
                partes.add(fila.get(c.nombre()));
                break;
            }
        }
        for (Columna c : claves(columnas)) {
            if (partes.size() >= 3) break;
            if (c.larga()) continue;
            String valor = fila.get(c.nombre());
            partes.add(valor == null ? NULO : corto(valor));
        }
        String texto = String.join(" · ", partes);
        return texto.length() > 40 ? texto.substring(0, 39) + "…" : texto;
    }

    // Los UUID se cortan a 8 caracteres; lo demás a 18
    static String corto(String valor) {
        if (valor.length() == 36 && valor.charAt(8) == '-') return valor.substring(0, 8) + "…";
        return valor.length() > 18 ? valor.substring(0, 17) + "…" : valor;
    }

    public static String tamano(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0).replace('.', ',');
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024)).replace('.', ',');
    }
}
