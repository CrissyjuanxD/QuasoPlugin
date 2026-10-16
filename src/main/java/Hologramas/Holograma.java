package Hologramas;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Locale;

// Un holograma (Text Display) con todo lo que se le puede cambiar desde el diálogo. Se guarda en hologramas.yml
public final class Holograma {

    public enum Fondo { DEFECTO, NINGUNO, COLOR }

    final String id;
    String mundo;
    double x, y, z;
    float yaw, pitch;
    String texto = "&fNuevo holograma";
    // Mientras no está publicado solo lo ven los admins de hologramas
    boolean publicado;
    Fondo fondo = Fondo.DEFECTO;
    String colorFondo = "#000000";
    int opacidadFondo = 25;      // % (0-100)
    boolean sombra;
    boolean verATraves;
    int opacidadTexto = 100;     // % (10-100)
    Display.Billboard billboard = Display.Billboard.CENTER;
    TextDisplay.TextAlignment alineacion = TextDisplay.TextAlignment.CENTER;
    float escala = 1f;
    int anchoLinea = 200;
    int brillo = -1;             // -1 = la luz del lugar, 0-15 = brillo fijo
    int distancia = 64;          // bloques desde los que se ve

    Holograma(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean publicado() {
        return publicado;
    }

    Location ubicacion() {
        World world = Bukkit.getWorld(mundo);
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    void mover(Location location) {
        mundo = location.getWorld().getName();
        x = location.getX();
        y = location.getY();
        z = location.getZ();
    }

    Holograma copia(String nuevoId) {
        Holograma copia = new Holograma(nuevoId);
        ConfigurationSection datos = new org.bukkit.configuration.MemoryConfiguration();
        guardar(datos);
        copia.cargar(datos);
        copia.publicado = false;
        return copia;
    }

    // Le pone al Text Display todos los atributos
    void aplicar(TextDisplay display) {
        display.text(TextoHolograma.convertir(texto));
        display.setRotation(yaw, pitch);
        display.setBillboard(billboard);
        display.setAlignment(alineacion);
        display.setLineWidth(anchoLinea);
        display.setShadowed(sombra);
        display.setSeeThrough(verATraves);
        display.setTextOpacity(opacidadTextoByte());
        display.setDefaultBackground(fondo == Fondo.DEFECTO);
        if (fondo == Fondo.NINGUNO) display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        else if (fondo == Fondo.COLOR) display.setBackgroundColor(colorFondo());
        display.setBrightness(brillo < 0 ? null : new Display.Brightness(brillo, brillo));
        display.setViewRange(distancia / 64f);
        display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                new Vector3f(escala, escala, escala), new AxisAngle4f()));
    }

    // Por debajo de ~10% Minecraft lo dibuja opaco, por eso el mínimo es 10%
    byte opacidadTextoByte() {
        int valor = Math.round(Math.max(10, Math.min(100, opacidadTexto)) * 2.55f);
        return (byte) Math.max(26, valor);
    }

    Color colorFondo() {
        int rgb = colorHex(colorFondo);
        int alfa = Math.round(Math.max(0, Math.min(100, opacidadFondo)) * 2.55f);
        return Color.fromARGB(alfa, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    static int colorHex(String hex) {
        String limpio = hex == null ? "" : hex.trim().replace("#", "").replace("&", "");
        if (!limpio.matches("[0-9a-fA-F]{6}")) return 0;
        return Integer.parseInt(limpio, 16);
    }

    static boolean hexValido(String hex) {
        return hex != null && hex.trim().replace("#", "").matches("[0-9a-fA-F]{6}");
    }

    void guardar(ConfigurationSection s) {
        s.set("mundo", mundo);
        s.set("x", x);
        s.set("y", y);
        s.set("z", z);
        s.set("yaw", (double) yaw);
        s.set("pitch", (double) pitch);
        s.set("texto", texto);
        s.set("publicado", publicado);
        s.set("fondo", fondo.name().toLowerCase(Locale.ROOT));
        s.set("color_fondo", colorFondo);
        s.set("opacidad_fondo", opacidadFondo);
        s.set("sombra", sombra);
        s.set("ver_a_traves", verATraves);
        s.set("opacidad_texto", opacidadTexto);
        s.set("billboard", billboard.name().toLowerCase(Locale.ROOT));
        s.set("alineacion", alineacion.name().toLowerCase(Locale.ROOT));
        s.set("escala", (double) escala);
        s.set("ancho_linea", anchoLinea);
        s.set("brillo", brillo);
        s.set("distancia", distancia);
    }

    void cargar(ConfigurationSection s) {
        mundo = s.getString("mundo", mundo);
        x = s.getDouble("x");
        y = s.getDouble("y");
        z = s.getDouble("z");
        yaw = (float) s.getDouble("yaw");
        pitch = (float) s.getDouble("pitch");
        texto = s.getString("texto", texto);
        publicado = s.getBoolean("publicado");
        fondo = valor(Fondo.class, s.getString("fondo"), Fondo.DEFECTO);
        colorFondo = s.getString("color_fondo", colorFondo);
        opacidadFondo = s.getInt("opacidad_fondo", opacidadFondo);
        sombra = s.getBoolean("sombra");
        verATraves = s.getBoolean("ver_a_traves");
        opacidadTexto = s.getInt("opacidad_texto", opacidadTexto);
        billboard = valor(Display.Billboard.class, s.getString("billboard"), Display.Billboard.CENTER);
        alineacion = valor(TextDisplay.TextAlignment.class, s.getString("alineacion"), TextDisplay.TextAlignment.CENTER);
        escala = (float) s.getDouble("escala", escala);
        anchoLinea = s.getInt("ancho_linea", anchoLinea);
        brillo = s.getInt("brillo", brillo);
        distancia = s.getInt("distancia", distancia);
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String nombre, E porDefecto) {
        if (nombre == null) return porDefecto;
        try {
            return Enum.valueOf(tipo, nombre.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }
}
