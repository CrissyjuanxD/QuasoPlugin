package items;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// El lore del equipo que se mejora en la herrería (Floral, Warden y Celestita), igual en todos: la historia de la pieza,
// sus atributos con íconos (el tooltip de atributos del juego se oculta) y la línea de progresión con la pieza actual
// resaltada, así se ve qué viene después
public final class EquipoLore {

    // Los escalones del equipo: Netherite » Floral » Warden (armadura) o Celestita (herramientas)
    public enum Rango {
        NETHERITE("Netherite", "#a59ba5"),
        FLORAL("Floral", "#FFB3D9"),
        WARDEN("Warden", "#3ad4d4"),
        CELESTITA("Celestita", "#8fd3ff");

        public final String nombre;
        public final String color;

        Rango(String nombre, String color) {
            this.nombre = nombre;
            this.color = color;
        }
    }

    // Un atributo del lore: su ícono y el texto (por ejemplo "⚔", "9 de daño")
    public record Atributo(String icono, String texto) {}

    public static final String DANO = "⚔";
    public static final String VELOCIDAD = "⚡";
    public static final String MINADO = "⛏";
    public static final String ARMADURA = "◆";
    public static final String VIDA = "❤";
    public static final String DESGASTE = "✚";
    public static final String EMPUJE = "⚓";
    // Contra un enemigo en especial (los mobs del End, el Rey Ender)
    public static final String CONTRA = "☠";
    public static final String CRISTAL = "◇";
    public static final String BLOQUEO = "⛨";
    public static final String IRROMPIBLE = "∞";
    public static final String PLANEO = "✈";
    public static final String ESCALAR = "⇈";

    private EquipoLore() {}

    // color: el del rango o el del bioma (armadura de Warden). armadura = true dice "Al equiparla" y la progresión
    // termina en Warden; si no, "En la mano" y termina en Celestita
    public static List<String> lore(String color, List<String> historia, boolean armadura, List<Atributo> atributos, Rango rango) {
        ChatColor oscuro = ChatColor.of(tono(color, 0.75));
        ChatColor fuerte = ChatColor.of(color);
        ChatColor claro = ChatColor.of(tono(color, 1.45));
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String linea : historia) lore.add(oscuro + linea);
        lore.add("");
        lore.add(ChatColor.GRAY + (armadura ? "Al equiparla:" : "En la mano:"));
        for (Atributo atributo : atributos) lore.add(" " + fuerte + atributo.icono() + " " + claro + atributo.texto());
        lore.add("");
        lore.add(progresion(armadura, rango));
        return lore;
    }

    // Netherite » Floral » Warden: lo que ya pasó en gris, la pieza actual en su color y en negrita, lo que falta oscuro
    static String progresion(boolean armadura, Rango actual) {
        Rango[] linea = {Rango.NETHERITE, Rango.FLORAL, armadura ? Rango.WARDEN : Rango.CELESTITA};
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < linea.length; i++) {
            if (i > 0) texto.append(ChatColor.DARK_GRAY).append(" » ");
            Rango rango = linea[i];
            if (rango == actual) texto.append(ChatColor.of(rango.color)).append(ChatColor.BOLD).append(rango.nombre);
            else if (rango.ordinal() < actual.ordinal()) texto.append(ChatColor.GRAY).append(rango.nombre);
            else texto.append(ChatColor.DARK_GRAY).append(rango.nombre);
        }
        return texto.toString();
    }

    // 9 → "9", 6.5 → "6,5", 1.25 → "1,3"
    public static String numero(double valor) {
        double redondo = Math.round(valor * 10) / 10.0;
        if (redondo == Math.floor(redondo)) return String.valueOf((long) redondo);
        return String.format(Locale.ROOT, "%.1f", redondo).replace('.', ',');
    }

    // El color más oscuro (factor < 1) o más claro (> 1, hacia el blanco)
    public static String tono(String hex, double factor) {
        java.awt.Color base = java.awt.Color.decode(hex);
        int[] rgb = {base.getRed(), base.getGreen(), base.getBlue()};
        for (int i = 0; i < 3; i++) {
            rgb[i] = factor <= 1 ? (int) Math.round(rgb[i] * factor)
                    : (int) Math.round(rgb[i] + (255 - rgb[i]) * (factor - 1));
            rgb[i] = Math.max(0, Math.min(255, rgb[i]));
        }
        return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
    }
}
