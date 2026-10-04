package Events.MissionSystem;

import items.ItemModels;
import items.Misionesitem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MissionGUI implements Listener {
    // El fondo del menú es una textura del resource pack que se dibuja con estos caracteres del título
    private static final String TITLE = "㈁㈁" + org.bukkit.ChatColor.WHITE + "㈂";
    private static final int PANEL_SLOTS = 18;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int[] MISSION_SLOTS = buildMissionSlots();

    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;

    // Marca el inventario como menú de misiones y recuerda la página
    private static class MissionMenu implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        MissionMenu(int page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public MissionGUI(JavaPlugin plugin, MissionHandler missionHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // Misiones del slot 18 al 52, menos el 45 que es la flecha (34 por página)
    private static int[] buildMissionSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int slot = PANEL_SLOTS; slot < 54; slot++) {
            if (slot != PREV_SLOT && slot != NEXT_SLOT) slots.add(slot);
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    // Las páginas salen de cuántas misiones hay en el menú (140 misiones = 5 páginas)
    private int maxPages() {
        return Math.max(1, (menuOrder().size() + MISSION_SLOTS.length - 1) / MISSION_SLOTS.length);
    }

    // Cada misión normal seguida de sus extras (#1, sus extras, #2, sus extras...); una extra sin su misión va al final
    private List<Integer> menuOrder() {
        List<Integer> order = new ArrayList<>();
        for (Mission mission : missionHandler.getMissions().values()) {
            if (mission.getParentMission() != 0) continue;
            order.add(mission.getMissionNumber());
            order.addAll(missionHandler.getExtras(mission.getMissionNumber()));
        }
        for (int number : missionHandler.getMissions().keySet()) {
            if (!order.contains(number)) order.add(number);
        }
        return order;
    }

    // El item de Misiones abre el menú (los viejos con custom model data 9999 también sirven)
    @EventHandler
    public void onItemInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!Misionesitem.isMisiones(event.getItem())) return;

        event.setCancelled(true);
        openMissionGUI(event.getPlayer(), 1);
    }

    public void openMissionGUI(Player player) {
        openMissionGUI(player, 1);
    }

    public void openMissionGUI(Player player, int page) {
        int pages = maxPages();
        MissionMenu menu = new MissionMenu(page);
        Inventory gui = Bukkit.createInventory(menu, 54, TITLE);
        menu.inventory = gui;

        ItemStack panel = createPanel();
        for (int slot = 0; slot < PANEL_SLOTS; slot++) {
            gui.setItem(slot, panel);
        }

        gui.setItem(PREV_SLOT, createArrow("§e⬅ Anterior Página", page, pages));
        gui.setItem(NEXT_SLOT, createArrow("§eSiguiente Página ➔", page, pages));

        List<Integer> order = menuOrder();
        int first = (page - 1) * MISSION_SLOTS.length;
        for (int i = 0; i < MISSION_SLOTS.length && first + i < order.size(); i++) {
            int missionNum = order.get(first + i);
            Mission mission = missionHandler.getMissions().get(missionNum);
            gui.setItem(MISSION_SLOTS[i], createMissionItem(mission, player, missionHandler.getData(player, missionNum), missionNum));
        }

        player.openInventory(gui);
    }

    private ItemStack createPanel() {
        ItemStack panel = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = panel.getItemMeta();
        meta.setDisplayName(" ");
        meta.setHideTooltip(true);
        ItemModels.apply(meta, "gui_panel");
        panel.setItemMeta(meta);
        return panel;
    }

    private ItemStack createArrow(String name, int page, int pages) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.of("#D3D3D3") + "Página " + page + " de " + pages));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // Papel con el item model según el estado: bloqueada, pendiente o completada
    private ItemStack createMissionItem(Mission mission, Player player, MissionData data, int missionNum) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (!data.isActive()) {
            meta.setDisplayName(ChatColor.of("#A0A0A0") + missionHandler.tag(missionNum) + " ???");
            lore.add(ChatColor.of("#D3D3D3") + "Misión no descubierta");
            ItemModels.apply(meta, "mision_bloqueada");
        } else {
            boolean completed = data.isCompleted();
            meta.setDisplayName(ChatColor.of(completed ? "#90EE90" : "#FFB6C1") + missionHandler.displayName(missionNum));
            ItemModels.apply(meta, completed ? "mision_completada" : "mision_pendiente");

            for (String line : mission.getDescription().split("\n")) {
                lore.add(ChatColor.of("#D3D3D3") + line);
            }
            lore.add("");
            if (mission instanceof BaseMission base) {
                lore.add(ChatColor.of("#F0E68C") + "Dificultad: " + base.getDifficulty().colored());
                lore.add(ChatColor.of("#F0E68C") + "Recompensa: " + ChatColor.of("#FFD700") + base.getCoins() + " DinoCoins "
                        + ChatColor.of("#D3D3D3") + "+ objetos");
            }
            if (mission.getParentMission() > 0) {
                lore.add(ChatColor.of("#7FD4FF") + "Misión extra (sale con la #" + mission.getParentMission() + ")");
            }
            lore.add(completed ? ChatColor.of("#98FB98") + "✔ Completada" : ChatColor.of("#FFA07A") + "✖ Pendiente");

            if (mission instanceof BaseMission base && !completed) {
                List<String> progress = base.progressLines(player, data);
                if (!progress.isEmpty()) {
                    lore.add("");
                    lore.addAll(progress);
                }
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    // Flechas para cambiar de página (dan la vuelta al llegar al final)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MissionMenu menu)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int pages = maxPages();
        if (event.getRawSlot() == PREV_SLOT) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            openMissionGUI(player, menu.page <= 1 ? pages : menu.page - 1);
        } else if (event.getRawSlot() == NEXT_SLOT) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            openMissionGUI(player, menu.page >= pages ? 1 : menu.page + 1);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MissionMenu) {
            event.setCancelled(true);
        }
    }
}
