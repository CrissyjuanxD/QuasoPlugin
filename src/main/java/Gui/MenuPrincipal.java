package Gui;

import Events.MissionSystem.MissionGUI;
import Habilidades.HabilidadesGUI;
import Habilidades.HabilidadesManager;
import Trabajos.TrabajosManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

// /menu: el menú principal para todos. Abre las misiones, los trabajos, el árbol de habilidades (si ya gastaste el
// libro), /proteccion y /home list
public class MenuPrincipal implements CommandExecutor, Listener {

    // El fondo es una textura del resource pack que se dibuja con estos caracteres del título (igual que misiones)
    static final String TITULO = "㈁㈁" + ChatColor.WHITE + "㈃";

    private static final ChatColor GRIS = ChatColor.of("#B8B0C8");
    private static final ChatColor ROJO = ChatColor.of("#FF8A8A");

    private final JavaPlugin plugin;
    private final MissionGUI misiones;
    private final TrabajosManager trabajos;
    private final HabilidadesManager habilidades;
    private final HabilidadesGUI arbol;

    private static final class Menu implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public MenuPrincipal(JavaPlugin plugin, MissionGUI misiones, TrabajosManager trabajos, HabilidadesManager habilidades, HabilidadesGUI arbol) {
        this.plugin = plugin;
        this.misiones = misiones;
        this.trabajos = trabajos;
        this.habilidades = habilidades;
        this.arbol = arbol;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }
        abrir(player);
        return true;
    }

    public void abrir(Player player) {
        Menu menu = new Menu();
        Inventory inventory = Bukkit.createInventory(menu, 54, TITULO);
        menu.inventory = inventory;

        for (SeccionMenu seccion : SeccionMenu.values()) {
            ItemStack boton = boton(player, seccion);
            for (int slot : seccion.slots()) inventory.setItem(slot, boton);
        }
        ItemStack relleno = Invisible.relleno();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (SeccionMenu.porSlot(slot) == null) inventory.setItem(slot, relleno);
        }

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
    }

    // El mismo item invisible en todos los slots de la zona; la descripción depende del jugador
    private ItemStack boton(Player player, SeccionMenu seccion) {
        ChatColor color = ChatColor.of(seccion.color);
        String abrir = color + "» Clic para abrir";
        List<String> lore = switch (seccion) {
            case MISIONES -> List.of(GRIS + "La misión del día, las extra", GRIS + "y las de trabajo.", "", abrir);
            case TRABAJOS -> List.of(GRIS + "Elige tu trabajo y mira tu", GRIS + "nivel en cada uno.",
                    GRIS + "Tu trabajo: " + TrabajosManager.lineaScoreboard(player), "", abrir);
            case HABILIDADES -> habilidades.tieneAcceso(player.getUniqueId())
                    ? List.of(GRIS + "Tu árbol de Vitalidad,", GRIS + "Resistencia y Agilidad.", "", abrir)
                    : List.of(ROJO + "Bloqueado", GRIS + "Usa un Libro de Habilidades", GRIS + "para desbloquear tu árbol.",
                    GRIS + "Lo vende la Biblioteca.");
            case PROTECCIONES -> List.of(GRIS + "Las protecciones de tu base.", "", abrir);
            case HOMES -> List.of(GRIS + "Tus homes guardados.", GRIS + "Guarda uno con /sethome.", "", abrir);
        };
        return Invisible.boton(color + "" + ChatColor.BOLD + seccion.nombre, lore);
    }

    // Cada zona abre lo suyo al tick siguiente; Protecciones y Homes cierran el menú y ejecutan el comando
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) return;

        SeccionMenu seccion = SeccionMenu.porSlot(event.getRawSlot());
        if (seccion == null) return;

        if (seccion == SeccionMenu.HABILIDADES && !habilidades.tieneAcceso(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
            player.sendMessage(ROJO + "Primero usa un Libro de Habilidades para desbloquear tu árbol. Lo vende la Biblioteca.");
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.8f, 1.2f);
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (seccion) {
                case MISIONES -> misiones.openMissionGUI(player);
                case TRABAJOS -> trabajos.abrirMenu(player);
                case HABILIDADES -> arbol.openHabilidadesGUI(player);
                case PROTECCIONES -> {
                    player.closeInventory();
                    player.performCommand("proteccion");
                }
                case HOMES -> {
                    player.closeInventory();
                    player.performCommand("home list");
                }
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
}
