package Events.MissionSystem;

import Trabajos.Trabajo;
import Trabajos.TrabajoSubeNivelEvent;
import Trabajos.TrabajosManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

import static Events.MissionSystem.MissionRewards.*;

// Misiones de trabajo (141 a 200): llegar al nivel 10, 20... 100 de cada trabajo. Están activas siempre
public class MisionTrabajo extends BaseMission {

    public static final int PRIMERA = 141;
    public static final int ULTIMA = PRIMERA + Trabajo.values().length * 10 - 1;

    private static final String[] RANGOS = {"Aprendiz", "Ayudante", "Oficial", "Experto", "Veterano",
            "Maestro", "Gran Maestro", "Élite", "Leyenda", "Dios"};
    private static final int[] MONEDAS = {5, 8, 12, 15, 20, 25, 30, 35, 40, 60};

    private final Trabajo trabajo;
    private final int nivel;

    private MisionTrabajo(JavaPlugin plugin, MissionHandler handler, Trabajo trabajo, int tramo) {
        super(plugin, handler, PRIMERA + trabajo.ordinal() * 10 + tramo, RANGOS[tramo] + " " + oficio(trabajo),
                dificultad(tramo), MONEDAS[tramo], "Llega al nivel " + (tramo + 1) * 10 + " del trabajo " + trabajo.nombre() + ".");
        this.trabajo = trabajo;
        this.nivel = (tramo + 1) * 10;
        counter("nivel", "Nivel de " + trabajo.nombre(), nivel);
    }

    public static List<Mission> todas(JavaPlugin plugin, MissionHandler handler) {
        List<Mission> misiones = new ArrayList<>();
        for (Trabajo trabajo : Trabajo.values()) {
            for (int tramo = 0; tramo < 10; tramo++) misiones.add(new MisionTrabajo(plugin, handler, trabajo, tramo));
        }
        return misiones;
    }

    public static boolean es(int numero) {
        return numero >= PRIMERA && numero <= ULTIMA;
    }

    public Trabajo getTrabajo() { return trabajo; }

    private static String oficio(Trabajo trabajo) {
        return switch (trabajo) {
            case MINERIA -> "Minero";
            default -> trabajo.nombre();
        };
    }

    private static MissionDifficulty dificultad(int tramo) {
        if (tramo <= 1) return MissionDifficulty.FACIL;
        if (tramo <= 3) return MissionDifficulty.MEDIA;
        if (tramo <= 6) return MissionDifficulty.DIFICIL;
        return MissionDifficulty.MUY_DIFICIL;
    }

    // El nivel se lee en vivo del sistema de trabajos
    @Override
    protected int value(Player player, MissionData data, MissionObjective objective) {
        return Math.max(0, TrabajosManager.nivel(player, trabajo));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNivel(TrabajoSubeNivelEvent event) {
        if (event.getTrabajo() == trabajo && event.getNivel() >= nivel) check(event.getPlayer());
    }

    // Por si ya tenía el nivel al conectarse o un admin se lo cambió
    @Override
    protected int tickSeconds() {
        return 30;
    }

    @Override
    protected void tick(Player player) {
        if (TrabajosManager.nivel(player, trabajo) >= nivel) check(player);
    }

    @Override
    protected List<List<ItemStack>> rewardItems() {
        int tramo = nivel / 10 - 1;
        return of(objetoDelTrabajo(tramo), comun(tramo));
    }

    private static ItemStack comun(int tramo) {
        return switch (tramo) {
            case 0 -> item(Material.EXPERIENCE_BOTTLE, 8);
            case 1 -> item(Material.GOLDEN_CARROT, 16);
            case 2 -> item(Material.DIAMOND, 3);
            case 3 -> item(Material.GOLDEN_APPLE, 3);
            case 4 -> item(Material.DIAMOND, 8);
            case 5 -> item(Material.NETHERITE_SCRAP, 1);
            case 6 -> item(Material.ENCHANTED_GOLDEN_APPLE, 1);
            case 7 -> item(Material.NETHERITE_SCRAP, 2);
            case 8 -> item(Material.NETHERITE_INGOT, 1);
            default -> item(Material.NETHERITE_INGOT, 2);
        };
    }

    private ItemStack objetoDelTrabajo(int tramo) {
        if (tramo == 9 && trabajo != Trabajo.CONSTRUCTOR) return book(Enchantment.MENDING, 1);
        return switch (trabajo) {
            case GUERRERO -> switch (tramo) {
                case 0 -> enchanted(Material.IRON_SWORD, Enchantment.SHARPNESS, 2, Enchantment.UNBREAKING, 1);
                case 1 -> item(Material.ARROW, 64);
                case 2 -> book(Enchantment.SHARPNESS, 3);
                case 3 -> enchanted(Material.BOW, Enchantment.POWER, 3, Enchantment.UNBREAKING, 2);
                case 4 -> enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3, Enchantment.LOOTING, 2);
                case 5 -> book(Enchantment.PROTECTION, 4);
                case 6 -> book(Enchantment.LOOTING, 3);
                case 7 -> enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                default -> book(Enchantment.SHARPNESS, 5);
            };
            case MINERIA -> switch (tramo) {
                case 0 -> enchanted(Material.IRON_PICKAXE, Enchantment.EFFICIENCY, 2, Enchantment.UNBREAKING, 1);
                case 1 -> item(Material.TORCH, 64);
                case 2 -> book(Enchantment.EFFICIENCY, 3);
                case 3 -> potion(PotionType.LONG_NIGHT_VISION, 3);
                case 4 -> enchanted(Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY, 3, Enchantment.FORTUNE, 2);
                case 5 -> book(Enchantment.UNBREAKING, 3);
                case 6 -> book(Enchantment.FORTUNE, 3);
                case 7 -> enchanted(Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3);
                default -> book(Enchantment.EFFICIENCY, 5);
            };
            case LENADOR -> switch (tramo) {
                case 0 -> enchanted(Material.IRON_AXE, Enchantment.EFFICIENCY, 2, Enchantment.UNBREAKING, 1);
                case 1 -> item(Material.APPLE, 16);
                case 2 -> book(Enchantment.EFFICIENCY, 3);
                case 3 -> enchanted(Material.DIAMOND_AXE, Enchantment.EFFICIENCY, 2, Enchantment.UNBREAKING, 2);
                case 4 -> enchanted(Material.DIAMOND_AXE, Enchantment.EFFICIENCY, 4, Enchantment.UNBREAKING, 3);
                case 5 -> book(Enchantment.UNBREAKING, 3);
                case 6 -> book(Enchantment.SHARPNESS, 4);
                case 7 -> enchanted(Material.DIAMOND_AXE, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3);
                default -> book(Enchantment.EFFICIENCY, 5);
            };
            case CONSTRUCTOR -> switch (tramo) {
                case 0 -> item(Material.SCAFFOLDING, 64);
                case 1 -> item(Material.GLASS, 64);
                case 2 -> enchanted(Material.DIAMOND_SHOVEL, Enchantment.EFFICIENCY, 3, Enchantment.UNBREAKING, 2);
                case 3 -> item(Material.QUARTZ_BLOCK, 64);
                case 4 -> item(Material.SHULKER_SHELL, 2);
                case 5 -> item(Material.LANTERN, 32);
                case 6 -> item(Material.SHULKER_SHELL, 4);
                case 7 -> item(Material.SHULKER_SHELL, 6);
                case 8 -> book(Enchantment.EFFICIENCY, 5);
                default -> item(Material.BEACON, 1);
            };
            case GRANJERO -> switch (tramo) {
                case 0 -> enchanted(Material.IRON_HOE, Enchantment.EFFICIENCY, 2, Enchantment.UNBREAKING, 1);
                case 1 -> item(Material.BONE_MEAL, 64);
                case 2 -> item(Material.GOLDEN_CARROT, 32);
                case 3 -> enchanted(Material.DIAMOND_HOE, Enchantment.EFFICIENCY, 3, Enchantment.FORTUNE, 2);
                case 4 -> item(Material.HAY_BLOCK, 32);
                case 5 -> book(Enchantment.FORTUNE, 3);
                case 6 -> item(Material.CAKE, 4);
                case 7 -> enchanted(Material.DIAMOND_HOE, Enchantment.FORTUNE, 3, Enchantment.UNBREAKING, 3);
                default -> item(Material.GOLDEN_CARROT, 64);
            };
            case PESCADOR -> switch (tramo) {
                case 0 -> enchanted(Material.FISHING_ROD, Enchantment.LURE, 1, Enchantment.UNBREAKING, 1);
                case 1 -> item(Material.COOKED_SALMON, 32);
                case 2 -> book(Enchantment.LURE, 2);
                case 3 -> enchanted(Material.FISHING_ROD, Enchantment.LURE, 2, Enchantment.LUCK_OF_THE_SEA, 2);
                case 4 -> book(Enchantment.LUCK_OF_THE_SEA, 3);
                case 5 -> item(Material.NAUTILUS_SHELL, 4);
                case 6 -> enchanted(Material.FISHING_ROD, Enchantment.LURE, 3, Enchantment.LUCK_OF_THE_SEA, 3);
                case 7 -> item(Material.HEART_OF_THE_SEA, 1);
                default -> enchanted(Material.TRIDENT, Enchantment.LOYALTY, 3, Enchantment.IMPALING, 3);
            };
        };
    }
}
