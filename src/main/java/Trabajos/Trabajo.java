package Trabajos;

import java.util.List;
import java.util.Locale;

// Los 6 trabajos: su lugar en el menú (/trabajos), su color, su item model y cómo se gana experiencia
public enum Trabajo {
    GUERRERO("guerrero", "Guerrero", "⚔", "#D98C7A", 1,
            List.of("Defiende el server de los", "monstruos que lo rondan."),
            List.of("Matar monstruos: 4 a 15 XP", "Mobs del plugin: 10 a 30 XP", "Wardens: 40 XP", "Monstruos élite: 20 a 60 XP",
                    "Jefes: de 400 (Wither) a 2.200 XP (Rey Ender)")),
    MINERIA("mineria", "Minería", "⛏", "#A9B7C6", 3,
            List.of("Baja a las cuevas y saca", "los minerales del mundo."),
            List.of("Piedra, deepslate y similares: 0,05 XP", "Carbón, cobre y redstone: 2 a 5 XP", "Hierro, oro y lapislázuli: 5 a 8 XP",
                    "Diamante, esmeralda y debris: 15 a 30 XP", "Minerales de la Warden Cave: 25 XP", "Racimos de amatista del End: 5 XP",
                    "Jefes: la mitad que Guerrero")),
    LENADOR("lenador", "Leñador", "☘", "#C4A076", 5,
            List.of("Tala los bosques del server", "tronco por tronco."),
            List.of("Troncos: 1,2 XP", "Tallos del Nether: 1,4 XP", "Troncos de la Warden Cave y del End: 1,8 XP",
                    "Raíces de manglar: 0,5 XP", "Jefes: la mitad que Guerrero")),
    CONSTRUCTOR("constructor", "Constructor", "⚒", "#C9B79C", 7,
            List.of("Levanta las construcciones", "más lindas del mapa."),
            List.of("Bloques básicos: 0,1 XP", "Bloques comunes: 0,3 XP", "Ladrillos, cuarzo, concreto...: 0,5 XP",
                    "En la Warden Cave y el End: 50% más", "Jefes: la mitad que Guerrero")),
    GRANJERO("granjero", "Granjero", "✿", "#A8C3A0", 11,
            List.of("Cosecha cultivos y cría", "animales para el server."),
            List.of("Cultivos maduros: 0,6 a 1,5 XP", "Sandías y calabazas: 1 XP", "Criar animales: 4 XP",
                    "Frutas de la Warden Cave: 1,5 a 2 XP", "Jefes: la mitad que Guerrero")),
    PESCADOR("pescador", "Pescador", "⚓", "#8FB8C9", 15,
            List.of("Paciencia y caña: lo que", "pica en el agua es tuyo."),
            List.of("Peces: 6 a 8 XP", "Tesoros: 12 XP", "Basura: 3 XP", "Jefes: la mitad que Guerrero"));

    public static final int NIVEL_MAXIMO = 100;

    private final String id;
    private final String nombre;
    private final String icono;
    private final String color;
    private final int slot;
    private final List<String> descripcion;
    private final List<String> fuentes;

    Trabajo(String id, String nombre, String icono, String color, int slot, List<String> descripcion, List<String> fuentes) {
        this.id = id;
        this.nombre = nombre;
        this.icono = icono;
        this.color = color;
        this.slot = slot;
        this.descripcion = descripcion;
        this.fuentes = fuentes;
    }

    public String id() { return id; }
    public String nombre() { return nombre; }
    public String icono() { return icono; }
    public String color() { return color; }
    public int slot() { return slot; }
    public List<String> descripcion() { return descripcion; }
    public List<String> fuentes() { return fuentes; }

    // El item model del papel del menú: minecraft:trabajo_<id> (la textura la pone el resource pack)
    public String modelo() { return "trabajo_" + id; }

    // El encantamiento que se gana con el trabajo (libro I en el nivel 10, II en el 40 y III en el 70)
    public Encantamientos.QuasoEnchant encantamiento() {
        return switch (this) {
            case GUERRERO -> Encantamientos.QuasoEnchant.VIGOR_DE_BATALLA;
            case MINERIA -> Encantamientos.QuasoEnchant.FERVOR_MINERO;
            case LENADOR -> Encantamientos.QuasoEnchant.TALADOR;
            case CONSTRUCTOR -> Encantamientos.QuasoEnchant.ALCANCE;
            case GRANJERO -> Encantamientos.QuasoEnchant.COSECHA;
            case PESCADOR -> Encantamientos.QuasoEnchant.ANZUELO_VELOZ;
        };
    }

    public static Trabajo porId(String id) {
        if (id == null) return null;
        String lower = id.toLowerCase(Locale.ROOT);
        for (Trabajo trabajo : values()) {
            if (trabajo.id.equals(lower) || trabajo.nombre.toLowerCase(Locale.ROOT).equals(lower)) return trabajo;
        }
        return null;
    }

    public static Trabajo porSlot(int slot) {
        for (Trabajo trabajo : values()) {
            if (trabajo.slot == slot) return trabajo;
        }
        return null;
    }
}
