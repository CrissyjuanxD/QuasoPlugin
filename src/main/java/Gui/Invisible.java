package Gui;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

// Items que no se ven en los menús: usan el modelo del aire, que viene en el juego, así no hace falta resource pack
public final class Invisible {

    private static final NamespacedKey AIRE = NamespacedKey.minecraft("air");

    private Invisible() {}

    // Relleno de los slots vacíos: sin modelo y sin tooltip, el slot se ve vacío
    public static ItemStack relleno() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(AIRE);
        meta.setHideTooltip(true);
        item.setItemMeta(meta);
        return item;
    }

    // Botón sobre la textura del menú: no se ve, pero al pasar el mouse sale el nombre y la descripción
    public static ItemStack boton(String nombre, List<String> lore) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(AIRE);
        meta.setDisplayName(nombre);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
