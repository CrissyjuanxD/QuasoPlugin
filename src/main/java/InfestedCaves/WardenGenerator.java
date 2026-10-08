package InfestedCaves;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.AmethystCluster;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.SculkShrieker;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WardenGenerator extends ChunkGenerator {

    static final int MIN_Y = -60;
    // No hay techo: la cueva se abre al cielo y esta es la altura máxima de los picos
    static final int TOP_Y = 230;
    static final int SPAWN_RADIUS = 70;
    static final int SPAWN_TRANSITION = 40;
    // Piso del cráter del centro, donde va la build con el portal de salida
    static final int SPAWN_Y = -40;

    private static final int WATER_LEVEL = -38;
    private static final int LAVA_LEVEL = -48;
    // 1 veta cada tantos bloques de roca, por bioma (Sculk, Pantano, Abismo, Ruinas): las islas del Abismo tienen
    // poca roca, así que ahí sale el doble
    private static final int[] ORE_CHANCE = {900, 900, 450, 650};
    private static final double FLUID_CORE = 0.9;
    private static final double FLUID_RIM = 0.2;
    private static final int FLUID_CITY_MARGIN = 12;
    // Arriba de esta altura el Abismo ya no tiene islas
    private static final int ISLAND_TOP = 135;

    private static final int HEIGHT = TOP_Y - MIN_Y + 1;
    // Arriba de esta altura la roca solo sigue si tiene roca abajo: los picos terminan en punta y nada queda colgando
    private static final int PEAK_RULE_Y = 110;
    private static final int GRID_Y0 = MIN_Y - 8;
    private static final int GRID_NY = (TOP_Y + 16 - GRID_Y0) / 8 + 1;
    // Cuánto se aplasta el ruido de las cuevas a lo alto en cada bioma (Sculk, Pantano, Abismo, Ruinas)
    private static final double[] CAVE_STRETCH = {0.8, 1.5, 2.0, 0.9};

    private static final int SCULK = WardenBiome.CAVERNA_SCULK.ordinal();
    private static final int SWAMP = WardenBiome.PANTANO_PROFUNDO.ordinal();
    private static final int ABYSS = WardenBiome.ABISMO_FLOTANTE.ordinal();
    private static final int ASH = WardenBiome.RUINAS_DE_CENIZA.ordinal();

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private static final Set<Material> BODY = EnumSet.of(
            Material.SCULK, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.BLACKSTONE,
            Material.SMOOTH_BASALT, Material.BASALT, Material.MUD);

    private static final Set<Material> ORE_HOST = EnumSet.of(
            Material.SCULK, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.BLACKSTONE,
            Material.SMOOTH_BASALT, Material.BASALT, Material.MUD, Material.MOSS_BLOCK, Material.CLAY, Material.CALCITE,
            Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK);

    private final JavaPlugin plugin;
    private static volatile Noises noises;
    private volatile BiomeProvider biomeProvider;

    public WardenGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        if (biomeProvider == null) biomeProvider = new WardenBiomeProvider();
        return biomeProvider;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return List.of(new WardenPopulator(this));
    }

    // Las Ancient City son las vanilla: solo pueden empezar en los chunks que calcula AncientCityLocator,
    // que son los que este generador deja vaciados
    @Override
    public boolean shouldGenerateStructures(WorldInfo info, Random random, int chunkX, int chunkZ) {
        return AncientCityLocator.isStartChunk(info.getSeed(), chunkX, chunkZ);
    }

    // Las piezas de las estructuras se ponen en el paso de decoración; los biomas del datapack no tienen
    // features, así que esto solo coloca las ciudades
    @Override
    public boolean shouldGenerateDecorations(WorldInfo info, Random random, int chunkX, int chunkZ) {
        return true;
    }

    // Genera todo el chunk: primero la roca (cavernas grandes que se abren al cielo, paredes que terminan en picos,
    // mezcladas en los bordes de los biomas), después borra lo que quedó flotando, pone los bloques, el agua y
    // la lava, las superficies con su decoración y al final las vetas de mineral
    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunk) {
        long seed = info.getSeed();
        Noises n = noises(seed);
        WardenBiomeMap map = WardenBiomeMap.forSeed(seed);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(seed, baseX + 8, baseZ + 8, 12);

        Column[] columns = new Column[256];
        boolean[] present = new boolean[4];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Column c = column(n, map, city, baseX + x, baseZ + z);
                columns[x << 4 | z] = c;
                for (int b = 0; b < 4; b++) if (c.w[b] > 0.001) present[b] = true;
            }
        }

        // Ruido 3D de cada bioma en una grilla de 4x8x4 bloques; cada bloque lo interpola (como hace el juego)
        double[][] grids = new double[4][];
        for (int b = 0; b < 4; b++) {
            if (present[b]) grids[b] = grid(n.cave(b), CAVE_STRETCH[b], baseX, baseZ);
        }

        boolean[] solid = new boolean[256 * HEIGHT];
        boolean[] anchor = new boolean[256 * HEIGHT];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                Column c = columns[col];
                boolean edge = x == 0 || x == 15 || z == 0 || z == 15;
                for (int y = MIN_Y; y <= TOP_Y; y++) {
                    int i = col * HEIGHT + y - MIN_Y;
                    boolean rock;
                    if (y == MIN_Y) rock = !c.voidFloor;
                    else if (y >= TOP_Y - 1) rock = false;
                    else {
                        double base = solidity(c, grids, city, x, y, z);
                        double ridge = ridgeTerm(c, y);
                        boolean supported = solid[i - 1];
                        // Arriba de Y 60 las crestas solo agregan roca encima de roca (nunca un techo colgando)
                        if (y <= 60 || ridge < 0) rock = base + ridge > 0;
                        else rock = base > 0 || (supported && base + ridge > 0);
                        rock = rock && (y <= PEAK_RULE_Y || c.floats || supported);
                    }
                    solid[i] = rock;
                    anchor[i] = rock && (edge || y <= MIN_Y + 3 || c.floats);
                }
            }
        }
        removeFloating(solid, anchor);

        int[] columnTop = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                Column c = columns[col];
                int wx = baseX + x;
                int wz = baseZ + z;
                for (int y = MIN_Y; y < TOP_Y; y++) {
                    if (!solid[col * HEIGHT + y - MIN_Y]) continue;
                    Material type;
                    if (y == MIN_Y) type = Material.BEDROCK;
                    else if (c.pillar[ASH] > 0 && c.biome == WardenBiome.RUINAS_DE_CENIZA && y < c.pillarTop[ASH]) type = Material.BASALT;
                    else type = bodyMaterial(c.biome, n, wx, y, wz);
                    chunk.setBlock(x, y, z, type);
                }

                if (c.spawnInfluence <= 0 && c.cityInfluence <= 0) {
                    boolean nearCity = city != null && Math.hypot(wx - city.centerX, wz - city.centerZ)
                            <= AncientCityLocator.CLEAR_RADIUS + AncientCityLocator.CLEAR_TRANSITION + FLUID_CITY_MARGIN;
                    Material rim = bodyMaterial(c.biome, n, wx, WATER_LEVEL, wz);
                    fluid(chunk, x, z, c.w[SWAMP], WATER_LEVEL, Material.WATER, !nearCity, rim);
                    fluid(chunk, x, z, c.w[ASH], LAVA_LEVEL, Material.LAVA, !nearCity, rim);
                }

                int surface = TOP_Y - 1;
                while (surface > MIN_Y && chunk.getType(x, surface, z) == Material.AIR) surface--;
                columnTop[col] = surface;
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                Column c = columns[col];
                surfaces(chunk, n, random, c, city, x, z, baseX + x, baseZ + z, columnTop[col]);
                if ((c.biome == WardenBiome.CAVERNA_SCULK || c.cityInfluence > 0.3) && c.spawnInfluence < 0.5) {
                    sculkWalls(chunk, random, c, city, x, z, columnTop[col]);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                Column c = columns[col];
                if (c.spawnInfluence >= 0.1) continue;
                Material ore = c.biome.ore();
                int chance = ORE_CHANCE[c.biome.ordinal()];
                for (int y = MIN_Y + 3; y <= columnTop[col] - 3; y++) {
                    if (inCityCavern(c, city, y)) continue;
                    if (ORE_HOST.contains(chunk.getType(x, y, z)) && random.nextInt(chance) == 0) {
                        vein(chunk, random, x, y, z, ore);
                    }
                }
            }
        }
    }

    // Lo que se calcula una vez por columna: los pesos de los biomas, el piso, las paredes, los picos y los pilares
    private static final class Column {
        final double[] w;
        WardenBiome biome;
        double spawnInfluence;
        double cityInfluence;
        double cityRoof;
        double cityTop;
        double cityHills;
        boolean voidFloor;
        boolean floats;
        final double[] floor = new double[4];
        final double[] peak = new double[4];
        final double[] pillar = new double[4];
        final double[] pillarTop = new double[4];
        double skylight;
        double crest;

        Column(double[] w) {
            this.w = w;
        }
    }

    private Column column(Noises n, WardenBiomeMap map, AncientCityLocator.CityInfo city, int x, int z) {
        Column c = new Column(map.weights(x, z));
        c.biome = map.biomeAt(x, z);
        double dist = Math.sqrt((double) x * x + (double) z * z);
        c.spawnInfluence = influence(dist, SPAWN_RADIUS, SPAWN_TRANSITION);
        c.cityInfluence = city != null ? AncientCityLocator.computeInfluence(city, x, z) : 0.0;
        if (c.cityInfluence > 0) {
            c.cityRoof = cityRoof(n, city, x, z);
            c.cityTop = c.cityRoof + 13 + n.crustVariation.noise(x * 1.7, z * 1.7, 0.5, 0.5, true) * 3;
            c.cityHills = c.cityTop + cityHills(n, x, z);
        }
        c.voidFloor = c.w[ABYSS] > 0.6 && c.spawnInfluence <= 0 && c.cityInfluence <= 0;
        // Las islas del Abismo flotan a propósito, no se borran
        c.floats = c.w[ABYSS] > 0.25 || c.spawnInfluence > 0;

        double floorNoise = n.floor.noise(x, z, 0.5, 0.5, true);
        c.skylight = smooth(0.25, 0.55, n.skylights.noise(x, z, 0.5, 0.5, true));
        double ridge = ridged(n, x, z);
        double needles = Math.max(0, n.spires.noise(x, z, 0.5, 0.5, true) - 0.8) / 0.2 * 34;

        c.floor[SCULK] = -48 + floorNoise * 5;
        c.floor[SWAMP] = -42 + floorNoise * 7;
        c.floor[ASH] = -50 + floorNoise * 4;

        // Altura de la cumbre en esta columna: lejos de las crestas queda abajo (no suma roca) y sube hasta el filo
        double crest = (ridge - 0.52) / 0.48;
        c.crest = crest;
        c.peak[SCULK] = 70 + crest * 160 + needles;
        c.peak[SWAMP] = 55 + crest * 70;
        c.peak[ASH] = 65 + crest * 140 + needles * 0.6;

        // Agujas de basalto de las Ruinas: finas y altas, salen de la lava
        double ashPillar = n.pillars.noise(x, z, 0.5, 0.5, true);
        c.pillar[ASH] = smooth(0.74, 0.8, ashPillar);
        c.pillarTop[ASH] = -15 + Math.max(0, ashPillar - 0.74) / 0.26 * 175;
        return c;
    }

    // Solidez del bloque mezclando los biomas de la columna: mayor que 0 es roca. lx y lz son la posición dentro
    // del chunk (para la grilla del ruido)
    private double solidity(Column c, double[][] grids, AncientCityLocator.CityInfo city, int lx, int y, int lz) {
        double s = 0;
        for (int b = 0; b < 4; b++) {
            if (c.w[b] <= 0.001) continue;
            s += c.w[b] * biomeSolidity(b, c, interpolate(grids[b], lx, y, lz), y);
        }

        // El centro es un cráter abierto con piso firme en SPAWN_Y, donde va la build con el portal de salida
        if (c.spawnInfluence > 0) s += ((SPAWN_Y - y) * 0.25 - s) * c.spawnInfluence;

        if (c.cityInfluence > 0) s = cityCarve(city, c, y, s);
        return s;
    }

    private double biomeSolidity(int b, Column c, double noise, int y) {
        if (b == ABYSS) {
            double s = (noise - 0.32) * 1.6;
            if (y > ISLAND_TOP) s -= (y - ISLAND_TOP) / 12.0;
            if (y < -20) s -= (-20 - y) / 40.0 * 0.6;
            return s;
        }

        double s = noise + openness(b, y);
        double floor = c.floor[b];
        if (y < floor) s += (floor - y) * 0.3;
        // Claraboyas: pozos enormes donde la cueva se abre de arriba abajo y desde el piso se ve el cielo
        else if (c.skylight > 0) s -= c.skylight * 1.2 * Math.min(1, (y - floor) / 6.0);

        if (c.pillar[b] > 0 && y < c.pillarTop[b]) s += c.pillar[b] * 2.4 * Math.min(1, (c.pillarTop[b] - y) / 25.0);
        return s;
    }

    // Las crestas: bajo el filo la roca es maciza desde el piso (los cimientos de la montaña) y arriba cada columna
    // llega hasta su propia altura según qué tan cerca está del filo, así quedan laderas y cumbres afiladas en vez de
    // mesetas. Lejos de las crestas, arriba de Y 60 saca roca para que quede abierto. En el cráter no hay, y sobre
    // una Ancient City siguen igual que en el resto del bioma pero sin meterse en su caverna
    private static double ridgeTerm(Column c, int y) {
        if (c.spawnInfluence > 0.5) return 0;
        double fade = c.cityInfluence > 0 && y <= c.cityTop ? 1 - c.cityInfluence : 1;
        if (fade <= 0) return 0;
        double foundation = smooth(0, 0.35, c.crest);
        double rise = y > 60 ? Math.min(1, (y - 60) / 25.0) : 0;
        double t = 0;
        for (int b = 0; b < 4; b++) {
            if (b == ABYSS || c.w[b] <= 0.001) continue;
            double profile = 1.6 * Math.min(1, (c.peak[b] - y) / 15.0);
            t += c.w[b] * (foundation * profile + (1 - foundation) * rise * profile);
        }
        return t * fade;
    }

    // Abajo la cueva es una esponja de cavernas grandes, mitad roca y mitad aire, hasta Y 50; de ahí para arriba la
    // roca se va haciendo cada vez más rala, así cada masa termina a su propia altura y en punta (no en una meseta)
    // y desde abajo se ve el cielo entre los salientes. El Pantano es más bajo
    private static double openness(int b, int y) {
        boolean swamp = b == SWAMP;
        double low = swamp ? 0.06 : 0.1;
        double mid = swamp ? -0.1 : -0.05;
        double midY = swamp ? 25 : 50;
        double topY = swamp ? 110 : 150;
        if (y <= -30) return low;
        if (y <= midY) return low + (mid - low) * (y + 30) / (midY + 30);
        if (y <= topY) return mid + (-0.8 - mid) * (y - midY) / (topY - midY);
        return -0.8 - (y - topY) * 0.05;
    }

    // Caverna de la Ancient City: piso firme abajo de las piezas, cúpula irregular (alta en el centro y bajando hacia
    // los bordes) y arriba una capa de roca de 10 a 16 bloques pegada a las paredes, así la ciudad queda bajo tierra
    // como en el deep dark y no quedan pedazos de terreno flotando sobre ella. Sobre esa capa van lomas con cuevas
    // (si no, quedaba una meseta plana) y después sigue el bioma con sus crestas
    private double cityCarve(AncientCityLocator.CityInfo city, Column c, int y, double s) {
        double influence = c.cityInfluence;
        if (y <= city.minY()) return s + (1.2 - s) * influence;
        if (y <= c.cityRoof) return s + (-1.2 - s) * influence;
        if (y <= c.cityTop) return s + (1.2 - s) * influence;
        if (y <= c.cityHills) return s + (Math.min(1.2, s + 0.5) - s) * influence;
        return s;
    }

    // Alto de las lomas sobre el techo de la ciudad: de 0 a unos 28 bloques, con valles y cerros redondeados
    private static double cityHills(Noises n, int x, int z) {
        double h = n.cityHills.noise(x, z, 0.5, 0.5, true);
        return 26 * smooth(-0.35, 0.5, h) + 3 * n.crustVariation.noise(x * 2.3, z * 2.3, 0.5, 0.5, true);
    }

    // Si esa posición está dentro de la caverna de una Ancient City (del piso al techo en cúpula). El Infested Warden
    // solo cuenta a los jugadores que están ahí, no a los que están arriba en el terreno
    public static boolean inCityCavern(long seed, double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(seed, bx, bz);
        if (city == null || AncientCityLocator.computeInfluence(city, bx, bz) <= 0.3) return false;
        return y >= city.minY() - 2 && y <= cityRoof(noises(seed), city, bx, bz) + 1;
    }

    private static double cityRoof(Noises n, AncientCityLocator.CityInfo city, int x, int z) {
        double d = Math.hypot(x - city.centerX, z - city.centerZ) / AncientCityLocator.CLEAR_RADIUS;
        return city.maxY() + 16 * (1 - Math.min(1, d * d)) + n.crustVariation.noise(x, z, 0.5, 0.5, true) * 5;
    }

    // Dentro de la caverna de una Ancient City (desde el piso hasta el techo en cúpula)
    private static boolean inCityCavern(Column c, AncientCityLocator.CityInfo city, int y) {
        return city != null && c.cityInfluence > 0.3 && y > city.minY() - 2 && y <= c.cityRoof + 2;
    }

    // Primera altura afuera de la caverna de una Ancient City en esa columna (MIN_Y si no hay ciudad): el populator
    // pone árboles y ruinas de ahí para arriba, igual que en el resto del bioma
    int aboveCityCavern(long seed, int x, int z) {
        AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(seed, x, z);
        if (city == null || AncientCityLocator.computeInfluence(city, x, z) <= 0.3) return MIN_Y;
        return (int) Math.ceil(cityRoof(noises(seed), city, x, z)) + 3;
    }

    // Ruido 3D en la grilla del chunk: 5x5 puntos cada 4 bloques y uno cada 8 de alto
    private static double[] grid(SimplexOctaveGenerator noise, double stretch, int baseX, int baseZ) {
        double[] g = new double[5 * 5 * GRID_NY];
        for (int gx = 0; gx < 5; gx++) {
            for (int gz = 0; gz < 5; gz++) {
                for (int gy = 0; gy < GRID_NY; gy++) {
                    int y = GRID_Y0 + gy * 8;
                    g[(gx * 5 + gz) * GRID_NY + gy] = noise.noise(baseX + gx * 4, y * stretch, baseZ + gz * 4, 0.5, 0.5, true);
                }
            }
        }
        return g;
    }

    private static double interpolate(double[] g, int x, int y, int z) {
        int gx = x >> 2;
        int gz = z >> 2;
        double tx = (x & 3) / 4.0;
        double tz = (z & 3) / 4.0;
        int gy = (y - GRID_Y0) >> 3;
        double ty = ((y - GRID_Y0) & 7) / 8.0;
        double c00 = lerp(g[(gx * 5 + gz) * GRID_NY + gy], g[(gx * 5 + gz) * GRID_NY + gy + 1], ty);
        double c01 = lerp(g[(gx * 5 + gz + 1) * GRID_NY + gy], g[(gx * 5 + gz + 1) * GRID_NY + gy + 1], ty);
        double c10 = lerp(g[((gx + 1) * 5 + gz) * GRID_NY + gy], g[((gx + 1) * 5 + gz) * GRID_NY + gy + 1], ty);
        double c11 = lerp(g[((gx + 1) * 5 + gz + 1) * GRID_NY + gy], g[((gx + 1) * 5 + gz + 1) * GRID_NY + gy + 1], ty);
        return lerp(lerp(c00, c01, tz), lerp(c10, c11, tz), tx);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smooth(double from, double to, double v) {
        double t = Math.max(0, Math.min(1, (v - from) / (to - from)));
        return t * t * (3 - 2 * t);
    }

    // Borra la roca que quedó flotando: lo que no llega al piso, al borde del chunk ni a una isla del Abismo
    private static void removeFloating(boolean[] solid, boolean[] anchor) {
        int[] queue = new int[solid.length];
        boolean[] reached = new boolean[solid.length];
        int head = 0;
        int tail = 0;
        for (int i = 0; i < solid.length; i++) {
            if (anchor[i]) {
                reached[i] = true;
                queue[tail++] = i;
            }
        }
        while (head < tail) {
            int i = queue[head++];
            int col = i / HEIGHT;
            int y = i % HEIGHT;
            int x = col >> 4;
            int z = col & 15;
            if (y > 0) tail = visit(solid, reached, queue, tail, i - 1);
            if (y < HEIGHT - 1) tail = visit(solid, reached, queue, tail, i + 1);
            if (x > 0) tail = visit(solid, reached, queue, tail, i - 16 * HEIGHT);
            if (x < 15) tail = visit(solid, reached, queue, tail, i + 16 * HEIGHT);
            if (z > 0) tail = visit(solid, reached, queue, tail, i - HEIGHT);
            if (z < 15) tail = visit(solid, reached, queue, tail, i + HEIGHT);
        }
        for (int i = 0; i < solid.length; i++) {
            if (solid[i] && !reached[i]) solid[i] = false;
        }
    }

    private static int visit(boolean[] solid, boolean[] reached, int[] queue, int tail, int i) {
        if (!solid[i] || reached[i]) return tail;
        reached[i] = true;
        queue[tail] = i;
        return tail + 1;
    }

    // Ridged multifractal: cada capa marca crestas y pesa según la anterior, así quedan cumbres afiladas y valles suaves (0 a 1).
    // Cada capa va girada y en noise 3D para que no se note la grilla del simplex (salían líneas rectas en diagonal)
    private double ridged(Noises n, int x, int z) {
        double frequency = 1.0 / 210;
        double amplitude = 1;
        double weight = 1;
        double sum = 0;
        double total = 0;
        for (int i = 0; i < n.ridgedOctaves.length; i++) {
            double angle = 0.6 + i * 1.37;
            double rx = x * Math.cos(angle) - z * Math.sin(angle);
            double rz = x * Math.sin(angle) + z * Math.cos(angle);
            double signal = 1 - Math.abs(n.ridgedOctaves[i].noise(rx * frequency, 7.31 + i * 13.7, rz * frequency));
            signal *= signal * weight;
            weight = Math.max(0, Math.min(1, signal * 2));
            sum += signal * amplitude;
            total += amplitude;
            frequency *= 2.1;
            amplitude *= 0.5;
        }
        return sum / total;
    }

    private Material bodyMaterial(WardenBiome biome, Noises n, int x, int y, int z) {
        return switch (biome) {
            case CAVERNA_SCULK -> {
                double s = n.layers.noise(x, y, z, 0.5, 0.5, true);
                yield s > 0.55 ? Material.COBBLED_DEEPSLATE : s < -0.62 ? Material.TUFF : Material.DEEPSLATE;
            }
            case PANTANO_PROFUNDO, ABISMO_FLOTANTE -> Material.DEEPSLATE;
            case RUINAS_DE_CENIZA -> {
                // Vetas onduladas de obsidiana que cruzan las paredes, con manchas de obsidiana llorosa
                if (Math.abs(n.obsidian.noise(x, y * 1.6, z, 0.5, 0.5, true)) < 0.035) {
                    yield n.layers.noise(x * 2.0, y * 2.0, z * 2.0, 0.5, 0.5, true) > 0.45 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                }
                double s = n.layers.noise(x, y, z, 0.5, 0.5, true);
                yield s > 0.35 ? Material.BLACKSTONE : s < -0.35 ? Material.SMOOTH_BASALT : Material.TUFF;
            }
        };
    }

    // Recorre la columna y le pone a cada suelo y techo los bloques y la decoración de su bioma, también en la caverna
    // de una Ancient City (ahí con un poco más de sculk, que sale de la ciudad)
    private void surfaces(ChunkData chunk, Noises n, Random r, Column c, AncientCityLocator.CityInfo city,
                          int x, int z, int wx, int wz, int top) {
        boolean decorate = c.spawnInfluence < 0.5;
        for (int y = MIN_Y + 1; y <= top; y++) {
            if (!BODY.contains(chunk.getType(x, y, z))) continue;

            Material above = chunk.getType(x, y + 1, z);
            Material below = chunk.getType(x, y - 1, z);
            boolean floor = above == Material.AIR;
            boolean wet = above == Material.WATER || above == Material.LAVA;
            boolean ceiling = below == Material.AIR;
            if (!floor && !wet && !ceiling) continue;

            boolean cavern = inCityCavern(c, city, y);
            boolean patch = c.biome != WardenBiome.CAVERNA_SCULK
                    && n.patches.noise(wx, y * 2, wz, 0.5, 0.5, true) > (cavern ? 0.2 : 0.45);

            switch (c.biome) {
                case CAVERNA_SCULK -> {
                    if (cavern) deepDarkSurface(chunk, n, r, x, y, z, wx, wz, floor, ceiling);
                    else sculkSurface(chunk, n, r, decorate, x, y, z, wx, wz, floor, ceiling);
                }
                case PANTANO_PROFUNDO -> swampSurface(chunk, r, decorate, patch, x, y, z, floor, wet, ceiling);
                case ABISMO_FLOTANTE -> abyssSurface(chunk, r, decorate, patch, x, y, z, floor, ceiling);
                case RUINAS_DE_CENIZA -> ashSurface(chunk, r, decorate, patch, !cavern, x, y, z, floor, wet, ceiling);
            }
        }
    }

    // Caverna Sculk: pizarra con el sculk creciendo en manchas por el suelo y el techo, catalizadores que alumbran,
    // chilladores y sensores, faroles de almas colgando, liquen y amatista en el techo
    private void sculkSurface(ChunkData chunk, Noises n, Random r, boolean decorate, int x, int y, int z,
                              int wx, int wz, boolean floor, boolean ceiling) {
        double patch = n.patches.noise(wx, y * 2, wz, 0.5, 0.5, true);
        if (floor) {
            if (patch > -0.45) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate) {
                    int roll = r.nextInt(1000);
                    if (roll < 3) sculkDecoration(chunk, x, y + 1, z, true);
                    else if (roll < 9) sculkDecoration(chunk, x, y + 1, z, false);
                    else if (roll < 12) chunk.setBlock(x, y, z, Material.SCULK_CATALYST);
                }
            } else if (decorate && r.nextInt(3) == 0) {
                facing(chunk, Material.SCULK_VEIN, x, y + 1, z, BlockFace.DOWN);
            }
        }
        if (ceiling) {
            if (patch > 0.05) chunk.setBlock(x, y, z, Material.SCULK);
            if (decorate) {
                int roll = r.nextInt(1000);
                if (roll < 28) {
                    cluster(chunk, x, y - 1, z, r.nextBoolean() ? Material.MEDIUM_AMETHYST_BUD : Material.LARGE_AMETHYST_BUD, BlockFace.DOWN);
                } else if (roll < 44) {
                    facing(chunk, Material.GLOW_LICHEN, x, y - 1, z, BlockFace.UP);
                } else if (roll < 47) {
                    soulLantern(chunk, r, x, y - 1, z);
                } else if (roll < 57) {
                    chunk.setBlock(x, y, z, Material.CRYING_OBSIDIAN);
                }
            }
        }
    }

    // La caverna de una Ancient City de la Caverna Sculk es como el deep dark: casi todo sculk, con chilladores,
    // sensores y catalizadores en el piso y venas en el techo. Lo que caiga donde va una pieza lo tapa la ciudad
    private void deepDarkSurface(ChunkData chunk, Noises n, Random r, int x, int y, int z, int wx, int wz,
                                 boolean floor, boolean ceiling) {
        double patch = n.patches.noise(wx, y * 2, wz, 0.5, 0.5, true);
        if (floor) {
            if (patch > -0.6) {
                chunk.setBlock(x, y, z, Material.SCULK);
                int roll = r.nextInt(1000);
                if (roll < 1) sculkDecoration(chunk, x, y + 1, z, true);
                else if (roll < 6) sculkDecoration(chunk, x, y + 1, z, false);
                else if (roll < 9) chunk.setBlock(x, y, z, Material.SCULK_CATALYST);
            } else if (r.nextInt(2) == 0) {
                facing(chunk, Material.SCULK_VEIN, x, y + 1, z, BlockFace.DOWN);
            }
        }
        if (ceiling) {
            if (patch > -0.2) chunk.setBlock(x, y, z, Material.SCULK);
            else if (r.nextInt(3) == 0) facing(chunk, Material.SCULK_VEIN, x, y - 1, z, BlockFace.UP);
        }
    }

    // Venas de sculk y liquen en las paredes de la Caverna Sculk y de la caverna de las Ancient City (en los otros
    // biomas solo unas pocas venas alrededor de la ciudad)
    private void sculkWalls(ChunkData chunk, Random r, Column c, AncientCityLocator.CityInfo city, int x, int z, int top) {
        boolean sculkBiome = c.biome == WardenBiome.CAVERNA_SCULK;
        int veins = sculkBiome ? 7 : 2;
        for (int y = MIN_Y + 2; y < top; y++) {
            if (chunk.getType(x, y, z) != Material.AIR) continue;
            if (!sculkBiome && !inCityCavern(c, city, y)) continue;
            for (BlockFace face : HORIZONTAL) {
                int nx = x + face.getModX();
                int nz = z + face.getModZ();
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || !BODY.contains(chunk.getType(nx, y, nz))) continue;
                int roll = r.nextInt(100);
                if (roll < veins) facing(chunk, Material.SCULK_VEIN, x, y, z, face);
                else if (roll < veins + 2) facing(chunk, Material.GLOW_LICHEN, x, y, z, face);
                break;
            }
        }
    }

    // Pantano Profundo: musgo, barro y arcilla, fondo de barro bajo el agua, enredaderas y líquenes en el techo
    private void swampSurface(ChunkData chunk, Random r, boolean decorate, boolean patch, int x, int y, int z,
                              boolean floor, boolean wet, boolean ceiling) {
        if (wet) {
            chunk.setBlock(x, y, z, r.nextInt(5) < 3 ? Material.MUD : Material.CLAY);
        } else if (floor) {
            if (patch) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate && r.nextInt(1000) < 3) sculkDecoration(chunk, x, y + 1, z, r.nextBoolean());
            } else {
                int roll = r.nextInt(100);
                chunk.setBlock(x, y, z, roll < 75 ? Material.MOSS_BLOCK : roll < 90 ? Material.MUD : Material.CLAY);
                if (decorate && roll < 75 && r.nextInt(100) < 12) chunk.setBlock(x, y + 1, z, Material.MOSS_CARPET);
            }
        }
        if (ceiling) {
            chunk.setBlock(x, y, z, patch ? Material.SCULK : Material.MOSS_BLOCK);
            if (decorate) {
                int roll = r.nextInt(100);
                if (roll < 4) hangVines(chunk, r, x, y - 1, z);
                else if (roll < 12) facing(chunk, Material.GLOW_LICHEN, x, y - 1, z, BlockFace.UP);
            }
        }
    }

    // Abismo Flotante: islas de pizarra con amatista y calcita arriba y cristales colgando abajo
    private void abyssSurface(ChunkData chunk, Random r, boolean decorate, boolean patch, int x, int y, int z,
                              boolean floor, boolean ceiling) {
        if (floor) {
            if (patch) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate && r.nextInt(1000) < 3) sculkDecoration(chunk, x, y + 1, z, r.nextBoolean());
            } else {
                int roll = r.nextInt(100);
                if (roll < 12) chunk.setBlock(x, y, z, Material.AMETHYST_BLOCK);
                else if (roll < 32) chunk.setBlock(x, y, z, Material.CALCITE);
                if (decorate && r.nextInt(100) < 3) cluster(chunk, x, y + 1, z, Material.AMETHYST_CLUSTER, BlockFace.UP);
            }
        }
        if (ceiling) {
            if (r.nextInt(100) < 3) chunk.setBlock(x, y, z, Material.CRYING_OBSIDIAN);
            if (decorate && r.nextInt(100) < 5) cluster(chunk, x, y - 1, z, Material.AMETHYST_CLUSTER, BlockFace.DOWN);
        }
    }

    // Ruinas de Ceniza: basalto y blackstone con obsidiana; alrededor de la lava, magma y obsidiana como si la lava
    // se hubiera enfriado (no en la caverna de una ciudad, que está más abajo que la lava pero no tiene), y de vez
    // en cuando obsidiana llorosa goteando del techo
    private void ashSurface(ChunkData chunk, Random r, boolean decorate, boolean patch, boolean shore, int x, int y, int z,
                            boolean floor, boolean wet, boolean ceiling) {
        if (wet) {
            int roll = r.nextInt(100);
            chunk.setBlock(x, y, z, roll < 40 ? Material.MAGMA_BLOCK : roll < 70 ? Material.OBSIDIAN : Material.BLACKSTONE);
        } else if (floor) {
            int roll = r.nextInt(1000);
            if (patch) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate && roll < 3) sculkDecoration(chunk, x, y + 1, z, r.nextBoolean());
            } else if (shore && y <= LAVA_LEVEL + 2) {
                chunk.setBlock(x, y, z, roll < 450 ? Material.MAGMA_BLOCK : roll < 750 ? Material.OBSIDIAN : Material.BLACKSTONE);
            } else if (roll < 15) {
                chunk.setBlock(x, y, z, Material.CRYING_OBSIDIAN);
            } else if (roll < 95) {
                chunk.setBlock(x, y, z, Material.OBSIDIAN);
            } else {
                chunk.setBlock(x, y, z, roll < 620 ? Material.BASALT : Material.BLACKSTONE);
            }
        }
        if (ceiling) {
            int roll = r.nextInt(100);
            if (patch) chunk.setBlock(x, y, z, Material.SCULK);
            else chunk.setBlock(x, y, z, roll < 4 ? Material.CRYING_OBSIDIAN : roll < 60 ? Material.BASALT : Material.BLACKSTONE);
        }
    }

    // Chillador (puede llamar al Warden) o sensor
    private void sculkDecoration(ChunkData chunk, int x, int y, int z, boolean shrieker) {
        if (y > TOP_Y || chunk.getType(x, y, z) != Material.AIR) return;
        if (shrieker) {
            SculkShrieker data = (SculkShrieker) Material.SCULK_SHRIEKER.createBlockData();
            data.setCanSummon(true);
            chunk.setBlock(x, y, z, data);
        } else {
            chunk.setBlock(x, y, z, Material.SCULK_SENSOR);
        }
    }

    private void cluster(ChunkData chunk, int x, int y, int z, Material type, BlockFace facing) {
        if (chunk.getType(x, y, z) != Material.AIR) return;
        AmethystCluster cluster = (AmethystCluster) type.createBlockData();
        cluster.setFacing(facing);
        chunk.setBlock(x, y, z, cluster);
    }

    // Liquen o venas de sculk pegados a la cara del bloque de al lado
    private void facing(ChunkData chunk, Material type, int x, int y, int z, BlockFace face) {
        if (y > TOP_Y || chunk.getType(x, y, z) != Material.AIR) return;
        MultipleFacing data = (MultipleFacing) type.createBlockData();
        data.setFace(face, true);
        chunk.setBlock(x, y, z, data);
    }

    // Farol de almas colgando del techo con 1 o 2 eslabones de cadena
    private void soulLantern(ChunkData chunk, Random r, int x, int y, int z) {
        int chain = 1 + r.nextInt(2);
        for (int i = 0; i <= chain; i++) {
            if (y - i <= MIN_Y + 1 || chunk.getType(x, y - i, z) != Material.AIR) return;
        }
        for (int i = 0; i < chain; i++) chunk.setBlock(x, y - i, z, Material.IRON_CHAIN);
        Lantern lantern = (Lantern) Material.SOUL_LANTERN.createBlockData();
        lantern.setHanging(true);
        chunk.setBlock(x, y - chain, z, lantern);
    }

    // Enredaderas de 1 a 4 bloques; algunas con bayas brillantes para que el pantano no sea tan oscuro
    private void hangVines(ChunkData chunk, Random r, int x, int y, int z) {
        int length = 0;
        int max = 1 + r.nextInt(4);
        while (length < max && y - length > MIN_Y + 1 && chunk.getType(x, y - length, z) == Material.AIR) {
            length++;
        }
        for (int i = 0; i < length; i++) {
            Material type = i == length - 1 ? Material.CAVE_VINES : Material.CAVE_VINES_PLANT;
            CaveVinesPlant vine = (CaveVinesPlant) type.createBlockData();
            vine.setBerries(r.nextInt(100) < 35);
            chunk.setBlock(x, y - i, z, vine);
        }
    }

    // Vetas chicas de terracota que hacen de mineral; cada bioma tiene la suya
    private void vein(ChunkData chunk, Random r, int sx, int sy, int sz, Material ore) {
        int size = 2 + r.nextInt(3);
        int x = sx, y = sy, z = sz;
        for (int i = 0; i < size; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y > MIN_Y + 1 && y < TOP_Y
                    && ORE_HOST.contains(chunk.getType(x, y, z))) {
                Directional data = (Directional) ore.createBlockData();
                data.setFacing(HORIZONTAL[r.nextInt(HORIZONTAL.length)]);
                chunk.setBlock(x, y, z, data);
            }
            switch (r.nextInt(6)) {
                case 0 -> x++;
                case 1 -> x--;
                case 2 -> y++;
                case 3 -> y--;
                case 4 -> z++;
                default -> z--;
            }
        }
    }

    // El líquido solo va en el centro del bioma; en el borde se rellena con roca hasta un bloque arriba del nivel,
    // así la lava o el agua nunca quedan tocando aire, una Ancient City o el vacío del bioma de al lado
    private void fluid(ChunkData chunk, int x, int z, double weight, int level, Material fluid,
                       boolean allowed, Material rim) {
        if (weight < FLUID_RIM) return;
        if (allowed && weight >= FLUID_CORE) {
            fill(chunk, x, z, level, fluid);
            return;
        }
        for (int y = MIN_Y + 1; y <= level + 1; y++) {
            if (chunk.getType(x, y, z) == Material.AIR) chunk.setBlock(x, y, z, rim);
        }
    }

    private void fill(ChunkData chunk, int x, int z, int level, Material fluid) {
        for (int y = MIN_Y + 1; y <= level; y++) {
            if (chunk.getType(x, y, z) == Material.AIR) chunk.setBlock(x, y, z, fluid);
        }
    }

    private static double influence(double dist, double radius, double transition) {
        if (dist <= radius) return 1.0;
        if (dist >= radius + transition) return 0.0;
        double t = 1.0 - (dist - radius) / transition;
        return t * t * (3 - 2 * t);
    }

    private static Noises noises(long seed) {
        Noises current = noises;
        if (current == null || current.seed != seed) {
            synchronized (WardenGenerator.class) {
                current = noises;
                if (current == null || current.seed != seed) {
                    current = new Noises(seed);
                    noises = current;
                }
            }
        }
        return current;
    }

    private static final class Noises {
        final long seed;
        final SimplexOctaveGenerator caveSculk;
        final SimplexOctaveGenerator caveSwamp;
        final SimplexOctaveGenerator caveAbyss;
        final SimplexOctaveGenerator caveAsh;
        final SimplexOctaveGenerator floor;
        final SimplexOctaveGenerator skylights;
        final SimplexOctaveGenerator sculk;
        final SimplexOctaveGenerator pillars;
        final SimplexOctaveGenerator patches;
        final SimplexOctaveGenerator layers;
        final SimplexOctaveGenerator obsidian;
        final SimplexOctaveGenerator spires;
        final SimplexOctaveGenerator crustVariation;
        final SimplexOctaveGenerator cityHills;
        final org.bukkit.util.noise.SimplexNoiseGenerator[] ridgedOctaves = new org.bukkit.util.noise.SimplexNoiseGenerator[4];

        Noises(long seed) {
            this.seed = seed;
            caveSculk = octaves(new Random(seed + 30), 3, 1.0 / 90);
            caveSwamp = octaves(new Random(seed + 31), 3, 1.0 / 80);
            caveAbyss = octaves(new Random(seed + 2), 4, 0.015);
            caveAsh = octaves(new Random(seed + 33), 3, 1.0 / 85);
            floor = octaves(new Random(seed + 34), 2, 1.0 / 60);
            skylights = octaves(new Random(seed + 36), 2, 1.0 / 110);
            sculk = octaves(new Random(seed), 4, 0.0085);
            pillars = octaves(new Random(seed + 4), 2, 0.08);
            patches = octaves(new Random(seed + 5), 3, 0.06);
            layers = octaves(new Random(seed + 6), 3, 0.04);
            obsidian = octaves(new Random(seed + 35), 2, 1.0 / 40);
            spires = octaves(new Random(seed + 12), 2, 1.0 / 16);
            crustVariation = octaves(new Random(seed + 16), 2, 1.0 / 28);
            cityHills = octaves(new Random(seed + 37), 3, 1.0 / 45);
            for (int i = 0; i < ridgedOctaves.length; i++) {
                ridgedOctaves[i] = new org.bukkit.util.noise.SimplexNoiseGenerator(new Random(seed + 20 + i));
            }
        }

        SimplexOctaveGenerator cave(int biome) {
            return switch (WardenBiome.values()[biome]) {
                case CAVERNA_SCULK -> caveSculk;
                case PANTANO_PROFUNDO -> caveSwamp;
                case ABISMO_FLOTANTE -> caveAbyss;
                case RUINAS_DE_CENIZA -> caveAsh;
            };
        }

        private static SimplexOctaveGenerator octaves(Random random, int octaves, double scale) {
            SimplexOctaveGenerator generator = new SimplexOctaveGenerator(random, octaves);
            generator.setScale(scale);
            return generator;
        }
    }
}
