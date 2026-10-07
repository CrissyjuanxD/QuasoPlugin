package Dificultades.CustomMobs;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.concurrent.ThreadLocalRandom;

// Las partículas de los mobs florales (Zombie Floral y Spider Floral): polvo de colores pastel como el de la Abeja
// Floral y pétalos de cerezo
public final class ParticulasFlorales {

    private static final Color[] COLORES = {
            Color.fromRGB(255, 180, 220), // rosa pastel
            Color.fromRGB(255, 90, 180),  // rosa encendido
            Color.fromRGB(255, 245, 160), // amarillo pastel
            Color.fromRGB(160, 240, 170), // verde pastel
            Color.fromRGB(205, 170, 255)  // morado pastel
    };

    private ParticulasFlorales() {}

    // Unas pocas alrededor del cuerpo, para que se note que es floral mientras camina
    public static void aura(Location loc, double alto) {
        World world = loc.getWorld();
        if (world == null) return;
        Location centro = loc.clone().add(0, alto / 2, 0);
        for (int i = 0; i < 3; i++) {
            world.spawnParticle(Particle.DUST, centro, 1, 0.35, alto / 3, 0.35, 0, polvo(1.0f));
        }
        world.spawnParticle(Particle.CHERRY_LEAVES, centro, 1, 0.3, 0.3, 0.3, 0);
    }

    // La estela del proyectil del Zombie Floral
    public static void estela(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.DUST, loc, 4, 0.12, 0.12, 0.12, 0, polvo(1.2f));
        world.spawnParticle(Particle.SPORE_BLOSSOM_AIR, loc, 2, 0.15, 0.15, 0.15, 0);
    }

    // Al morir o al dejar la telaraña: una explosión de pétalos
    public static void estallido(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 0; i < 12; i++) {
            world.spawnParticle(Particle.DUST, loc, 1, 0.5, 0.5, 0.5, 0, polvo(1.4f));
        }
        world.spawnParticle(Particle.CHERRY_LEAVES, loc, 10, 0.5, 0.5, 0.5, 0);
    }

    private static Particle.DustOptions polvo(float tamano) {
        return new Particle.DustOptions(COLORES[ThreadLocalRandom.current().nextInt(COLORES.length)], tamano);
    }
}
