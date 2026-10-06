package Events.AchievementParty;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AchievementGUI implements Listener {
    private final JavaPlugin plugin;
    private final AchievementPartyHandler achievementHandler;
    private final File achievementsFile;

    private final int[] achievementSlots = {18, 19, 20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 40};

    public AchievementGUI(JavaPlugin plugin, AchievementPartyHandler achievementHandler) {
        this.plugin = plugin;
        this.achievementHandler = achievementHandler;
        this.achievementsFile = achievementHandler.getAchievementsFile();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Menú con todos los logros, marcando los que el jugador ya completó
    public void openAchievementGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.of("#FF1493") + "" + ChatColor.BOLD + "Fiesta de Logros");

        ItemStack border = createBorderItem();
        for (int i = 0; i < 54; i++) {
            if (!isAchievementSlot(i)) {
                gui.setItem(i, border);
            }
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
        String playerName = player.getName();

        List<Achievement> allAchievements = new ArrayList<>(achievementHandler.getAchievements().values());

        allAchievements.sort(Comparator.comparingInt(a -> achievementHandler.getAchievementIndex(a)));

        for (int i = 0; i < Math.min(allAchievements.size(), achievementSlots.length); i++) {
            Achievement achievement = allAchievements.get(i);
            String achievementId = achievementHandler.getAchievementKey(achievement);

            boolean isCompleted = data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false);

            gui.setItem(achievementSlots[i], createAchievementItem(achievement, isCompleted, playerName));
        }

        player.openInventory(gui);
    }

    private ItemStack createAchievementItem(Achievement achievement, boolean isCompleted, String playerName) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        ChatColor completedColor = ChatColor.of("#FF6B35");
        ChatColor pendingColor = ChatColor.of("#A8A8A8");

        if (isCompleted) {
            meta.setDisplayName(completedColor + achievement.getName());
        } else {
            meta.setDisplayName(pendingColor + achievement.getName());
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.of("#F8F8FF") + achievement.getDescription());
        lore.add("");
        lore.add(isCompleted ? ChatColor.of("#32CD32") + "✔ Completado" : ChatColor.of("#FFD700") + "✖ Pendiente");

        if (achievement instanceof Achievement2) {
            lore.add("");
            lore.add(ChatColor.of("#FF1493") + "" + ChatColor.BOLD + "Progreso de flores:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
            Achievement2 flowerAchievement = (Achievement2) achievement;

            for (Material flower : flowerAchievement.getRequiredFlowers()) {
                boolean hasFlower = data.getBoolean(
                        "players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false);

                lore.add((hasFlower ? ChatColor.of("#00BFFF") : ChatColor.of("#F8F8FF")) + "- " + formatMaterialName(flower));
            }
        }

        if (achievement instanceof Achievement5) {
            lore.add("");
            lore.add(ChatColor.of("#FF8C00") + "" + ChatColor.BOLD + "Progreso de bloques:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);
            Achievement5 blockAchievement = (Achievement5) achievement;

            for (Material block : blockAchievement.getRequiredBlocks()) {
                boolean hasBlock = data.getBoolean(
                        "players." + playerName + ".achievements.touch_grass.broken." + block.name(), false);

                lore.add((hasBlock ? ChatColor.of("#FFD700") : ChatColor.of("#F8F8FF")) + "- " + formatMaterialName(block));
            }
        }

        if (achievement instanceof Achievement8) {
            lore.add("");
            lore.add(ChatColor.of("#DC143C") + "" + ChatColor.BOLD + "Progreso de chilladores:");

            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementsFile);

            int broken = data.getInt("players." + playerName + ".achievements.sculk_shrieker.broken", 0);
            lore.add(ChatColor.of("#F8F8FF") + "- Rotos: " + ChatColor.of("#FF1493") + broken +
                    ChatColor.of("#F8F8FF") + "/" + ChatColor.of("#FF1493") + Achievement8.REQUIRED_SCULK_SHRIEKERS);
        }

        meta.setLore(lore);

        meta.setCustomModelData(isCompleted ? 3000 : 3001);
        items.ItemModels.apply(meta, isCompleted ? "logro_completado" : "logro_pendiente");

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        meta.setCustomModelData(3002);
        items.ItemModels.apply(meta, "logro_borde");
        border.setItemMeta(meta);
        return border;
    }

    private boolean isAchievementSlot(int slot) {
        for (int achievementSlot : achievementSlots) {
            if (slot == achievementSlot) {
                return true;
            }
        }
        return false;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.of("#FF1493") + "" + ChatColor.BOLD + "Fiesta de Logros")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }
}
