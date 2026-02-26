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
        loadConfig(); // Cargamos los datos de la imagen
        initializeDatabase();
    }

    public void loadConfig() {
        // Leemos la sección Database1 del config.yml
        this.host = plugin.getConfig().getString("Database1.host");
        this.port = plugin.getConfig().getInt("Database1.port");
        this.database = plugin.getConfig().getString("Database1.database");
        this.username = plugin.getConfig().getString("Database1.username");
        this.password = plugin.getConfig().getString("Database1.password");
    }

    // Método para obtener una conexión fresca cada vez
    private Connection getConnection() throws SQLException {
        // URL de conexión MySQL con opciones para evitar errores de SSL y reconexión
        String url = "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false&autoReconnect=true";
        return DriverManager.getConnection(url, this.username, this.password);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabla Jugadores
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "first_join DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "team_name VARCHAR(20) DEFAULT 'ZMiembro');");

            // Tabla Misiones (Nueva)
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_missions (" +
                    "uuid VARCHAR(36), " +
                    "player_name VARCHAR(16), " +
                    "mission_id INT, " +
                    "mission_name VARCHAR(64), " +
                    "is_active BOOLEAN DEFAULT 0, " +
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

            plugin.getLogger().info("Conectado a MySQL y tablas verificadas.");

        } catch (SQLException e) {
            plugin.getLogger().severe("No se pudo conectar a MySQL. Revisa el config.yml y el nombre de la DB.");
            plugin.getLogger().severe("Error DB: " + e.getMessage());
        }
    }

    // Ya no necesitamos cerrar manual, pero lo dejamos vacío por compatibilidad
    public void closeConnection() {
    }

    // Verificar jugador
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

    // Registrar jugador
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

    public Map<Integer, MissionData> loadPlayerMissions(UUID uuid) {
        Map<Integer, MissionData> missions = new HashMap<>();
        String sql = "SELECT mission_id, is_active, is_completed, reward_claimed, progress_json FROM player_missions WHERE uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("mission_id");
                boolean active = rs.getBoolean("is_active");
                boolean completed = rs.getBoolean("is_completed");
                boolean claimed = rs.getBoolean("reward_claimed");
                String json = rs.getString("progress_json");

                missions.put(id, new MissionData(active, completed, claimed, json));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando misiones: " + e.getMessage());
        }
        return missions;
    }

    public Set<Integer> getGlobalActiveMissions() {
        Set<Integer> activeMissions = new HashSet<>();
        // Buscamos cualquier mission_id que al menos un jugador tenga como is_active = true
        String sql = "SELECT DISTINCT mission_id FROM player_missions WHERE is_active = 1";

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

    public void deactivateMissionGlobally(int missionId) {
        String sql = "UPDATE player_missions SET is_active = 0 WHERE mission_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, missionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("Error desactivando misión globalmente: " + e.getMessage());
        }
    }

   /* public void saveMissionAsync(UUID uuid, String playerName, int missionId, String missionName, MissionData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO player_missions (uuid, player_name, mission_id, mission_name, is_active, is_completed, reward_claimed, progress_json) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE player_name=?, mission_name=?, is_active=?, is_completed=?, reward_claimed=?, progress_json=?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                String json = data.getJsonProgress();

                // Insert Params
                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, missionId);
                stmt.setString(4, missionName);
                stmt.setBoolean(5, data.isActive());
                stmt.setBoolean(6, data.isCompleted());
                stmt.setBoolean(7, data.isRewardClaimed());
                stmt.setString(8, json);

                // Update Params
                stmt.setString(9, playerName);
                stmt.setString(10, missionName);
                stmt.setBoolean(11, data.isActive());
                stmt.setBoolean(12, data.isCompleted());
                stmt.setBoolean(13, data.isRewardClaimed());
                stmt.setString(14, json);

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error guardando misión " + missionId + " de " + playerName + ": " + e.getMessage());
            }
        });
    }*/

    public void savePlayerMissionsBatchSync(UUID uuid, String playerName, Map<Integer, MissionData> missionsToSave, Map<Integer, String> missionNames) {
        if (missionsToSave.isEmpty()) return;

        String sql = "INSERT INTO player_missions (uuid, player_name, mission_id, mission_name, is_active, is_completed, reward_claimed, progress_json) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name=?, mission_name=?, is_active=?, is_completed=?, reward_claimed=?, progress_json=?";

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
                stmt.setBoolean(5, data.isActive());
                stmt.setBoolean(6, data.isCompleted());
                stmt.setBoolean(7, data.isRewardClaimed());
                stmt.setString(8, json);

                stmt.setString(9, playerName);
                stmt.setString(10, missionName);
                stmt.setBoolean(11, data.isActive());
                stmt.setBoolean(12, data.isCompleted());
                stmt.setBoolean(13, data.isRewardClaimed());
                stmt.setString(14, json);

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

    public void saveBackpack(String backpackUuid, UUID ownerUuid, String ownerName, String itemName, int level, ItemStack[] items) throws SQLException {
        String contents = ItemSerializer.serialize(items);
        if (contents == null || contents.isEmpty()) return;

        String sql = "INSERT INTO player_backpacks (backpack_uuid, owner_uuid, owner_name, item_name, item_level, contents) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE item_name=?, item_level=?, contents=?, updated_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Valores para INSERT (Nueva mochila)
            stmt.setString(1, backpackUuid);
            stmt.setString(2, ownerUuid.toString());
            stmt.setString(3, ownerName);
            stmt.setString(4, itemName);
            stmt.setInt(5, level);
            stmt.setString(6, contents);

            // Valores para UPDATE (Mochila existente)
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

    // Clase auxiliar para guardar info básica en la GUI de admin
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
        return null; // Retorna null si no se encuentra en la base de datos
    }

    public void reload() {
        loadConfig();
        initializeDatabase();
        plugin.getLogger().info("¡Configuración de base de datos recargada!");
    }
}