package ShopSystem;

import Dificultades.CustomMobs.CustomBoat;
import Dificultades.DayOneChanges;
import Habilidades.HabilidadesBook;
import imp.crissyjuanxd.QuasoPlugin;
import items.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CustomItemRegistry {

    private static QuasoPlugin plugin;

    public static void init(QuasoPlugin pl) {
        plugin = pl;
    }

    public static ItemStack getCustomItem(String name, int amount) {
        ItemStack item = null;

        // Instancias necesarias para ciertos items
        items.DoubleLifeTotem doubleLifeTotem = new items.DoubleLifeTotem(plugin);
        CustomBoat customBoat = new CustomBoat(plugin);

        switch (name.toLowerCase()) {
            case "doubletotem": item = doubleLifeTotem.createDoubleLifeTotem(); break;
            case "corrupted_steak": item = DayOneChanges.corruptedSteak(); break;
            case "customboat": item = customBoat.createBoatItem(null); break; // Ojo: customboat requiere target, pon null si es genérico o arréglalo en tu lógica
            case "fuel": item = customBoat.createFuelItem(); break;
            case "corrupted_golden_apple": item = CorruptedGoldenApple.createCorruptedGoldenApple(); break;
            case "apilate_gold_block": item = CorruptedGoldenApple.createApilateGoldBlock(); break;
            case "libro_habilidades": item = HabilidadesBook.createHabilidadesBook(); break;
            case "dinocoins": item = EconomyItems.createVithiumCoin(); break;
            case "dinofichas": item = EconomyItems.createVithiumToken(); break;
            case "mochila_nivel_1": item = EconomyItems.createNormalMochila(); break;
            case "mochila_nivel_2": item = EconomyItems.createGreenMochila(); break;
            case "mochila_nivel_3": item = EconomyItems.createRedMochila(); break;
            case "mochila_nivel_4": item = EconomyItems.createBlueMochila(); break;
            case "mochila_nivel_5": item = EconomyItems.createPurpleMochila(); break;
            case "enderbag": item = EconomyItems.createEnderBag(); break;
            case "gancho": item = EconomyItems.createGancho(); break;
            case "panic_apple": item = EconomyItems.createManzanaPanico(); break;
            case "artefacto_nivel_1": item = EconomyItems.createYunqueReparadorNivel1(); break;
            case "artefacto_nivel_2": item = EconomyItems.createYunqueReparadorNivel2(); break;
            case "misiones": item = Misionesitem.createMisiones(); break;
            case "corrupted_rotten": item = CorruptedMobItems.createCorruptedMeet(); break;
            default:
                // Intento de material vanilla
                try {
                    item = new ItemStack(Material.valueOf(name.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }

        if (item != null) item.setAmount(amount);
        return item;
    }

    public static List<String> getAllCustomNames() {
        List<String> list = new ArrayList<>();
        list.add("doubletotem");
        list.add("corrupted_steak");
        list.add("customboat");
        list.add("fuel");
        list.add("corrupted_golden_apple");
        list.add("apilate_gold_block");
        list.add("libro_habilidades");
        list.add("dinocoins");
        list.add("dinofichas");
        list.add("mochila_nivel_1");
        list.add("mochila_nivel_2");
        list.add("mochila_nivel_3");
        list.add("mochila_nivel_4");
        list.add("mochila_nivel_5");
        list.add("enderbag");
        list.add("gancho");
        list.add("panic_apple");
        list.add("artefacto_nivel_1");
        list.add("artefacto_nivel_2");
        list.add("misiones");
        list.add("corrupted_rotten");
        list.add("corrupted_spidereyes");
        return list;
    }
}