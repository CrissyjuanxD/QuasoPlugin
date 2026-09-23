package SistemaTumbas;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class GravesManager {
    private final JavaPlugin plugin;
    private final Map<UUID, Grave> activeGraves = new HashMap<>();

    private File configFile;
    private FileConfiguration config;
    private File dataFile;
    private FileConfiguration data;

    private boolean anyoneCanOpen;
    private int expiryMinutes;

    public GravesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadData();
        startExpiryTask();
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        configFile = new File(plugin.getDataFolder(), "tumbas_config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
                    writer.println("# ¿Puede cualquier persona abrir una tumba ajena? (Si es false, solo el dueño puede)");
                    writer.println("anyone-can-open: false");
                    writer.println("");
                    writer.println("# Minutos antes de que la tumba desaparezca y suelte las cosas al suelo");
                    writer.println("expiry-minutes: 30");
                }
            } catch (IOException e) { e.printStackTrace(); }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        anyoneCanOpen = config.getBoolean("anyone-can-open", false);
        expiryMinutes = config.getInt("expiry-minutes", 30);
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "tumbas_data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        activeGraves.clear();

        if (data.contains("graves")) {
            for (String key : data.getConfigurationSection("graves").getKeys(false)) {
                UUID id = UUID.fromString(key);
                UUID owner = UUID.fromString(data.getString("graves." + key + ".owner"));
                String ownerName = data.getString("graves." + key + ".ownerName");
                Location loc = data.getLocation("graves." + key + ".location");
                long creation = data.getLong("graves." + key + ".creationTime");
                long expiry = data.getLong("graves." + key + ".expiryTime");
                List<ItemStack> items = (List<ItemStack>) data.getList("graves." + key + ".items");

                Grave grave = new Grave(id, owner, ownerName, loc, creation, expiry, items);
                activeGraves.put(id, grave);

                cleanupVisuals(id, loc);
                spawnGraveVisuals(id, ownerName, loc, creation);
            }
        }
    }

    public void saveData() {
        data.set("graves", null);
        for (Grave grave : activeGraves.values()) {
            String path = "graves." + grave.getId().toString();
            data.set(path + ".owner", grave.getOwner().toString());
            data.set(path + ".ownerName", grave.getOwnerName());
            data.set(path + ".location", grave.getLocation());
            data.set(path + ".creationTime", grave.getCreationTime());
            data.set(path + ".expiryTime", grave.getExpiryTime());
            data.set(path + ".items", grave.getItems());
        }
        try { data.save(dataFile); } catch (IOException ignored) {}
    }

    public void createGrave(Player player, List<ItemStack> items) {
        UUID id = UUID.randomUUID();

        Location loc = player.getLocation();
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 1.0, loc.getBlockZ() + 0.5);

        if (blockLoc.getY() < blockLoc.getWorld().getMinHeight()) {
            blockLoc.setY(blockLoc.getWorld().getMinHeight() + 5);
        }

        long creationTime = System.currentTimeMillis();
        long expiryTime = creationTime + (expiryMinutes * 60 * 1000L);

        Grave grave = new Grave(id, player.getUniqueId(), player.getName(), blockLoc, creationTime, expiryTime, items);
        activeGraves.put(id, grave);
        saveData();

        spawnGraveVisuals(id, player.getName(), blockLoc, creationTime);
    }

    public void createFakeGrave(String fakeName, Location loc) {
        UUID id = UUID.randomUUID();
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 1.0, loc.getBlockZ() + 0.5);
        long creationTime = System.currentTimeMillis();
        long expiryTime = creationTime + (expiryMinutes * 60 * 1000L);

        Grave grave = new Grave(id, UUID.randomUUID(), fakeName, blockLoc, creationTime, expiryTime, new ArrayList<>());
        activeGraves.put(id, grave);
        saveData();

        spawnGraveVisuals(id, fakeName, blockLoc, creationTime);
    }

    private void spawnGraveVisuals(UUID graveId, String playerName, Location loc, long creationTime) {
        Date date = new Date(creationTime);
        String dayMonthYear = new SimpleDateFormat("dd/MM/yyyy").format(date);
        String hour = new SimpleDateFormat("HH:mm").format(date);

        String tag = "grave_" + graveId.toString();
        String timerTag = "timer_" + graveId.toString();

        String interactionCmd = String.format("execute in %s positioned %f %f %f run summon interaction ~ ~0.5 ~ {width:1f,height:1.2f,Tags:[\"%s\"]}",
                loc.getWorld().getKey().toString(), loc.getX(), loc.getY(), loc.getZ(), tag);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), interactionCmd);

        String rawNbt = "{Tags:[\"" + tag + "\"],Passengers:[" +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-706285507,552059348,1919193404,638722781],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTcxOTI2OCwKICAicHJvZmlsZUlkIiA6ICJjM2ZmNTY5OWZlNWI0OTY2YTYzYzdhMTEzNTBjZGIyNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWNobzkwMDAiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWRkNTZkYjVhYWE3MDM3MDA1MzI2OTgzZjMzZGY3OTRlYzJkMzEzZGE1ZGNmNjQ3ZWRkNWVhYWRiMzI0NWZiOCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.3671875f,0f,1f,0f,0.703125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-44807437,1502350709,-1118570811,-56742203],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTcyNTU4NCwKICAicHJvZmlsZUlkIiA6ICJhYzY1NDYwOWVkZjM0ODhmOTM0ZWNhMDRmNjlkNGIwMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJzcGFjZUd1cmxTa3kiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVjN2FkZmNkZDkzMTEzMjk3ZGViZjE3YjVlNGYxMzZiYWE5MTU2MzQxNjcxODU2ODEwMDExYzllYmZlMjBmYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.5f,0f,0f,0.7421875f,0f,1f,0f,0.703125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1871777221,-2134856434,551084704,1439255443],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIyNDcyNiwKICAicHJvZmlsZUlkIiA6ICJmZDlhMmNhNGNhMWM0YTkwYWEyODkyNThkYmM3MmFiNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJJRnJhbmtvWCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8xMDg2YTBkNDYzZGM3OTEzZDBjOTRiODM3MzkzZWJhZjcyOGM0YzM1NjNlYzQwYjMwYWJhYjU0MmMwZTFmMTA3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.4921875f,0f,0.25f,0f,0.953125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1495279794,-16549190,-867311589,-900151558],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIyOTc4NywKICAicHJvZmlsZUlkIiA6ICI3ZGEyYWIzYTkzY2E0OGVlODMwNDhhZmMzYjgwZTY4ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHb2xkYXBmZWwiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTQwZjcyNmZiN2MxNGMyNDQ3YWIzODQ5ZDU5MzVmYTRmNmU1NmYzZWIwNDE5MTBiNzc3NGU3NDdlNGM1YTk2OSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.5f,0f,0f,0.7421875f,0f,0.25f,0f,0.828125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-542678401,1263327314,1384696590,-1535808441],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTIzNzAzNCwKICAicHJvZmlsZUlkIiA6ICJiZTQxM2Y4M2Y4ZWE0MjE0OGMwMjk0YTJiYzIyN2U2NSIsCiAgInByb2ZpbGVOYW1lIiA6ICJGaWdodGJveTEwMyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83YmJmZDQyOTBhZjFlYTViZGZmNzYxY2VkMDk5ZjdiYjRkYzZjMGJkMGRjYjk1ZmVlMjdkYTExM2RmMTEyZjljIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=\"}]}}},item_display:\"none\",transformation:[-1f,0f,0f,0.3671875f,0f,0.25f,0f,0.828125f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1680774573,1407103419,1238979372,-1645546705],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTI0MjY1NiwKICAicHJvZmlsZUlkIiA6ICI3YTVkYmRlNDk0NWU0YTE4Yjg2OWY1MGY1NTJjNjlkYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdWtraXRBUEkiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JjYjk1NGJjYTRjNTAyMjE4NzRhZDk1ODZkNGE3NDg0ZjQ3YTUxNzk4YWQzYzhmZmZjOTg0NjQ3ZmZjNTBkNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.125f,0f,0f,0.2109375f,0f,0.125f,0f,0.890625f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:player_head\",Count:1,components:{\"minecraft:profile\":{id:[I;-1857821422,-1014503370,-956361272,1290224529],properties:[{name:\"textures\",value:\"ewogICJ0aW1lc3RhbXAiIDogMTc1NDA3MTI0ODM2NiwKICAicHJvZmlsZUlkIiA6ICI1N2I3OWE2NTllZTM0ODhmODBhM2ExOGQwNDYxMTg3YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaW1hdGlrX1giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGU4YjMwNTQzNzZhZDI5OWUwYTc5MmVlNmU2NjQxZTA1NjUwNzkyYWE2OTUwNWQyNTIwNDMwZWEwYjA3MDllMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9\"}]}}},item_display:\"none\",transformation:[-0.125f,0f,0f,0.7734375f,0f,0.125f,0f,0.890625f,0f,0f,-0.5f,0.8046875f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + playerName + "\",color:\"#95abbb\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.45f,0f,0f,0.495f,0f,0.55f,0f,0.655f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + dayMonthYear + "\",color:\"#818e98\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.536875f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + tag + "\"],text:[{text:\"" + hour + "\",color:\"#818e98\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.474375f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}," +
                "{id:\"minecraft:item_display\",Tags:[\"" + tag + "\"],item:{id:\"minecraft:smooth_stone_slab\",Count:1},item_display:\"none\",transformation:[1f,0f,0f,0.5f,0f,0.5f,0f,0.2175f,0f,0f,1f,0.5f,0f,0f,0f,1f]}," +
                "{id:\"minecraft:text_display\",Tags:[\"" + timerTag + "\",\"" + tag + "\"],text:[{text:\"00:00:00\",color:\"#858585\",bold:true,font:\"minecraft:uniform\"}],text_opacity:255,background:0,alignment:\"center\",line_width:210,transformation:[-0.3f,0f,0f,0.495f,0f,0.3f,0f,0.3125f,0f,0f,-1f,0.67875f,0f,0f,0f,1f],brightness:{sky:15,block:15}}" +
                "]}";

        String command = String.format("execute in %s positioned %f %f %f run summon block_display ~0.5 ~0.5 ~0.5 %s",
                loc.getWorld().getKey().toString(), loc.getX(), loc.getY(), loc.getZ(), rawNbt);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public void removeGrave(UUID id) {
        Grave grave = activeGraves.remove(id);
        if (grave != null) {
            cleanupVisuals(id, grave.getLocation());
            saveData();
        }
    }

    private void cleanupVisuals(UUID id, Location loc) {
        String tag = "grave_" + id.toString();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + loc.getWorld().getKey().toString() + " run kill @e[tag=" + tag + "]");
    }

    private void startExpiryTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();

                for (Grave grave : activeGraves.values()) {
                    long remaining = grave.getExpiryTime() - now;

                    if (remaining <= 0) {
                        World world = grave.getLocation().getWorld();
                        if (world != null) {
                            for (ItemStack item : grave.getItems()) {
                                world.dropItemNaturally(grave.getLocation(), item);
                            }
                        }
                        toRemove.add(grave.getId());
                    } else {
                        if (grave.getLocation().getWorld() != null && grave.getLocation().getWorld().isChunkLoaded(grave.getLocation().getBlockX() >> 4, grave.getLocation().getBlockZ() >> 4)) {
                            long totalSecs = remaining / 1000;
                            long mins = totalSecs / 60;
                            long secs = totalSecs % 60;
                            String timeStr = String.format("00:%02d:%02d", mins, secs);
                            String timerTag = "timer_" + grave.getId().toString();

                            for (Entity entity : grave.getLocation().getWorld().getNearbyEntities(grave.getLocation(), 2, 2, 2)) {
                                if (entity instanceof TextDisplay textDisplay && entity.getScoreboardTags().contains(timerTag)) {
                                    textDisplay.setText(ChatColor.of("#858585") + "" + ChatColor.BOLD + timeStr);
                                }
                            }
                        }
                    }
                }

                for (UUID id : toRemove) {
                    removeGrave(id);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Collection<Grave> getGraves() { return activeGraves.values(); }
    public boolean canAnyoneOpen() { return anyoneCanOpen; }
    public Grave getGraveById(UUID id) { return activeGraves.get(id); }
}