package Pesca;

import imp.crissyjuanxd.QuasoPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class FishingWandListener implements Listener {

    private static final String WAND_KEY = "§b§lVara de Pesca";

    private final QuasoPlugin plugin;
    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    public FishingWandListener(QuasoPlugin plugin) {
        this.plugin = plugin;
    }

    public static ItemStack createWand() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(WAND_KEY);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#61B1F2") + "Click Izquierdo " + ChatColor.GRAY + "→ Seleccionar POS1");
            lore.add(ChatColor.of("#61B1F2") + "Click Derecho " + ChatColor.GRAY + "→ Seleccionar POS2");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Usa /pesca set <nombre> para guardar la zona");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.STICK) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String nombreLimpio = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        return nombreLimpio.contains("Vara de Pesca");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (!isWand(inHand)) return;

        event.setCancelled(true);

        Action action = event.getAction();
        Block b = event.getClickedBlock();

        if (b == null) {
            b = player.getTargetBlockExact(5);
        }

        if (b != null) {
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
                pos1Map.put(player.getUniqueId(), b.getLocation());
                player.sendMessage(ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "[Pesca] "
                        + ChatColor.GRAY + "POS1 fijada en: "
                        + ChatColor.WHITE + formatLoc(b.getLocation()));

            } else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                pos2Map.put(player.getUniqueId(), b.getLocation());
                player.sendMessage(ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "[Pesca] "
                        + ChatColor.GRAY + "POS2 fijada en: "
                        + ChatColor.WHITE + formatLoc(b.getLocation()));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (isWand(inHand)) {
            event.setCancelled(true);

            pos1Map.put(player.getUniqueId(), event.getBlock().getLocation());
            player.sendMessage(ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "[Pesca] "
                    + ChatColor.GRAY + "POS1 fijada en: "
                    + ChatColor.WHITE + formatLoc(event.getBlock().getLocation()));
        }
    }

    public Location getPos1(UUID uuid) { return pos1Map.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2Map.get(uuid); }

    public boolean hasBothPositions(UUID uuid) {
        return pos1Map.containsKey(uuid) && pos2Map.containsKey(uuid);
    }

    public void clearPositions(UUID uuid) {
        pos1Map.remove(uuid);
        pos2Map.remove(uuid);
    }

    private String formatLoc(Location loc) {
        return String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
    }
}