package Trabajos;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import java.util.List;

import static Trabajos.TrabajosTexto.*;

// El menú de /trabajos: 27 slots, un papel por trabajo (1, 3, 5, 7, 11 y 15) y la guía en el 22
final class TrabajosGUI implements Listener {

    private static final int SLOT_GUIA = 22;

    private final JavaPlugin plugin;
    private final TrabajosManager manager;

    private static final class Menu implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    TrabajosGUI(JavaPlugin plugin, TrabajosManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    void abrir(Player player) {
        Menu menu = new Menu();
        Inventory inventory = Bukkit.createInventory(menu, 27, CAFE_OSCURO + "" + ChatColor.BOLD + "☕ " + CAFE + ChatColor.BOLD + "Trabajos");
        menu.inventory = inventory;
        llenar(player, inventory);
        player.openInventory(inventory);
    }

    private void llenar(Player player, Inventory inventory) {
        ItemStack fondo = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta metaFondo = fondo.getItemMeta();
        metaFondo.setHideTooltip(true);
        fondo.setItemMeta(metaFondo);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, fondo);

        DatosTrabajo datos = manager.datos(player);
        for (Trabajo trabajo : Trabajo.values()) inventory.setItem(trabajo.slot(), papel(datos, trabajo));
        inventory.setItem(SLOT_GUIA, guia());
    }

    // El lore cambia con el nivel: lo que falta para subir, la recompensa del próximo nivel y el próximo bonus
    private ItemStack papel(DatosTrabajo datos, Trabajo trabajo) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.minecraft(trabajo.modelo()));
        meta.setDisplayName(color(trabajo) + "" + ChatColor.BOLD + trabajo.icono() + " " + trabajo.nombre());

        List<String> lore = new ArrayList<>();
        for (String linea : trabajo.descripcion()) lore.add(GRIS + linea);
        lore.add("");

        if (datos == null) {
            lore.add(ROSA + "Cargando tus datos...");
        } else {
            int nivel = datos.nivel(trabajo);
            lore.add(CAFE + "Nivel: " + BLANCO + nivel + GRIS + "/" + Trabajo.NIVEL_MAXIMO);
            if (nivel >= Trabajo.NIVEL_MAXIMO) {
                lore.add(DORADO + "¡Nivel máximo alcanzado!");
            } else {
                int necesita = TrabajoNiveles.xpParaNivel(nivel + 1);
                lore.add(barra(datos.xp(trabajo), necesita));
                lore.add(CAFE + "Te faltan: " + BLANCO + numero(Math.ceil(necesita - datos.xp(trabajo))) + " XP" + GRIS + " para el nivel " + (nivel + 1));
                lore.add(CAFE + "Al subir: " + DORADO + TrabajoNiveles.monedasTotales(nivel + 1) + " DinoCoins" + GRIS + " + "
                        + TrabajoNiveles.experiencia(nivel + 1) + " de experiencia");
                int bonus = TrabajoNiveles.proximoBonus(nivel);
                if (bonus > 0) {
                    lore.add(CAFE + "Próximo bonus: " + BLANCO + "nivel " + bonus + GRIS + " (" + DORADO + "+" + TrabajoNiveles.bonus(bonus) + " DinoCoins" + GRIS + ")");
                }
            }
        }

        lore.add("");
        lore.add(BEIGE + "Cómo ganar XP:");
        for (String fuente : trabajo.fuentes()) lore.add(GRIS + " · " + CREMA + fuente);
        lore.add("");

        if (datos != null && datos.activo == trabajo) {
            lore.add(SALVIA + "✔ Es tu trabajo actual");
            long espera = manager.esperaRestante(datos);
            if (espera > 0) lore.add(GRIS + "Podrás cambiarte en " + tiempo(espera));
            meta.setEnchantmentGlintOverride(true);
        } else {
            long espera = datos == null ? 0 : manager.esperaRestante(datos);
            lore.add(BEIGE + "Entrar cuesta: " + DORADO + TrabajosManager.COSTO_MONEDAS + " DinoCoins" + GRIS + ", "
                    + BLANCO + TrabajosManager.COSTO_NIVELES + " niveles" + GRIS + " y " + ChatColor.of("#9FE2E8") + TrabajosManager.COSTO_DIAMANTES + " diamantes");
            if (espera > 0) lore.add(ROSA + "Podrás cambiarte en " + tiempo(espera));
            else lore.add(SALVIA + "» Clic para entrar");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack guia() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CAFE + "" + ChatColor.BOLD + "✎ Guía de Trabajos");
        meta.setLore(List.of(GRIS + "Todo lo que tienes que saber:", GRIS + "costos, niveles, recompensas", GRIS + "y cómo se gana XP.", "", SALVIA + "» Clic para leer"));
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Menu)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) return;

        if (event.getRawSlot() == SLOT_GUIA) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                TrabajosGuia.abrir(player);
            });
            return;
        }

        Trabajo trabajo = Trabajo.porSlot(event.getRawSlot());
        DatosTrabajo datos = manager.datos(player);
        if (trabajo == null || datos == null || datos.activo == trabajo) return;
        if (manager.esperaRestante(datos) > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1f, 1f);
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.8f, 1.2f);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            manager.unirse(player, trabajo);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }
}
