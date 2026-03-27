package Events.MissionSystem;

import Handlers.ActionBarHandler;
import TitleListener.SuccessNotification;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Mission14 implements Mission, Listener {
    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final SuccessNotification successNotification;
    private final ActionBarHandler actionBarHandler;

    // Única llave necesaria: "Ya se usó para la misión"
    private final NamespacedKey markKey;

    private final List<Material> flowers = Arrays.asList(
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
            Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP,
            Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
            Material.OXEYE_DAISY, Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE, Material.SUNFLOWER, Material.LILAC,
            Material.ROSE_BUSH, Material.PEONY, Material.TORCHFLOWER,
            Material.PITCHER_PLANT, Material.PINK_PETALS, Material.SPORE_BLOSSOM
    );

    public Mission14(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.successNotification = new SuccessNotification(plugin);
        this.actionBarHandler = new ActionBarHandler(plugin);
        this.markKey = new NamespacedKey(plugin, "mission14_counted");
    }

    @Override
    public String getName() { return "Stardew Valley"; }

    @Override
    public String getDescription() { return "Encuentra y recoge 25 unidades\nde todas las flores del juego."; }

    @Override
    public int getMissionNumber() { return 14; }

    @Override
    public List<ItemStack> getRewards() {
        List<ItemStack> rewards = new ArrayList<>();
        ItemStack coins = EconomyItems.createVithiumCoin();
        coins.setAmount(20);
        ItemStack goldenApples = new ItemStack(Material.GOLDEN_APPLE, 25);
        ItemStack diamonds = new ItemStack(Material.DIAMOND, 64);
        ItemStack xpFill = new ItemStack(Material.EXPERIENCE_BOTTLE, 2);
        for (int i = 0; i < 27; i++) {
            if (i == 11) rewards.add(goldenApples);
            else if (i == 13) rewards.add(coins);
            else if (i == 15) rewards.add(diamonds);
            else rewards.add(xpFill.clone());
        }
        return rewards;
    }

    @Override
    public void initializePlayerData(String playerName) {}

    @Override
    public void checkCompletion(String playerName) {}

    public List<Material> getRequiredFlowers() { return flowers; }

    // --- SISTEMA ANTI EXPLOIT (TRASPASO DE MARCA) ---

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // 1. SOLUCIÓN: Siempre limpiamos cualquier metadata "fantasma" que haya quedado en estas coordenadas
        if (event.getBlockPlaced().hasMetadata("mission14_marked")) {
            event.getBlockPlaced().removeMetadata("mission14_marked", plugin);
        }

        ItemStack itemInHand = event.getItemInHand();
        Material type = itemInHand.getType();

        // 2. Incluimos las semillas en la validación (TORCHFLOWER_SEEDS, PITCHER_POD)
        if (flowers.contains(type) || type.name().contains("SEED") || type.name().contains("PITCHER") || type.name().contains("TORCHFLOWER")) {
            ItemMeta meta = itemInHand.getItemMeta();
            // Si el jugador planta una flor o semilla que YA está marcada, le pasamos la marca al bloque
            if (meta != null && meta.getPersistentDataContainer().has(markKey, PersistentDataType.BYTE)) {
                event.getBlockPlaced().setMetadata("mission14_marked", new FixedMetadataValue(plugin, true));
            }
        }
    }

    @EventHandler
    public void onBlockDropItem(BlockDropItemEvent event) {
        // Si el bloque roto tenía la marca de "ya estaba contado", se la devolvemos al ítem dropeado
        if (event.getBlockState().hasMetadata("mission14_marked") || event.getBlock().hasMetadata("mission14_marked")) {
            for (Item itemEntity : event.getItems()) {
                ItemStack item = itemEntity.getItemStack();
                Material type = item.getType();

                // Marcamos también las semillas si caen de un cultivo previamente marcado
                if (flowers.contains(type) || type.name().contains("SEED") || type.name().contains("PITCHER") || type.name().contains("TORCHFLOWER")) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte) 1);
                        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                        if (!lore.contains(ChatColor.of("#A9A9A9") + "Flor ya recolectada para la misión")) {
                            lore.add(ChatColor.of("#A9A9A9") + "Flor ya recolectada para la misión");
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                        itemEntity.setItemStack(item);
                    }
                }
            }
            // SOLUCIÓN: Limpiamos la metadata al romper el bloque por seguridad extra
            event.getBlock().removeMetadata("mission14_marked", plugin);
        }
    }

    // --- RECOLECCIÓN ---

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkFlower(player, event.getItem().getItemStack());
        }
    }

    private void checkFlower(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        Material type = item.getType();
        if (!flowers.contains(type)) return;
        if (!missionHandler.isMissionActive(player, 14)) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Si ya tiene la marca de contada, ignorar
        if (meta.getPersistentDataContainer().has(markKey, PersistentDataType.BYTE)) return;

        MissionData data = missionHandler.getData(player, 14);
        if (data.isCompleted()) return;

        String key = "collected_" + type.name();
        int currentAmount = data.getProgressInt(key);

        if (currentAmount < 25) { // Límite subido a 25
            int amountToAdd = item.getAmount();
            int newAmount = Math.min(25, currentAmount + amountToAdd);
            data.setProgressValue(key, newAmount);

            int completedTypes = 0;
            for (Material f : flowers) {
                if (data.getProgressInt("collected_" + f.name()) >= 25) {
                    completedTypes++;
                }
            }

            missionHandler.saveData(player, 14, data);

            if (completedTypes >= flowers.size()) {
                successNotification.showSuccess(player);
                missionHandler.completeMission(player, 14);
            } else {
                String flowerName = type.name().toLowerCase().replace('_', ' ');
                flowerName = flowerName.substring(0, 1).toUpperCase() + flowerName.substring(1);

                String amountColor = (newAmount >= 25 ? ChatColor.GREEN.toString() : ChatColor.of("#FFA07A").toString());

                String msg = ChatColor.GOLD + "۞ " +
                        ChatColor.of("#FFCC99") + "Flor: " + ChatColor.GREEN + flowerName + " " +
                        amountColor + newAmount + ChatColor.of("#FFE4B5") + "/25" +
                        ChatColor.GRAY + " (" + completedTypes + "/" + flowers.size() + " Tipos)";

                actionBarHandler.sendActionBar(player, msg);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            }
        }

        // Marcamos la flor como ya usada sin importar si completó la misión o no
        meta.getPersistentDataContainer().set(markKey, PersistentDataType.BYTE, (byte) 1);
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (!lore.contains(ChatColor.of("#A9A9A9") + "Flor ya recolectada para la misión")) {
            lore.add(ChatColor.of("#A9A9A9") + "Flor ya recolectada para la misión");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}