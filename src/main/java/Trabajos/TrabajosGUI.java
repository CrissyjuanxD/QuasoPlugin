package Trabajos;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static Trabajos.TrabajosTexto.*;

// El menú de /trabajos: 27 slots, un papel por trabajo (1, 3, 5, 7, 11 y 15) y la guía en el 22
final class TrabajosGUI implements Listener {

    private static final int SLOT_GUIA = 22;
    private static final long CONFIRMAR = 5000;

    private final JavaPlugin plugin;
    private final TrabajosManager manager;
    // Entrar pide dos clics seguidos sobre el mismo papel
    private final Map<UUID, Trabajo> confirmando = new HashMap<>();
    private final Map<UUID, Long> confirmandoDesde = new HashMap<>();

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
        for (Trabajo trabajo : Trabajo.values()) inventory.setItem(trabajo.slot(), papel(player, datos, trabajo));
        inventory.setItem(SLOT_GUIA, guia());
    }

    // El lore cambia con el nivel: lo que falta para subir, la recompensa del próximo nivel y el próximo bonus
    private ItemStack papel(Player player, DatosTrabajo datos, Trabajo trabajo) {
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
            if (espera > 0) {
                lore.add(ROSA + "Podrás cambiarte en " + tiempo(espera));
            } else if (trabajo == confirmando.get(player.getUniqueId())) {
                lore.add(DORADO + "» ¡Clic otra vez para confirmar!");
            } else {
                lore.add(SALVIA + "» Clic para entrar");
            }
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
        UUID uuid = player.getUniqueId();

        if (event.getRawSlot() == SLOT_GUIA) {
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                abrirGuia(player);
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

        long ahora = System.currentTimeMillis();
        if (trabajo == confirmando.get(uuid) && ahora - confirmandoDesde.getOrDefault(uuid, 0L) < CONFIRMAR) {
            confirmando.remove(uuid);
            confirmandoDesde.remove(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                manager.unirse(player, trabajo);
            });
            return;
        }
        confirmando.put(uuid, trabajo);
        confirmandoDesde.put(uuid, ahora);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.8f, 1.2f);
        llenar(player, event.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Menu) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        confirmando.remove(event.getPlayer().getUniqueId());
        confirmandoDesde.remove(event.getPlayer().getUniqueId());
    }

    // La guía (libro): colores oscuros porque el papel del libro es claro
    void abrirGuia(Player player) {
        List<String> paginas = List.of(
                "§6§l  Guía de Trabajos\n\n§0Elige un oficio y gana §6DinoCoins §0y experiencia mientras juegas.\n\n"
                        + "§8Hay §06 trabajos§8 y cada uno tiene §0100 niveles§8.\n\n§8Pasa la página →",
                "§6§lCómo entrar\n\n§0Entrar cuesta:\n§8• §05 DinoCoins\n§8• §010 niveles de exp.\n§8• §05 diamantes\n\n"
                        + "§8Las DinoCoins salen del inventario o de tus monederos.\n\n§0Solo puedes tener §lun§0 trabajo a la vez.",
                "§6§lCambiar de trabajo\n\n§0Después de entrar hay que esperar §l24 horas§0 para cambiarte.\n\n"
                        + "§0Tu nivel en cada trabajo §2se guarda§0: si vuelves a uno sigues donde lo dejaste.",
                "§6§lRecompensas\n\n§0Cada nivel da DinoCoins según lo difícil que es:\n§8Nv 1-20: §01\n§8Nv 21-40: §02\n"
                        + "§8Nv 41-60: §03\n§8Nv 61-80: §04\n§8Nv 81-100: §05\n\n§0Y experiencia de Minecraft.",
                "§6§lBonus\n\n§0Cada §l5 niveles§0 hay un bonus de DinoCoins que crece con el nivel:\n§8Nv 5: §0+8\n§8Nv 25: §0+18\n"
                        + "§8Nv 50: §0+30\n§8Nv 100: §0+55\n\n§8Cada 25 niveles se entera todo el server.",
                "§4§l⚔ Guerrero\n§0Monstruos: 4 a 15 XP según su vida. Jefes: 250, Wither 400 y Dragón 600.\n\n"
                        + "§8§l⛏ Minería\n§0Carbón y cobre 2-3, hierro 5-6, oro y lapis 7-8, diamante 15-18, esmeralda 20-22 y debris 30. Piedra 0,05.",
                "§6§l☘ Leñador\n§0Troncos 1,2 XP, tallos del Nether 1,4 y raíces de manglar 0,5.\n\n"
                        + "§6§l⚒ Constructor\n§0Bloques básicos 0,1, comunes 0,3 y trabajados (ladrillo, cuarzo, concreto...) 0,5.",
                "§2§l✿ Granjero\n§0Cultivos maduros 0,6 a 1,5 XP, sandías y calabazas 1, caña 0,3, bayas 0,5 y criar animales 4.\n\n"
                        + "§3§l⚓ Pescador\n§0Peces 6 a 8 XP, tesoros 12 y basura 3. En las zonas de pesca también cuenta.",
                "§6§lReglas\n\n§0• Los bloques que pones no dan XP al romperlos.\n§0• Los mobs de spawner no cuentan.\n"
                        + "§0• Si estás AFK no ganas XP.\n§0• Pasadas 1.500 XP en una hora, lo demás rinde 25%.",
                "§6§lComandos\n\n§0/trabajos\n§8Abre el menú.\n\n§0/trabajos info\n§8Tu trabajo, nivel, lo que falta y la recompensa.\n\n"
                        + "§0Cada trabajo tiene §610 misiones§0 (niveles 10 al 100)."
        );
        List<Component> componentes = new ArrayList<>();
        for (String pagina : paginas) componentes.add(LegacyComponentSerializer.legacySection().deserialize(pagina));
        player.openBook(Book.book(Component.text("Guía de Trabajos"), Component.text("QuasoPlugin"), componentes));
    }
}
