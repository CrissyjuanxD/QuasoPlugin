package Habilidades;

import Handlers.DayHandler;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

public class HabilidadesGUI implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    private final DayHandler dayHandler;
    private final String GUI_TITLE = ChatColor.of("#C77DFF") + "Libro de Habilidades";

    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final int MAX_PAGES = 2;

    public HabilidadesGUI(JavaPlugin plugin, HabilidadesManager manager, DayHandler dayHandler) {
        this.plugin = plugin;
        this.manager = manager;
        this.dayHandler = dayHandler;
    }

    public void openHabilidadesGUI(Player player) {
        openHabilidadesGUI(player, 1);
    }

    public void openHabilidadesGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE + " - Pág " + page);

        fillGUIWithPanels(gui, page);
        fillGUIWithSkills(gui, player, page);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    private void fillGUIWithPanels(Inventory gui, int page) {
        ItemStack purpleDye = createPanel(Material.PURPLE_DYE, " ");
        ItemStack magentaDye = createPanel(Material.MAGENTA_DYE, " ");
        ItemStack blackDye = createPanel(Material.BLACK_DYE, " ");

        int[] purpleSlots = {0, 8, 45, 53};
        for (int slot : purpleSlots) {
            gui.setItem(slot, purpleDye);
        }

        int[] magentaSlots = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : magentaSlots) {
            gui.setItem(slot, magentaDye);
        }

        int[] blackSlots = {19, 20, 21, 22, 23, 24, 25, 37, 38, 39, 40, 41, 42, 43};
        for (int slot : blackSlots) {
            gui.setItem(slot, blackDye);
        }

        ItemStack nextSkill = createPanel(Material.IRON_NUGGET, ChatColor.GRAY + "Siguiente Habilidad");
        ItemMeta nextMeta = nextSkill.getItemMeta();
        if (nextMeta != null) {
            nextMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            nextMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            nextSkill.setItemMeta(nextMeta);
        }

        int[] nextSlots = {11, 13, 15, 29, 31, 33, 47, 49, 51};
        for (int slot : nextSlots) {
            gui.setItem(slot, nextSkill);
        }

        // --- Flechas de paginación ---
        if (page == 2) {
            gui.setItem(45, createArrow("§e⬅ Anterior Página"));
        }
        if (page == 1) {
            gui.setItem(53, createArrow("§eSiguiente Página ➔"));
        }
    }

    private void fillGUIWithSkills(Inventory gui, Player player, int page) {
        int offset = (page == 1) ? 0 : 4;

        gui.setItem(10, createHabilidadItem(player, HabilidadesType.VITALIDAD, 1 + offset));
        gui.setItem(12, createHabilidadItem(player, HabilidadesType.VITALIDAD, 2 + offset));
        gui.setItem(14, createHabilidadItem(player, HabilidadesType.VITALIDAD, 3 + offset));
        gui.setItem(16, createHabilidadItem(player, HabilidadesType.VITALIDAD, 4 + offset));

        gui.setItem(28, createHabilidadItem(player, HabilidadesType.AGILIDAD, 1 + offset));
        gui.setItem(30, createHabilidadItem(player, HabilidadesType.AGILIDAD, 2 + offset));
        gui.setItem(32, createHabilidadItem(player, HabilidadesType.AGILIDAD, 3 + offset));
        gui.setItem(34, createHabilidadItem(player, HabilidadesType.AGILIDAD, 4 + offset));

        gui.setItem(46, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 1 + offset));
        gui.setItem(48, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 2 + offset));
        gui.setItem(50, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 3 + offset));
        gui.setItem(52, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 4 + offset));
    }

    private ItemStack createPanel(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
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

    private ItemStack createHabilidadItem(Player player, HabilidadesType type, int level) {
        boolean isUnlocked = manager.hasHabilidadPurchased(player.getUniqueId(), type, level);
        boolean canUnlock = manager.canUnlock(player.getUniqueId(), type, level);

        Material mat;
        switch (type) {
            case AGILIDAD:
                mat = Material.FLOW_BANNER_PATTERN;
                break;
            case RESISTENCIA:
                mat = Material.CREEPER_BANNER_PATTERN;
                break;
            case VITALIDAD:
                mat = Material.FLOWER_BANNER_PATTERN;
                break;
            default:
                mat = Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = getDisplayName(type, level, isUnlocked);
            meta.setDisplayName(displayName);

            List<String> lore = getLore(type, level, isUnlocked, canUnlock);
            meta.setLore(lore);

            if (isUnlocked) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }
        return item;
    }

    private String getDisplayName(HabilidadesType type, int level, boolean isUnlocked) {
        String name = type.getDisplayName() + " Nivel " + level;
        if (isUnlocked) {
            return ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + name + " ✓";
        } else {
            return ChatColor.of("#9D4EDD") + "" + ChatColor.BOLD + name;
        }
    }

    private List<String> getLore(HabilidadesType type, int level, boolean isUnlocked, boolean canUnlock) {
        List<String> lore = new ArrayList<>();

        if (isUnlocked) {
            lore.add(ChatColor.of("#7FFF00") + "✓ Desbloqueado");
            lore.add("");
        } else if (!canUnlock) {
            lore.add(ChatColor.of("#FF6B6B") + "Bloqueado");
            lore.add(ChatColor.GRAY + "Desbloquea el nivel anterior primero.");
            lore.add("");
        }

        lore.addAll(getHabilidadDescription(type, level));

        if (!isUnlocked && canUnlock) {
            lore.add("");
            lore.add(ChatColor.of("#E0AAFF") + "Costo:");
            addCostLore(lore, level);
            lore.add("");
            lore.add(ChatColor.of("#9D4EDD") + "Click para desbloquear");
        }

        return lore;
    }

    private List<String> getHabilidadDescription(HabilidadesType type, int level) {
        List<String> desc = new ArrayList<>();
        switch (type) {
            case VITALIDAD:
                if (level <= 4) {
                    desc.add(ChatColor.GRAY + "Otorga +2.5 Corazones permanentes.");
                } else {
                    desc.add(ChatColor.GRAY + "Otorga +4 Corazones permanentes.");
                }
                break;
            case AGILIDAD:
                switch (level) {
                    case 1: desc.add(ChatColor.GRAY + "Haste I infinito."); break;
                    case 2: desc.add(ChatColor.GRAY + "Doble Salto y Gracia del Delfín II infinito."); break;
                    case 3: desc.add(ChatColor.GRAY + "Velocidad I infinito."); break;
                    case 4: desc.add(ChatColor.GRAY + "Triple Salto."); break;
                    case 5: desc.add(ChatColor.GRAY + "Fuerza I infinita."); break;
                    case 6: desc.add(ChatColor.GRAY + "Salto Alto I infinito."); break;
                    case 7: desc.add(ChatColor.GRAY + "Velocidad II infinita."); break;
                    case 8: desc.add(ChatColor.GRAY + "Cuádruple Salto."); break;
                }
                break;
            case RESISTENCIA:
                switch (level) {
                    case 1: desc.add(ChatColor.GRAY + "8% prob. bloquear daño directo de proyectiles."); break;
                    case 2: desc.add(ChatColor.GRAY + "8% prob. bloquear daño directo de monstruos."); break;
                    case 3: desc.add(ChatColor.GRAY + "8% prob. bloquear cualquier daño."); break;
                    case 4: desc.add(ChatColor.GRAY + "Resistencia I infinita."); break;
                    case 5: desc.add(ChatColor.GRAY + "14% prob. bloquear daño directo de proyectiles."); break;
                    case 6: desc.add(ChatColor.GRAY + "14% prob. bloquear daño directo de monstruos."); break;
                    case 7: desc.add(ChatColor.GRAY + "14% prob. bloquear cualquier daño."); break;
                    case 8: desc.add(ChatColor.GRAY + "Resistencia II infinita."); break;
                }
                break;
        }
        return desc;
    }

    private void addCostLore(List<String> lore, int level) {
        int xp = 0;
        String item = "";
        int coins = 0;

        switch(level) {
            case 1: xp = 30; item = "12 Bloques de Oro"; coins = 5; break;
            case 2: xp = 40; item = "15 Bloques de Diamante"; coins = 10; break;
            case 3: xp = 50; item = "32 Bloques de Esmeralda"; coins = 15; break;
            case 4: xp = 60; item = "3 Bloques de Netherite"; coins = 20; break;
            case 5: xp = 70; item = "30 Bloques de Oro"; coins = 10; break;
            case 6: xp = 80; item = "40 Bloques de Diamante"; coins = 20; break;
            case 7: xp = 90; item = "64 Bloques de Esmeralda"; coins = 30; break;
            case 8: xp = 100; item = "6 Bloques de Netherite"; coins = 40; break;
        }

        lore.add(ChatColor.of("#C77DFF") + "• " + xp + " Niveles de XP");
        lore.add(ChatColor.of("#C77DFF") + "• " + item);
        lore.add(ChatColor.of("#C77DFF") + "• " + coins + " DinoCoins");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        int page = playerPages.getOrDefault(player.getUniqueId(), 1);

        if (slot == 45 && page == 2) {
            openHabilidadesGUI(player, 1);
            return;
        }
        if (slot == 53 && page == 1) {
            openHabilidadesGUI(player, 2);
            return;
        }

        HabilidadesType type = getTypeFromSlot(slot);
        int level = getLevelFromSlot(slot, page);

        if (type == null || level == 0) return;

        handleUnlock(player, type, level);
    }

    private HabilidadesType getTypeFromSlot(int slot) {
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) return HabilidadesType.VITALIDAD;
        if (slot == 28 || slot == 30 || slot == 32 || slot == 34) return HabilidadesType.AGILIDAD;
        if (slot == 46 || slot == 48 || slot == 50 || slot == 52) return HabilidadesType.RESISTENCIA;
        return null;
    }

    private int getLevelFromSlot(int slot, int page) {
        int offset = (page == 1) ? 0 : 4;
        if (slot == 10 || slot == 28 || slot == 46) return 1 + offset;
        if (slot == 12 || slot == 30 || slot == 48) return 2 + offset;
        if (slot == 14 || slot == 32 || slot == 50) return 3 + offset;
        if (slot == 16 || slot == 34 || slot == 52) return 4 + offset;
        return 0;
    }

    private void handleUnlock(Player player, HabilidadesType type, int level) {
        if (manager.hasHabilidadPurchased(player.getUniqueId(), type, level)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!manager.canUnlock(player.getUniqueId(), type, level)) {
            player.sendMessage(ChatColor.RED + "Desbloquea el nivel anterior primero.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int xpCost = 0;
        Material matCost = null;
        int matAmount = 0;
        int coinCost = 0;

        switch(level) {
            case 1: xpCost = 30; matCost = Material.GOLD_BLOCK; matAmount = 12; coinCost = 5; break;
            case 2: xpCost = 40; matCost = Material.DIAMOND_BLOCK; matAmount = 15; coinCost = 10; break;
            case 3: xpCost = 50; matCost = Material.EMERALD_BLOCK; matAmount = 32; coinCost = 15; break;
            case 4: xpCost = 60; matCost = Material.NETHERITE_BLOCK; matAmount = 3; coinCost = 20; break;
            case 5: xpCost = 70; matCost = Material.GOLD_BLOCK; matAmount = 30; coinCost = 10; break;
            case 6: xpCost = 80; matCost = Material.DIAMOND_BLOCK; matAmount = 40; coinCost = 20; break;
            case 7: xpCost = 90; matCost = Material.EMERALD_BLOCK; matAmount = 64; coinCost = 30; break;
            case 8: xpCost = 100; matCost = Material.NETHERITE_BLOCK; matAmount = 6; coinCost = 40; break;
        }

        if (player.getLevel() < xpCost) {
            player.sendMessage(ChatColor.RED + "No tienes suficiente experiencia.");
            return;
        }
        if (!player.getInventory().containsAtLeast(new ItemStack(matCost), matAmount)) {
            player.sendMessage(ChatColor.RED + "No tienes los materiales necesarios.");
            return;
        }
        if (!hasDinoCoins(player, coinCost)) {
            player.sendMessage(ChatColor.RED + "No tienes suficientes DinoCoins.");
            return;
        }

        // Cobrar
        player.setLevel(player.getLevel() - xpCost);
        player.getInventory().removeItem(new ItemStack(matCost, matAmount));
        removeDinoCoins(player, coinCost);

        // Desbloquear
        manager.unlockHabilidad(player.getUniqueId(), type, level);
        player.closeInventory();

        HabilidadesEffects effects = new HabilidadesEffects(plugin);
        effects.playUnlockAnimation(player, type, level);
    }

    private boolean hasDinoCoins(Player player, int amount) {
        int count = 0;
        ItemStack coinItem = EconomyItems.createVithiumCoin();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(coinItem)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeDinoCoins(Player player, int amount) {
        int remaining = amount;
        ItemStack coinItem = EconomyItems.createVithiumCoin();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(coinItem)) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    return;
                } else {
                    remaining -= item.getAmount();
                    player.getInventory().remove(item);
                    if (remaining <= 0) return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().startsWith(GUI_TITLE)) event.setCancelled(true);
    }
}