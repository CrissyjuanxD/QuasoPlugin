package Hologramas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Convierte lo que escribe el admin en el texto del holograma. Acepta a la vez:
//  - códigos & de siempre (&a, &l, &o, &n, &m, &k, &r) y hex con &#RRGGBB
//  - etiquetas de MiniMessage (<gradient:#ff0000:#0000ff>, <rainbow>, <bold>, <#ff00aa>...)
//  - saltos de línea con Enter, \n o <br>, y unicodes pegados o escritos como ❤
public final class TextoHolograma {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern UNICODE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
    private static final Pattern CODIGO = Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");
    private static final Map<Character, String> CODIGOS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));

    private TextoHolograma() {}

    public static Component convertir(String texto) {
        return MINI.deserialize(aMiniMessage(texto == null ? "" : texto));
    }

    // Pasa los códigos & a etiquetas de MiniMessage. Un color de los de & borra el formato anterior, igual que en
    // el chat, así que delante de cada color va un <reset>
    static String aMiniMessage(String texto) {
        String resultado = texto.replace("\r\n", "\n").replace("\\n", "\n");
        Matcher unicode = UNICODE.matcher(resultado);
        StringBuilder sb = new StringBuilder();
        while (unicode.find()) {
            unicode.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(unicode.group(1), 16))));
        }
        unicode.appendTail(sb);
        resultado = HEX.matcher(sb.toString()).replaceAll("<reset><#$1>");

        Matcher codigo = CODIGO.matcher(resultado);
        sb = new StringBuilder();
        while (codigo.find()) {
            char c = Character.toLowerCase(codigo.group(1).charAt(0));
            String etiqueta = CODIGOS.get(c);
            boolean color = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            codigo.appendReplacement(sb, Matcher.quoteReplacement(color ? "<reset><" + etiqueta + ">" : "<" + etiqueta + ">"));
        }
        codigo.appendTail(sb);
        return sb.toString();
    }
}
