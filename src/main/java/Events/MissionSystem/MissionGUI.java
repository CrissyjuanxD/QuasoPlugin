package Events.MissionSystem;

import Gui.Invisible;
import items.Misionesitem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import java.util.Comparator;
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

    // El menú tiene tres secciones seguidas, cada una desde una página nueva: las misiones (1 a 100), las extras
    // (ordenadas por su misión: Extra #1, Extra #2...) y las de trabajo (141 a 200)
    private List<Integer> order(TipoMision tipo) {
        List<Integer> order = new ArrayList<>();
        for (Mission mission : missionHandler.getMissions().values()) {
            if (missionHandler.tipo(mission.getMissionNumber()) == tipo) order.add(mission.getMissionNumber());
        }
        if (tipo == TipoMision.EXTRA) {
            order.sort(Comparator.comparingInt(number -> missionHandler.getMissions().get(number).getParentMission()));
        }
        return order;
    }

    private int pagesOf(List<Integer> order) {
        return (order.size() + MISSION_SLOTS.length - 1) / MISSION_SLOTS.length;
    }

    private int maxPages() {
        int pages = 0;
        for (TipoMision tipo : TipoMision.values()) pages += pagesOf(order(tipo));
        return Math.max(1, pages);
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

        // Busca en qué sección cae la página
        TipoMision tipo = TipoMision.NORMAL;
        List<Integer> order = List.of();
        int first = 0;
        int before = 0;
        for (TipoMision candidate : TipoMision.values()) {
            List<Integer> list = order(candidate);
            int sectionPages = pagesOf(list);
            if (page <= before + sectionPages) {
                tipo = candidate;
                order = list;
                first = (page - before - 1) * MISSION_SLOTS.length;
                break;
            }
            before += sectionPages;
        }

        // Las dos primeras filas (el panel de la textura) y los slots sin misión llevan items invisibles
        ItemStack relleno = Invisible.relleno();
        for (int slot = 0; slot < PANEL_SLOTS; slot++) gui.setItem(slot, relleno);

        gui.setItem(PREV_SLOT, createArrow("§e⬅ Anterior Página", page, pages, tipo));
        gui.setItem(NEXT_SLOT, createArrow("§eSiguiente Página ➔", page, pages, tipo));

        for (int i = 0; i < MISSION_SLOTS.length; i++) {
            if (first + i >= order.size()) {
                gui.setItem(MISSION_SLOTS[i], relleno);
                continue;
            }
            int missionNum = order.get(first + i);
            Mission mission = missionHandler.getMissions().get(missionNum);
            gui.setItem(MISSION_SLOTS[i], createMissionItem(mission, player, missionHandler.getData(player, missionNum), missionNum));
        }

        player.openInventory(gui);
    }

    private ItemStack createArrow(String name, int page, int pages, TipoMision tipo) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        String section = switch (tipo) {
            case NORMAL -> "Misiones";
            case EXTRA -> "Misiones extra";
            case TRABAJO -> "Misiones de trabajo";
        };
        meta.setLore(List.of(ChatColor.of("#D3D3D3") + "Página " + page + " de " + pages, ChatColor.of(tipo.primario) + section));
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    // Papel con el item model según el estado: bloqueada, pendiente o completada. El nombre va con el color de su tipo
    private ItemStack createMissionItem(Mission mission, Player player, MissionData data, int missionNum) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        TipoMision tipo = missionHandler.tipo(missionNum);

        if (!data.isActive()) {
            meta.setDisplayName(ChatColor.of("#A0A0A0") + missionHandler.tag(missionNum) + " ???");
            lore.add(ChatColor.of("#D3D3D3") + "Misión no descubierta");
            meta.setItemModel(NamespacedKey.minecraft("map"));
        } else {
            boolean completed = data.isCompleted();
            meta.setDisplayName(ChatColor.of(completed ? "#90EE90" : tipo.secundario) + missionHandler.displayName(missionNum));
            meta.setItemModel(NamespacedKey.minecraft(completed ? "lime_banner" : "guster_banner_pattern"));

            for (String line : mission.getDescription().split("\n")) {
                lore.add(ChatColor.of("#D3D3D3") + line);
            }
            lore.add("");
            if (mission instanceof BaseMission base) {
                lore.add(ChatColor.of("#F0E68C") + "Dificultad: " + base.getDifficulty().colored());
                lore.add(ChatColor.of("#F0E68C") + "Recompensa: " + ChatColor.of("#FFD700") + base.getCoins() + " DinoCoins "
                        + ChatColor.of("#D3D3D3") + (tipo == TipoMision.EXTRA ? "directo al monedero" : "+ objetos"));
            }
            if (tipo == TipoMision.EXTRA) {
                lore.add(ChatColor.of(tipo.primario) + "Misión extra de la #" + mission.getParentMission());
            }
            if (mission instanceof MisionTrabajo trabajo) {
                lore.add(ChatColor.of(trabajo.getTrabajo().color()) + trabajo.getTrabajo().icono() + " Misión de trabajo");
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
