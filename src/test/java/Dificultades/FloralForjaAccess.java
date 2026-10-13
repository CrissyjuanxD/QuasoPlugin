package Dificultades;

import org.bukkit.inventory.ItemStack;

// Para probar la receta de la Mejora Floral desde otros paquetes
public final class FloralForjaAccess {
    private FloralForjaAccess() {}

    public static boolean alcanza(ItemStack[] matrix) { return FloralForja.alcanza(matrix); }
    public static boolean completo(ItemStack[] matrix) { return FloralForja.completo(matrix); }
    public static void gastar(ItemStack[] matrix) { FloralForja.gastar(matrix); }
}
