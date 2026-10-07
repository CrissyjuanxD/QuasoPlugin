package Gui;

// Las cinco zonas del /menu con sus slots. El dibujo lo pone la textura del título y cada zona está llena de items
// invisibles con el nombre, así se puede hacer clic en cualquier parte del dibujo
public enum SeccionMenu {
    MISIONES("Misiones", "#C9A7EB", 1, 2, 3, 10, 11, 12, 19, 20, 21),
    TRABAJOS("Trabajos", "#C8A27C", 5, 6, 7, 14, 15, 16, 23, 24, 25),
    HABILIDADES("Habilidades", "#C77DFF", 27, 28, 29, 36, 37, 38, 45, 46, 47),
    PROTECCIONES("Protecciones", "#8FD3A8", 33, 34, 35, 42, 43, 44, 51, 52, 53),
    HOMES("Homes", "#E28B20", 39, 40, 41, 48, 49, 50);

    public final String nombre;
    public final String color;
    private final int[] slots;

    SeccionMenu(String nombre, String color, int... slots) {
        this.nombre = nombre;
        this.color = color;
        this.slots = slots;
    }

    public int[] slots() {
        return slots.clone();
    }

    // La zona de un slot, o null si es fondo
    public static SeccionMenu porSlot(int slot) {
        for (SeccionMenu seccion : values()) {
            for (int propio : seccion.slots) {
                if (propio == slot) return seccion;
            }
        }
        return null;
    }
}
