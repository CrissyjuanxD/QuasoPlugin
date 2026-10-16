package Hologramas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HologramasTest {

    private static String plano(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // Recorre el componente y junta los trozos de texto con su estilo
    private static List<Component> trozos(Component component) {
        List<Component> lista = new ArrayList<>();
        juntar(component, Component.empty(), lista);
        return lista;
    }

    private static void juntar(Component component, Component padre, List<Component> lista) {
        Component conEstilo = component.style(padre.style().merge(component.style()));
        if (component instanceof net.kyori.adventure.text.TextComponent texto && !texto.content().isEmpty()) {
            lista.add(Component.text(texto.content(), conEstilo.style()));
        }
        for (Component hijo : component.children()) juntar(hijo, conEstilo, lista);
    }

    private static Component trozo(Component component, String texto) {
        return trozos(component).stream().filter(t -> plano(t).equals(texto)).findFirst().orElseThrow();
    }

    @Test
    void ampersandCodesHexAndBoldWorkLikeInChat() {
        Component texto = TextoHolograma.convertir("&#B79CFF&lTítulo &aVerde");
        assertEquals("Título Verde", plano(texto));
        Component titulo = trozo(texto, "Título ");
        assertEquals(TextColor.fromHexString("#B79CFF"), titulo.color());
        assertEquals(TextDecoration.State.TRUE, titulo.decoration(TextDecoration.BOLD));
        // Un color nuevo quita la negrita, igual que en el chat
        Component verde = trozo(texto, "Verde");
        assertEquals(NamedTextColor.GREEN, verde.color());
        assertNotEquals(TextDecoration.State.TRUE, verde.decoration(TextDecoration.BOLD));
    }

    @Test
    void miniMessageLinesAndUnicodesAreAccepted() {
        assertEquals("Hola\nmundo\nfin ❤", plano(TextoHolograma.convertir("<gradient:#B79CFF:#A8E4FF>Hola</gradient>\nmundo\\nfin \\u2764")));
        assertEquals("a\nb", plano(TextoHolograma.convertir("a<br>b")));
        assertEquals("★ ۞", plano(TextoHolograma.convertir("&e★ &6۞")));
        assertEquals("", plano(TextoHolograma.convertir(null)));
    }

    @Test
    void everyAttributeSurvivesSavingAndLoading() {
        Holograma original = new Holograma("spawn");
        original.mundo = "Crossiants";
        original.x = 10.5;
        original.y = 70.25;
        original.z = -3.75;
        original.yaw = 90;
        original.pitch = -15;
        original.texto = "&#B79CFF&lBienvenidos\n&7a Crossiants";
        original.publicado = true;
        original.fondo = Holograma.Fondo.COLOR;
        original.colorFondo = "#1A1A2E";
        original.opacidadFondo = 60;
        original.sombra = true;
        original.verATraves = true;
        original.opacidadTexto = 80;
        original.billboard = Display.Billboard.FIXED;
        original.alineacion = TextDisplay.TextAlignment.LEFT;
        original.escala = 2.5f;
        original.anchoLinea = 320;
        original.brillo = 15;
        original.distancia = 128;

        YamlConfiguration yml = new YamlConfiguration();
        original.guardar(yml.createSection("h"));
        Holograma leido = new Holograma("spawn");
        leido.cargar(YamlConfiguration.loadConfiguration(new java.io.StringReader(yml.saveToString())).getConfigurationSection("h"));

        YamlConfiguration a = new YamlConfiguration(), b = new YamlConfiguration();
        original.guardar(a);
        leido.guardar(b);
        assertEquals(a.saveToString(), b.saveToString());

        Holograma copia = original.copia("spawn_2");
        assertEquals("spawn_2", copia.id());
        assertFalse(copia.publicado(), "una copia empieza como borrador");
        assertEquals(original.texto, copia.texto);
    }

    @Test
    void opacitiesAndColorsStayInValidRanges() {
        Holograma holograma = new Holograma("h");
        holograma.opacidadTexto = 100;
        assertEquals((byte) 255, holograma.opacidadTextoByte());
        holograma.opacidadTexto = 0;
        assertTrue((holograma.opacidadTextoByte() & 0xFF) >= 26, "por debajo Minecraft lo dibuja opaco");
        holograma.colorFondo = "#FF8000";
        holograma.opacidadFondo = 100;
        assertEquals(255, holograma.colorFondo().getAlpha());
        assertEquals(0xFF8000, holograma.colorFondo().asRGB());
        assertTrue(Holograma.hexValido("1a1a2e"));
        assertFalse(Holograma.hexValido("#12345"));
        assertTrue(HologramasManager.idValido("spawn_2"));
        assertFalse(HologramasManager.idValido("Spawn Principal"));
        assertEquals(-90f, HologramasDialogos.normalizar(270f));
        assertEquals(180f, HologramasDialogos.normalizar(-180f));
    }
}
