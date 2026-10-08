package Web;

import com.google.common.collect.Multimap;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Pasa los ItemStack al formato de la web (el mismo de data/juego.json): nombre y lore con sus códigos de color,
// rareza, brillo, encantamientos, atributos y efectos. Los items custom se reconocen por su firma (material, nombre
// sin colores y item model) para que dentro de las recetas y recompensas salgan como {id, n}
final class ItemsWeb {

    private final Map<String, String> firmas = new HashMap<>();

    void registrar(String id, ItemStack item) {
        if (item != null) firmas.putIfAbsent(firma(item), id);
    }

    static String firma(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String nombre = meta != null && meta.hasDisplayName() ? sinColores(meta.getDisplayName()) : "";
        String modelo = meta != null && meta.hasItemModel() ? String.valueOf(meta.getItemModel()) : "";
        return clave(item.getType()) + "|" + nombre + "|" + modelo;
    }

    static String sinColores(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("(?i)[§&]x([§&][0-9a-f]){6}", "").replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }

    static String clave(Material material) {
        return material.getKey().getKey();
    }

    static String clave(Keyed keyed) {
        var key = keyed.getKey();
        return "minecraft".equals(key.getNamespace()) ? key.getKey() : key.getNamespace() + ":" + key.getKey();
    }

    // Item completo, como lo muestra el tooltip del juego
    Map<String, Object> item(ItemStack item, boolean custom) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("material", clave(item.getType()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) out.put("nombre", meta.getDisplayName());
            if (meta.hasLore() && meta.getLore() != null) out.put("lore", new ArrayList<>(meta.getLore()));
            if (meta.hasRarity()) out.put("rareza", meta.getRarity().name().toLowerCase(Locale.ROOT));

            Map<String, Integer> encantamientos = encantamientos(meta.getEnchants());
            Map<String, Integer> guardados = meta instanceof EnchantmentStorageMeta libro
                    ? encantamientos(libro.getStoredEnchants()) : Map.of();
            if (meta.hasEnchantmentGlintOverride()) {
                out.put("brillo", Boolean.TRUE.equals(meta.getEnchantmentGlintOverride()));
            } else if (!encantamientos.isEmpty() || !guardados.isEmpty()) {
                out.put("brillo", true);
            }
            if (!encantamientos.isEmpty()) out.put("encantamientos", encantamientos);
            if (!guardados.isEmpty()) out.put("guardados", guardados);

            List<String> ocultar = new ArrayList<>();
            List<String> flags = new ArrayList<>();
            for (ItemFlag flag : meta.getItemFlags()) flags.add(flag.name());
            if (flags.contains("HIDE_ENCHANTS")) ocultar.add("encantamientos");
            if (flags.contains("HIDE_ATTRIBUTES")) ocultar.add("atributos");
            if (flags.contains("HIDE_ADDITIONAL_TOOLTIP")) ocultar.add("extra");
            if (!ocultar.isEmpty()) out.put("ocultar", ocultar);

            Multimap<Attribute, AttributeModifier> modificadores = meta.getAttributeModifiers();
            if (modificadores != null && !modificadores.isEmpty()) {
                List<Map<String, Object>> atributos = new ArrayList<>();
                for (Map.Entry<Attribute, AttributeModifier> entry : modificadores.entries()) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("atributo", entry.getKey().getKey().getKey());
                    a.put("valor", entry.getValue().getAmount());
                    a.put("operacion", entry.getValue().getOperation().name());
                    a.put("ranura", String.valueOf(entry.getValue().getSlotGroup()).toLowerCase(Locale.ROOT));
                    atributos.add(a);
                }
                if (!atributos.isEmpty()) out.put("atributos", atributos);
            }

            if (meta instanceof PotionMeta pocion) {
                PotionType base = pocion.getBasePotionType();
                if (base != null) out.put("pocion", base.getKey().getKey());
                if (pocion.hasColor() && pocion.getColor() != null) out.put("color", hex(pocion.getColor()));
                if (pocion.hasCustomEffects()) {
                    List<Map<String, Object>> efectos = new ArrayList<>();
                    for (PotionEffect efecto : pocion.getCustomEffects()) {
                        Map<String, Object> e = new LinkedHashMap<>();
                        e.put("efecto", efecto.getType().getKey().getKey());
                        e.put("nivel", efecto.getAmplifier() + 1);
                        e.put("ticks", efecto.getDuration());
                        efectos.add(e);
                    }
                    if (!efectos.isEmpty()) out.put("efectos", efectos);
                }
            }
            if (meta instanceof LeatherArmorMeta cuero) out.put("color", hex(cuero.getColor()));
            if (meta instanceof SkullMeta cabeza && cabeza.getPlayerProfile() != null && cabeza.getPlayerProfile().getName() != null) {
                out.put("cabeza", cabeza.getPlayerProfile().getName());
            }
            if (meta.isUnbreakable()) out.put("irrompible", true);
            if (meta.isGlider()) out.put("planeador", true);
            if (meta.hasItemModel() && meta.getItemModel() != null) out.put("modelo", meta.getItemModel().toString());
            if (meta.hasMaxStackSize()) out.put("maxStack", meta.getMaxStackSize());
        }
        if (custom) out.put("custom", true);
        return out;
    }

    // Referencia a un item dentro de una receta o una recompensa
    Map<String, Object> ref(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        String id = firmas.get(firma(item));
        if (id != null) {
            out.put("id", id);
        } else {
            out.put("vanilla", true);
            out.putAll(item(item, false));
        }
        out.put("n", item.getAmount());
        return out;
    }

    Map<String, Object> eleccion(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exacta && !exacta.getChoices().isEmpty()) {
            ItemStack primera = exacta.getChoices().get(0).clone();
            primera.setAmount(1);
            return ref(primera);
        }
        if (choice instanceof RecipeChoice.MaterialChoice materiales && !materiales.getChoices().isEmpty()) {
            List<Material> lista = materiales.getChoices();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("vanilla", true);
            out.put("material", clave(lista.get(0)));
            out.put("n", 1);
            if (lista.size() > 1) {
                List<String> otras = new ArrayList<>();
                for (Material m : lista.subList(1, lista.size())) otras.add(clave(m));
                out.put("alternativas", otras);
            }
            return out;
        }
        return null;
    }

    private static Map<String, Integer> encantamientos(Map<Enchantment, Integer> mapa) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (mapa == null) return out;
        for (Map.Entry<Enchantment, Integer> entry : mapa.entrySet()) out.put(clave(entry.getKey()), entry.getValue());
        return out;
    }

    private static String hex(Color color) {
        return String.format("#%06x", color.asRGB());
    }
}
