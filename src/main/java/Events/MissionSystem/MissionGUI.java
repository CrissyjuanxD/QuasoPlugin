package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MissionGUI implements Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final String guiTitle;

    private final ItemStack purpleDye;
    private final ItemStack blackDye;
    private final ItemStack nextArrow;
    private final ItemStack prevArrow;

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final int MAX_PAGES = 3;

    private final int[] MISSION_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            37, 38, 39, 40, 41, 42, 43
    };

    public MissionGUI(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.guiTitle = ChatColor.of("#FFA500") + "Misiones";

        this.purpleDye = createDecorativeItem(Material.PURPLE_DYE);
        this.blackDye = createDecorativeItem(Material.BLACK_DYE);

        this.nextArrow = createArrow("§eSiguiente Página ➔");
        this.prevArrow = createArrow("§e⬅ Anterior Página");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private ItemStack createDecorativeItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrow(String name) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MAP) return;
        if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) return;

        if (item.getItemMeta().getCustomModelData() == 9999) {
            event.setCancelled(true);
            openMissionGUI(event.getPlayer(), 1);
        }
    }

    public void openMissionGUI(Player player) {
        openMissionGUI(player, 1);
    }

    public void openMissionGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle + " - Pág " + page);

        int[] purpleSlots = {0, 4, 8, 45, 49, 53};
        for (int slot : purpleSlots) {
            gui.setItem(slot, purpleDye);
        }

        int[] blackSlots = {1, 2, 3, 5, 6, 7, 46, 47, 48, 50, 51, 52};
        for (int slot : blackSlots) {
            gui.setItem(slot, blackDye);
        }

        gui.setItem(36, prevArrow);
        gui.setItem(44, nextArrow);

        Map<Integer, Mission> allMissions = missionHandler.getMissions();
        int startIndex = (page - 1) * MISSION_SLOTS.length;

        for (int i = 0; i < MISSION_SLOTS.length; i++) {
            int slot = MISSION_SLOTS[i];
            int missionNum = startIndex + i + 1;

            if (allMissions.containsKey(missionNum)) {
                Mission mission = allMissions.get(missionNum);
                MissionData data = missionHandler.getData(player, missionNum);

                boolean isActive = data.isActive();
                boolean isCompleted = data.isCompleted();

                gui.setItem(slot, createMissionItem(mission, isActive, isCompleted, player, data, missionNum));
            } else {
                gui.setItem(slot, createMissionItem(null, false, false, player, null, missionNum));
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createMissionItem(Mission mission, boolean isActive, boolean isCompleted, Player player, MissionData data, int missionNum) {
        ItemStack item;
        ItemMeta meta;
        String displayName;
        List<String> lore = new ArrayList<>();

        if (mission == null) {
            item = new ItemStack(Material.MAP);
            meta = item.getItemMeta();
            meta.setDisplayName("§7Misión no implementada.");
            lore.add("§8Esta misión aún no ha sido programada.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
            return item;
        }

        if (!isActive) {
            item = new ItemStack(Material.MAP);
            displayName = "§7???";
        } else if (isCompleted) {
            item = new ItemStack(Material.LIME_BANNER);
            org.bukkit.inventory.meta.BannerMeta bannerMeta = (org.bukkit.inventory.meta.BannerMeta) item.getItemMeta();
            if (bannerMeta != null) {
                bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(org.bukkit.DyeColor.WHITE, org.bukkit.block.banner.PatternType.FLOWER));
                item.setItemMeta(bannerMeta);
            }
            displayName = "§a" + mission.getName();
        } else {
            item = new ItemStack(Material.GUSTER_BANNER_PATTERN);
            displayName = ChatColor.of("#FFA500") + mission.getName();
        }

        meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        if (isActive) {
            String[] descriptionLines = mission.getDescription().split("\n");
            for (String line : descriptionLines) {
                lore.add("§7" + line);
            }

            lore.add("");
            lore.add(isCompleted ? "§a✔ Completada" : "§c✖ Pendiente");

            addMissionSpecificProgress(mission, data, lore, player);
        } else {
            lore.add("§7Misión no descubierta");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addMissionSpecificProgress(Mission mission, MissionData data, List<String> lore, Player player) {
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

                String color = (mined >= 10 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString());
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
            lore.add(ChatColor.GRAY + "- Mobs en BloodMoon: " + (kills >= 125 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + kills + "/125");
        } else if (mission instanceof Mission3) {
            lore.add("");
            boolean raid = data.getProgressBool("raid_completed");
            int apples = data.getProgressInt("apples_crafted");
            lore.add(ChatColor.GRAY + "- Raid: " + (raid ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
            lore.add(ChatColor.GRAY + "- Manzanas: " + (apples >= 20 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + apples + "/20");
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
            lore.add(ChatColor.GRAY + "- Elite Zombies: " + (killed >= 15 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + killed + "/15");
        } else if (mission instanceof Mission6) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int z = data.getProgressInt("zombies_killed");
            int s = data.getProgressInt("spiders_killed");
            lore.add(ChatColor.GRAY + "- Zombies Corr.: " + (z >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + z + "/10");
            lore.add(ChatColor.GRAY + "- Arañas Corr.: " + (s >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + s + "/10");
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
            lore.add(ChatColor.GRAY + "- Elite Spiders: " + (spiders >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + spiders + "/10");
            lore.add(ChatColor.GRAY + "- Elite Skeletons: " + (skeletons >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + skeletons + "/10");
        } else if (mission instanceof Mission12) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Sacrificio de Amistad: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission13) {
            lore.add("");
            int bees = data.getProgressInt("bees_killed");
            int bombs = data.getProgressInt("bombitas_killed");
            lore.add(ChatColor.GRAY + "- Corrupted Bees: " + (bees >= 30 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + bees + "/30");
            lore.add(ChatColor.GRAY + "- Bombitas: " + (bombs >= 30 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + bombs + "/30");
        } else if (mission instanceof Mission14 m14) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso de flores:");

            int collectedCount = 0;
            for (Material flower : m14.getRequiredFlowers()) {
                boolean has = data.getProgressBool("collected_" + flower.name());
                if (has) collectedCount++;
                String name = flower.name().toLowerCase().replace('_', ' ');
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                lore.add((has ? ChatColor.GREEN : ChatColor.GRAY) + "- " + name);
            }
            lore.add("");
            lore.add(ChatColor.of("#FFA07A") + "Total: " + collectedCount + "/" + m14.getRequiredFlowers().size());
        } else if (mission instanceof Mission15) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- 300 Bloques en 7s: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission16) {
            lore.add("");
            int killed = data.getProgressInt("withers_killed");
            lore.add(ChatColor.GRAY + "- Withers: " + (killed >= 3 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + killed + "/3");
        } else if (mission instanceof Mission17) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int endermen = data.getProgressInt("elite_endermen_killed");
            int creepers = data.getProgressInt("elite_creepers_killed");
            lore.add(ChatColor.GRAY + "- Elite Endermans: " + (endermen >= 20 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + endermen + "/20");
            lore.add(ChatColor.GRAY + "- Elite Creepers: " + (creepers >= 20 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + creepers + "/20");
        } else if (mission instanceof Mission18) {
            lore.add("");
            int popped = data.getProgressInt("totems_popped");
            lore.add(ChatColor.GRAY + "- Totems Usados: " + (popped >= 8 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + popped + "/8");
        } else if (mission instanceof Mission19) {
            lore.add("");
            int hearts = data.getProgressInt("hearts_broken");
            lore.add(ChatColor.GRAY + "- Corazones Rotos: " + (hearts >= 15 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + hearts + "/15");
        } else if (mission instanceof Mission20) {
            lore.add("");
            int broken = data.getProgressInt("shriekers_broken");
            lore.add(ChatColor.GRAY + "- Chilladores: " + (broken >= 20 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + broken + "/20");
        } else if (mission instanceof Mission21) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Desafío: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission22) {
            lore.add("");
            int killed = data.getProgressInt("guardians_killed");
            lore.add(ChatColor.GRAY + "- Elders Guardians: " + (killed >= 3 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + killed + "/3");
        } else if (mission instanceof Mission23) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Venganza: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission24) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Riesgo Mortal: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission25) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");
            int witherSkeletons = data.getProgressInt("elite_wither_skeletons_killed");
            int piglins = data.getProgressInt("elite_piglins_killed");
            lore.add(ChatColor.GRAY + "- Elite Wither Skeletons: " + (witherSkeletons >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + witherSkeletons + "/10");
            lore.add(ChatColor.GRAY + "- Elite Piglins: " + (piglins >= 10 ? ChatColor.GREEN : ChatColor.of("#FFA07A")) + piglins + "/10");
        } else if (mission instanceof Mission26 m26) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Bloques Rotos:");
            for (Material m : m26.getRequiredBlocks()) {
                boolean has = data.getProgressBool("broken_" + m.name());
                String name = m.name().toLowerCase().replace("_", " ");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                lore.add((has ? ChatColor.GREEN : ChatColor.GRAY) + "- " + name);
            }
        } else if (mission instanceof Mission27) {
            lore.add("");
            lore.add(ChatColor.GRAY + "- Objetivo Eliminado: " + (data.isCompleted() ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
        } else if (mission instanceof Mission28) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso de armadura:");

            String[] armorPieces = {"helmet", "chestplate", "leggings", "boots"};
            String[] armorNames = {"Casco", "Peto", "Pantalones", "Botas"};
            int totalEquipped = 0;

            for (int i = 0; i < armorPieces.length; i++) {
                boolean hasPiece = data.getProgressBool("netherite_prot5_" + armorPieces[i]);

                if (hasPiece) {
                    totalEquipped++;
                    lore.add(ChatColor.of("#98FB98") + "- " + armorNames[i] + " de Netherite con Protección V ✓");
                } else {
                    lore.add(ChatColor.of("#D3D3D3") + "- " + armorNames[i] + " de Netherite con Protección V ✖");
                }
            }

            lore.add("");
            lore.add(ChatColor.of("#FFA07A") + "Total Equipado: " + totalEquipped + "/4");
        } else if (mission instanceof Mission29) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");

            long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long hoursPlayed = ticks / (20 * 60 * 60);

            String color = hoursPlayed >= 200 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();

            lore.add(ChatColor.GRAY + "- Horas jugadas: " + color + hoursPlayed + ChatColor.of("#FFE4B5") + "/160");
        } else if (mission instanceof Mission30) {
            lore.add("");
            lore.add(ChatColor.of("#FFCC99") + "Progreso:");

            int completedMissions = 0;
            for (int i = 1; i <= 29; i++) {
                if (missionHandler.isMissionCompleted(player, i)) {
                    completedMissions++;
                }
            }

            String color = completedMissions >= 29 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString();
            lore.add(ChatColor.GRAY + "- Misiones completadas: " + color + completedMissions + ChatColor.of("#FFE4B5") + "/29");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith(guiTitle)) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;

            int slot = event.getRawSlot();
            int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

            if (slot == 36) {
                // Rotación hacia atrás
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                int newPage = (currentPage == 1) ? MAX_PAGES : currentPage - 1;
                openMissionGUI(player, newPage);
            } else if (slot == 44) {
                // Rotación hacia adelante
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                int newPage = (currentPage == MAX_PAGES) ? 1 : currentPage + 1;
                openMissionGUI(player, newPage);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
        }
    }
}