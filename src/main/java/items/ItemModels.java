package items;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

// En la 26.2 el custom model data ya no cambia la textura: cada item usa un item model.
// Los nombres van en config.yml (modelos.<clave>) para poner los del resource pack sin recompilar;
// Los ítems custom usan IDs únicos; el resource pack debe definir assets/<namespace>/items/<id>.json.
// Se conserva CustomModelData en los ítems antiguos que todavía lo usan como identificador.
public final class ItemModels {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("mision_completada", "minecraft:lime_banner");
        DEFAULTS.put("mision_pendiente", "minecraft:guster_banner_pattern");
        DEFAULTS.put("mision_bloqueada", "minecraft:map");
        DEFAULTS.put("ficha_mision", "minecraft:popped_chorus_fruit");
        DEFAULTS.put("libro_misiones", "minecraft:map");
        DEFAULTS.put("retorno_warden", "minecraft:recovery_compass");
        for (int card = 5000; card < 5052; card++) {
            DEFAULTS.put("carta_" + card, "minecraft:carta_" + card);
        }
        DEFAULTS.put("barco_custom", "minecraft:barco_custom");
        DEFAULTS.put("combustible_barco", "minecraft:combustible_barco");
        DEFAULTS.put("quaso_ticket", "minecraft:quaso_ticket");
        DEFAULTS.put("spawner_custom", "minecraft:spawner_custom");
        DEFAULTS.put("logro_completado", "minecraft:logro_completado");
        DEFAULTS.put("logro_borde", "minecraft:logro_borde");
        DEFAULTS.put("carta_oculta", "minecraft:carta_oculta");
        DEFAULTS.put("logro_pendiente", "minecraft:logro_pendiente");
        DEFAULTS.put("casino_cerrar", "minecraft:casino_cerrar");
        DEFAULTS.put("casino_girar", "minecraft:casino_girar");
        DEFAULTS.put("casino_linea_izquierda", "minecraft:casino_linea_izquierda");
        DEFAULTS.put("casino_linea_derecha", "minecraft:casino_linea_derecha");
        DEFAULTS.put("casino_fondo", "minecraft:casino_fondo");
        DEFAULTS.put("algodon_azucar", "minecraft:algodon_azucar");
        DEFAULTS.put("alma_infested_cave_spider", "minecraft:alma_infested_cave_spider");
        DEFAULTS.put("alma_infested_creeper", "minecraft:alma_infested_creeper");
        DEFAULTS.put("alma_infested_ghast", "minecraft:alma_infested_ghast");
        DEFAULTS.put("alma_infested_skeleton", "minecraft:alma_infested_skeleton");
        DEFAULTS.put("amulet_bloodmoon", "minecraft:amulet_bloodmoon");
        DEFAULTS.put("amuleto_inmortalidad", "minecraft:immunity");
        DEFAULTS.put("amuleto_invisibilidad", "minecraft:amuleto_invisibilidad");
        DEFAULTS.put("amuleto_ultima_esperanza", "minecraft:amuleto_esperanza");
        DEFAULTS.put("arco_hielo", "minecraft:arco_hielo");
        DEFAULTS.put("arco_nivel1", "minecraft:arco_nivel1");
        DEFAULTS.put("arco_nivel2", "minecraft:arco_nivel2");
        DEFAULTS.put("arco_nivel3", "minecraft:arco_nivel3");
        DEFAULTS.put("artefacto_nivel_1", "minecraft:rep_hierro");
        DEFAULTS.put("artefacto_nivel_2", "minecraft:rep_oro");
        DEFAULTS.put("bar_azulito", "minecraft:bar_azulito");
        DEFAULTS.put("bar_cerveza", "minecraft:bar_cerveza");
        DEFAULTS.put("bar_ginebra", "minecraft:bar_ginebra");
        DEFAULTS.put("bar_margarita", "minecraft:bar_margarita");
        DEFAULTS.put("bar_mezcal", "minecraft:bar_mezcal");
        DEFAULTS.put("bar_michelada", "minecraft:bar_michelada");
        DEFAULTS.put("bar_pulque", "minecraft:bar_pulque");
        DEFAULTS.put("bar_ron", "minecraft:bar_ron");
        DEFAULTS.put("bar_sake", "minecraft:bar_sake");
        DEFAULTS.put("bar_tequila", "minecraft:bar_tequila");
        DEFAULTS.put("bar_vodka", "minecraft:bar_vodka");
        DEFAULTS.put("bar_whisky", "minecraft:bar_whisky");
        DEFAULTS.put("baya_luminosa", "minecraft:baya_luminosa");
        DEFAULTS.put("baya_sculk", "minecraft:baya_sculk");
        DEFAULTS.put("blood_fragment", "minecraft:blood_fragment");
        DEFAULTS.put("bloque_oro_apilado", "minecraft:bloque_oro_apilado");
        DEFAULTS.put("bota_warden", "minecraft:bota_warden");
        DEFAULTS.put("caramelo", "minecraft:caramelo");
        DEFAULTS.put("casco_warden", "minecraft:casco_warden");
        DEFAULTS.put("chatarra", "minecraft:chatarra");
        DEFAULTS.put("corazon_warden_boss", "minecraft:corazon_warden_boss");
        DEFAULTS.put("corrupted_golden_apple", "minecraft:corrupted_golden_apple");
        DEFAULTS.put("corrupted_meat", "minecraft:corrupted_meat");
        DEFAULTS.put("corrupted_spider_eye", "minecraft:corrupted_spider_eye");
        DEFAULTS.put("corrupted_steak", "minecraft:corrupted_steak");
        DEFAULTS.put("cristal_celestita", "minecraft:cristal_celestita");
        DEFAULTS.put("cristal_hielo", "minecraft:cristal_hielo");
        DEFAULTS.put("dinocoins", "minecraft:dinocoins");
        DEFAULTS.put("monedero", "minecraft:monedero");
        DEFAULTS.put("dinofichas", "minecraft:dinofichas");
        DEFAULTS.put("doubletotem_1", "minecraft:totem_doble1");
        DEFAULTS.put("doubletotem_2", "minecraft:totem_doble2");
        DEFAULTS.put("enderbag", "minecraft:ender_bag");
        DEFAULTS.put("energia_warden", "minecraft:energia_warden");
        DEFAULTS.put("esencia_marchita", "minecraft:esencia_marchita");
        DEFAULTS.put("estatua_protectora", "minecraft:statue_pr");
        DEFAULTS.put("excavator_pickaxe", "minecraft:excavator_pickaxe");
        DEFAULTS.put("flytotem", "minecraft:flytotem");
        DEFAULTS.put("fosiles_pequenos", "minecraft:fosiles_pequenos");
        DEFAULTS.put("fragmento_astral", "minecraft:fragmento_astral");
        DEFAULTS.put("fragmento_profundo_cian", "minecraft:fragmento_profundo_cian");
        DEFAULTS.put("fragmento_profundo_gris", "minecraft:fragmento_profundo_gris");
        DEFAULTS.put("fragmento_profundo_morado", "minecraft:fragmento_profundo_morado");
        DEFAULTS.put("fragmento_profundo_verde", "minecraft:fragmento_profundo_verde");
        DEFAULTS.put("fragmentos_ambar", "minecraft:fragmentos_ambar");
        DEFAULTS.put("frasco_de_velocidad", "minecraft:frasco_de_velocidad");
        DEFAULTS.put("fruta_abisal", "minecraft:fruta_abisal");
        DEFAULTS.put("fuel_campfire", "minecraft:fuel_campfire");
        DEFAULTS.put("gancho", "minecraft:gancho");
        DEFAULTS.put("happy_ghast_enchant", "minecraft:happy_ghast_enchant");
        DEFAULTS.put("icetotem", "minecraft:icetotem");
        DEFAULTS.put("keep_inventory_liquido", "minecraft:keep_inv_liquido");
        DEFAULTS.put("libro_anclaje", "minecraft:libro_anclaje");
        DEFAULTS.put("libro_habilidades", "minecraft:libro_habilidades");
        DEFAULTS.put("libro_paso_igneo", "minecraft:libro_paso_igneo");
        DEFAULTS.put("libro_purificacion", "minecraft:libro_purificacion");
        DEFAULTS.put("libro_retorno_del_vacio", "minecraft:libro_retorno_del_vacio");
        DEFAULTS.put("libro_vision_abisal", "minecraft:libro_vision_abisal");
        DEFAULTS.put("life_campfire", "minecraft:life_campfire");
        DEFAULTS.put("lingote_platino", "minecraft:lingote_platino");
        DEFAULTS.put("lingote_profundo", "minecraft:lingote_profundo");
        DEFAULTS.put("manzana_podrida", "minecraft:manzana_podrida");
        DEFAULTS.put("manzana_vida", "minecraft:manzana_vida");
        DEFAULTS.put("mejora_bota_warden", "minecraft:mejora_bota_warden");
        DEFAULTS.put("mejora_casco_warden", "minecraft:mejora_casco_warden");
        DEFAULTS.put("mejora_pantalon_warden", "minecraft:mejora_pantalon_warden");
        DEFAULTS.put("mejora_peto_warden", "minecraft:mejora_peto_warden");
        DEFAULTS.put("mineral_crudo_cian", "minecraft:mineral_crudo_cian");
        DEFAULTS.put("mineral_crudo_gris", "minecraft:mineral_crudo_gris");
        DEFAULTS.put("mineral_crudo_morado", "minecraft:mineral_crudo_morado");
        DEFAULTS.put("mineral_crudo_verde", "minecraft:mineral_crudo_verde");
        DEFAULTS.put("mochila_nivel_1", "minecraft:lime_bundle");
        DEFAULTS.put("mochila_nivel_2", "minecraft:blue_bundle");
        DEFAULTS.put("mochila_nivel_3", "minecraft:orange_bundle");
        DEFAULTS.put("mochila_nivel_4", "minecraft:red_bundle");
        DEFAULTS.put("mochila_nivel_5", "minecraft:purple_bundle");
        DEFAULTS.put("ojo_rey_ender", "minecraft:ojo_rey_ender");
        DEFAULTS.put("panic_apple", "minecraft:panic_apple");
        DEFAULTS.put("pantalon_warden", "minecraft:pantalon_warden");
        DEFAULTS.put("pepitas_diamante", "minecraft:pepitas_diamante");
        DEFAULTS.put("pepitas_hierro_oxidadas", "minecraft:pepitas_hierro_oxidadas");
        DEFAULTS.put("perla_infinita", "minecraft:perla_infinita");
        DEFAULTS.put("peto_warden", "minecraft:peto_warden");
        DEFAULTS.put("piruleta", "minecraft:piruleta");
        DEFAULTS.put("pluma_levitacion", "minecraft:pluma_levi");
        DEFAULTS.put("pluma_levitacion_mejorada", "minecraft:pluma_levi_mejorada");
        DEFAULTS.put("potion_haste_2", "minecraft:potion_haste_2");
        DEFAULTS.put("potion_haste_3", "minecraft:potion_haste_3");
        DEFAULTS.put("potion_resistance_2", "minecraft:potion_resistance_2");
        DEFAULTS.put("potion_slow_falling", "minecraft:potion_slow_falling");
        DEFAULTS.put("soda", "minecraft:soda");
        DEFAULTS.put("special_totem", "minecraft:special_totem");
        DEFAULTS.put("splash_absorption_10", "minecraft:splash_absorption_10");
        DEFAULTS.put("splash_regeneration_3", "minecraft:splash_regeneration_3");
        DEFAULTS.put("splash_resistance_3", "minecraft:splash_resistance_3");
        DEFAULTS.put("statue_effect", "minecraft:statue_effect");
        DEFAULTS.put("tarta_calabaza_mejorada", "minecraft:tarta_calabaza_mejorada");
        DEFAULTS.put("zanahoria_encantada", "minecraft:zanahoria_encantada");
    }

    private ItemModels() {}

    // Agrega a config.yml los modelos que falten, así aparecen para cambiarlos
    public static void load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean changed = false;
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            String path = "modelos." + entry.getKey();
            if (!config.isString(path)) {
                config.set(path, entry.getValue());
                changed = true;
            }
        }
        if (changed) plugin.saveConfig();
    }

    public static void apply(ItemMeta meta, String key) {
        String value = QuasoPlugin.getInstance().getConfig().getString("modelos." + key, DEFAULTS.get(key));
        NamespacedKey model = value != null ? NamespacedKey.fromString(value) : null;
        if (model == null) {
            QuasoPlugin.getInstance().getLogger().warning("El item model '" + value + "' de modelos." + key + " no es válido.");
            model = NamespacedKey.fromString(DEFAULTS.get(key));
        }
        if (model != null) meta.setItemModel(model);
    }
}
