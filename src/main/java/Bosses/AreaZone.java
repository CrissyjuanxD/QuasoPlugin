package Bosses;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AreaZone {

    public enum Shape { CIRCULAR, SQUARE }

    private final Location center;
    private final int radius;
    private final int heightUp;
    private final int heightDown;
    private final Shape shape;

    public AreaZone(Location center, int radius, int heightUp, int heightDown, Shape shape) {
        this.center = center.clone();
        this.radius = radius;
        this.heightUp = heightUp;
        this.heightDown = heightDown;
        this.shape = shape;
    }

    public boolean isInside(Location loc) {
        if (!loc.getWorld().equals(center.getWorld())) return false;

        double y = loc.getY();
        double centerY = center.getY();

        if (y > centerY + heightUp || y < centerY - heightDown) {
            return false;
        }

        if (shape == Shape.CIRCULAR) {
            return Math.pow(loc.getX() - center.getX(), 2) + Math.pow(loc.getZ() - center.getZ(), 2) <= radius * radius;
        }

        // SQUARE
        return Math.abs(loc.getX() - center.getX()) <= radius &&
                Math.abs(loc.getZ() - center.getZ()) <= radius;
    }

    public void debug(Player p) {
        World w = center.getWorld();
        if (w == null) return;

        double minY = center.getY() - heightDown;
        double maxY = center.getY() + heightUp;

        // Densidad de escaneo (0.8 = un poco más denso que 1 bloque)
        double density = 0.8;

        // 1. DIBUJAR PAREDES (Vertical Walls)
        // Recorremos desde Y min hasta Y max y dibujamos el perímetro en cada altura
        for (double y = minY; y <= maxY; y += density) {
            Location layerCenter = center.clone();
            layerCenter.setY(y);

            // Color de las paredes: Blanco
            drawPerimeter(w, layerCenter, radius, Color.WHITE);
        }

        // 2. DIBUJAR SUELO (Grid Rojo)
        drawCap(w, minY, Color.RED);

        // 3. DIBUJAR TECHO (Grid Aqua)
        drawCap(w, maxY, Color.AQUA);

        // 4. Marca central
        w.spawnParticle(Particle.END_ROD, center, 1, 0, 0, 0, 0);
    }

    // Dibuja el borde exterior (Perímetro) en una altura Y específica
    private void drawPerimeter(World w, Location loc, double r, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);

        if (shape == Shape.CIRCULAR) {
            // Círculo: Dibujamos puntos alrededor
            double step = 5; // Grados
            for (double angle = 0; angle < 360; angle += step) {
                double rad = Math.toRadians(angle);
                double x = loc.getX() + Math.cos(rad) * r;
                double z = loc.getZ() + Math.sin(rad) * r;
                w.spawnParticle(Particle.DUST, new Location(w, x, loc.getY(), z), 1, dust);
            }
        } else {
            // Cuadrado: Dibujamos las 4 líneas externas
            double minX = loc.getX() - r;
            double maxX = loc.getX() + r;
            double minZ = loc.getZ() - r;
            double maxZ = loc.getZ() + r;
            double y = loc.getY();

            // Dibujar líneas densas
            drawLine(w, new Location(w, minX, y, minZ), new Location(w, maxX, y, minZ), dust); // Norte
            drawLine(w, new Location(w, maxX, y, minZ), new Location(w, maxX, y, maxZ), dust); // Este
            drawLine(w, new Location(w, maxX, y, maxZ), new Location(w, minX, y, maxZ), dust); // Sur
            drawLine(w, new Location(w, minX, y, maxZ), new Location(w, minX, y, minZ), dust); // Oeste
        }
    }

    // Dibuja la tapa (relleno o rejilla) en una altura Y específica
    private void drawCap(World w, double y, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
        Location loc = center.clone();
        loc.setY(y);

        if (shape == Shape.CIRCULAR) {
            // Para círculo: Dibujamos anillos concéntricos hacia adentro
            // Esto crea un efecto de "diana" o tapa sólida
            for (double r = 1; r <= radius; r += 1.0) {
                drawPerimeter(w, loc, r, color);
            }
        } else {
            // Para cuadrado: Dibujamos una cuadrícula (Grid)
            double minX = loc.getX() - radius;
            double maxX = loc.getX() + radius;
            double minZ = loc.getZ() - radius;
            double maxZ = loc.getZ() + radius;

            // Líneas a lo largo del eje X (recorriendo Z)
            for (double z = minZ; z <= maxZ; z += 1.0) {
                drawLine(w, new Location(w, minX, y, z), new Location(w, maxX, y, z), dust);
            }
            // Líneas a lo largo del eje Z (recorriendo X)
            for (double x = minX; x <= maxX; x += 1.0) {
                drawLine(w, new Location(w, x, y, minZ), new Location(w, x, y, maxZ), dust);
            }
        }
    }

    // Utilidad para dibujar línea continua entre A y B
    private void drawLine(World w, Location p1, Location p2, Particle.DustOptions dust) {
        double distance = p1.distance(p2);
        double space = 0.8; // Espacio entre partículas en la línea
        Vector v = p2.toVector().subtract(p1.toVector()).normalize().multiply(space);

        Location current = p1.clone();
        for (double d = 0; d < distance; d += space) {
            w.spawnParticle(Particle.DUST, current, 1, dust);
            current.add(v);
        }
    }

    public int getHeightUp() { return heightUp; }
    public int getHeightDown() { return heightDown; }
    public Location getCenter() { return center.clone(); }
}