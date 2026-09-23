package InfestedCaves;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.AmethystCluster;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.generator.BlockPopulator;

public class WardenGenerator extends ChunkGenerator {
    private final JavaPlugin plugin;

    private static final int WORLD_RADIUS    = 3000;
    private static final int BORDER_SMOOTHING = 50;
    private static final int SPAWN_RADIUS    = 70;
    private static final int SPAWN_TRANSITION = 40;

    public WardenGenerator(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomes) {
        ChunkData chunk = createChunkData(world);
        SimplexOctaveGenerator noise = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
        noise.setScale(0.01);

        int realX = chunkX * 16;
        int realZ = chunkZ * 16;

        // Detectar Ancient City cercana (puro cálculo por seed, sin cargar chunks)
        AncientCityLocator.CityInfo cityInfo =
                AncientCityLocator.findCityNear(world.getSeed(), realX + 8, realZ + 8);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int cx = realX + x;
                int cz = realZ + z;

                int distX = Math.abs(cx);
                int distZ = Math.abs(cz);
                double distToSpawn = Math.sqrt((double) cx * cx + (double) cz * cz);

                // ── Pared de bedrock (límite del mundo) ──────────────────────────
                if (distX >= WORLD_RADIUS || distZ >= WORLD_RADIUS) {
                    for (int y = -60; y <= 120; y++) chunk.setBlock(x, y, z, Material.BEDROCK);
                    continue;
                }

                // ── Influencias ──────────────────────────────────────────────────
                double spawnInfluence = 0;
                if (distToSpawn < SPAWN_RADIUS + SPAWN_TRANSITION) {
                    spawnInfluence = (distToSpawn <= SPAWN_RADIUS) ? 1.0
                            : 1.0 - (distToSpawn - SPAWN_RADIUS) / SPAWN_TRANSITION;
                }

                double cityInfluence = (cityInfo != null)
                        ? AncientCityLocator.computeInfluence(cityInfo, cx, cz) : 0.0;

                // ── Precalcular densidades ───────────────────────────────────────
                double[] densities = new double[183]; // índice 0 = y -61
                for (int y = -61; y <= 121; y++) {
                    double v = noise.noise(cx, y, cz, 0.5, 0.5);

                    // Limpieza spawn (solo hacia arriba desde -58)
                    if (spawnInfluence > 0 && y >= -58)
                        v += spawnInfluence * 1.2;

                    // Limpieza Ancient City: SOLO el volumen real del schem (minY..maxY).
                    // La capa -59 no se toca (y > minY): el suelo base permanece sculk.
                    // Por encima de maxY el noise corre totalmente libre → techo natural.
                    if (cityInfluence > 0 && cityInfo != null
                            && y > cityInfo.minY()    // > -59: no tocar suelo base
                            && y <= cityInfo.maxY())  // hasta el techo real del schem
                        v += cityInfluence * 1.2;

                    // ANTI-TECHO-PLANO: en la franja justo por encima del schem
                    // (maxY < y <= maxY+15) inyectamos ruido extra de alta frecuencia
                    // para que la transición suelo→techo sea orgánica, no una capa plana.
                    // Esto solo afecta a la zona de ciudad y solo en esa franja de 15 bloques.
                    if (cityInfluence > 0 && cityInfo != null
                            && y > cityInfo.maxY()
                            && y <= cityInfo.maxY() + 15) {
                        double transition = (double)(y - cityInfo.maxY()) / 15.0; // 0→1
                        // Ruido extra de alta frecuencia (4x más denso) que rompe la planitud
                        double extraNoise = noise.noise(cx * 3.7, y * 2.1, cz * 3.7, 0.5, 0.5);
                        // Se mezcla: fuerte cerca del techo del schem, cero 15 bloques arriba
                        v += cityInfluence * (1.0 - transition) * extraNoise * 0.6;
                    }

                    densities[y + 61] = v;
                }

                // ── Colocar bloques ──────────────────────────────────────────────
                for (int y = -60; y <= 120; y++) {
                    int idx = y + 61;

                    if (y == -60 || y == 120) {
                        chunk.setBlock(x, y, z, Material.BEDROCK);
                        continue;
                    }

                    double density = densities[idx];
                    biomes.setBiome(x, y, z, Biome.DEEP_DARK);

                    if (density > 0.2) {
                        // ── AIRE ────────────────────────────────────────────────
                        chunk.setBlock(x, y, z, Material.AIR);

                        // Decoración: permitida en TODA la dimensión salvo spawn.
                        // La ciudad NO bloquea decoración: el schem sobreescribe lo
                        // que necesite, y los huecos naturales quedan decorados.
                        // Nunca decoramos en Y=119 desde aquí (bedrock en 120 encima).
                        if (spawnInfluence < 0.5) {
                            if (y > -60 && y < 119 && densities[idx - 1] <= 0.2)
                                decorateFloor(chunk, x, y - 1, z, random);
                            if (y < 119 && densities[idx + 1] <= 0.2)
                                decorateCeiling(chunk, x, y + 1, z, random);
                        }

                    } else {
                        // ── SÓLIDO ───────────────────────────────────────────────
                        if (chunk.getType(x, y, z) == Material.AIR) {
                            chunk.setBlock(x, y, z, Material.SCULK);

                            // Venas de mineral (no en spawn ni en interior de la ciudad)
                            if (spawnInfluence < 0.1 && cityInfluence < 0.1
                                    && random.nextInt(500) == 0) {
                                double c = random.nextDouble();
                                generateVein(chunk, x, y, z,
                                        c < 0.10 ? 3 : (c < 0.35 ? 2 : 1), random);
                            }
                        }
                    }
                }

                // ── Capa -59: suelo sculk base ───────────────────────────────────
                // SIEMPRE se fuerza a SCULK (evita parches de bedrock).
                // FIX ERROR CONSOLA: SCULK_CATALYST tiene block-entity y NO puede
                // colocarse via ChunkData.setBlock() — solo colocamos SCULK aquí.
                // La decoración encima solo si hay ≥2 bloques de aire (no bajo montañas)
                // y estamos fuera del interior puro de la ciudad.
                chunk.setBlock(x, -59, z, Material.SCULK);

                // Decoración sobre la capa -59: siempre que haya ≥2 bloques de aire
                // (no bajo montañas). Ciudad no bloquea: el schem sobreescribe igual.
                boolean twoAirAbove = chunk.getType(x, -58, z) == Material.AIR
                        && chunk.getType(x, -57, z) == Material.AIR;
                if (twoAirAbove && spawnInfluence < 0.5) {
                    decorateFloorCapeTop(chunk, x, -59, z, random);
                }

                // ── Techo superior: sculk en Y=119 cubre la bedrock de Y=120 ─────
                // Sin condición de ciudad: siempre se decora el techo natural.
                // decorateCeiling coloca SCULK en y y amethyst en y-1 (Y=118).
                // Solo se salta si la posición Y=119 ya tiene algo (amethyst puesto
                // por el bucle de densidades desde abajo).
                if (chunk.getType(x, 119, z) == Material.AIR)
                    decorateCeiling(chunk, x, 119, z, random);

                if (distX > WORLD_RADIUS - BORDER_SMOOTHING
                        || distZ > WORLD_RADIUS - BORDER_SMOOTHING)
                    smoothBorder(chunk, x, z, distX, distZ);
            }
        }
        return chunk;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void generateVein(ChunkData chunk, int sx, int sy, int sz, int size, Random r) {
        int x = sx, y = sy, z = sz;
        for (int i = 0; i < size; i++) {
            if (x >= 0 && x < 16 && z >= 0 && z < 16 && y > -60 && y < 120) {
                Material t = chunk.getType(x, y, z);
                if (t != Material.AIR && t != Material.BEDROCK)
                    chunk.setBlock(x, y, z, Material.BUBBLE_CORAL_BLOCK);
            }
            switch (r.nextInt(6)) {
                case 0: x++; break; case 1: x--; break;
                case 2: y++; break; case 3: y--; break;
                case 4: z++; break; case 5: z--; break;
            }
        }
    }

    private void smoothBorder(ChunkData chunk, int x, int z, int distX, int distZ) {
        double px = (double)(distX - (WORLD_RADIUS - BORDER_SMOOTHING)) / BORDER_SMOOTHING;
        double pz = (double)(distZ - (WORLD_RADIUS - BORDER_SMOOTHING)) / BORDER_SMOOTHING;
        if (Math.max(px, pz) > 0.8) {
            for (int y = -60; y <= 120; y++)
                if (chunk.getType(x, y, z) == Material.AIR)
                    chunk.setBlock(x, y, z, Material.SCULK);
        }
    }

    private void decorateFloor(ChunkData chunk, int x, int y, int z, Random r) {
        if (r.nextInt(1000) < 10) {
            // Shrieker/sensor encima del bloque sólido
            if (r.nextBoolean()) {
                org.bukkit.block.data.type.SculkShrieker sh =
                        (org.bukkit.block.data.type.SculkShrieker)
                                Material.SCULK_SHRIEKER.createBlockData();
                sh.setCanSummon(true);
                chunk.setBlock(x, y + 1, z, sh);
            } else {
                chunk.setBlock(x, y + 1, z, Material.SCULK_SENSOR);
            }
            chunk.setBlock(x, y, z, Material.SCULK);
        } else {
            // FIX ERROR CONSOLA: SCULK_CATALYST tiene block-entity; NO se puede
            // colocar con ChunkData.setBlock() → siempre ponemos SCULK puro.
            chunk.setBlock(x, y, z, Material.SCULK);
        }
    }

    /**
     * Decoración encima de la capa -59 (sensores/shriekers sobre el suelo base).
     * Solo se llama cuando hay ≥2 bloques de aire arriba.
     * FIX ERROR CONSOLA: SCULK_CATALYST eliminado de aquí — no es seguro via ChunkData.
     */
    private void decorateFloorCapeTop(ChunkData chunk, int x, int y, int z, Random r) {
        if (r.nextInt(1000) < 8) {
            if (r.nextBoolean()) {
                org.bukkit.block.data.type.SculkShrieker sh =
                        (org.bukkit.block.data.type.SculkShrieker)
                                Material.SCULK_SHRIEKER.createBlockData();
                sh.setCanSummon(true);
                chunk.setBlock(x, y + 1, z, sh);
            } else {
                chunk.setBlock(x, y + 1, z, Material.SCULK_SENSOR);
            }
        }
    }

    private void decorateCeiling(ChunkData chunk, int x, int y, int z, Random r) {
        if (r.nextInt(100) < 5) {
            chunk.setBlock(x, y, z, Material.SCULK);
            AmethystCluster bud = r.nextBoolean()
                    ? (AmethystCluster) Material.MEDIUM_AMETHYST_BUD.createBlockData()
                    : (AmethystCluster) Material.LARGE_AMETHYST_BUD.createBlockData();
            bud.setFacing(BlockFace.DOWN);
            chunk.setBlock(x, y - 1, z, bud);
        } else {
            chunk.setBlock(x, y, z, r.nextDouble() < 0.015
                    ? Material.CRYING_OBSIDIAN : Material.SCULK);
        }
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world) {
        return Collections.singletonList(new WardenPopulator(plugin));
    }
}