package EndBiomes;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.FlowerBed;
import org.bukkit.block.data.type.Snow;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// El suelo y las plantas de los biomas nuevos del End. Lo usa el populator al generar el chunk y también EndIslandFix,
// que repasa los chunks nuevos cuando ya están completos (las islas chicas que pone el chunk de al lado después quedaban
// mitad end stone y mitad bioma)
final class EndSuelo {

    static final int MAX_Y = 254;
    // Hasta qué profundidad baja la tierra; desde la 3ra capa se va mezclando con end stone
    private static final int SOIL_DEPTH = 8;

    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private EndSuelo() {}

    // Acceso a los bloques: el LimitedRegion del populator o el chunk ya cargado
    interface Bloques {
        boolean dentro(int x, int y, int z);

        Material get(int x, int y, int z);

        void set(int x, int y, int z, Material type);

        void set(int x, int y, int z, BlockData data);
    }

    static Bloques de(LimitedRegion region) {
        return new Bloques() {
            public boolean dentro(int x, int y, int z) {
                return region.isInRegion(x, y, z);
            }

            public Material get(int x, int y, int z) {
                return region.getType(x, y, z);
            }

            public void set(int x, int y, int z, Material type) {
                region.setType(x, y, z, type);
            }

            public void set(int x, int y, int z, BlockData data) {
                region.setBlockData(x, y, z, data);
            }
        };
    }

    // Ruidos fijos por seed, así el dibujo del hielo sale igual en el populator y en el repaso
    static final class Ruido {
        private static final Map<Long, Ruido> CACHE = new ConcurrentHashMap<>();
        final SimplexOctaveGenerator lineas;
        final SimplexOctaveGenerator dunas;

        private Ruido(long seed) {
            lineas = new SimplexOctaveGenerator(new Random(seed ^ 0x1CEL), 2);
            lineas.setScale(1.0 / 36);
            dunas = new SimplexOctaveGenerator(new Random(seed ^ 0x5A0L), 2);
            dunas.setScale(1.0 / 22);
        }

        static Ruido of(long seed) {
            return CACHE.computeIfAbsent(seed, Ruido::new);
        }
    }

    // Pasa cada superficie de end stone de la columna al suelo del bioma. Devuelve la superficie más alta (o MIN_VALUE)
    static int suelo(Bloques b, Random r, Ruido ruido, int x, int z, EndBiome biome) {
        int highest = Integer.MIN_VALUE;
        for (int y = MAX_Y; y > 1; y--) {
            if (!b.dentro(x, y, z) || b.get(x, y, z) != Material.END_STONE || !b.get(x, y + 1, z).isAir()) continue;
            if (biome == EndBiome.PICOS_HELADOS) hielo(b, r, ruido, x, y, z);
            else tierra(b, r, x, y, z, biome == EndBiome.PARAMO_MARCHITO);
            if (highest == Integer.MIN_VALUE) highest = y;
        }
        return highest;
    }

    private static void tierra(Bloques b, Random r, int x, int y, int z, boolean paramo) {
        for (int d = 0; d < SOIL_DEPTH && y - d > 0; d++) {
            if (b.get(x, y - d, z) != Material.END_STONE) break;
            double keep = d < 3 ? 1 : 1 - (d - 2) / (double) (SOIL_DEPTH - 2);
            if (r.nextDouble() > keep) continue;
            b.set(x, y - d, z, soilBlock(r, d, paramo));
        }
    }

    private static Material soilBlock(Random r, int depth, boolean paramo) {
        if (paramo) {
            if (depth == 0) return r.nextInt(100) < 80 ? Material.SOUL_SOIL : Material.MUD;
            return r.nextInt(100) < 55 ? Material.MUD : Material.SOUL_SOIL;
        }
        if (depth == 0) return Material.GRASS_BLOCK;
        return r.nextInt(100) < 15 ? Material.COARSE_DIRT : Material.DIRT;
    }

    // Picos Helados: nieve de 3 a 5 capas con líneas de hielo que dibujan curvas de nivel, manchas peladas de end stone
    // y debajo hielo compacto que se mezcla con la piedra
    private static void hielo(Bloques b, Random r, Ruido ruido, int x, int y, int z) {
        double n = ruido.lineas.noise(x, z, 0.5, 0.5, true);
        if (n > 0.62) return;
        int snow = 3 + r.nextInt(3);
        for (int d = 0; d < snow + 3 && y - d > 0; d++) {
            if (b.get(x, y - d, z) != Material.END_STONE) break;
            Material type;
            if (d < snow) type = d < 2 && n > -0.4 && n < -0.31 ? Material.ICE : Material.SNOW_BLOCK;
            else if (r.nextInt(3) == 0) continue;
            else type = Material.PACKED_ICE;
            b.set(x, y - d, z, type);
        }
    }

    // Dunas: en los Picos Helados el suelo sube 1 o 2 bloques de nieve donde el ruido es alto
    static int dunas(Bloques b, Ruido ruido, int x, int top, int z) {
        if (b.get(x, top, z) != Material.SNOW_BLOCK) return top;
        double n = ruido.dunas.noise(x, z, 0.5, 0.5, true);
        int extra = n > 0.55 ? 2 : n > 0.25 ? 1 : 0;
        for (int i = 1; i <= extra; i++) {
            if (!b.dentro(x, top + i, z) || !b.get(x, top + i, z).isAir()) return top + i - 1;
            b.set(x, top + i, z, Material.SNOW_BLOCK);
        }
        return top + extra;
    }

    static void planta(Bloques b, Random r, int x, int y, int z, EndBiome biome) {
        if (!libre(b, x, y, z)) return;
        switch (biome) {
            case PARAMO_MARCHITO -> marchita(b, r, x, y, z);
            case PICOS_HELADOS -> helada(b, r, x, y, z);
            default -> prismatica(b, r, x, y, z);
        }
    }

    // Bosque Prismático: pasto, flores de muchos colores, pétalos, raíces y brotes de amatista (los brotes no dan
    // Celestita, solo los racimos). Pocos arbustos de luciérnagas: muchas partículas bajaban los FPS
    private static void prismatica(Bloques b, Random r, int x, int y, int z) {
        if (b.get(x, y - 1, z) != Material.GRASS_BLOCK) return;
        int roll = r.nextInt(1000);
        if (roll < 200) b.set(x, y, z, Material.SHORT_GRASS);
        else if (roll < 240) tall(b, x, y, z, Material.TALL_GRASS);
        else if (roll < 255) b.set(x, y, z, Material.BUSH);
        else if (roll < 275) b.set(x, y, z, Material.WARPED_ROOTS);
        else if (roll < 290) b.set(x, y, z, Material.NETHER_SPROUTS);
        else if (roll < 330) petals(b, r, x, y, z, r.nextBoolean() ? Material.PINK_PETALS : Material.WILDFLOWERS);
        else if (roll < 375) b.set(x, y, z, FLORES[r.nextInt(FLORES.length)]);
        else if (roll < 383) tall(b, x, y, z, r.nextBoolean() ? Material.LILAC : Material.ROSE_BUSH);
        else if (roll < 389) bud(b, r, x, y, z);
        else if (roll < 391) b.set(x, y, z, Material.FIREFLY_BUSH);
    }

    private static final Material[] FLORES = {
            Material.POPPY, Material.OXEYE_DAISY, Material.AZURE_BLUET, Material.ALLIUM, Material.DANDELION,
            Material.CORNFLOWER, Material.BLUE_ORCHID, Material.PINK_TULIP, Material.LILY_OF_THE_VALLEY};

    // Páramo Marchito: rosas del Wither, arbustos secos, pasto seco y hojarasca
    private static void marchita(Bloques b, Random r, int x, int y, int z) {
        Material ground = b.get(x, y - 1, z);
        if (ground != Material.SOUL_SOIL && ground != Material.MUD) return;
        int roll = r.nextInt(1000);
        if (roll < 28) b.set(x, y, z, Material.WITHER_ROSE);
        else if (roll < 60) b.set(x, y, z, Material.DEAD_BUSH);
        else if (roll < 130) b.set(x, y, z, Material.SHORT_DRY_GRASS);
        else if (roll < 150) b.set(x, y, z, Material.TALL_DRY_GRASS);
        else if (roll < 175) b.set(x, y, z, Material.LEAF_LITTER);
    }

    // Picos Helados: capas de nieve sueltas que suavizan las dunas
    private static void helada(Bloques b, Random r, int x, int y, int z) {
        if (b.get(x, y - 1, z) != Material.SNOW_BLOCK || r.nextInt(100) >= 22) return;
        Snow snow = (Snow) Material.SNOW.createBlockData();
        snow.setLayers(1 + r.nextInt(2));
        b.set(x, y, z, snow);
    }

    private static void petals(Bloques b, Random r, int x, int y, int z, Material type) {
        FlowerBed petals = (FlowerBed) type.createBlockData();
        petals.setFlowerAmount(1 + r.nextInt(4));
        petals.setFacing(SIDES[r.nextInt(SIDES.length)]);
        b.set(x, y, z, petals);
    }

    private static void bud(Bloques b, Random r, int x, int y, int z) {
        AmethystCluster bud = (AmethystCluster) (r.nextBoolean() ? Material.SMALL_AMETHYST_BUD : Material.MEDIUM_AMETHYST_BUD).createBlockData();
        bud.setFacing(BlockFace.UP);
        b.set(x, y, z, bud);
    }

    private static void tall(Bloques b, int x, int y, int z, Material type) {
        if (!libre(b, x, y + 1, z)) return;
        Bisected lower = (Bisected) type.createBlockData();
        lower.setHalf(Bisected.Half.BOTTOM);
        Bisected upper = (Bisected) type.createBlockData();
        upper.setHalf(Bisected.Half.TOP);
        b.set(x, y, z, lower);
        b.set(x, y + 1, z, upper);
    }

    private static boolean libre(Bloques b, int x, int y, int z) {
        return y < MAX_Y && b.dentro(x, y, z) && b.get(x, y, z).isAir();
    }
}
