package Armors;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Botas de Warden Escaladoras: en el aire y pegado a una pared el jugador se queda agarrado (baja lento hasta 2.5
// segundos) y con salto se impulsa hacia arriba y lejos de la pared, como parkour. Con shift se suelta
public class BotasEscaladoras implements Listener {

    static final int MAX_AGARRE = 50;
    static final double CAIDA_AGARRADO = -0.08;
    static final double IMPULSO_AFUERA = 0.45;
    static final double IMPULSO_ARRIBA = 0.62;
    private static final long ESPERA_MS = 250;
    private static final BlockFace[] LADOS = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private static final Particle.DustOptions POLVO = new Particle.DustOptions(Color.fromRGB(0x5fd16a), 1.0f);

    private final Map<UUID, Integer> agarre = new HashMap<>();
    private final Map<UUID, Long> ultimoSalto = new HashMap<>();
    private final Map<UUID, Boolean> saltando = new HashMap<>();

    static boolean puede(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle() || player.isSwimming()) return false;
        return WardenArmor.isClimbing(player.getInventory().getBoots());
    }

    // La pared que toca el jugador (a los pies o a la altura del pecho) o null
    static BlockFace pared(Player player) {
        Location loc = player.getLocation();
        for (BlockFace lado : LADOS) {
            for (double alto : new double[]{0.2, 1.1}) {
                Block block = loc.clone().add(lado.getModX() * 0.45, alto, lado.getModZ() * 0.45).getBlock();
                if (block.isSolid() && !block.isPassable()) return lado;
            }
        }
        return null;
    }

    // Pegado a la pared cae despacio; en el piso se recupera el agarre
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOnGround() || !puede(player)) {
            agarre.remove(player.getUniqueId());
            return;
        }
        Vector vel = player.getVelocity();
        if (vel.getY() >= CAIDA_AGARRADO || player.isSneaking() || pared(player) == null) return;
        int ticks = agarre.merge(player.getUniqueId(), 1, Integer::sum);
        if (ticks > MAX_AGARRE) return;
        player.setVelocity(vel.setY(CAIDA_AGARRADO).multiply(new Vector(0.6, 1, 0.6)));
        player.setFallDistance(0);
        if (ticks % 4 == 0) {
            player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 3, 0.25, 0.3, 0.25, 0, POLVO);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_HONEY_BLOCK_SLIDE, 0.35f, 1.3f);
        }
    }

    // Al apretar salto en el aire junto a una pared: impulso hacia arriba y hacia afuera
    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        boolean salto = event.getInput().isJump();
        boolean antes = Boolean.TRUE.equals(saltando.put(player.getUniqueId(), salto));
        if (!salto || antes || player.isOnGround() || !puede(player)) return;
        BlockFace lado = pared(player);
        if (lado == null) return;
        long ahora = System.currentTimeMillis();
        Long ultimo = ultimoSalto.get(player.getUniqueId());
        if (ultimo != null && ahora - ultimo < ESPERA_MS) return;
        ultimoSalto.put(player.getUniqueId(), ahora);
        agarre.remove(player.getUniqueId());

        player.setVelocity(impulso(lado, player.getLocation().getDirection()));
        player.setFallDistance(0);
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.SCULK_CHARGE_POP, loc.clone().add(0, 0.3, 0), 10, 0.25, 0.2, 0.25, 0.02);
        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(lado.getModX() * 0.4, 0.6, lado.getModZ() * 0.4),
                8, 0.1, 0.4, 0.1, 0, POLVO);
        player.getWorld().playSound(loc, Sound.BLOCK_SCULK_STEP, 0.9f, 1.4f);
        player.getWorld().playSound(loc, Sound.ENTITY_BREEZE_JUMP, 0.5f, 1.6f);
    }

    // Lejos de la pared y un poco hacia donde mira el jugador
    static Vector impulso(BlockFace pared, Vector mirada) {
        Vector afuera = new Vector(-pared.getModX(), 0, -pared.getModZ()).multiply(IMPULSO_AFUERA);
        Vector hacia = mirada.clone().setY(0);
        if (hacia.lengthSquared() > 0) afuera.add(hacia.normalize().multiply(0.15));
        return afuera.setY(IMPULSO_ARRIBA);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        agarre.remove(id);
        ultimoSalto.remove(id);
        saltando.remove(id);
    }
}
