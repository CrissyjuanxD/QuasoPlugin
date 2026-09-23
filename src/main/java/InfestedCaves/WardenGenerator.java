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
    // Ya no hay techo de bedrock: arriba hay montañas al aire libre y esta es la altura máxima de los picos
    static final int TOP_Y = 230;
    static final int SPAWN_RADIUS = 70;
    static final int SPAWN_TRANSITION = 40;
    // Altura de la meseta del spawn, donde va la build con el portal de salida
    static final int SPAWN_Y = 100;

    private static final int WATER_LEVEL = -38;
    private static final int LAVA_LEVEL = -48;
    private static final int ORE_CHANCE = 900;
    private static final double FLUID_CORE = 0.9;
    private static final double FLUID_RIM = 0.2;
    private static final int FLUID_CITY_MARGIN = 12;
    private static final double AIR = 0.2;
    // Roca entre las cuevas y la superficie, así las cuevas no se abren al cielo por todos lados
    private static final int CRUST = 14;
    // Arriba de esta altura el Abismo ya no tiene islas
    private static final int ISLAND_TOP = 135;

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
    private volatile Noises noises;
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
        return List.of(new WardenPopulator());
    }

    // Genera todo el chunk: las montañas de arriba y las cuevas de adentro según el bioma (mezclados en los bordes),
    // el agua y la lava, las superficies con su decoración y al final las vetas de mineral
    @Override
    public void generateNoise(WorldInfo info, Random random, int chunkX, int chunkZ, ChunkData chunk) {
        long seed = info.getSeed();
        Noises n = noises(seed);
        WardenBiomeMap map = WardenBiomeMap.forSeed(seed);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        AncientCityLocator.CityInfo city = AncientCityLocator.findCityNear(seed, baseX + 8, baseZ + 8, 12);

        WardenBiome[] columnBiome = new WardenBiome[256];
        boolean[] decorate = new boolean[256];
        boolean[] oreAllowed = new boolean[256];
        int[] columnTop = new int[256];
        double[] v = new double[TOP_Y - MIN_Y + 3];
        double[] height = new double[4];

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;
                int col = x << 4 | z;

                double[] w = map.weights(wx, wz);
                WardenBiome biome = materialBiome(n, w, wx, wz);
                columnBiome[col] = biome;

                double dist = Math.sqrt((double) wx * wx + (double) wz * wz);
                double spawnInfluence = influence(dist, SPAWN_RADIUS, SPAWN_TRANSITION);
                double cityInfluence = city != null ? AncientCityLocator.computeInfluence(city, wx, wz) : 0.0;
                decorate[col] = spawnInfluence < 0.5 && cityInfluence < 0.5;
                oreAllowed[col] = spawnInfluence < 0.1 && cityInfluence < 0.1;

                // Altura de la superficie de cada bioma; en el spawn todas se aplanan hacia la meseta
                int top = MIN_Y + 2;
                for (int b = 0; b < 4; b++) {
                    if (w[b] <= 0.001) continue;
                    double h = b == ABYSS ? ISLAND_TOP + 10 : surfaceHeight(n, b, wx, wz);
                    height[b] = h + (SPAWN_Y - h) * spawnInfluence;
                    top = Math.max(top, (int) Math.ceil(height[b]) + 2);
                }

                // Agujas de basalto de las Ruinas: salen de las cuevas y pasan la superficie
                double pillarTop = Double.NEGATIVE_INFINITY;
                if (w[ASH] > 0.5 && spawnInfluence <= 0) {
                    double p = n.pillars.noise(wx, wz, 0.5, 0.5, true);
                    if (p > 0.78) {
                        pillarTop = height[ASH] + 8 + (p - 0.78) / 0.22 * 34;
                        top = Math.max(top, (int) Math.ceil(pillarTop) + 2);
                    }
                }
                top = Math.min(top, TOP_Y);

                for (int y = MIN_Y - 1; y <= top + 1; y++) {
                    double d = 0;
                    for (int b = 0; b < 4; b++) {
                        if (w[b] > 0.001) d += w[b] * density(n, b, wx, y, wz, height[b], spawnInfluence, pillarTop);
                    }
                    if (cityInfluence > 0) d += cityCarve(n, city, cityInfluence, wx, y, wz);
                    v[y - MIN_Y + 1] = d;
                }

                // Piso de 3 a 6 bloques arriba de la bedrock (en el Abismo es vacío y en las Ancient City queda una capa)
                boolean voidFloor = w[ABYSS] > 0.6 && spawnInfluence <= 0 && cityInfluence <= 0;
                int floor = voidFloor ? 0 : cityInfluence > 0 ? 1 : 3 + (int) ((n.crust.noise(wx, wz, 0.5, 0.5, true) + 1) * 1.6);

                for (int y = MIN_Y; y <= top; y++) {
                    if (y == MIN_Y) {
                        if (!voidFloor) chunk.setBlock(x, y, z, Material.BEDROCK);
                    } else if (y <= MIN_Y + floor || v[y - MIN_Y + 1] <= AIR) {
                        chunk.setBlock(x, y, z, pillarTop > y ? Material.BASALT : bodyMaterial(biome, n, wx, y, wz));
                    }
                }

                if (spawnInfluence <= 0 && cityInfluence <= 0) {
                    boolean nearCity = city != null && Math.hypot(wx - city.centerX, wz - city.centerZ)
                            <= AncientCityLocator.CLEAR_RADIUS + AncientCityLocator.CLEAR_TRANSITION + FLUID_CITY_MARGIN;
                    Material rim = bodyMaterial(biome, n, wx, WATER_LEVEL, wz);
                    fluid(chunk, x, z, w[SWAMP], WATER_LEVEL, Material.WATER, !nearCity, rim);
                    fluid(chunk, x, z, w[ASH], LAVA_LEVEL, Material.LAVA, !nearCity, rim);
                }

                int surface = top;
                while (surface > MIN_Y && chunk.getType(x, surface, z) == Material.AIR) surface--;
                columnTop[col] = surface;
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                surfaces(chunk, n, random, columnBiome[col], decorate[col], x, z, baseX + x, baseZ + z, columnTop[col]);
                if (columnBiome[col] == WardenBiome.CAVERNA_SCULK && decorate[col]) {
                    sculkWalls(chunk, random, x, z, columnTop[col]);
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int col = x << 4 | z;
                if (!oreAllowed[col]) continue;
                Material ore = columnBiome[col].ore();
                for (int y = MIN_Y + 3; y <= columnTop[col] - 3; y++) {
                    if (ORE_HOST.contains(chunk.getType(x, y, z)) && random.nextInt(ORE_CHANCE) == 0) {
                        vein(chunk, random, x, y, z, ore);
                    }
                }
            }
        }
    }

    // Montañas de arriba: cordilleras con crestas y picos afilados (más altas en el Sculk, lomas bajas en el Pantano).
    // "ranges" decide dónde hay cordillera grande y dónde quedan valles más tranquilos; el cuadrado del ridged
    // hace que las cumbres suban mucho más que los valles
    private double surfaceHeight(Noises n, int biome, int x, int z) {
        double hills = n.hills.noise(x, z, 2.0, 0.5, true);
        double range = (n.ranges.noise(x, z, 0.5, 0.5, true) + 1) / 2;
        double ridge = ridged(n, x, z);
        double mountain = ridge * ridge * (0.3 + 0.7 * range);
        double detail = n.detail.noise(x, z, 2.0, 0.5, true);

        if (biome == SCULK) {
            double spire = n.spires.noise(x, z, 0.5, 0.5, true);
            double needles = spire > 0.8 ? (spire - 0.8) / 0.2 * 34 : 0;
            return 96 + hills * 10 + mountain * 135 + detail * 4 + needles;
        }
        if (biome == SWAMP) {
            return 86 + hills * 8 + mountain * 40 + detail * 2;
        }
        return 94 + hills * 10 + mountain * 110 + detail * 5;
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

    // Densidad de cada bioma: arriba de 0.2 es aire. Adentro van las cuevas de cada bioma; cerca de la superficie
    // se cierran y arriba de la superficie es cielo. El Morado es casi todo aire con islas flotando bajo el cielo
    private double density(Noises n, int biome, int x, int y, int z, double height, double spawnInfluence, double pillarTop) {
        if (biome == ABYSS) {
            double v = AIR + (0.32 - n.abyss.noise(x, y * 2.0, z, 0.5, 0.5, true)) * 1.6;
            if (y > ISLAND_TOP) v += (y - ISLAND_TOP) / 12.0;
            if (y < -20) v += (-20 - y) / 40.0 * 0.6;
            return v;
        }

        double cave;
        if (biome == SCULK) cave = sculkCave(n, x, y, z);
        else if (biome == SWAMP) cave = n.swamp.noise(x, y * 1.8, z, 0.5, 0.5) + 0.05;
        else cave = n.ash.noise(x, y * 1.3, z, 0.5, 0.5) + 0.03;

        // El grosor de la capa de roca varía en 3D, así el techo de las cavernas no queda plano
        double crust = CRUST + n.crustVariation.noise(x, y, z, 0.5, 0.5, true) * 7;
        double depth = height - y;
        if (depth < crust) cave -= (crust - depth) / crust * 1.5;
        cave -= spawnInfluence * 2.5;

        double d = Math.max(cave, AIR + (y - height) * 0.2);
        if (pillarTop > Double.NEGATIVE_INFINITY && biome == ASH) d = Math.min(d, AIR + (y - pillarTop) * 0.2);
        return d;
    }

    // Caverna Sculk: cavernas anchas unidas por túneles y con pilares que cambian de grosor a lo alto
    private double sculkCave(Noises n, int x, int y, int z) {
        double cavern = AIR + (n.sculk.noise(x, y * 1.4, z, 0.5, 0.5, true) - 0.2) * 2.2;
        double tunnel = AIR + (0.07 - Math.abs(n.tunnels.noise(x, y * 1.7, z, 0.5, 0.5, true))) * 5.0;
        double d = Math.max(cavern, tunnel);
        double pillar = n.sculkPillars.noise(x, z, 0.5, 0.5, true);
        double threshold = 0.66 + Math.abs(n.pillarTaper.noise(x, y, z, 0.5, 0.5, true)) * 0.4;
        if (pillar > threshold) d -= (pillar - threshold) * 9;
        return d;
    }

    // En la franja de mezcla entre biomas cada columna toma los bloques de uno u otro en manchas,
    // así el cambio de bloques también es gradual y no una línea recta
    // Solo cuentan los biomas con 20% o más, así no aparecen columnas sueltas lejos del borde
    private WardenBiome materialBiome(Noises n, double[] w, int x, int z) {
        double sum = 0;
        for (double weight : w) if (weight >= 0.2) sum += weight;
        double r = (n.dither.noise(x, z, 0.5, 0.5, true) + 1) / 2 * sum;
        double total = 0;
        for (int b = 0; b < 4; b++) {
            if (w[b] < 0.2) continue;
            total += w[b];
            if (r <= total) return WardenBiome.values()[b];
        }
        return dominant(w);
    }

    // Vacía el lugar donde va la Ancient City y deja la transición irregular arriba de ella
    private double cityCarve(Noises n, AncientCityLocator.CityInfo city, double cityInfluence, int x, int y, int z) {
        if (y > city.minY() && y <= city.maxY()) return cityInfluence * 1.2;
        if (y > city.maxY() && y <= city.maxY() + 15) {
            double transition = (double) (y - city.maxY()) / 15.0;
            double extraNoise = n.sculk.noise(x * 3.7, y * 2.1, z * 3.7, 0.5, 0.5);
            return cityInfluence * (1.0 - transition) * extraNoise * 0.6;
        }
        return 0.0;
    }

    private Material bodyMaterial(WardenBiome biome, Noises n, int x, int y, int z) {
        return switch (biome) {
            case CAVERNA_SCULK -> {
                double s = n.layers.noise(x, y, z, 0.5, 0.5, true);
                yield s > 0.55 ? Material.COBBLED_DEEPSLATE : s < -0.62 ? Material.TUFF : Material.DEEPSLATE;
            }
            case PANTANO_PROFUNDO, ABISMO_FLOTANTE -> Material.DEEPSLATE;
            case RUINAS_DE_CENIZA -> {
                double s = n.layers.noise(x, y, z, 0.5, 0.5, true);
                yield s > 0.35 ? Material.BLACKSTONE : s < -0.35 ? Material.SMOOTH_BASALT : Material.TUFF;
            }
        };
    }

    // Recorre la columna y le pone a cada suelo y techo los bloques y la decoración de su bioma
    private void surfaces(ChunkData chunk, Noises n, Random r, WardenBiome biome, boolean decorate,
                          int x, int z, int wx, int wz, int top) {
        for (int y = MIN_Y + 1; y <= top; y++) {
            if (!BODY.contains(chunk.getType(x, y, z))) continue;

            Material above = chunk.getType(x, y + 1, z);
            Material below = chunk.getType(x, y - 1, z);
            boolean floor = above == Material.AIR;
            boolean wet = above == Material.WATER || above == Material.LAVA;
            boolean ceiling = below == Material.AIR;
            if (!floor && !wet && !ceiling) continue;

            boolean outdoor = floor && y == top;
            boolean patch = biome != WardenBiome.CAVERNA_SCULK
                    && n.patches.noise(wx, y * 2, wz, 0.5, 0.5, true) > 0.45;

            switch (biome) {
                case CAVERNA_SCULK -> sculkSurface(chunk, n, r, decorate, x, y, z, wx, wz, floor, ceiling, outdoor);
                case PANTANO_PROFUNDO -> swampSurface(chunk, r, decorate, patch, x, y, z, floor, wet, ceiling);
                case ABISMO_FLOTANTE -> abyssSurface(chunk, r, decorate, patch, x, y, z, floor, ceiling);
                case RUINAS_DE_CENIZA -> ashSurface(chunk, r, decorate, patch, x, y, z, floor, wet, ceiling);
            }
        }
    }

    // Caverna Sculk: pizarra con el sculk creciendo en manchas por el suelo y el techo, catalizadores que alumbran,
    // pocos chilladores (solo adentro de las cuevas), faroles de almas colgando, liquen y amatista en el techo
    private void sculkSurface(ChunkData chunk, Noises n, Random r, boolean decorate, int x, int y, int z,
                              int wx, int wz, boolean floor, boolean ceiling, boolean outdoor) {
        double patch = n.patches.noise(wx, y * 2, wz, 0.5, 0.5, true);
        if (floor) {
            if (patch > -0.45) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate) {
                    int roll = r.nextInt(1000);
                    if (roll < 3 && !outdoor) sculkDecoration(chunk, x, y + 1, z, true);
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

    // Venas de sculk y liquen en las paredes de las cuevas del Sculk
    private void sculkWalls(ChunkData chunk, Random r, int x, int z, int top) {
        for (int y = MIN_Y + 2; y < top; y++) {
            if (chunk.getType(x, y, z) != Material.AIR) continue;
            for (BlockFace face : HORIZONTAL) {
                int nx = x + face.getModX();
                int nz = z + face.getModZ();
                if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || !BODY.contains(chunk.getType(nx, y, nz))) continue;
                int roll = r.nextInt(100);
                if (roll < 7) facing(chunk, Material.SCULK_VEIN, x, y, z, face);
                else if (roll < 9) facing(chunk, Material.GLOW_LICHEN, x, y, z, face);
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

    // Ruinas de Ceniza: basalto y blackstone, magma alrededor de la lava
    private void ashSurface(ChunkData chunk, Random r, boolean decorate, boolean patch, int x, int y, int z,
                            boolean floor, boolean wet, boolean ceiling) {
        if (wet) {
            chunk.setBlock(x, y, z, r.nextBoolean() ? Material.MAGMA_BLOCK : Material.BLACKSTONE);
        } else if (floor) {
            if (patch) {
                chunk.setBlock(x, y, z, Material.SCULK);
                if (decorate && r.nextInt(1000) < 3) sculkDecoration(chunk, x, y + 1, z, r.nextBoolean());
            } else if (y <= LAVA_LEVEL + 2 && r.nextBoolean()) {
                chunk.setBlock(x, y, z, Material.MAGMA_BLOCK);
            } else {
                chunk.setBlock(x, y, z, r.nextInt(100) < 65 ? Material.BASALT : Material.BLACKSTONE);
            }
        }
        if (ceiling) {
            chunk.setBlock(x, y, z, patch ? Material.SCULK : Material.BASALT);
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

    private static WardenBiome dominant(double[] w) {
        int best = 0;
        for (int i = 1; i < w.length; i++) {
            if (w[i] > w[best]) best = i;
        }
        return WardenBiome.values()[best];
    }

    private static double influence(double dist, double radius, double transition) {
        if (dist <= radius) return 1.0;
        if (dist >= radius + transition) return 0.0;
        double t = 1.0 - (dist - radius) / transition;
        return t * t * (3 - 2 * t);
    }

    private Noises noises(long seed) {
        Noises current = noises;
        if (current == null || current.seed != seed) {
            synchronized (this) {
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
        final SimplexOctaveGenerator sculk;
        final SimplexOctaveGenerator tunnels;
        final SimplexOctaveGenerator sculkPillars;
        final SimplexOctaveGenerator swamp;
        final SimplexOctaveGenerator abyss;
        final SimplexOctaveGenerator ash;
        final SimplexOctaveGenerator pillars;
        final SimplexOctaveGenerator patches;
        final SimplexOctaveGenerator layers;
        final SimplexOctaveGenerator hills;
        final SimplexOctaveGenerator ridges;
        final SimplexOctaveGenerator detail;
        final SimplexOctaveGenerator spires;
        final SimplexOctaveGenerator crust;
        final SimplexOctaveGenerator dither;
        final SimplexOctaveGenerator ranges;
        final SimplexOctaveGenerator crustVariation;
        final SimplexOctaveGenerator pillarTaper;
        final org.bukkit.util.noise.SimplexNoiseGenerator[] ridgedOctaves = new org.bukkit.util.noise.SimplexNoiseGenerator[4];

        Noises(long seed) {
            this.seed = seed;
            sculk = octaves(new Random(seed), 4, 0.0085);
            tunnels = octaves(new Random(seed + 7), 2, 0.012);
            sculkPillars = octaves(new Random(seed + 8), 2, 0.085);
            swamp = octaves(new Random(seed + 1), 6, 0.012);
            abyss = octaves(new Random(seed + 2), 6, 0.015);
            ash = octaves(new Random(seed + 3), 6, 0.011);
            pillars = octaves(new Random(seed + 4), 2, 0.08);
            patches = octaves(new Random(seed + 5), 3, 0.06);
            layers = octaves(new Random(seed + 6), 3, 0.04);
            hills = octaves(new Random(seed + 9), 3, 1.0 / 420);
            ridges = octaves(new Random(seed + 10), 4, 1.0 / 95);
            detail = octaves(new Random(seed + 11), 2, 1.0 / 36);
            spires = octaves(new Random(seed + 12), 2, 1.0 / 16);
            crust = octaves(new Random(seed + 13), 2, 1.0 / 24);
            dither = octaves(new Random(seed + 14), 2, 1.0 / 9);
            ranges = octaves(new Random(seed + 15), 2, 1.0 / 650);
            crustVariation = octaves(new Random(seed + 16), 2, 1.0 / 28);
            pillarTaper = octaves(new Random(seed + 17), 2, 1.0 / 22);
            for (int i = 0; i < ridgedOctaves.length; i++) {
                ridgedOctaves[i] = new org.bukkit.util.noise.SimplexNoiseGenerator(new Random(seed + 20 + i));
            }
        }

        private static SimplexOctaveGenerator octaves(Random random, int octaves, double scale) {
            SimplexOctaveGenerator generator = new SimplexOctaveGenerator(random, octaves);
            generator.setScale(scale);
            return generator;
        }
    }
}
