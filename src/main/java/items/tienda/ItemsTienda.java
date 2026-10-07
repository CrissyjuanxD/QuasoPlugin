package items.tienda;

import items.EndItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Los 30 items nuevos de la tienda: 10 para el día 10, 10 para el día 20 (Warden Cave) y 10 para el día 30 en adelante
// (End y Rey Ender). Se reconocen por la PDC "custom_item" como los items del End, y el item model es su id
public final class ItemsTienda {

    public static final NamespacedKey USOS = new NamespacedKey("quasoplugin", "usos");

    private static final ChatColor GRIS = ChatColor.GRAY;
    private static final ChatColor TIEMPO = ChatColor.of("#0099cc");
    private static final ChatColor USO = ChatColor.WHITE;

    private record Def(String id, String nombre, String color, Material material, ItemRarity rareza, int usos, String[] lore) {}

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    static {
        // ---------------------------------------------------------------- Día 10
        def("iman_botin", "Imán de Botín", "#d6d6e0", Material.IRON_NUGGET, ItemRarity.UNCOMMON, 0,
                "Atrae los items del suelo que", "estén a 8 bloques de ti.", "", "> Llévalo en la mano secundaria");
        def("red_animales", "Red Atrapa-Animales", "#c8b48a", Material.PHANTOM_MEMBRANE, ItemRarity.UNCOMMON, 0,
                "Guarda un animal para llevarlo", "a donde quieras.", "",
                "> Click derecho a un animal: lo atrapa", "> Click derecho a un bloque: lo suelta");
        def("abono_concentrado", "Abono Concentrado", "#9bd47a", Material.GLOWSTONE_DUST, ItemRarity.COMMON, 0,
                "Hace crecer los cultivos y", "árboles en 5x5 alrededor.", "", "> Click derecho a un cultivo");
        def("brujula_explorador", "Brújula del Explorador", "#e8c07a", Material.COMPASS, ItemRarity.UNCOMMON, 3,
                "Busca la estructura más cercana:", "aldea en el Overworld, fortaleza", "en el Nether y ciudad en el End.",
                "", "> Click derecho");
        def("racion_viaje", "Ración de Viaje", "#d9a066", Material.BREAD, ItemRarity.COMMON, 0,
                "Llena el hambre y la saturación.", "", efecto("Regeneración I", "10 s"));
        def("bomba_humo", "Bomba de Humo", "#9a9a9a", Material.SNOWBALL, ItemRarity.COMMON, 0,
                "Al tirarla te vuelves invisible y", "rápido 6 s; donde cae, los mobs", "pierden tu rastro y quedan ciegos.");
        def("incienso_ahuyentador", "Incienso Ahuyentador", "#e0a35e", Material.BLAZE_POWDER, ItemRarity.UNCOMMON, 0,
                "Por 10 minutos no aparecen mobs", "hostiles a 24 bloques de ti.", "", "> Click derecho");
        def("elixir_minero", "Elixir del Minero", "#7ec8e3", Material.POTION, ItemRarity.COMMON, 0);
        def("elixir_igneo", "Elixir Ígneo", "#f08a4b", Material.POTION, ItemRarity.COMMON, 0);
        def("galleta_fortuna", "Galleta de la Fortuna", "#e8b96a", Material.COOKIE, ItemRarity.COMMON, 0,
                "Trae un mensaje y un efecto", "bueno al azar por 1 minuto.");

        // ---------------------------------------------------------------- Día 20 (Warden Cave)
        def("bengala_sculk", "Bengala de Sculk", "#3fd0d4", Material.SNOWBALL, ItemRarity.UNCOMMON, 0,
                "Al caer hace brillar a los mobs", "a 16 bloques por 15 s.");
        def("polvo_silencioso", "Polvo Silencioso", "#8fb3c9", Material.FLINT, ItemRarity.UNCOMMON, 0,
                "Por 3 minutos tus pasos no", "activan sensores ni chilladores", "y los Wardens no te oyen.", "", "> Click derecho");
        def("elixir_abisal", "Elixir Abisal", "#2a7f9e", Material.POTION, ItemRarity.UNCOMMON, 0,
                "Ves en la oscuridad y la Oscuridad", "no te afecta por 5 minutos.", "", efecto("Sin Oscuridad", "5 min"));
        def("botiquin", "Botiquín", "#e86a6a", Material.PAPER, ItemRarity.UNCOMMON, 0,
                "Cura 5 corazones y quita veneno,", "wither, hambre, debilidad, lentitud,", "fatiga, náusea, ceguera y oscuridad.",
                "", "> Click derecho");
        def("granada_sonica", "Granada Sónica", "#4ec9b0", Material.SNOWBALL, ItemRarity.RARE, 0,
                "Al caer hace 8 de daño y empuja", "a los mobs a 5 bloques.");
        def("radar_minerales", "Radar de Minerales", "#b0e0ff", Material.CLOCK, ItemRarity.RARE, 3,
                "Marca los minerales a 8 bloques", "por 10 s, a través de las paredes.", "", "> Click derecho");
        def("cristal_eco", "Cristal de Eco", "#5ad1c5", Material.AMETHYST_SHARD, ItemRarity.RARE, 0,
                "Guarda un lugar y te lleva de", "vuelta (en la misma dimensión).", "",
                "> Shift + click derecho: marca el lugar", "> Click derecho: viaja (3 s quieto)");
        def("elixir_agilidad", "Elixir de Agilidad", "#9be36a", Material.POTION, ItemRarity.COMMON, 0);
        def("caldo_profundo", "Caldo Profundo", "#7aa6b8", Material.MUSHROOM_STEW, ItemRarity.COMMON, 0,
                "Llena 8 de hambre.", "", efecto("Absorción II", "2 min"), efecto("Resistencia I", "1 min"));
        def("linterna_almas", "Linterna de Almas", "#6fd6ff", Material.SOUL_LANTERN, ItemRarity.RARE, 0,
                "Te da Visión Nocturna mientras", "la lleves. No se puede poner.", "", "> Llévala en la mano secundaria");

        // ---------------------------------------------------------------- Día 30 en adelante (End y Rey Ender)
        def("elixir_coloso", "Elixir del Coloso", "#e05656", Material.POTION, ItemRarity.RARE, 0);
        def("elixir_furia", "Elixir de Furia", "#c43a3a", Material.POTION, ItemRarity.RARE, 0);
        def("tonico_antilevitacion", "Tónico Antilevitación", "#c9a7ff", Material.POTION, ItemRarity.UNCOMMON, 0,
                "", efecto("Inmune a la Levitación", "4 min"), "", GRIS + "Ideal para las End Cities.");
        def("antidoto", "Antídoto Universal", "#a6e3a1", Material.POTION, ItemRarity.UNCOMMON, 0,
                "Quita solo los efectos malos", "y deja los buenos.");
        def("frasco_sabiduria", "Frasco de Sabiduría", "#b6f06e", Material.POTION, ItemRarity.RARE, 0,
                "", efecto("Doble experiencia", "10 min"));
        def("propulsor_estelar", "Propulsor Estelar", "#9ab8ff", Material.FIREWORK_ROCKET, ItemRarity.RARE, 5,
                "Un impulso fuerte mientras", "planeas con Elytra.", "", "> Click derecho planeando");
        def("estandarte_guerra", "Estandarte de Guerra", "#d94a4a", Material.RED_BANNER, ItemRarity.RARE, 0,
                "Los jugadores a 12 bloques", "reciben por 60 s:", efecto("Fuerza I", "60 s"), efecto("Resistencia I", "60 s"),
                "", "> Click derecho");
        def("talisman_botin", "Talismán del Botín", "#f2c46b", Material.NAUTILUS_SHELL, ItemRarity.EPIC, 0,
                "Por 10 minutos los monstruos que", "mates sueltan más botín y más", "experiencia.", "", "> Click derecho");
        def("pluma_vacio", "Pluma del Vacío", "#b57edc", Material.FEATHER, ItemRarity.EPIC, 0,
                "Si caes al vacío del End te", "devuelve al último suelo firme.", "Se gasta al salvarte.", "", "> Llévala en el inventario");
        def("caja_misteriosa", "Caja Misteriosa", "#f5d76e", Material.CHEST, ItemRarity.RARE, 0,
                "Trae un premio al azar: comida,", "elixires, tótems y a veces algo", "muy bueno.", "", "> Click derecho");
    }

    private ItemsTienda() {}

    private static void def(String id, String nombre, String color, Material material, ItemRarity rareza, int usos, String... lore) {
        DEFS.put(id, new Def(id, nombre, color, material, rareza, usos, lore));
    }

    private static String efecto(String nombre, String duracion) {
        return GRIS + "> " + ChatColor.of("#ffd27f") + nombre + GRIS + " (" + TIEMPO + duracion + GRIS + ")";
    }

    public static List<String> claves() {
        return new ArrayList<>(DEFS.keySet());
    }

    public static boolean existe(String id) {
        return DEFS.containsKey(id);
    }

    public static String idOf(ItemStack item) {
        String id = EndItems.idOf(item);
        return id != null && DEFS.containsKey(id) ? id : null;
    }

    public static boolean isItem(ItemStack item) {
        return idOf(item) != null;
    }

    public static ItemStack crear(String id, int cantidad) {
        Def def = DEFS.get(id);
        if (def == null) return null;
        ItemStack item = new ItemStack(def.material(), Math.max(1, cantidad));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.of(def.color()) + "" + ChatColor.BOLD + def.nombre());
        meta.getPersistentDataContainer().set(EndItems.ITEM_KEY, PersistentDataType.STRING, id);
        meta.setRarity(def.rareza());
        meta.setItemModel(NamespacedKey.minecraft(id));

        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String linea : def.lore()) lore.add(linea.isEmpty() || linea.startsWith("§") ? linea
                : linea.startsWith(">") ? GRIS + "> " + USO + linea.substring(2) : ChatColor.of("#d8d0c0") + linea);

        if (meta instanceof PotionMeta pocion) {
            pocion.setColor(colorPocion(def.color()));
            for (PotionEffect efecto : efectos(id)) {
                pocion.addCustomEffect(efecto, true);
                lore.add(GRIS + "> " + ChatColor.of("#ffd27f") + nombreEfecto(efecto.getType()) + " " + romano(efecto.getAmplifier() + 1)
                        + GRIS + " (" + TIEMPO + duracion(efecto.getDuration()) + GRIS + ")");
            }
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
        if (def.usos() > 0) {
            meta.setMaxStackSize(1);
            meta.getPersistentDataContainer().set(USOS, PersistentDataType.INTEGER, def.usos());
            lore.add("");
            lore.add(lineaUsos(def.usos(), def.usos()));
        }
        // Los que guardan algo propio (un animal, un lugar) no se apilan
        if (id.equals("red_animales") || id.equals("cristal_eco")) meta.setMaxStackSize(1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Premios de la Caja Misteriosa (suman 100). Vale en promedio menos de lo que cuesta, pero a veces sale algo grande
    private static final Object[][] CAJA = {
            {20, "racion_viaje", 4}, {15, "galleta_fortuna", 6}, {12, "elixir_furia", 1}, {12, "elixir_coloso", 1},
            {10, "botiquin", 2}, {8, "granada_sonica", 4}, {8, "totem_of_undying", 1}, {6, "doubletotem", 1},
            {4, "diamond", 8}, {3, "netherite_ingot", 1}, {2, "mochila_nivel_3", 1}
    };

    public static Object[] premioCaja(int tirada) {
        for (Object[] premio : CAJA) {
            tirada -= (int) premio[0];
            if (tirada < 0) return premio;
        }
        return CAJA[0];
    }

    public static int usosMaximos(String id) {
        Def def = DEFS.get(id);
        return def == null ? 0 : def.usos();
    }

    static String lineaUsos(int quedan, int maximo) {
        return GRIS + "Usos: " + ChatColor.WHITE + quedan + GRIS + "/" + maximo;
    }

    // Los elixires son pociones con efectos vanilla; los que hacen algo propio (abisal, tónico, antídoto, sabiduría)
    // los aplica el listener al tomarlos
    static List<PotionEffect> efectos(String id) {
        return switch (id) {
            case "elixir_minero" -> List.of(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 8, 0),
                    new PotionEffect(PotionEffectType.HASTE, 20 * 60 * 4, 0));
            case "elixir_igneo" -> List.of(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60 * 6, 0),
                    new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 2, 0));
            case "elixir_agilidad" -> List.of(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 3, 1),
                    new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 60 * 3, 1));
            case "elixir_coloso" -> List.of(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 60 * 5, 1),
                    new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60 * 5, 0));
            case "elixir_furia" -> List.of(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60 * 2, 1),
                    new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 2, 0));
            case "elixir_abisal" -> List.of(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 60 * 5, 0));
            default -> List.of();
        };
    }

    private static Color colorPocion(String hex) {
        return Color.fromRGB(Integer.parseInt(hex.substring(1), 16));
    }

    private static String nombreEfecto(PotionEffectType tipo) {
        if (tipo.equals(PotionEffectType.NIGHT_VISION)) return "Visión Nocturna";
        if (tipo.equals(PotionEffectType.HASTE)) return "Prisa";
        if (tipo.equals(PotionEffectType.FIRE_RESISTANCE)) return "Resistencia al Fuego";
        if (tipo.equals(PotionEffectType.RESISTANCE)) return "Resistencia";
        if (tipo.equals(PotionEffectType.SPEED)) return "Velocidad";
        if (tipo.equals(PotionEffectType.JUMP_BOOST)) return "Salto";
        if (tipo.equals(PotionEffectType.HEALTH_BOOST)) return "Vida Extra";
        if (tipo.equals(PotionEffectType.STRENGTH)) return "Fuerza";
        return tipo.getKey().getKey();
    }

    private static String romano(int nivel) {
        return switch (nivel) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(nivel);
        };
    }

    private static String duracion(int ticks) {
        int segundos = ticks / 20;
        return segundos % 60 == 0 ? segundos / 60 + " min" : segundos + " s";
    }
}
