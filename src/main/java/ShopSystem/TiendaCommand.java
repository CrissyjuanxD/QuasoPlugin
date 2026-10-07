package ShopSystem;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// /tienda: carga el catálogo por secciones en los aldeanos de la tienda. El día que toca, el admin pone /tienda dia 20
// y todos los aldeanos asignados cambian sus tradeos (items nuevos y precios 15% más altos)
public class TiendaCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISO = "quasoplugin.tienda.admin";
    private static final String PREFIJO = ChatColor.of("#E6C77A") + "" + ChatColor.BOLD + "Tienda " + ChatColor.of("#9C7A5B") + "» " + ChatColor.of("#F5EBDD");
    private static final ChatColor GRIS = ChatColor.of("#A89F94");
    private static final ChatColor BLANCO = ChatColor.WHITE;
    private static final ChatColor DORADO = ChatColor.of("#E6C77A");

    private final ShopManager shopManager;

    public TiendaCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISO)) {
            sender.sendMessage(PREFIJO + ChatColor.RED + "No tienes permiso.");
            return true;
        }
        if (args.length == 0) {
            ayuda(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "dia" -> {
                Integer dia = numero(sender, args, 1);
                if (dia != null) cambiarSeccion(sender, CatalogoTienda.seccionDelDia(dia));
            }
            case "seccion" -> {
                Integer seccion = numero(sender, args, 1);
                if (seccion == null) return true;
                if (seccion < 1 || seccion > CatalogoTienda.DIAS.length) {
                    sender.sendMessage(PREFIJO + "La sección va de 1 a " + CatalogoTienda.DIAS.length + ".");
                    return true;
                }
                cambiarSeccion(sender, seccion);
            }
            case "crear" -> crear(sender, args);
            case "asignar" -> asignar(sender, args);
            case "precios" -> precios(sender, args);
            case "precio" -> precio(sender, args);
            case "ajuste" -> ajuste(sender, args);
            case "soltar" -> soltar(sender);
            case "nueva" -> nueva(sender, args);
            case "info" -> info(sender);
            default -> ayuda(sender);
        }
        return true;
    }

    private void cambiarSeccion(CommandSender sender, int seccion) {
        int antes = shopManager.getSeccion();
        shopManager.setSeccion(seccion);
        int actualizadas = 0;
        for (Map.Entry<String, String> entrada : shopManager.getCatalogos().entrySet()) {
            CatalogoTienda.Tienda tienda = CatalogoTienda.tienda(entrada.getValue());
            if (tienda == null) continue;
            shopManager.aplicarCatalogo(entrada.getKey(), tienda, seccion);
            actualizadas++;
        }
        sender.sendMessage(PREFIJO + "Sección " + BLANCO + seccion + GRIS + " (día " + CatalogoTienda.diaDeLaSeccion(seccion) + ")"
                + ChatColor.of("#F5EBDD") + ": " + BLANCO + actualizadas + ChatColor.of("#F5EBDD") + " aldeanos actualizados.");
        if (seccion > antes) {
            Bukkit.broadcastMessage(PREFIJO + DORADO + "¡La tienda del spawn se actualizó!" + ChatColor.of("#F5EBDD")
                    + " Hay items nuevos y lo que ya estaba subió de precio. " + GRIS + "/tiendas");
        }
    }

    // Aparece un aldeano nuevo donde estás, con el nombre y los tradeos de esa tienda del catálogo
    private void crear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIJO + "Solo jugadores.");
            return;
        }
        CatalogoTienda.Tienda tienda = tiendaDe(sender, args);
        if (tienda == null) return;
        Location lugar = player.getLocation();
        Villager.Profession profesion = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(tienda.profesion()));
        Villager villager = shopManager.spawnShop(tienda.nombre(), lugar, Villager.Type.PLAINS, profesion);
        String shopId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
        shopManager.setCatalogo(shopId, tienda.id());
        shopManager.aplicarCatalogo(shopId, tienda, shopManager.getSeccion());
        sender.sendMessage(PREFIJO + "Tienda " + BLANCO + tienda.id() + ChatColor.of("#F5EBDD") + " creada con la sección " + shopManager.getSeccion() + ".");
    }

    // Le pone una tienda del catálogo al aldeano que estás mirando (reemplaza sus tradeos)
    private void asignar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIJO + "Solo jugadores.");
            return;
        }
        CatalogoTienda.Tienda tienda = tiendaDe(sender, args);
        if (tienda == null) return;
        Villager villager = aldeanoMirado(player);
        if (villager == null) {
            sender.sendMessage(PREFIJO + ChatColor.RED + "Mira a un aldeano de la tienda (a 5 bloques o menos).");
            return;
        }
        String shopId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
        shopManager.setCatalogo(shopId, tienda.id());
        shopManager.aplicarCatalogo(shopId, tienda, shopManager.getSeccion());
        sender.sendMessage(PREFIJO + "Ese aldeano ahora es la tienda " + BLANCO + tienda.id() + ChatColor.of("#F5EBDD") + ".");
    }

    // /tienda precio <tienda> <producto> <precio|auto>: fija el precio de un producto en todas las secciones
    private void precio(CommandSender sender, String[] args) {
        CatalogoTienda.Tienda tienda = tiendaDe(sender, args);
        if (tienda == null) return;
        CatalogoTienda.Oferta oferta = args.length > 2 ? oferta(tienda, args[2]) : null;
        if (oferta == null || args.length < 4) {
            sender.sendMessage(PREFIJO + "Uso: /tienda precio " + tienda.id() + " <producto> <precio|auto>");
            sender.sendMessage(GRIS + "Productos: " + BLANCO + String.join(", ", productos(tienda)));
            return;
        }
        Integer precio = null;
        if (!args[3].equalsIgnoreCase("auto")) {
            precio = numero(sender, args, 3);
            if (precio == null) return;
            if (precio < 1 || precio > CatalogoTienda.PRECIO_MAXIMO) {
                sender.sendMessage(PREFIJO + ChatColor.RED + "El precio va de 1 a " + CatalogoTienda.PRECIO_MAXIMO + ".");
                return;
            }
        }
        shopManager.setPrecioManual(tienda.id(), oferta.producto(), precio);
        int aldeanos = shopManager.reaplicar(tienda);
        sender.sendMessage(PREFIJO + BLANCO + oferta.producto() + ChatColor.of("#F5EBDD") + " en " + BLANCO + tienda.id() + ChatColor.of("#F5EBDD")
                + (precio == null ? " vuelve al precio automático (" + DORADO + shopManager.precio(tienda, oferta, shopManager.getSeccion()) + ChatColor.of("#F5EBDD") + ")"
                : " ahora cuesta " + DORADO + precio + " " + oferta.pago()) + GRIS + " · " + aldeanos + (aldeanos == 1 ? " aldeano" : " aldeanos"));
    }

    // /tienda ajuste <tienda|todas> <porcentaje>: sube o baja todos los precios automáticos (-20 = 20% más barato, 0 = normal)
    private void ajuste(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIJO + "Uso: /tienda ajuste <tienda|todas> <porcentaje>  " + GRIS + "(-20 = 20% más barato, 0 = normal)");
            return;
        }
        List<CatalogoTienda.Tienda> tiendas = new ArrayList<>();
        if (args[1].equalsIgnoreCase("todas")) {
            tiendas.addAll(CatalogoTienda.tiendas());
        } else {
            CatalogoTienda.Tienda tienda = tiendaDe(sender, args);
            if (tienda == null) return;
            tiendas.add(tienda);
        }
        Integer ajuste = numero(sender, new String[]{args[2].replace("%", "").replace("+", "")}, 0);
        if (ajuste == null) return;
        if (ajuste < -90 || ajuste > 300) {
            sender.sendMessage(PREFIJO + ChatColor.RED + "El ajuste va de -90 a 300.");
            return;
        }
        int aldeanos = 0;
        for (CatalogoTienda.Tienda tienda : tiendas) {
            shopManager.setAjuste(tienda.id(), ajuste);
            aldeanos += shopManager.reaplicar(tienda);
        }
        sender.sendMessage(PREFIJO + "Ajuste de " + BLANCO + (ajuste > 0 ? "+" : "") + ajuste + "%" + ChatColor.of("#F5EBDD") + " en "
                + BLANCO + (tiendas.size() == 1 ? tiendas.get(0).id() : "todas las tiendas") + GRIS + " · " + aldeanos
                + (aldeanos == 1 ? " aldeano actualizado" : " aldeanos actualizados") + ". Los precios puestos a mano no cambian.");
    }

    // El aldeano que miras deja de ser automático: se queda con sus tradeos y se edita con shift + clic derecho
    private void soltar(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIJO + "Solo jugadores.");
            return;
        }
        Villager villager = aldeanoMirado(player);
        if (villager == null) {
            sender.sendMessage(PREFIJO + ChatColor.RED + "Mira a un aldeano de la tienda (a 5 bloques o menos).");
            return;
        }
        String shopId = villager.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
        String catalogo = shopManager.getCatalogos().get(shopId);
        if (catalogo == null) {
            sender.sendMessage(PREFIJO + "Ese aldeano ya es una tienda normal: edítala con shift + clic derecho.");
            return;
        }
        shopManager.soltarCatalogo(shopId);
        sender.sendMessage(PREFIJO + "Ese aldeano ya no es la tienda automática " + BLANCO + catalogo + ChatColor.of("#F5EBDD")
                + ": se queda con sus tradeos y se edita con shift + clic derecho. " + GRIS + "(/tienda asignar " + catalogo + " para volver)");
    }

    // Una tienda vacía donde estás, para llenarla a mano con shift + clic derecho
    private void nueva(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIJO + "Solo jugadores.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIJO + "Uso: /tienda nueva <nombre>  " + GRIS + "(acepta colores con &)");
            return;
        }
        String nombre = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        shopManager.spawnShop(nombre, player.getLocation(), Villager.Type.PLAINS, Villager.Profession.NONE);
        sender.sendMessage(PREFIJO + "Tienda " + ChatColor.translateAlternateColorCodes('&', nombre) + ChatColor.of("#F5EBDD")
                + " creada. Llénala con shift + clic derecho.");
    }

    private static CatalogoTienda.Oferta oferta(CatalogoTienda.Tienda tienda, String producto) {
        for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
            if (oferta.producto().equalsIgnoreCase(producto)) return oferta;
        }
        return null;
    }

    private static List<String> productos(CatalogoTienda.Tienda tienda) {
        List<String> productos = new ArrayList<>();
        for (CatalogoTienda.Oferta oferta : tienda.ofertas()) productos.add(oferta.producto());
        return productos;
    }

    private void precios(CommandSender sender, String[] args) {
        CatalogoTienda.Tienda tienda = tiendaDe(sender, args);
        if (tienda == null) return;
        int seccion = shopManager.getSeccion();
        if (args.length > 2) {
            try {
                seccion = Math.max(1, Math.min(CatalogoTienda.DIAS.length, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        int ajuste = shopManager.getAjuste(tienda.id());
        Map<String, Integer> manuales = shopManager.getPreciosManuales(tienda.id());
        sender.sendMessage(PREFIJO + ChatColor.translateAlternateColorCodes('&', tienda.nombre()) + GRIS + " · sección " + seccion
                + " (día " + CatalogoTienda.diaDeLaSeccion(seccion) + ")" + (ajuste != 0 ? " · ajuste " + (ajuste > 0 ? "+" : "") + ajuste + "%" : ""));
        for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
            String linea = GRIS + " · " + BLANCO + oferta.producto() + GRIS + " x" + oferta.cantidadProducto() + "  ";
            if (!CatalogoTienda.abierta(oferta, seccion)) {
                linea += GRIS + "(día " + CatalogoTienda.diaDeLaSeccion(oferta.seccion()) + ")";
            } else {
                linea += DORADO + "" + CatalogoTienda.precioFinal(oferta, seccion, manuales.get(oferta.producto()), ajuste) + " " + oferta.pago()
                        + (manuales.containsKey(oferta.producto()) ? GRIS + " (a mano)" : "");
            }
            sender.sendMessage(linea);
        }
    }

    private void info(CommandSender sender) {
        int seccion = shopManager.getSeccion();
        Map<String, Integer> cuantos = new HashMap<>();
        for (String catalogo : shopManager.getCatalogos().values()) cuantos.merge(catalogo, 1, Integer::sum);
        sender.sendMessage(PREFIJO + "Sección " + BLANCO + seccion + GRIS + " (día " + CatalogoTienda.diaDeLaSeccion(seccion) + ")");
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            int aldeanos = cuantos.getOrDefault(tienda.id(), 0);
            sender.sendMessage(GRIS + " · " + BLANCO + tienda.id() + GRIS + " - " + (aldeanos == 0 ? ChatColor.of("#D9A5A0") + "sin aldeano (/tienda crear " + tienda.id() + ")"
                    : aldeanos + (aldeanos == 1 ? " aldeano" : " aldeanos")));
        }
    }

    private CatalogoTienda.Tienda tiendaDe(CommandSender sender, String[] args) {
        CatalogoTienda.Tienda tienda = args.length > 1 ? CatalogoTienda.tienda(args[1]) : null;
        if (tienda == null) sender.sendMessage(PREFIJO + "Tiendas: " + BLANCO + String.join(", ", ids()));
        return tienda;
    }

    private Villager aldeanoMirado(Player player) {
        RayTraceResult mira = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 5.0,
                entity -> entity instanceof Villager && entity.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING));
        if (mira != null && mira.getHitEntity() instanceof Villager villager) return villager;
        return null;
    }

    private Integer numero(CommandSender sender, String[] args, int indice) {
        if (args.length <= indice) {
            ayuda(sender);
            return null;
        }
        try {
            return Integer.parseInt(args[indice]);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIJO + ChatColor.RED + "'" + args[indice] + "' no es un número.");
            return null;
        }
    }

    private static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) ids.add(tienda.id());
        return ids;
    }

    private void ayuda(CommandSender sender) {
        sender.sendMessage(PREFIJO + "Comandos:");
        sender.sendMessage(GRIS + " /tienda dia <día> " + ChatColor.of("#F5EBDD") + "- abre la sección de ese día (1, 20, 30, 40, 50 o 100)");
        sender.sendMessage(GRIS + " /tienda seccion <1-6> " + ChatColor.of("#F5EBDD") + "- lo mismo, por número de sección");
        sender.sendMessage(GRIS + " /tienda crear <tienda> " + ChatColor.of("#F5EBDD") + "- aldeano nuevo donde estás");
        sender.sendMessage(GRIS + " /tienda asignar <tienda> " + ChatColor.of("#F5EBDD") + "- el aldeano que miras pasa a ser esa tienda");
        sender.sendMessage(GRIS + " /tienda precios <tienda> [sección] " + ChatColor.of("#F5EBDD") + "- lo que vende y a cuánto");
        sender.sendMessage(GRIS + " /tienda precio <tienda> <producto> <precio|auto> " + ChatColor.of("#F5EBDD") + "- precio fijo de un producto");
        sender.sendMessage(GRIS + " /tienda ajuste <tienda|todas> <porcentaje> " + ChatColor.of("#F5EBDD") + "- sube o baja todos sus precios");
        sender.sendMessage(GRIS + " /tienda soltar " + ChatColor.of("#F5EBDD") + "- el aldeano que miras deja de ser automático");
        sender.sendMessage(GRIS + " /tienda nueva <nombre> " + ChatColor.of("#F5EBDD") + "- tienda vacía para llenar a mano (shift + clic derecho)");
        sender.sendMessage(GRIS + " /tienda info " + ChatColor.of("#F5EBDD") + "- sección actual y qué tiendas faltan");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission(PERMISO)) return List.of();
        Collection<String> opciones = switch (args.length) {
            case 1 -> List.of("dia", "seccion", "crear", "asignar", "precios", "precio", "ajuste", "soltar", "nueva", "info");
            case 2 -> switch (args[0].toLowerCase()) {
                case "dia" -> List.of("1", "20", "30", "40", "50", "100");
                case "seccion" -> List.of("1", "2", "3", "4", "5", "6");
                case "crear", "asignar", "precios", "precio" -> ids();
                case "ajuste" -> {
                    List<String> opciones2 = new ArrayList<>(ids());
                    opciones2.add(0, "todas");
                    yield opciones2;
                }
                default -> List.of();
            };
            case 3 -> switch (args[0].toLowerCase()) {
                case "precios" -> List.of("1", "2", "3", "4", "5", "6");
                case "precio" -> {
                    CatalogoTienda.Tienda tienda = CatalogoTienda.tienda(args[1]);
                    yield tienda != null ? productos(tienda) : List.<String>of();
                }
                case "ajuste" -> List.of("-20", "-10", "0", "10", "20");
                default -> List.of();
            };
            case 4 -> args[0].equalsIgnoreCase("precio") ? List.of("auto") : List.of();
            default -> List.of();
        };
        return StringUtil.copyPartialMatches(args[args.length - 1], opciones, new ArrayList<>());
    }
}
