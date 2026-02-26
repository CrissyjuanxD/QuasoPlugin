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
import java.util.List;

public class HabilidadesGUI implements Listener {

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    private final DayHandler dayHandler;
    // Título en morado pastel
    private final String GUI_TITLE = ChatColor.of("#C77DFF") + "Libro de Habilidades";

    public HabilidadesGUI(JavaPlugin plugin, HabilidadesManager manager, DayHandler dayHandler) {
        this.plugin = plugin;
        this.manager = manager;
        this.dayHandler = dayHandler;
    }

    public void openHabilidadesGUI(Player player) {
        // 54 slots (6 filas)
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE);

        fillGUIWithPanels(gui);
        fillGUIWithSkills(gui, player);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    private void fillGUIWithPanels(Inventory gui) {
        ItemStack purplePanel = createPanel(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack magentaPanel = createPanel(Material.MAGENTA_STAINED_GLASS_PANE, " ");

        // CORRECCIÓN DE SLOTS:
        // Purple Panels: 0, 8, 45, 53, 19-25, 37-43
        int[] purpleSlots = {0, 8, 45, 53, 19, 20, 21, 22, 23, 24, 25, 37, 38, 39, 40, 41, 42, 43};
        for (int slot : purpleSlots) {
            gui.setItem(slot, purplePanel);
        }

        // Magenta Panels: 1-7, 9, 17, 18, 26, 27, 35, 36, 44
        int[] magentaSlots = {1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : magentaSlots) {
            gui.setItem(slot, magentaPanel);
        }

        // Separadores "Siguiente Habilidad"
        ItemStack nextSkill = createPanel(Material.IRON_NUGGET, ChatColor.GRAY + "Siguiente Habilidad");
        int[] nextSlots = {11, 13, 15, 29, 31, 33, 47, 49, 51};
        for (int slot : nextSlots) {
            gui.setItem(slot, nextSkill);
        }
    }

    private void fillGUIWithSkills(Inventory gui, Player player) {
        // Vitalidad: 10, 12, 14, 16
        gui.setItem(10, createHabilidadItem(player, HabilidadesType.VITALIDAD, 1));
        gui.setItem(12, createHabilidadItem(player, HabilidadesType.VITALIDAD, 2));
        gui.setItem(14, createHabilidadItem(player, HabilidadesType.VITALIDAD, 3));
        gui.setItem(16, createHabilidadItem(player, HabilidadesType.VITALIDAD, 4));

        // Agilidad: 28, 30, 32, 34
        gui.setItem(28, createHabilidadItem(player, HabilidadesType.AGILIDAD, 1));
        gui.setItem(30, createHabilidadItem(player, HabilidadesType.AGILIDAD, 2));
        gui.setItem(32, createHabilidadItem(player, HabilidadesType.AGILIDAD, 3));
        gui.setItem(34, createHabilidadItem(player, HabilidadesType.AGILIDAD, 4));

        // Resistencia: 46, 48, 50, 52
        gui.setItem(46, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 1));
        gui.setItem(48, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 2));
        gui.setItem(50, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 3));
        gui.setItem(52, createHabilidadItem(player, HabilidadesType.RESISTENCIA, 4));
    }

    private ItemStack createPanel(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHabilidadItem(Player player, HabilidadesType type, int level) {
        boolean isUnlocked = manager.hasHabilidad(player.getUniqueId(), type, level);
        boolean canUnlock = manager.canUnlock(player.getUniqueId(), type, level);

        ItemStack item;
        if (isUnlocked) {
            // Tinte encantado según el tipo
            switch (type) {
                case AGILIDAD: item = new ItemStack(Material.LIGHT_BLUE_DYE); break;
                case RESISTENCIA: item = new ItemStack(Material.PURPLE_DYE); break;
                case VITALIDAD: item = new ItemStack(Material.RED_DYE); break;
                default: item = new ItemStack(Material.GRAY_DYE);
            }
        } else {
            item = new ItemStack(Material.PAPER);
        }

        ItemMeta meta = item.getItemMeta();
        String displayName = getDisplayName(type, level, isUnlocked);
        meta.setDisplayName(displayName);

        List<String> lore = getLore(type, level, isUnlocked, canUnlock);
        meta.setLore(lore);

        if (isUnlocked) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true); // Visual encantado
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        // Flags para ocultar atributos
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
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
                // Todas dan 1 corazón extra
                desc.add(ChatColor.GRAY + "Otorga +1 Corazón permanente.");
                break;
            case AGILIDAD:
                switch (level) {
                    case 1: desc.add(ChatColor.GRAY + "Haste I infinito."); break;
                    case 2: desc.add(ChatColor.GRAY + "Doble Salto y Salto I infinito."); break;
                    case 3: desc.add(ChatColor.GRAY + "Velocidad I infinito."); break;
                    case 4: desc.add(ChatColor.GRAY + "Triple Salto."); break;
                }
                break;
            case RESISTENCIA:
                switch (level) {
                    case 1: desc.add(ChatColor.GRAY + "10% prob. bloquear proyectiles."); break;
                    case 2: desc.add(ChatColor.GRAY + "5% Parry a jugadores."); break;
                    case 3: desc.add(ChatColor.GRAY + "10% prob. bloquear monstruos."); break;
                    case 4: desc.add(ChatColor.GRAY + "Resistencia I infinita."); break;
                }
                break;
        }
        return desc;
    }

    private void addCostLore(List<String> lore, int level) {
        // Nivel 1: 20 XP, 5 Bloques Oro, 1 Manucoin
        // Nivel 2: 30 XP, 5 Bloques Diamante, 2 Manucoins
        // Nivel 3: 40 XP, 8 Bloques Esmeralda, 2 Manucoins
        // Nivel 4: 50 XP, 1 Bloque Netherite, 3 Manucoins

        int xp = 0;
        String item = "";
        int coins = 0;

        switch(level) {
            case 1: xp = 20; item = "5 Bloques de Oro"; coins = 1; break;
            case 2: xp = 30; item = "5 Bloques de Diamante"; coins = 2; break;
            case 3: xp = 40; item = "8 Bloques de Esmeralda"; coins = 2; break;
            case 4: xp = 50; item = "1 Bloque de Netherite"; coins = 3; break;
        }

        lore.add(ChatColor.of("#C77DFF") + "• " + xp + " Niveles de XP");
        lore.add(ChatColor.of("#C77DFF") + "• " + item);
        lore.add(ChatColor.of("#C77DFF") + "• " + coins + " ManuCoins");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true); // Evitar mover items

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot >= 54) return; // Click fuera

        HabilidadesType type = getTypeFromSlot(slot);
        int level = getLevelFromSlot(slot);

        if (type == null || level == 0) return;

        handleUnlock(player, type, level);
    }

    private HabilidadesType getTypeFromSlot(int slot) {
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) return HabilidadesType.VITALIDAD;
        if (slot == 28 || slot == 30 || slot == 32 || slot == 34) return HabilidadesType.AGILIDAD;
        if (slot == 46 || slot == 48 || slot == 50 || slot == 52) return HabilidadesType.RESISTENCIA;
        return null;
    }

    private int getLevelFromSlot(int slot) {
        if (slot == 10 || slot == 28 || slot == 46) return 1;
        if (slot == 12 || slot == 30 || slot == 48) return 2;
        if (slot == 14 || slot == 32 || slot == 50) return 3;
        if (slot == 16 || slot == 34 || slot == 52) return 4;
        return 0;
    }

    private void handleUnlock(Player player, HabilidadesType type, int level) {
        if (manager.hasHabilidad(player.getUniqueId(), type, level)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!manager.canUnlock(player.getUniqueId(), type, level)) {
            player.sendMessage(ChatColor.RED + "Desbloquea el nivel anterior primero.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Costos
        int xpCost = 0;
        Material matCost = null;
        int matAmount = 0;
        int coinCost = 0;

        switch(level) {
            case 1: xpCost = 20; matCost = Material.GOLD_BLOCK; matAmount = 5; coinCost = 1; break;
            case 2: xpCost = 30; matCost = Material.DIAMOND_BLOCK; matAmount = 5; coinCost = 2; break;
            case 3: xpCost = 40; matCost = Material.EMERALD_BLOCK; matAmount = 8; coinCost = 2; break;
            case 4: xpCost = 50; matCost = Material.NETHERITE_BLOCK; matAmount = 1; coinCost = 3; break;
        }

        if (player.getLevel() < xpCost) {
            player.sendMessage(ChatColor.RED + "No tienes suficiente experiencia.");
            return;
        }
        if (!player.getInventory().containsAtLeast(new ItemStack(matCost), matAmount)) {
            player.sendMessage(ChatColor.RED + "No tienes los materiales necesarios.");
            return;
        }
        if (!hasManuCoins(player, coinCost)) {
            player.sendMessage(ChatColor.RED + "No tienes suficientes ManuCoins.");
            return;
        }

        // Cobrar
        player.setLevel(player.getLevel() - xpCost);
        player.getInventory().removeItem(new ItemStack(matCost, matAmount));
        removeManuCoins(player, coinCost);

        // Desbloquear
        manager.unlockHabilidad(player.getUniqueId(), type, level);
        player.closeInventory();

        // Efectos y aplicar
        HabilidadesEffects effects = new HabilidadesEffects(plugin);
        effects.playUnlockAnimation(player, type, level); // Animación existente
    }

    private boolean hasManuCoins(Player player, int amount) {
        int count = 0;
        ItemStack coinItem = EconomyItems.createVithiumCoin();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(coinItem)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeManuCoins(Player player, int amount) {
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
        if (event.getView().getTitle().equals(GUI_TITLE)) event.setCancelled(true);
    }
}