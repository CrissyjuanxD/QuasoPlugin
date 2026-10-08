package EndBiomes;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// La isla del dragón con toques nuevos (la idea es la de Stellarity): vetas de basalto y manchas de obsidiana en el
// suelo, las torres de obsidiana con la base ensanchada y vetas de obsidiana llorosa, y alrededor del portal de salida
// una plaza redonda con un anillo de vidrio de colores, marcos en los 4 lugares de los cristales y 4 obeliscos. Se
// hace una sola vez sobre lo que ya está generado (las torres y el portal son los del juego, así el ritual de vanilla
// sigue funcionando igual)
public class EndIslaPrincipal {

    private static final int VERSION = 1;
    private static final int ISLAND_RADIUS = 112;
    private static final int PLAZA_RADIUS = 10;

    private static final Material[] VIDRIO = {
            Material.MAGENTA_STAINED_GLASS, Material.PURPLE_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS,
            Material.CYAN_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.PINK_STAINED_GLASS};

    private final JavaPlugin plugin;

    public EndIslaPrincipal(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Centro de cada una de las 10 torres: el juego las pone siempre en un círculo de radio 42
    static int[][] torres() {
        int[][] centers = new int[10][2];
        for (int i = 0; i < 10; i++) {
            double a = 2.0 * (-Math.PI + Math.PI / 10 * i);
            centers[i][0] = (int) Math.floor(42.0 * Math.cos(a));
            centers[i][1] = (int) Math.floor(42.0 * Math.sin(a));
        }
        return centers;
    }

    // Arreglo del suelo y las torres: de a 4 chunks por tick para no trabar el server
    public void decorar(World world) {
        if (plugin.getConfig().getInt("end.isla_principal", 0) >= VERSION) return;
        List<int[]> chunks = new ArrayList<>();
        int r = ISLAND_RADIUS >> 4;
        for (int cx = -r - 1; cx <= r; cx++) {
            for (int cz = -r - 1; cz <= r; cz++) chunks.add(new int[]{cx, cz});
        }
        int[][] centers = torres();
        int[] radios = new int[10];
        Random random = new Random(world.getSeed() ^ 0xD1A60L);
        SimplexOctaveGenerator vetas = noise(world.getSeed() + 77, 1.0 / 24);
        SimplexOctaveGenerator manchas = noise(world.getSeed() + 78, 1.0 / 12);
        SimplexOctaveGenerator lomas = noise(world.getSeed() + 79, 1.0 / 18);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            for (int n = 0; n < 4 && !chunks.isEmpty(); n++) {
                int[] c = chunks.removeFirst();
                world.getChunkAt(c[0], c[1]);
            }
            if (!chunks.isEmpty()) return;
            task.cancel();
            for (int i = 0; i < 10; i++) radios[i] = radio(world, centers[i][0], centers[i][1]);
            for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
                for (int z = -ISLAND_RADIUS; z <= ISLAND_RADIUS; z++) {
                    if (x * x + z * z > ISLAND_RADIUS * ISLAND_RADIUS || x * x + z * z <= (PLAZA_RADIUS + 3) * (PLAZA_RADIUS + 3)) continue;
                    if (cercaDeTorre(x, z, centers, radios)) continue;
                    suelo(world, random, x, z, vetas, manchas, lomas);
                }
            }
            for (int i = 0; i < 10; i++) torre(world, random, centers[i][0], centers[i][1], radios[i]);
            plugin.getConfig().set("end.isla_principal", VERSION);
            plugin.saveConfig();
            plugin.getLogger().info("Isla principal del End decorada.");
        }, 1L, 1L);
    }

    private static SimplexOctaveGenerator noise(long seed, double scale) {
        SimplexOctaveGenerator g = new SimplexOctaveGenerator(new Random(seed), 2);
        g.setScale(scale);
        return g;
    }

    private static boolean cercaDeTorre(int x, int z, int[][] centers, int[] radios) {
        for (int i = 0; i < centers.length; i++) {
            int dx = x - centers[i][0], dz = z - centers[i][1];
            int r = radios[i] + 5;
            if (dx * dx + dz * dz <= r * r) return true;
        }
        return false;
    }

    // Radio de la torre midiendo la obsidiana unos bloques arriba del suelo (0 si no hay torre)
    private static int radio(World world, int cx, int cz) {
        int y = suelo(world, cx + 9, cz) + 4;
        if (world.getBlockAt(cx, y, cz).getType() != Material.OBSIDIAN) return 0;
        int r = 0;
        while (r < 8 && world.getBlockAt(cx + r + 1, y, cz).getType() == Material.OBSIDIAN) r++;
        return r;
    }

    private static int suelo(World world, int x, int z) {
        for (int y = 90; y > 10; y--) {
            if (world.getBlockAt(x, y, z).getType() == Material.END_STONE) return y;
        }
        return 50;
    }

    // Suelo de la isla: vetas finas de basalto con borde de basalto liso, manchas de obsidiana y lomitas de end stone
    private static void suelo(World world, Random random, int x, int z, SimplexOctaveGenerator vetas,
                              SimplexOctaveGenerator manchas, SimplexOctaveGenerator lomas) {
        int top = world.getHighestBlockYAt(x, z);
        Block block = world.getBlockAt(x, top, z);
        if (block.getType() != Material.END_STONE) return;
        double v = vetas.noise(x, z, 0.5, 0.5, true);
        double m = manchas.noise(x, z, 0.5, 0.5, true);
        if (Math.abs(v) < 0.03) {
            Orientable basalt = (Orientable) Material.BASALT.createBlockData();
            basalt.setAxis(Axis.Y);
            block.setBlockData(basalt, false);
        } else if (Math.abs(v) < 0.05) {
            block.setType(Material.SMOOTH_BASALT, false);
        } else if (m > 0.72) {
            block.setType(Material.CRYING_OBSIDIAN, false);
        } else if (m > 0.6) {
            block.setType(Material.OBSIDIAN, false);
        } else {
            double l = lomas.noise(x, z, 0.5, 0.5, true);
            int extra = l > 0.68 ? 2 : l > 0.48 ? 1 : 0;
            for (int i = 1; i <= extra; i++) {
                if (!world.getBlockAt(x, top + i, z).getType().isAir()) break;
                world.getBlockAt(x, top + i, z).setType(Material.END_STONE, false);
            }
        }
    }

    // Base ensanchada de obsidiana, obsidiana llorosa y blackstone, y vetas de obsidiana llorosa subiendo por la torre
    private static void torre(World world, Random random, int cx, int cz, int radius) {
        if (radius <= 0) return;
        int ground = suelo(world, cx + radius + 4, cz);
        for (int dx = -radius - 4; dx <= radius + 4; dx++) {
            for (int dz = -radius - 4; dz <= radius + 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= radius + 0.5 || d > radius + 3.5) continue;
                int height = (int) Math.round((radius + 3.5 - d) * 2.2 + random.nextDouble());
                int base = suelo(world, cx + dx, cz + dz);
                if (Math.abs(base - ground) > 6) continue;
                for (int y = base + 1; y <= base + height; y++) {
                    Block b = world.getBlockAt(cx + dx, y, cz + dz);
                    if (!b.getType().isAir()) continue;
                    int roll = random.nextInt(100);
                    b.setType(roll < 68 ? Material.OBSIDIAN : roll < 84 ? Material.CRYING_OBSIDIAN : Material.BLACKSTONE, false);
                }
            }
        }
        for (int y = ground; y < ground + 70; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d < radius - 0.9 || d > radius + 0.5) continue;
                    Block b = world.getBlockAt(cx + dx, y, cz + dz);
                    if (b.getType() == Material.OBSIDIAN && random.nextInt(100) < 6) b.setType(Material.CRYING_OBSIDIAN, false);
                }
            }
        }
    }

    // La plaza del portal de salida, una sola vez cuando el portal ya existe
    public void plaza(World world, Location portalLocation) {
        if (plugin.getConfig().getInt("end.plaza", 0) >= VERSION) return;
        int px = portalLocation.getBlockX(), py = portalLocation.getBlockY(), pz = portalLocation.getBlockZ();
        Random random = new Random(world.getSeed() ^ 0x9A2AL);
        for (int dx = -PLAZA_RADIUS - 1; dx <= PLAZA_RADIUS + 1; dx++) {
            for (int dz = -PLAZA_RADIUS - 1; dz <= PLAZA_RADIUS + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 4 || d > PLAZA_RADIUS + 0.5) continue;
                int x = px + dx, z = pz + dz;
                // Piso parejo a la altura del portal: se rellena abajo y se saca la end stone que sobresale
                for (int y = py - 3; y < py; y++) {
                    if (world.getBlockAt(x, y, z).getType().isAir()) world.getBlockAt(x, y, z).setType(Material.END_STONE, false);
                }
                for (int y = py + 1; y <= py + 6; y++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() == Material.END_STONE || b.getType() == Material.CHORUS_PLANT || b.getType() == Material.CHORUS_FLOWER) b.setType(Material.AIR, false);
                }
                world.getBlockAt(x, py, z).setType(piso(d, Math.atan2(dz, dx), random), false);
            }
        }
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            marco(world, px, py, pz, face);
        }
        for (int[] diag : new int[][]{{7, 7}, {-7, 7}, {7, -7}, {-7, -7}}) obelisco(world, px + diag[0], py, pz + diag[1]);
        plugin.getConfig().set("end.plaza", VERSION);
        plugin.saveConfig();
    }

    // Anillos del piso desde adentro: ladrillos de blackstone, obsidiana con obsidiana llorosa en los 8 puntos,
    // vidrio de colores, blackstone y un borde de ladrillos de end stone
    static Material piso(double d, double angle, Random random) {
        if (d < 5.5) return Material.POLISHED_BLACKSTONE_BRICKS;
        if (d < 6.5) {
            double eighth = Math.abs(((angle / (Math.PI / 4)) % 1 + 1) % 1 - 0.5);
            return eighth > 0.38 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
        }
        if (d < 7.5) {
            int index = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * VIDRIO.length * 2);
            return VIDRIO[Math.floorMod(index, VIDRIO.length)];
        }
        if (d < 9.5) return random.nextInt(5) == 0 ? Material.BLACKSTONE : Material.POLISHED_BLACKSTONE;
        return Material.END_STONE_BRICKS;
    }

    // Detrás de cada lugar de cristal: escalones de pizarra a los costados y una losa atrás
    private static void marco(World world, int px, int py, int pz, BlockFace face) {
        int bx = px + face.getModX() * 4, bz = pz + face.getModZ() * 4;
        Slab slab = (Slab) Material.POLISHED_DEEPSLATE_SLAB.createBlockData();
        slab.setType(Slab.Type.BOTTOM);
        world.getBlockAt(bx, py + 1, bz).setBlockData(slab, false);
        BlockFace side = face == BlockFace.NORTH || face == BlockFace.SOUTH ? BlockFace.EAST : BlockFace.NORTH;
        for (int s = -1; s <= 1; s += 2) {
            int sx = px + face.getModX() * 3 + side.getModX() * s * 2;
            int sz = pz + face.getModZ() * 3 + side.getModZ() * s * 2;
            Stairs stairs = (Stairs) Material.POLISHED_DEEPSLATE_STAIRS.createBlockData();
            stairs.setFacing(s > 0 ? side.getOppositeFace() : side);
            world.getBlockAt(sx, py + 1, sz).setBlockData(stairs, false);
        }
    }

    // Obelisco de obsidiana con una franja de obsidiana llorosa y una vara del End arriba
    private static void obelisco(World world, int x, int py, int z) {
        for (int y = py + 1; y <= py + 5; y++) {
            world.getBlockAt(x, y, z).setType(y == py + 3 ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN, false);
        }
        Directional rod = (Directional) Material.END_ROD.createBlockData();
        rod.setFacing(BlockFace.UP);
        world.getBlockAt(x, py + 6, z).setBlockData(rod, false);
        for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
            Stairs stairs = (Stairs) Material.POLISHED_BLACKSTONE_STAIRS.createBlockData();
            stairs.setFacing(face.getOppositeFace());
            Block b = world.getBlockAt(x + face.getModX(), py + 1, z + face.getModZ());
            if (b.getType().isAir()) b.setBlockData(stairs, false);
        }
    }
}
