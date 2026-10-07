package ShopSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Lo que vende cada aldeano de la tienda en cada sección. No hay contador de días: el admin abre cada sección con
// /tienda dia <n> (1 el día 1, 2 el 20, 3 el 30, 4 el 40, 5 el 50 y 6 el 100). Cada sección agrega items y sube 15%
// lo que ya estaba. Nunca se venden las armaduras, las herramientas del End ni los minerales nuevos
public final class CatalogoTienda {

    public static final int[] DIAS = {1, 20, 30, 40, 50, 100};
    public static final double SUBIDA = 0.15;
    // Dos stacks de DinoCoins en los dos lugares del precio
    public static final int PRECIO_MAXIMO = 128;
    public static final String MONEDA = "dinocoins";

    // Un tradeo: se paga "pago" x cantidadPago y se recibe "producto" x cantidadProducto. Los fijos (cambios y
    // pescadería) no suben de precio
    public record Oferta(String producto, int cantidadProducto, String pago, int cantidadPago, int seccion, boolean fijo) {}

    // La profesión va como texto (farmer, cleric...) para no tocar el registro del juego al cargar la clase
    public record Tienda(String id, String nombre, String profesion, List<Oferta> ofertas) {}

    private static final Map<String, Tienda> TIENDAS = new LinkedHashMap<>();

    static {
        tienda("mercado", "&6&lMercado", "farmer",
                vende("monedero", 1, 5, 1),
                vende("corrupted_steak", 16, 4, 1),
                vende("tarta_calabaza_mejorada", 8, 4, 1),
                vende("frasco_de_velocidad", 1, 2, 1),
                vende("panic_apple", 1, 8, 1),
                vende("manzana_vida", 1, 10, 2),
                vende("corrupted_golden_apple", 1, 25, 2),
                vende("keep_inventory_liquido", 1, 60, 3),
                vende("bar_ginebra", 1, 2, 4),
                vende("bar_azulito", 1, 2, 4));
        tienda("pociones", "&d&lPociones", "cleric",
                vende("potion_haste_2", 1, 5, 1),
                vende("potion_slow_falling", 1, 3, 1),
                vende("potion_resistance_2", 1, 6, 1),
                vende("potion_haste_3", 1, 8, 2),
                vende("splash_regeneration_3", 1, 8, 2),
                vende("splash_absorption_10", 1, 10, 2),
                vende("splash_resistance_3", 1, 12, 3),
                vende("baya_sculk", 4, 3, 3),
                vende("fruta_abisal", 4, 4, 3),
                vende("baya_luminosa", 4, 3, 3));
        tienda("bar", "&e&lBar", "nitwit",
                vende("bar_tequila", 1, 1, 1),
                vende("bar_margarita", 1, 1, 1),
                vende("bar_mezcal", 1, 1, 1),
                vende("bar_pulque", 1, 1, 1),
                vende("bar_cerveza", 1, 1, 1),
                vende("bar_michelada", 1, 1, 1),
                vende("bar_ron", 1, 1, 2),
                vende("bar_vodka", 1, 1, 2),
                vende("bar_whisky", 1, 1, 3),
                vende("bar_sake", 1, 1, 3));
        tienda("utilidad", "&b&lUtilidad", "toolsmith",
                vende("artefacto_nivel_1", 1, 10, 1),
                vende("totem_of_undying", 1, 12, 1),
                vende("flytotem", 1, 15, 1),
                vende("icetotem", 1, 20, 1),
                vende("life_campfire", 1, 20, 1),
                vende("fuel_campfire", 2, 4, 1),
                vende("gancho", 1, 20, 1),
                vende("enderbag", 1, 25, 1),
                vende("artefacto_nivel_2", 1, 30, 2),
                vende("doubletotem", 1, 45, 2));
        tienda("mochilas", "&a&lMochilas y Amuletos", "leatherworker",
                vende("mochila_nivel_1", 1, 15, 1),
                vende("mochila_nivel_2", 1, 30, 1),
                vende("amulet_bloodmoon", 1, 40, 1),
                vende("happy_ghast_enchant", 1, 25, 1),
                vende("mochila_nivel_3", 1, 55, 2),
                vende("amuleto_inmortalidad", 1, 35, 2),
                vende("happy_ghast_enchant_2", 1, 60, 2),
                vende("mochila_nivel_4", 1, 85, 3),
                vende("amuleto_invisiblidad", 1, 60, 3),
                vende("mochila_nivel_5", 1, 120, 5));
        tienda("novedades_1", "&6&lNovedades &7(día 10)", "cartographer",
                vende("iman_botin", 1, 30, 1),
                vende("red_animales", 1, 25, 1),
                vende("abono_concentrado", 4, 3, 1),
                vende("brujula_explorador", 1, 12, 1),
                vende("racion_viaje", 4, 4, 1),
                vende("bomba_humo", 2, 5, 1),
                vende("incienso_ahuyentador", 1, 6, 1),
                vende("elixir_minero", 1, 5, 1),
                vende("elixir_igneo", 1, 6, 1),
                vende("galleta_fortuna", 4, 4, 1));
        tienda("novedades_2", "&3&lNovedades &7(día 20)", "cartographer",
                vende("bengala_sculk", 2, 4, 2),
                vende("polvo_silencioso", 1, 8, 2),
                vende("elixir_abisal", 1, 8, 2),
                vende("botiquin", 1, 6, 2),
                vende("granada_sonica", 2, 8, 2),
                vende("radar_minerales", 1, 15, 2),
                vende("cristal_eco", 1, 12, 2),
                vende("elixir_agilidad", 1, 6, 2),
                vende("caldo_profundo", 1, 3, 2),
                vende("linterna_almas", 1, 35, 2));
        tienda("novedades_3", "&5&lNovedades &7(día 30)", "cartographer",
                vende("elixir_coloso", 1, 12, 3),
                vende("elixir_furia", 1, 12, 3),
                vende("tonico_antilevitacion", 1, 8, 3),
                vende("antidoto", 1, 6, 3),
                vende("frasco_sabiduria", 1, 15, 3),
                vende("propulsor_estelar", 1, 25, 4),
                vende("estandarte_guerra", 1, 20, 4),
                vende("talisman_botin", 1, 30, 4),
                vende("pluma_vacio", 1, 20, 4),
                vende("caja_misteriosa", 1, 15, 5));
        tienda("explorador", "&2&lExplorador", "cartographer",
                vende("retorno_warden", 1, 6, 2),
                vende("pluma_levitacion", 1, 3, 2),
                vende("pluma_levitacion_mejorada", 1, 6, 3),
                vende("amuleto_ultima_esperanza", 1, 15, 3),
                vende("alma_infested_skeleton", 1, 25, 6),
                vende("alma_infested_ghast", 1, 25, 6),
                vende("alma_infested_creeper", 1, 25, 6),
                vende("alma_infested_cave_spider", 1, 25, 6),
                vende("fragmento_astral", 1, 8, 6),
                vende("esencia_marchita", 1, 8, 6));
        tienda("biblioteca", "&9&lBiblioteca", "librarian",
                vende("misiones", 1, 1, 1),
                vende("libro_habilidades", 1, 1, 1),
                vende("libro_purificacion_1", 1, 30, 4),
                vende("libro_paso_igneo_1", 1, 40, 4),
                vende("libro_vision_abisal_1", 1, 40, 4),
                vende("libro_anclaje_1", 1, 30, 4),
                vende("libro_paso_igneo_2", 1, 70, 5),
                vende("libro_anclaje_2", 1, 60, 5),
                vende("libro_retorno_del_vacio_1", 1, 60, 5),
                vende("libro_purificacion_5", 1, 110, 6));
        tienda("legendario", "&c&lLegendario", "weaponsmith",
                vende("excavator_pickaxe", 1, 100, 4),
                vende("arco_nivel1", 1, 60, 4),
                vende("arco_hielo_jugador", 1, 50, 4),
                vende("arco_nivel2", 1, 90, 5),
                vende("perla_infinita", 1, 110, 5),
                vende("arco_nivel3", 1, 120, 6),
                vende("estatua_protectora", 1, 100, 6),
                vende("energia_warden", 1, 12, 6),
                vende("corazon_warden_boss", 1, 90, 6),
                vende("ojo_rey_ender", 1, 120, 6));
        tienda("cambios", "&e&lCambios", "none",
                new Oferta("dinofichas", 5, MONEDA, 1, 1, true),
                new Oferta(MONEDA, 1, "dinofichas", 6, 1, true),
                new Oferta(MONEDA, 1, "blood_fragment", 6, 1, true));
        tienda("pescaderia", "&3&lPescadería", "fisherman",
                compra("chatarra", 64),
                compra("manzana_podrida", 48),
                compra("zanahoria_encantada", 24),
                compra("pepitas_hierro_oxidadas", 16),
                compra("pepitas_diamante", 8),
                compra("fragmentos_ambar", 4),
                compra("fosiles_pequenos", 2),
                compra("lingote_platino", 1));
    }

    private CatalogoTienda() {}

    private static void tienda(String id, String nombre, String profesion, Oferta... ofertas) {
        if (ofertas.length > 10) throw new IllegalStateException("La tienda " + id + " tiene más de 10 tradeos");
        TIENDAS.put(id, new Tienda(id, nombre, profesion, List.of(ofertas)));
    }

    private static Oferta vende(String item, int cantidad, int precio, int seccion) {
        return new Oferta(item, cantidad, MONEDA, precio, seccion, false);
    }

    // La pescadería compra los premios de pesca a 1 DinoCoin
    private static Oferta compra(String item, int cantidad) {
        return new Oferta(MONEDA, 1, item, cantidad, 1, true);
    }

    public static Tienda tienda(String id) {
        return id == null ? null : TIENDAS.get(id.toLowerCase());
    }

    public static List<Tienda> tiendas() {
        return new ArrayList<>(TIENDAS.values());
    }

    // El precio en una sección: el de entrada subido 15% por cada sección que pasó, con tope de 128
    public static int precio(Oferta oferta, int seccion) {
        if (oferta.fijo()) return oferta.cantidadPago();
        int pasaron = Math.max(0, seccion - oferta.seccion());
        return (int) Math.min(PRECIO_MAXIMO, Math.round(oferta.cantidadPago() * Math.pow(1 + SUBIDA, pasaron)));
    }

    public static boolean abierta(Oferta oferta, int seccion) {
        return seccion >= oferta.seccion();
    }

    // La sección que toca un día: el día 25 sigue con la del 20
    public static int seccionDelDia(int dia) {
        int seccion = 1;
        for (int i = 0; i < DIAS.length; i++) {
            if (dia >= DIAS[i]) seccion = i + 1;
        }
        return seccion;
    }

    public static int diaDeLaSeccion(int seccion) {
        return DIAS[Math.max(1, Math.min(DIAS.length, seccion)) - 1];
    }
}
