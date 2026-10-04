package Managers;

import Armors.WardenArmor;
import Dificultades.OneChanges;
import Encantamientos.QuasoEnchant;
import Habilidades.HabilidadesBook;
import imp.crissyjuanxd.QuasoPlugin;
import items.*;
import items.IceBow.IceBowItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final QuasoPlugin plugin;

    private final DoubleLifeTotem doubleLifeTotem;
    private final EconomyIceTotem economyIceTotem;
    private final EconomyFlyTotem economyFlyTotem;
    private final excavatorItem ExcavatorItem;
    private final AmuletBloodM amuletBloodM;
    private final AmuletInmortal amuletInmortal;
    private final LifeCampfire lifeCampfire;
    private final IceBowItem iceBowItem;
    private final HappyGhastEnchant happyGhastEnchant;
    private final ItemsEventos itemsEventos;
    private final AmuletInvisibility amuletInvisibility;
    private final ExplosiveBow explosiveBow;
    private final InfestedSoulsItems infestedSoulsItems;
    private final WardenUpgrades wardenUpgrades;
    private final WardenArmor wardenArmor;

    private final List<String> registeredItems;

    public ItemManager(QuasoPlugin plugin) {
        this.plugin = plugin;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.economyIceTotem = new EconomyIceTotem(plugin);
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.ExcavatorItem = new excavatorItem(plugin);
        this.amuletBloodM = new AmuletBloodM(plugin);
        this.amuletInmortal = new AmuletInmortal(plugin);
        this.lifeCampfire = new LifeCampfire(plugin);
        this.iceBowItem = new IceBowItem(plugin);
        this.happyGhastEnchant = new HappyGhastEnchant(plugin);
        this.itemsEventos = new ItemsEventos(plugin);
        this.amuletInvisibility = new AmuletInvisibility(plugin);
        this.explosiveBow = new ExplosiveBow(plugin);
        this.infestedSoulsItems = new InfestedSoulsItems(plugin);
        this.wardenUpgrades = new WardenUpgrades(plugin);
        this.wardenArmor = new WardenArmor(plugin);

        this.registeredItems = new ArrayList<>();
        cargarNombresDeItems();
    }

    private void cargarNombresDeItems() {
        String[] items = {
                "doubletotem", "corrupted_steak", "corrupted_golden_apple", "libro_habilidades",
                "dinocoins", "dinofichas", "blood_fragment", "mochila_nivel_1", "mochila_nivel_2",
                "mochila_nivel_3", "mochila_nivel_4", "mochila_nivel_5", "enderbag", "gancho",
                "panic_apple", "artefacto_nivel_1", "artefacto_nivel_2", "misiones", "icetotem",
                "flytotem", "excavator_pickaxe", "potion_resistance_2", "splash_resistance_3",
                "potion_slow_falling", "splash_regeneration_3", "potion_haste_3", "potion_haste_2",
                "splash_absorption_10", "frasco_de_velocidad", "amulet_bloodmoon", "amuleto_inmortalidad",
                "life_campfire", "fuel_campfire", "special_totem", "cristal_hielo", "arco_hielo",
                "happy_ghast_enchant", "happy_ghast_enchant_2", "perla_infinita", "retorno_warden", "tarta_calabaza_mejorada", "bar_tequila", "bar_margarita",
                "bar_mezcal", "bar_pulque", "bar_cerveza", "bar_ron", "bar_vodka", "bar_whisky",
                "bar_sake", "bar_ginebra", "bar_azulito", "bar_michelada", "manzana_vida", "pluma_levitacion",

                "amuleto_invisiblidad", "arco_nivel1", "arco_nivel2", "arco_nivel3", "alma_infested_skeleton",
                "alma_infested_ghast", "alma_infested_creeper", "alma_infested_cave_spider", "energia_warden",
                "mineral_crudo_cian", "mineral_crudo_verde", "mineral_crudo_morado", "mineral_crudo_gris",
                "fragmento_profundo_cian", "fragmento_profundo_verde", "fragmento_profundo_morado",
                "fragmento_profundo_gris", "lingote_profundo", "corazon_warden_boss",
                "mejora_casco_warden", "mejora_peto_warden", "mejora_pantalon_warden", "mejora_bota_warden",

                "casco_warden", "peto_warden", "pantalon_warden", "bota_warden",

                "chatarra", "manzana_podrida", "zanahoria_encantada",
                "pepitas_hierro_oxidadas", "pepitas_diamante",
                "fragmentos_ambar", "fosiles_pequenos", "lingote_platino"
        };
        for (String item : items) {
            registeredItems.add(item);
        }
        registeredItems.addAll(QuasoEnchant.commandNames());
    }

    public ItemStack getItem(String itemName, int cantidad, Player target) {
        return getItem(itemName, cantidad, target, -1);
    }

    // Crea cualquier item custom del plugin por su nombre (lo usan /giveqp, el casino y las tiendas)
    public ItemStack getItem(String itemName, int cantidad, Player target, int usosEspeciales) {
        ItemStack item = null;

        switch (itemName.toLowerCase()) {
            case "doubletotem": item = doubleLifeTotem.createDoubleLifeTotem(); break;
            case "corrupted_steak": item = OneChanges.corruptedSteak(); break;
            case "corrupted_golden_apple": item = CorruptedGoldenApple.createCorruptedGoldenApple(); break;
            case "libro_habilidades": item = HabilidadesBook.createHabilidadesBook(); break;
            case "dinocoins": item = EconomyItems.createVithiumCoin(); break;
            case "dinofichas": item = EconomyItems.createVithiumToken(); break;
            case "blood_fragment": item = EconomyItems.createBloodFragment(); break;
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
            case "icetotem": item = economyIceTotem.createIceTotem(); break;
            case "flytotem": item = economyFlyTotem.createFlyTotem(); break;
            case "excavator_pickaxe": item = ExcavatorItem.createExcavator(); break;
            case "potion_resistance_2": item = CustomPotions.getResistanceIIPotion(); break;
            case "splash_resistance_3": item = CustomPotions.getSplashResistanceIIIPotion(); break;
            case "potion_slow_falling": item = CustomPotions.getSlowFallingPotion(); break;
            case "splash_regeneration_3": item = CustomPotions.getSplashRegenerationIIIPotion(); break;
            case "potion_haste_3": item = CustomPotions.getHasteIIIPotion(); break;
            case "potion_haste_2": item = CustomPotions.getHasteIIPotion(); break;
            case "splash_absorption_10": item = CustomPotions.getSplashAbsorptionXPotion(); break;
            case "frasco_de_velocidad": item = CustomPotions.getSpeedHoneyBottle(); break;
            case "amulet_bloodmoon": item = amuletBloodM.createAmulet(); break;
            case "amuleto_inmortalidad": item = amuletInmortal.createAmulet(); break;
            case "life_campfire": item = lifeCampfire.createCampfire(); break;
            case "fuel_campfire": item = lifeCampfire.createFuel(); break;
            case "special_totem": item = ItemsTotems.createSpecialTotem(); break;
            case "cristal_hielo": item = ItemsTotems.createIceCrystal(); break;
            case "arco_hielo": item = iceBowItem.createIceBow(); break;
            case "happy_ghast_enchant": item = happyGhastEnchant.createFastFlightBook(1); break;
            case "happy_ghast_enchant_2": item = happyGhastEnchant.createFastFlightBook(2); break;
            case "perla_infinita": item = InfinitePearl.createPearl(); break;
            case "retorno_warden": item = WardenReturnItem.create(); break;
            case "tarta_calabaza_mejorada": item = OneChanges.improvedPumpkinPie(); break;
            case "bar_tequila": item = CustomPotions.getTequila(); break;
            case "bar_margarita": item = CustomPotions.getMargarita(); break;
            case "bar_mezcal": item = CustomPotions.getMezcal(); break;
            case "bar_pulque": item = CustomPotions.getPulque(); break;
            case "bar_cerveza": item = CustomPotions.getBeer(); break;
            case "bar_ron": item = CustomPotions.getRum(); break;
            case "bar_vodka": item = CustomPotions.getVodka(); break;
            case "bar_whisky": item = CustomPotions.getWhisky(); break;
            case "bar_sake": item = CustomPotions.getSake(); break;
            case "bar_ginebra": item = CustomPotions.getGin(); break;
            case "bar_azulito": item = CustomPotions.getAzulito(); break;
            case "bar_michelada": item = CustomPotions.getMichelada(); break;
            case "manzana_vida": item = itemsEventos.createManzanaVida(); break;
            case "pluma_levitacion": item = itemsEventos.createPlumaLevitacion(); break;

            case "amuleto_invisiblidad": item = amuletInvisibility.createAmulet(); break;
            case "arco_nivel1": item = explosiveBow.createExplosiveBowLevel1(); break;
            case "arco_nivel2": item = explosiveBow.createExplosiveBowLevel2(); break;
            case "arco_nivel3": item = explosiveBow.createExplosiveBowLevel3(); break;
            case "alma_infested_skeleton": item = infestedSoulsItems.createInfestedSkeletonSoul(); break;
            case "alma_infested_ghast": item = infestedSoulsItems.createInfestedGhastSoul(); break;
            case "alma_infested_creeper": item = infestedSoulsItems.createInfestedCreeperSoul(); break;
            case "alma_infested_cave_spider": item = infestedSoulsItems.createInfestedCaveSpiderSoul(); break;
            case "energia_warden": item = WardenCaveItems.createWardenEnergy(); break;
            case "mineral_crudo_cian": item = WardenCaveItems.createRawOre(WardenCaveItems.Variant.CIAN); break;
            case "mineral_crudo_verde": item = WardenCaveItems.createRawOre(WardenCaveItems.Variant.VERDE); break;
            case "mineral_crudo_morado": item = WardenCaveItems.createRawOre(WardenCaveItems.Variant.MORADO); break;
            case "mineral_crudo_gris": item = WardenCaveItems.createRawOre(WardenCaveItems.Variant.GRIS); break;
            case "fragmento_profundo_cian": item = WardenCaveItems.createFragment(WardenCaveItems.Variant.CIAN); break;
            case "fragmento_profundo_verde": item = WardenCaveItems.createFragment(WardenCaveItems.Variant.VERDE); break;
            case "fragmento_profundo_morado": item = WardenCaveItems.createFragment(WardenCaveItems.Variant.MORADO); break;
            case "fragmento_profundo_gris": item = WardenCaveItems.createFragment(WardenCaveItems.Variant.GRIS); break;
            case "lingote_profundo": item = WardenCaveItems.createDeepIngot(); break;
            case "corazon_warden_boss": item = WardenCaveItems.createWardenBossHeart(); break;
            case "mejora_casco_warden": item = wardenUpgrades.createHelmetWardenUpgrade(); break;
            case "mejora_peto_warden": item = wardenUpgrades.createChestplateWardenUpgrade(); break;
            case "mejora_pantalon_warden": item = wardenUpgrades.createLeggingsWardenUpgrade(); break;
            case "mejora_bota_warden": item = wardenUpgrades.createBootsWardenUpgrade(); break;

            case "casco_warden": item = wardenArmor.createWardenHelmet(); break;
            case "peto_warden": item = wardenArmor.createWardenChestplate(); break;
            case "pantalon_warden": item = wardenArmor.createWardenLeggings(); break;
            case "bota_warden": item = wardenArmor.createWardenBoots(); break;

            case "chatarra": item = FishingItems.createChatarra(); break;
            case "manzana_podrida": item = FishingItems.createManzanaPodrida(); break;
            case "zanahoria_encantada": item = FishingItems.createZanahoriaEncantada(); break;
            case "pepitas_hierro_oxidadas": item = FishingItems.createPepitasHierroOxidadas(); break;
            case "pepitas_diamante": item = FishingItems.createPepitasDiamante(); break;
            case "fragmentos_ambar": item = FishingItems.createFragmentosAmbar(); break;
            case "fosiles_pequenos": item = FishingItems.createFosilesP(); break;
            case "lingote_platino": item = FishingItems.createLingotePlatino(); break;
            default:
                item = QuasoEnchant.fromCommand(itemName.toLowerCase());
                if (item == null) return null;
        }

        if (item != null) {
            item.setAmount(cantidad);
        }
        return item;
    }

    public List<String> getRegisteredItems() {
        return registeredItems;
    }
}