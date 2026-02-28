package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MissionGUI implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final String guiTitle;

    private final ItemStack yellowPane;
    private final ItemStack orangePane;

    public MissionGUI(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;

        this.guiTitle = ChatColor.of("#FFA500") + "Misiones";

        this.yellowPane = createPane(Material.YELLOW_STAINED_GLASS_PANE);
        this.orangePane = createPane(Material.ORANGE_STAINED_GLASS_PANE);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createPane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FILLED_MAP) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        if (item.getItemMeta().getCustomModelData() == 9999) {
            event.setCancelled(true);
            openMissionGUI(event.getPlayer());
        }
    }

    public void openMissionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, guiTitle);

        ItemStack[] pattern = {yellowPane, orangePane, orangePane, yellowPane, orangePane, yellowPane, orangePane, orangePane, yellowPane};

        for (int i = 0; i < 9; i++) gui.setItem(i, pattern[i]);
        for (int i = 0; i < 9; i++) gui.setItem(36 + i, pattern[i]);

        Map<Integer, Mission> allMissions = missionHandler.getMissions();

        for (int missionNum = 1; missionNum <= 27; missionNum++) {
            int slot = 8 + missionNum;
            if (slot > 35) break;

            if (allMissions.containsKey(missionNum)) {
                Mission mission = allMissions.get(missionNum);

                MissionData data = missionHandler.getData(player, missionNum);

                boolean isActive = data.isActive();
                boolean isCompleted = data.isCompleted();

                gui.setItem(slot, createMissionItem(mission, isActive, isCompleted, player, data));
            } else {
                gui.setItem(slot, new ItemStack(Material.AIR));
            }
        }
        player.openInventory(gui);
    }

    private ItemStack createMissionItem(Mission mission, boolean isActive, boolean isCompleted, Player player, MissionData data) {
        ItemStack item;

        if (!isActive) {
            item = new ItemStack(Material.FILLED_MAP);
        } else if (isCompleted) {
            item = new ItemStack(Material.LIME_DYE);
        } else {
            item = new ItemStack(Material.FIREWORK_STAR);
        }

        ItemMeta meta = item.getItemMeta();
        String displayName;

        if (!isActive) {
            displayName = ChatColor.GRAY + "???";
        } else if (isCompleted) {
            displayName = ChatColor.GREEN + mission.getName();
        } else {
            displayName = ChatColor.of("#FFA500") + mission.getName();
        }

        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();

        if (isActive) {
            String[] descriptionLines = mission.getDescription().split("\n");
            for (String line : descriptionLines) {
                lore.add(ChatColor.GRAY + line);
            }

            lore.add("");
            lore.add(isCompleted ? ChatColor.GREEN + "✔ Completada" : ChatColor.RED + "✖ Pendiente");

            addMissionSpecificProgress(mission, data, lore);
        } else {
            lore.add(ChatColor.GRAY + "Misión no descubierta");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addMissionSpecificProgress(Mission mission, MissionData data, List<String> lore) {
        if (mission instanceof Mission1) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");

            Mission1 m1 = (Mission1) mission;
            List<Material> ores = m1.getRequiredOres();
            StringBuilder currentLine = new StringBuilder();

            for (int i = 0; i < ores.size(); i++) {
                Material ore = ores.get(i);
                int mined = data.getProgressInt("ore_" + ore.name());

                String name = ore.name();
                name = name.replace("DEEPSLATE_", "Piz. ");
                name = name.replace("NETHER_GOLD_ORE", "Oro Nether");
                name = name.replace("NETHER_QUARTZ_ORE", "Cuarzo");
                name = name.replace("ANCIENT_DEBRIS", "Escombro Ancestral");

                name = name.replace("COAL_ORE", "Carbón");
                name = name.replace("COPPER_ORE", "Cobre");
                name = name.replace("IRON_ORE", "Hierro");
                name = name.replace("GOLD_ORE", "Oro");
                name = name.replace("LAPIS_ORE", "Lapislázuli");
                name = name.replace("REDSTONE_ORE", "Redstone");
                name = name.replace("DIAMOND_ORE", "Diamante");
                name = name.replace("EMERALD_ORE", "Esmeralda");

                String color = (mined >= 10 ? ChatColor.GREEN.toString() : ChatColor.YELLOW.toString());
                String formattedOre = ChatColor.GRAY + "- " + name + ": " + color + mined + "/10";

                if (i % 2 == 0) {
                    currentLine.append(formattedOre);
                    if (i == ores.size() - 1) {
                        lore.add(currentLine.toString());
                    }
                } else {
                    currentLine.append(ChatColor.DARK_GRAY).append(" │ ").append(formattedOre);
                    lore.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
        } else if (mission instanceof Mission2) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int kills = data.getProgressInt("bloodmoon_kills");
            lore.add(ChatColor.GRAY + "- Mobs en BloodMoon: " + (kills >= 60 ? ChatColor.GREEN : ChatColor.YELLOW) + kills + "/60");
        } else if (mission instanceof Mission3) {
            lore.add("");
            boolean raid = data.getProgressBool("raid_completed");
            int apples = data.getProgressInt("apples_crafted");
            lore.add(ChatColor.GRAY + "- Raid: " + (raid ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
            lore.add(ChatColor.GRAY + "- Manzanas: " + (apples >= 20 ? ChatColor.GREEN : ChatColor.YELLOW) + apples + "/20");
        } else if (mission instanceof Mission4) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            String[] types = {"LEATHER", "GOLDEN", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE"};
            String[] names = {"Cuero", "Oro", "Malla", "Hierro", "Diamante", "Netherite"};
            String[] parts = {"_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS"};
            int totalEquipped = 0;
            for (int i = 0; i < types.length; i++) {
                int typeCount = 0;
                for (String part : parts) {
                    if (data.getProgressBool("armor_" + types[i] + part)) {
                        typeCount++;
                        totalEquipped++;
                    }
                }
                String color = (typeCount >= 4) ? ChatColor.GREEN.toString() : ChatColor.GRAY.toString();
                lore.add(color + "- " + names[i] + ": " + typeCount + "/4");
            }
            lore.add("");
            lore.add(ChatColor.of("#FFA07A") + "Total: " + totalEquipped + "/24");
        } else if (mission instanceof Mission5) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int killed = data.getProgressInt("elite_zombies_killed");
            lore.add(ChatColor.GRAY + "- Elite Zombies: " + (killed >= 15 ? ChatColor.GREEN : ChatColor.YELLOW) + killed + "/15");
        } else if (mission instanceof Mission6) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int z = data.getProgressInt("zombies_killed");
            int s = data.getProgressInt("spiders_killed");
            lore.add(ChatColor.GRAY + "- Zombies Corr.: " + (z >= 10 ? ChatColor.GREEN : ChatColor.YELLOW) + z + "/10");
            lore.add(ChatColor.GRAY + "- Arañas Corr.: " + (s >= 10 ? ChatColor.GREEN : ChatColor.YELLOW) + s + "/10");
        } else if (mission instanceof Mission7) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Salto: " + (data.isCompleted() ? ChatColor.GREEN + "Completado" : ChatColor.RED + "Pendiente"));
        } else if (mission instanceof Mission8) {
            lore.add("");
            boolean hit = data.getProgressBool("hit_projectile");
            boolean killed = data.getProgressBool("killed_warden");
            boolean completed = data.isCompleted();
            lore.add(ChatColor.GRAY + "- Hit Proyectil: " + (hit || completed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
            lore.add(ChatColor.GRAY + "- Eliminar Warden: " + (killed || completed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission9) {
            lore.add("");
            boolean spotted = data.getProgressBool("spotted");
            boolean completed = data.isCompleted();
            lore.add(ChatColor.GRAY + "- Avistado: " + (spotted || completed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
            lore.add(ChatColor.GRAY + "- Eliminado: " + (completed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission10) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Reina Derrotada: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission11) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int spiders = data.getProgressInt("elite_spiders_killed");
            int skeletons = data.getProgressInt("elite_skeletons_killed");
            lore.add(ChatColor.GRAY + "- Elite Spiders: " + (spiders >= 10 ? ChatColor.GREEN : ChatColor.YELLOW) + spiders + "/10");
            lore.add(ChatColor.GRAY + "- Elite Skeletons: " + (skeletons >= 10 ? ChatColor.GREEN : ChatColor.YELLOW) + skeletons + "/10");
        } else if (mission instanceof Mission12) {
            lore.add("");
            int bees = data.getProgressInt("bees_killed");
            int bombs = data.getProgressInt("bombitas_killed");
            lore.add(ChatColor.GRAY + "- Corrupted Bees: " + (bees >= 30 ? ChatColor.GREEN : ChatColor.YELLOW) + bees + "/30");
            lore.add(ChatColor.GRAY + "- Bombitas: " + (bombs >= 30 ? ChatColor.GREEN : ChatColor.YELLOW) + bombs + "/30");
        } else if (mission instanceof Mission13) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso de flores:");

            Mission13 m13 = (Mission13) mission;
            int collectedCount = 0;
            for (Material flower : m13.getRequiredFlowers()) {
                boolean has = data.getProgressBool("collected_" + flower.name());
                if (has) collectedCount++;
                String name = flower.name().toLowerCase().replace('_', ' ');
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                lore.add((has ? ChatColor.GREEN : ChatColor.GRAY) + "- " + name);
            }
            lore.add("");
            lore.add(ChatColor.of("#FFA07A") + "Total: " + collectedCount + "/" + m13.getRequiredFlowers().size());
        } else if (mission instanceof Mission14) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- 300 Bloques en 7s: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission15) {
            lore.add("");
            int killed = data.getProgressInt("withers_killed");
            lore.add(ChatColor.GRAY + "- Withers: " + (killed >= 3 ? ChatColor.GREEN : ChatColor.YELLOW) + killed + "/3");
        } else if (mission instanceof Mission16) {
            lore.add("");
            int killed = data.getProgressInt("skeletons_killed");
            lore.add(ChatColor.GRAY + "- Guardian C. Skeletons: " + (killed >= 20 ? ChatColor.GREEN : ChatColor.YELLOW) + killed + "/20");
        } else if (mission instanceof Mission17) {
            lore.add("");
            int popped = data.getProgressInt("totems_popped");
            lore.add(ChatColor.GRAY + "- Totems Usados: " + (popped >= 5 ? ChatColor.GREEN : ChatColor.YELLOW) + popped + "/5");
        } else if (mission instanceof Mission18) {
            lore.add("");
            int hearts = data.getProgressInt("hearts_broken");
            lore.add(ChatColor.GRAY + "- Corazones Rotos: " + (hearts >= 10 ? ChatColor.GREEN : ChatColor.YELLOW) + hearts + "/10");
        } else if (mission instanceof Mission19) {
            lore.add("");
            int broken = data.getProgressInt("shriekers_broken");
            lore.add(ChatColor.GRAY + "- Chilladores: " + (broken >= 20 ? ChatColor.GREEN : ChatColor.YELLOW) + broken + "/20");
        } else if (mission instanceof Mission20) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Desafío: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission21) {
            lore.add("");
            int killed = data.getProgressInt("guardians_killed");
            lore.add(ChatColor.GRAY + "- Elders Guardians: " + (killed >= 3 ? ChatColor.GREEN : ChatColor.YELLOW) + killed + "/3");
        } else if (mission instanceof Mission22) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Venganza: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission23) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Tiro Espectral: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission24) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Riesgo Mortal: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission25) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Bloques Rotos:");

            Mission25 m25 = (Mission25) mission;
            for (Material m : m25.getRequiredBlocks()) {
                boolean has = data.getProgressBool("broken_" + m.name());
                String name = m.name().toLowerCase().replace("_", " ");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                lore.add((has ? ChatColor.GREEN : ChatColor.GRAY) + "- " + name);
            }
        } else if (mission instanceof Mission26) {
            lore.add("");
            int creepers = data.getProgressInt("creepers_killed");
            int ghasts = data.getProgressInt("ghasts_killed");
            lore.add(ChatColor.GRAY + "- Spectral Creepers: " + (creepers >= 15 ? ChatColor.GREEN : ChatColor.YELLOW) + creepers + "/15");
            lore.add(ChatColor.GRAY + "- Spectral Ghasts: " + (ghasts >= 15 ? ChatColor.GREEN : ChatColor.YELLOW) + ghasts + "/15");
        } else if (mission instanceof Mission27) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Objetivo Eliminado: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
        }
    }
}