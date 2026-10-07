package Handlers;

import Events.MissionSystem.MissionData;
import items.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private String host, database, username, password;
    private int port;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        connectWithRetry(3);
    }

    public void loadConfig() {
        this.host = plugin.getConfig().getString("Database1.host");
        this.port = plugin.getConfig().getInt("Database1.port");
        this.database = plugin.getConfig().getString("Database1.database");
        this.username = plugin.getConfig().getString("Database1.username");
        this.password = plugin.getConfig().getString("Database1.password");
    }

    // Abre una conexión nueva cada vez (el driver de MySQL ya viene con Paper)
    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database +
                "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000";
        return DriverManager.getConnection(url, this.username, this.password);
    }

    // Intenta conectar y crear las tablas hasta 3 veces antes de rendirse
    private void connectWithRetry(int maxRetries) {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                initializeDatabase();
                return;
            } catch (SQLException e) {
                attempt++;
                plugin.getLogger().warning("Intento " + attempt + " fallido al conectar a MySQL: " + e.getMessage());
                if (attempt >= maxRetries) {
                    plugin.getLogger().severe("¡No se pudo conectar a la base de datos después de " + maxRetries + " intentos!");
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    // Crea las tablas de jugadores, misiones, mochilas e inventarios de eventos si no existen
    private void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "first_join DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "team_name VARCHAR(20) DEFAULT 'ZMiembro');");

            // Viciont: el saldo se deriva del contenido físico de los monederos.
            for (String column : List.of("dinocoins", "dinofichas")) {
                try {
                    stmt.executeUpdate("ALTER TABLE players ADD COLUMN " + column + " INT DEFAULT 0");
                } catch (SQLException error) {
                    if (error.getErrorCode() != 1060) throw error; // MySQL: columna ya existente.
                }
            }

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS global_missions (" +
                    "mission_id INT PRIMARY KEY, " +
                    "is_active BOOLEAN DEFAULT 1, " +
                    "activation_date DATETIME DEFAULT CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_missions (" +
                    "uuid VARCHAR(36), " +
                    "player_name VARCHAR(16), " +
                    "mission_id INT, " +
                    "mission_name VARCHAR(64), " +
                    "is_completed BOOLEAN DEFAULT 0, " +
                    "reward_claimed BOOLEAN DEFAULT 0, " +
                    "progress_json TEXT, " +
                    "PRIMARY KEY (uuid, mission_id));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_backpacks (" +
                    "backpack_uuid VARCHAR(36) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36), " +
                    "owner_name VARCHAR(16), " +
                    "item_name VARCHAR(128), " +
                    "item_level INT DEFAULT 1, " +
                    "contents LONGTEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS event_inventories (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "inventory_contents LONGTEXT, " +
                    "saved_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

            // Trabajos: el nivel y la experiencia de cada trabajo, y cuál tiene ahora y desde cuándo
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "uuid VARCHAR(36), " +
                    "job VARCHAR(20), " +
                    "level INT DEFAULT 0, " +
                    "xp DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY (uuid, job));");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_job_state (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16), " +
                    "active_job VARCHAR(20), " +
                    "joined_at BIGINT DEFAULT 0);");

            plugin.getLogger().info("Conectado a MySQL y tablas verificadas.");

        }
    }

    public void closeConnection() {
    }

    // INSERT IGNORE: si ya tenía un inventario guardado no se pisa
    public boolean saveEventInventory(UUID uuid, String playerName, ItemStack[] contents) {
        String data = ItemSerializer.serialize(contents);
        if (data == null || data.isEmpty()) return false;

        String sql = "INSERT IGNORE INTO event_inventories (uuid, player_name, inventory_contents) VALUES (?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, data);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando inventario: " + e.getMessage());
            return false;
        }
    }

    public ItemStack[] getEventInventory(UUID uuid) {
        String sql = "SELECT inventory_contents FROM event_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("inventory_contents");
                if (data != null && !data.isEmpty()) {
                    return ItemSerializer.deserialize(data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando inventario de evento: " + e.getMessage());
        }
        return null;
    }

    public void deleteEventInventory(UUID uuid) {
        String sql = "DELETE FROM event_inventories WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error borrando inventario de evento: " + e.getMessage());
        }
    }

    public boolean hasJoinedBefore(UUID uuid) {
        String sql = "SELECT uuid FROM players WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            plugin.getLogger().severe("¡ERROR CRÍTICO EN BASE DE DATOS! " + e.getMessage());
            return true;
        }
    }

    public void registerPlayer(UUID uuid, String name, String teamName) {
        String sql = "INSERT INTO players (uuid, name, team_name) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setString(3, teamName);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<Integer> getGlobalActiveMissions() {
        Set<Integer> activeMissions = new HashSet<>();
        String sql = "SELECT mission_id FROM global_missions WHERE is_active = 1";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activeMissions.add(rs.getInt("mission_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando misiones activas globales: " + e.getMessage());
        }
        return activeMissions;
    }

    public void setMissionGlobalState(int missionId, boolean active) {
        String sql = "INSERT INTO global_missions (mission_id, is_active) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE is_active = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, missionId);
            stmt.setBoolean(2, active);
            stmt.setBoolean(3, active);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Error actualizando estado global de misión: " + e.getMessage());
        }
    }

    // Borra el progreso de misiones de todos los jugadores y deja todas desactivadas
    public void deleteAllMissionData() {
        try (Connection conn = getConnection();
             PreparedStatement players = conn.prepareStatement("DELETE FROM player_missions");
             PreparedStatement global = conn.prepareStatement("DELETE FROM global_missions")) {
            players.executeUpdate();
            global.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error borrando los datos de misiones: " + e.getMessage());
        }
    }

    public Map<Integer, MissionData> loadPlayerMissions(UUID uuid) {
        Map<Integer, MissionData> missions = new HashMap<>();
        String sql = "SELECT mission_id, is_completed, reward_claimed, progress_json FROM player_missions WHERE uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("mission_id");
                boolean completed = rs.getBoolean("is_completed");
                boolean claimed = rs.getBoolean("reward_claimed");
                String json = rs.getString("progress_json");

                missions.put(id, new MissionData(false, completed, claimed, json));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando misiones: " + e.getMessage());
        }
        return missions;
    }

    // Guarda en un solo batch todas las misiones que cambiaron
    public void savePlayerMissionsBatchSync(UUID uuid, String playerName, Map<Integer, MissionData> missionsToSave, Map<Integer, String> missionNames) {
        if (missionsToSave.isEmpty()) return;

        String sql = "INSERT INTO player_missions (uuid, player_name, mission_id, mission_name, is_completed, reward_claimed, progress_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=?, mission_name=?, is_completed=?, reward_claimed=?, progress_json=?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Map.Entry<Integer, MissionData> entry : missionsToSave.entrySet()) {
                int missionId = entry.getKey();
                MissionData data = entry.getValue();
                String json = data.getJsonProgress();
                String missionName = missionNames.getOrDefault(missionId, "Unknown");

                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, missionId);
                stmt.setString(4, missionName);
                stmt.setBoolean(5, data.isCompleted());
                stmt.setBoolean(6, data.isRewardClaimed());
                stmt.setString(7, json);

                stmt.setString(8, playerName);
                stmt.setString(9, missionName);
                stmt.setBoolean(10, data.isCompleted());
                stmt.setBoolean(11, data.isRewardClaimed());
                stmt.setString(12, json);

                stmt.addBatch();
            }

            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error en guardado Batch (Masivo) para " + playerName + ": " + e.getMessage());
        }
    }

    public ItemStack[] loadBackpackContents(String backpackUuid) {
        String sql = "SELECT contents FROM player_backpacks WHERE backpack_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackUuid);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String data = rs.getString("contents");
                if (data != null && !data.isEmpty()) {
                    return ItemSerializer.deserialize(data);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error cargando mochila " + backpackUuid + ": " + e.getMessage());
        }
        return null;
    }

    // Como loadBackpackContents, pero un error de la base de datos se avisa en vez de parecer una mochila vacía
    // (si no, quien la modifica después la guardaría vacía). Devuelve null si la mochila todavía no se guardó
    public ItemStack[] loadBackpackContentsStrict(String backpackUuid) throws SQLException {
        String sql = "SELECT contents FROM player_backpacks WHERE backpack_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackUuid);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) return null;
            String data = rs.getString("contents");
            if (data == null || data.isEmpty()) return null;
            ItemStack[] items = ItemSerializer.deserialize(data);
            if (items == null) throw new SQLException("contenido ilegible de " + backpackUuid);
            return items;
        }
    }

    // Los monederos pasan a ser de quien los lleva encima: si alguien regala o saquea uno, deja de contar para el
    // dueño anterior
    public void claimWallets(UUID ownerUuid, String ownerName, Collection<String> walletUuids, int walletLevel) {
        if (walletUuids.isEmpty()) return;
        String marks = String.join(",", Collections.nCopies(walletUuids.size(), "?"));
        String sql = "UPDATE player_backpacks SET owner_uuid = ?, owner_name = ? WHERE item_level = ? AND owner_uuid <> ? AND backpack_uuid IN (" + marks + ")";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, ownerName);
            stmt.setInt(3, walletLevel);
            stmt.setString(4, ownerUuid.toString());
            int i = 5;
            for (String uuid : walletUuids) stmt.setString(i++, uuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("No se pudo actualizar el dueño de los monederos de " + ownerName + ": " + e.getMessage());
        }
    }

    // Inserta la mochila o actualiza su contenido si ya existe
    public void saveBackpack(String backpackUuid, UUID ownerUuid, String ownerName, String itemName, int level, ItemStack[] items) throws SQLException {
        String contents = ItemSerializer.serialize(items);
        if (contents == null || contents.isEmpty()) return;

        String sql = "INSERT INTO player_backpacks (backpack_uuid, owner_uuid, owner_name, item_name, item_level, contents) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE item_name=?, item_level=?, contents=?, updated_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackUuid);
            stmt.setString(2, ownerUuid.toString());
            stmt.setString(3, ownerName);
            stmt.setString(4, itemName);
            stmt.setInt(5, level);
            stmt.setString(6, contents);

            stmt.setString(7, itemName);
            stmt.setInt(8, level);
            stmt.setString(9, contents);

            stmt.executeUpdate();
        }
    }

    public boolean deleteBackpack(String backpackId) {
        String sql = "DELETE FROM player_backpacks WHERE backpack_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, backpackId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class BackpackInfo {
        public String uuid;
        public String itemName;
        public int level;
        public String updatedAt;

        public BackpackInfo(String uuid, String itemName, int level, String updatedAt) {
            this.uuid = uuid;
            this.itemName = itemName;
            this.level = level;
            this.updatedAt = updatedAt;
        }
    }

    public List<BackpackInfo> getPlayerBackpacks(UUID ownerUuid) {
        List<BackpackInfo> list = new ArrayList<>();
        String sql = "SELECT backpack_uuid, item_name, item_level, updated_at FROM player_backpacks WHERE owner_uuid = ? ORDER BY updated_at DESC";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new BackpackInfo(
                        rs.getString("backpack_uuid"),
                        rs.getString("item_name"),
                        rs.getInt("item_level"),
                        rs.getString("updated_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public UUID getUuidByName(String playerName) {
        String sql = "SELECT uuid FROM players WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error buscando UUID por nombre: " + e.getMessage());
        }
        return null;
    }

    public void reload() {
        loadConfig();
        connectWithRetry(3);
        plugin.getLogger().info("¡Configuración de base de datos recargada!");
    }

    public int getDinoCoins(UUID uuid) { return getCurrencyBalance(uuid, "dinocoins"); }
    public int getDinoFichas(UUID uuid) { return getCurrencyBalance(uuid, "dinofichas"); }

    private int getCurrencyBalance(UUID uuid, String column) {
        String sql = "SELECT " + column + " FROM players WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(column);
            }
        } catch (SQLException error) {
            plugin.getLogger().severe("Error obteniendo " + column + ": " + error.getMessage());
        }
        return 0;
    }

    public void setCurrencyBalances(UUID uuid, int coins, int tokens) {
        String sql = "UPDATE players SET dinocoins = ?, dinofichas = ? WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, coins);
            stmt.setInt(2, tokens);
            stmt.setString(3, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException error) {
            plugin.getLogger().severe("Error registrando monedas: " + error.getMessage());
        }
    }


    // ---------------------------------------------------------------- Trabajos

    public record JobProgress(int level, double xp) {}

    public record JobsData(String activeJob, long joinedAt, Map<String, JobProgress> progress) {}

    // Lanza la excepción si MySQL falla: así el jugador no gana XP sobre datos vacíos que después pisarían los reales
    public JobsData loadJobs(UUID uuid) throws SQLException {
        Map<String, JobProgress> progress = new HashMap<>();
        String activeJob = null;
        long joinedAt = 0;
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT active_job, joined_at FROM player_job_state WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    activeJob = rs.getString("active_job");
                    joinedAt = rs.getLong("joined_at");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT job, level, xp FROM player_jobs WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) progress.put(rs.getString("job"), new JobProgress(rs.getInt("level"), rs.getDouble("xp")));
            }
        }
        return new JobsData(activeJob, joinedAt, progress);
    }

    public void saveJobs(UUID uuid, String playerName, JobsData data) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement state = conn.prepareStatement("INSERT INTO player_job_state (uuid, player_name, active_job, joined_at) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), active_job=VALUES(active_job), joined_at=VALUES(joined_at)");
                 PreparedStatement jobs = conn.prepareStatement("INSERT INTO player_jobs (uuid, job, level, xp) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE level=VALUES(level), xp=VALUES(xp)")) {
                state.setString(1, uuid.toString());
                state.setString(2, playerName);
                state.setString(3, data.activeJob());
                state.setLong(4, data.joinedAt());
                state.executeUpdate();
                for (Map.Entry<String, JobProgress> entry : data.progress().entrySet()) {
                    jobs.setString(1, uuid.toString());
                    jobs.setString(2, entry.getKey());
                    jobs.setInt(3, entry.getValue().level());
                    jobs.setDouble(4, entry.getValue().xp());
                    jobs.addBatch();
                }
                jobs.executeBatch();
                conn.commit();
            } catch (SQLException error) {
                conn.rollback();
                throw error;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // /trabajos reset: todos empiezan de cero
    public void deleteAllJobs() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM player_jobs");
            stmt.executeUpdate("DELETE FROM player_job_state");
        }
    }

}
