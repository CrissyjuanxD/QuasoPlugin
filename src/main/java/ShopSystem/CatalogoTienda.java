package ShopSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Lo que vende cada aldeano de la tienda en cada sección. No hay contador de días: el admin abre cada sección con
// /tienda dia <n> (1 el día 1, 2 el 20, 3 el 30, 4 el 40, 5 el 50 y 6 el 100). Cada sección agrega items y sube 15%
// lo que ya estaba. Nunca se vende lo que se farmea en las dimensiones o de los jefes para el equipo (armaduras,
// herramientas, minerales, almas, energía, materiales del End, corazones y ojos de invocación ni frutas de la Warden Cave)
public final class CatalogoTienda {

    public static final int[] DIAS = {1, 20, 30, 40, 50, 100};
    public static final double SUBIDA = 0.15;
    // Dos stacks de DinoCoins en los dos lugares del precio
    public static final int PRECIO_MAXIMO = 128;
    public static final String MONEDA = "dinocoins";

    // Un tradeo: se paga "pago" x cantidadPago (más "extra" x cantidadExtra en el segundo lugar, si tiene) y se recibe
    // "producto" x cantidadProducto. Los fijos (cambios y pescadería) no suben de precio
    public record Oferta(String producto, int cantidadProducto, String pago, int cantidadPago, int seccion, boolean fijo,
                         String extra, int cantidadExtra) {

        public Oferta(String producto, int cantidadProducto, String pago, int cantidadPago, int seccion, boolean fijo) {
            this(producto, cantidadProducto, pago, cantidadPago, seccion, fijo, null, 0);
        }

        // El item que se pide además de las DinoCoins en el segundo lugar del tradeo
        public Oferta con(String extra, int cantidadExtra) {
            return new Oferta(producto, cantidadProducto, pago, cantidadPago, seccion, fijo, extra, cantidadExtra);
        }
    }

    // La profesión va como texto (farmer, cleric...) para no tocar el registro del juego al cargar la clase
    public record Tienda(String id, String nombre, String profesion, List<Oferta> ofertas) {}

    private static final Map<String, Tienda> TIENDAS = new LinkedHashMap<>();

    static {
        tienda("mercado", "&6&lMercado", "farmer",
                vende("monedero", 1, 5, 1).con("leather", 4),
                vende("corrupted_steak", 16, 4, 1).con("rotten_flesh", 16),
                vende("tarta_calabaza_mejorada", 8, 4, 1).con("pumpkin", 4),
                vende("frasco_de_velocidad", 1, 2, 1).con("sugar", 8),
                vende("panic_apple", 1, 8, 1).con("apple", 4),
                vende("manzana_vida", 1, 10, 2).con("golden_apple", 1),
                vende("corrupted_golden_apple", 1, 25, 2).con("gold_block", 1),
                vende("keep_inventory_liquido", 1, 60, 3).con("ghast_tear", 2),
                vende("bar_ginebra", 1, 2, 4),
                vende("bar_azulito", 1, 2, 4));
        tienda("pociones", "&d&lPociones", "cleric",
                vende("potion_haste_2", 1, 5, 1).con("glowstone_dust", 4),
                vende("potion_slow_falling", 1, 3, 1).con("phantom_membrane", 1),
                vende("potion_resistance_2", 1, 6, 1).con("iron_ingot", 4),
                vende("potion_haste_3", 1, 8, 2).con("glowstone", 2),
                vende("splash_regeneration_3", 1, 8, 2).con("ghast_tear", 1),
                vende("splash_absorption_10", 1, 10, 2).con("golden_carrot", 4),
                vende("splash_resistance_3", 1, 12, 3).con("blaze_powder", 4));
        // Los tragos piden la botella vacía
        tienda("bar", "&e&lBar", "nitwit",
                vende("bar_tequila", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_margarita", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_mezcal", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_pulque", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_cerveza", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_michelada", 1, 1, 1).con("glass_bottle", 1),
                vende("bar_ron", 1, 1, 2).con("glass_bottle", 1),
                vende("bar_vodka", 1, 1, 2).con("glass_bottle", 1),
                vende("bar_whisky", 1, 1, 3).con("glass_bottle", 1),
                vende("bar_sake", 1, 1, 3).con("glass_bottle", 1));
        tienda("utilidad", "&b&lUtilidad", "toolsmith",
                vende("artefacto_nivel_1", 1, 10, 1).con("iron_ingot", 8),
                vende("life_campfire", 1, 20, 1).con("campfire", 1),
                vende("fuel_campfire", 2, 4, 1).con("coal", 8),
                vende("gancho", 1, 20, 1).con("tripwire_hook", 2),
                vende("enderbag", 1, 25, 1).con("ender_pearl", 4),
                vende("artefacto_nivel_2", 1, 30, 2).con("diamond", 4));
        // Todos los tótems juntos: el Sculk con la Warden Cave (día 20) y el Ender con el End (día 30)
        tienda("totems", "&e&lTótems", "armorer",
                vende("totem_of_undying", 1, 12, 1).con("emerald", 4),
                vende("flytotem", 1, 15, 1).con("feather", 16),
                vende("icetotem", 1, 20, 1).con("packed_ice", 8),
                vende("doubletotem", 1, 45, 2).con("totem_of_undying", 1),
                vende("sculk_totem", 1, 35, 2).con("echo_shard", 2),
                vende("ender_totem", 1, 55, 3).con("shulker_shell", 2));
        // Minecraft guarda poco por jugador: las mochilas 1, 2 y 3 desde el día 1, la 4 en el día 20 y la 5 en el 30
        tienda("mochilas", "&a&lMochilas y Amuletos", "leatherworker",
                vende("mochila_nivel_1", 1, 15, 1).con("bundle", 1),
                vende("mochila_nivel_2", 1, 30, 1).con("bundle", 2),
                vende("mochila_nivel_3", 1, 45, 1).con("bundle", 4),
                vende("amulet_bloodmoon", 1, 40, 1).con("blood_fragment", 4),
                vende("happy_ghast_enchant", 1, 25, 1).con("ghast_tear", 2),
                vende("mochila_nivel_4", 1, 60, 2).con("bundle", 6),
                vende("amuleto_inmortalidad", 1, 35, 2).con("totem_of_undying", 1),
                vende("happy_ghast_enchant_2", 1, 60, 2).con("ghast_tear", 4),
                vende("mochila_nivel_5", 1, 64, 3).con("shulker_shell", 2),
                vende("amuleto_invisiblidad", 1, 60, 3).con("fermented_spider_eye", 8));
        tienda("novedades_1", "&6&lNovedades 1", "cartographer",
                vende("iman_botin", 1, 30, 1).con("hopper", 1),
                vende("red_animales", 1, 25, 1).con("lead", 2),
                vende("abono_concentrado", 4, 3, 1).con("bone_meal", 8),
                vende("brujula_explorador", 1, 12, 1).con("compass", 1),
                vende("racion_viaje", 4, 4, 1).con("bread", 4),
                vende("bomba_humo", 2, 5, 1).con("gunpowder", 2),
                vende("incienso_ahuyentador", 1, 6, 1),
                vende("elixir_minero", 1, 5, 1),
                vende("elixir_igneo", 1, 6, 1).con("magma_cream", 2),
                vende("galleta_fortuna", 4, 4, 1).con("cocoa_beans", 2));
        tienda("novedades_2", "&3&lNovedades 2", "cartographer",
                vende("bengala_sculk", 2, 4, 2),
                vende("polvo_silencioso", 1, 8, 2).con("white_wool", 4),
                vende("elixir_abisal", 1, 8, 2).con("prismarine_crystals", 4),
                vende("botiquin", 1, 6, 2).con("glistering_melon_slice", 1),
                vende("granada_sonica", 2, 8, 2).con("gunpowder", 4),
                vende("radar_minerales", 1, 15, 2).con("redstone", 16),
                vende("cristal_eco", 1, 12, 2).con("amethyst_shard", 4),
                vende("elixir_agilidad", 1, 6, 2).con("rabbit_foot", 1),
                vende("caldo_profundo", 1, 3, 2),
                vende("linterna_almas", 1, 35, 2).con("soul_lantern", 1));
        tienda("novedades_3", "&5&lNovedades 3", "cartographer",
                vende("elixir_coloso", 1, 12, 3).con("iron_block", 1),
                vende("elixir_furia", 1, 12, 3).con("blaze_powder", 4),
                vende("tonico_antilevitacion", 1, 8, 3).con("phantom_membrane", 2),
                vende("antidoto", 1, 6, 3).con("milk_bucket", 1),
                vende("frasco_sabiduria", 1, 15, 3).con("lapis_lazuli", 16),
                vende("propulsor_estelar", 1, 25, 4).con("firework_rocket", 16),
                vende("estandarte_guerra", 1, 20, 4).con("iron_ingot", 8),
                vende("talisman_botin", 1, 30, 4).con("emerald", 8),
                vende("pluma_vacio", 1, 20, 4).con("chorus_fruit", 8),
                vende("caja_misteriosa", 1, 15, 5));
        tienda("explorador", "&2&lExplorador", "cartographer",
                vende("retorno_warden", 1, 6, 2).con("ender_pearl", 2),
                vende("pluma_levitacion", 1, 3, 2).con("feather", 4),
                vende("pluma_levitacion_mejorada", 1, 6, 3).con("phantom_membrane", 2),
                vende("amuleto_ultima_esperanza", 1, 15, 3).con("totem_of_undying", 1));
        tienda("biblioteca", "&9&lBiblioteca", "librarian",
                vende("misiones", 1, 1, 1),
                vende("libro_habilidades", 1, 30, 1).con("book", 1),
                vende("libro_purificacion_1", 1, 30, 4).con("lapis_lazuli", 16),
                vende("libro_paso_igneo_1", 1, 40, 4).con("lapis_lazuli", 16),
                vende("libro_vision_abisal_1", 1, 40, 4).con("lapis_lazuli", 16),
                vende("libro_anclaje_1", 1, 30, 4).con("lapis_lazuli", 16),
                vende("libro_paso_igneo_2", 1, 70, 5),
                vende("libro_anclaje_2", 1, 60, 5),
                vende("libro_retorno_del_vacio_1", 1, 60, 5),
                vende("libro_purificacion_5", 1, 110, 6));
        tienda("legendario", "&c&lLegendario", "weaponsmith",
                vende("excavator_pickaxe", 1, 100, 4),
                vende("arco_nivel1", 1, 60, 4).con("bow", 1),
                vende("arco_hielo", 1, 50, 4).con("packed_ice", 16),
                vende("arco_nivel2", 1, 90, 5),
                vende("perla_infinita", 1, 110, 5),
                vende("arco_nivel3", 1, 120, 6),
                vende("estatua_protectora", 1, 100, 6));
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

    // El precio que se cobra: el que puso el admin a mano (/tienda precio) o el de la sección con el ajuste en % de esa
    // tienda (/tienda ajuste), siempre entre 1 y 128
    public static int precioFinal(Oferta oferta, int seccion, Integer manual, int ajuste) {
        int precio = manual != null ? manual : (int) Math.round(precio(oferta, seccion) * (100 + ajuste) / 100.0);
        return Math.max(1, Math.min(PRECIO_MAXIMO, precio));
    }

    // El extra va en el segundo lugar mientras el precio quepa en un stack; solo se quita cuando las DinoCoins
    // necesitan los dos lugares. El extra no sube de precio, solo las DinoCoins
    public static boolean conExtra(Oferta oferta, int seccion, int precio) {
        return oferta.extra() != null && precio <= 64;
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
