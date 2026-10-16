package Habilidades;

import Gui.Invisible;
import Gui.dinocoins.DinoCoinsManager;
import items.EconomyItemsFunctions;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HabilidadesGUI implements Listener {

    // El fondo es una textura del resource pack que se dibuja con estos caracteres del título (igual que misiones)
    static final String TITULO = "\u3201" + ChatColor.WHITE + "\u3206";

    // Los slots que antes tenían tintes morados, magentas y negros
    static final int[] SEPARADORES = {0, 8, 45, 53, 1, 2, 3, 4, 5, 6, 7, 9, 17, 18, 26, 27, 35, 36, 44,
            19, 20, 21, 22, 23, 24, 25, 37, 38, 39, 40, 41, 42, 43};

    private final JavaPlugin plugin;
    private final HabilidadesManager manager;
    // Para cobrar de los monederos (null en los tests: entonces solo cuenta el inventario)
    private final DinoCoinsManager monedas;
    private final Set<UUID> pagando = new HashSet<>();

    // Marca el inventario como árbol de habilidades y recuerda la página
    private static final class Menu implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        Menu(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public HabilidadesGUI(JavaPlugin plugin, HabilidadesManager manager, DinoCoinsManager monedas) {
        this.plugin = plugin;
        this.manager = manager;
        this.monedas = monedas;
    }

    public void openHabilidadesGUI(Player player) {
        openHabilidadesGUI(player, 1);
    }

    // Dos páginas: niveles 1-4 y 5-8 de cada habilidad
    public void openHabilidadesGUI(Player player, int page) {
        Menu menu = new Menu(page);
        Inventory gui = Bukkit.createInventory(menu, 54, TITULO);
        menu.inventory = gui;

        fillGUIWithPanels(gui, page);
        fillGUIWithSkills(gui, player, page);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
    }

    // Los separadores no se ven (modelo del aire) porque el árbol lo dibuja la textura del menú
    private void fillGUIWithPanels(Inventory gui, int page) {
        ItemStack separador = Invisible.relleno();
        for (int slot : SEPARADORES) {
            gui.setItem(slot, separador);
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

        if (page == 2) {
            gui.setItem(45, createArrow("§e⬅ Anterior Página", true));
        }
        if (page == 1) {
            gui.setItem(53, createArrow("§eSiguiente Página ➔", false));
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

    private ItemStack createArrow(String name, boolean previous) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setItemModel(NamespacedKey.minecraft(previous ? "flecha_izquierda" : "flecha_derecha"));
            meta.setEnchantmentGlintOverride(false);
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

        // La textura se ve encendida si ya lo tiene y apagada si no, con el nivel escrito (N1 a N8)
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = getDisplayName(type, level, isUnlocked);
            meta.setDisplayName(displayName);

            List<String> lore = getLore(type, level, isUnlocked, canUnlock);
            meta.setLore(lore);
            meta.setItemModel(NamespacedKey.minecraft(modelo(type, level, isUnlocked)));

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }
        return item;
    }

    // minecraft:habilidad_<rama>_<nivel>_<on u off>: una textura por nivel con la placa N1 a N8
    static String modelo(HabilidadesType type, int level, boolean isUnlocked) {
        return "habilidad_" + type.name().toLowerCase() + "_" + level + "_" + (isUnlocked ? "on" : "off");
    }

    private String getDisplayName(HabilidadesType type, int level, boolean isUnlocked) {
        String name = type.getDisplayName() + " Nivel " + level;
        if (isUnlocked) {
            return ChatColor.of("#8FE3A7") + "" + ChatColor.BOLD + name + " ✓";
        } else {
            return ChatColor.of("#6CCB8A") + "" + ChatColor.BOLD + name;
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
            lore.add(ChatColor.of("#C8F5D2") + "Costo:");
            addCostLore(lore, level);
            lore.add("");
            lore.add(ChatColor.of("#6CCB8A") + "Click para desbloquear");
        }

        return lore;
    }

    private List<String> getHabilidadDescription(HabilidadesType type, int level) {
        List<String> desc = new ArrayList<>();
        desc.add(ChatColor.GRAY + type.descripcion(level));
        return desc;
    }

    private void addCostLore(List<String> lore, int level) {
        HabilidadesType.Costo costo = HabilidadesType.costo(level);
        lore.add(ChatColor.of("#8FE3A7") + "• " + costo.xp() + " Niveles de XP");
        lore.add(ChatColor.of("#8FE3A7") + "• " + costo.cantidad() + " " + costo.nombre());
        lore.add(ChatColor.of("#8FE3A7") + "• " + costo.dinocoins() + " DinoCoins " + ChatColor.GRAY + "(inventario o monedero)");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu menu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        int page = menu.page;

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

    // Cobra XP, bloques y DinoCoins según el nivel y desbloquea la habilidad con la animación. Las DinoCoins salen
    // primero del inventario y lo que falte de los monederos; si al final no alcanza, se devuelve lo que se sacó
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

        HabilidadesType.Costo costo = HabilidadesType.costo(level);
        String falta = falta(player, costo);
        if (falta != null) {
            player.sendMessage(ChatColor.RED + falta);
            return;
        }
        UUID uuid = player.getUniqueId();
        if (pagando.contains(uuid)) return;
        int coinCost = costo.dinocoins();
        int enInventario = monedasEnInventario(player);
        if (enInventario >= coinCost || monedas == null) {
            if (enInventario < coinCost) {
                player.sendMessage(ChatColor.RED + "Necesitas " + coinCost + " DinoCoins.");
                return;
            }
            cobrarYDesbloquear(player, type, level, costo, coinCost);
            return;
        }
        int faltan = coinCost - enInventario;
        pagando.add(uuid);
        monedas.changeAsyncMoved(player, -faltan, movidas -> {
            pagando.remove(uuid);
            if (!player.isOnline()) {
                if (movidas > 0) plugin.getLogger().warning(player.getName() + " se fue mientras compraba una habilidad: se le cobraron "
                        + movidas + " DinoCoins de su monedero.");
                return;
            }
            String ahoraFalta = falta(player, costo);
            boolean yaNoSePuede = manager.hasHabilidadPurchased(uuid, type, level) || !manager.canUnlock(uuid, type, level);
            if (movidas < faltan || ahoraFalta != null || yaNoSePuede || monedasEnInventario(player) < enInventario) {
                if (movidas > 0) monedas.deposit(player, movidas);
                if (!yaNoSePuede) player.sendMessage(ChatColor.RED + (ahoraFalta != null ? ahoraFalta
                        : "Necesitas " + coinCost + " DinoCoins en el inventario o en tus monederos."));
                return;
            }
            cobrarYDesbloquear(player, type, level, costo, enInventario);
        });
    }

    // Lo que falta aparte de las DinoCoins (null = tiene todo)
    private static String falta(Player player, HabilidadesType.Costo costo) {
        if (player.getLevel() < costo.xp()) return "Necesitas " + costo.xp() + " niveles de experiencia.";
        if (!player.getInventory().containsAtLeast(new ItemStack(costo.bloque()), costo.cantidad())) {
            return "Necesitas " + costo.cantidad() + " " + costo.nombre() + ".";
        }
        return null;
    }

    private void cobrarYDesbloquear(Player player, HabilidadesType type, int level, HabilidadesType.Costo costo, int delInventario) {
        player.setLevel(player.getLevel() - costo.xp());
        player.getInventory().removeItem(new ItemStack(costo.bloque(), costo.cantidad()));
        quitarMonedasDelInventario(player, delInventario);

        manager.unlockHabilidad(player.getUniqueId(), type, level);
        player.closeInventory();

        HabilidadesEffects effects = new HabilidadesEffects(plugin);
        effects.playUnlockAnimation(player, type, level);
    }

    static int monedasEnInventario(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (EconomyItemsFunctions.isDinoCoin(item)) total += item.getAmount();
        }
        return total;
    }

    private static void quitarMonedasDelInventario(Player player, int cantidad) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && cantidad > 0; i++) {
            ItemStack item = contents[i];
            if (!EconomyItemsFunctions.isDinoCoin(item)) continue;
            int quitar = Math.min(cantidad, item.getAmount());
            item.setAmount(item.getAmount() - quitar);
            cantidad -= quitar;
            if (item.getAmount() <= 0) contents[i] = null;
        }
        player.getInventory().setStorageContents(contents);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
}