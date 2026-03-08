package ShopSystem;

import Dificultades.CustomMobs.CustomBoat;
import Dificultades.DayOneChanges;
import Habilidades.HabilidadesBook;
import imp.crissyjuanxd.QuasoPlugin;
import items.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class CustomItemRegistry {

    private static JavaPlugin plugin;

    public static void init(JavaPlugin pl) {
        plugin = pl;
    }

    public static ItemStack getCustomItem(String name, int amount) {
        ItemStack item = null;

        // Instancias necesarias para ciertos items
        items.DoubleLifeTotem doubleLifeTotem = new items.DoubleLifeTotem(plugin);
        items.EconomyIceTotem economyIceTotem = new items.EconomyIceTotem(plugin);
        items.EconomyFlyTotem economyFlyTotem = new items.EconomyFlyTotem(plugin);
        items.AmuletBloodM amuletBloodM = new items.AmuletBloodM(plugin);
        items.AmuletInmortal amuletInmortal = new items.AmuletInmortal(plugin);
        items.LifeCampfire lifeCampfire = new items.LifeCampfire(plugin);
        items.excavatorItem ExcavatorItem = new items.excavatorItem(plugin);
        items.IceBow.IceBowItem iceBowItem = new items.IceBow.IceBowItem(plugin);
        items.HappyGhastEnchant happyGhastEnchant = new items.HappyGhastEnchant(plugin);
        CustomBoat customBoat = new CustomBoat(plugin);

        switch (name.toLowerCase()) {
            case "doubletotem": item = doubleLifeTotem.createDoubleLifeTotem();break;
            case "corrupted_steak": item = DayOneChanges.corruptedSteak();break;
            case "corrupted_golden_apple": item = CorruptedGoldenApple.createCorruptedGoldenApple();break;
            case "libro_habilidades": item = HabilidadesBook.createHabilidadesBook();break;
            case "dinocoins": item = EconomyItems.createVithiumCoin();break;
            case "dinofichas": item = EconomyItems.createVithiumToken();break;
            case "mochila_nivel_1": item = EconomyItems.createNormalMochila();break;
            case "mochila_nivel_2": item = EconomyItems.createGreenMochila();break;
            case "mochila_nivel_3": item = EconomyItems.createRedMochila();break;
            case "mochila_nivel_4": item = EconomyItems.createBlueMochila();break;
            case "mochila_nivel_5": item = EconomyItems.createPurpleMochila();break;
            case "enderbag": item = EconomyItems.createEnderBag();break;
            case "gancho": item = EconomyItems.createGancho();break;
            case "panic_apple": item = EconomyItems.createManzanaPanico();break;
            case "artefacto_nivel_1": item = EconomyItems.createYunqueReparadorNivel1();break;
            case "artefacto_nivel_2": item = EconomyItems.createYunqueReparadorNivel2();break;
            case "misiones": item = Misionesitem.createMisiones();break;
            case "icetotem": item = economyIceTotem.createIceTotem();break;
            case "flytotem": item = economyFlyTotem.createFlyTotem();break;
            case "excavator_pickaxe": item = ExcavatorItem.createExcavator();break;
            case "potion_resistance_2": item = CustomPotions.getResistanceIIPotion();break;
            case "splash_resistance_3": item = CustomPotions.getSplashResistanceIIIPotion();break;
            case "potion_slow_falling": item = CustomPotions.getSlowFallingPotion();break;
            case "splash_regeneration_3": item = CustomPotions.getSplashRegenerationIIIPotion();break;
            case "potion_haste_3": item = CustomPotions.getHasteIIIPotion();break;
            case "potion_haste_2": item = CustomPotions.getHasteIIPotion();break;
            case "splash_absorption_10": item = CustomPotions.getSplashAbsorptionXPotion();break;
            case "frasco_de_velocidad": item = CustomPotions.getSpeedHoneyBottle();break;
            case "amulet_bloodmoon": item = amuletBloodM.createAmulet();break;
            case "amuleto_inmortalidad": item = amuletInmortal.createAmulet();break;
            case "life_campfire": item = lifeCampfire.createCampfire();break;
            case "fuel_campfire": item = lifeCampfire.createFuel();break;
            case "special_totem": item = ItemsTotems.createSpecialTotem();break;
            case "cristal_hielo": item = ItemsTotems.createIceCrystal();break;
            case "arco_hielo": item = iceBowItem.createIceBow();break;
            case "happy_ghast_enchant": item = happyGhastEnchant.createFastFlightBook(1);break;
            default:
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
        list.add("corrupted_golden_apple");
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
        list.add("icetotem");
        list.add("flytotem");
        list.add("excavator_pickaxe");
        list.add("potion_resistance_2");
        list.add("splash_resistance_3");
        list.add("potion_slow_falling");
        list.add("splash_regeneration_3");
        list.add("potion_haste_3");
        list.add("potion_haste_2");
        list.add("splash_absorption_10");
        list.add("frasco_de_velocidad");
        list.add("amulet_bloodmoon");
        list.add("amuleto_inmortalidad");
        list.add("life_campfire");
        list.add("fuel_campfire");
        list.add("special_totem");
        list.add("cristal_hielo");
        list.add("arco_hielo");
        list.add("happy_ghast_enchant");
        return list;
    }
}