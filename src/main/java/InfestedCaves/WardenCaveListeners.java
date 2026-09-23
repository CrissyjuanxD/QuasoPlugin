package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WardenCaveListeners implements Listener {
    private final QuasoPlugin plugin;
    private final PortalManager portalManager;
    private final StructureManager structureManager;
    private boolean templeGenerated = false;

    public WardenCaveListeners(QuasoPlugin plugin, PortalManager portalManager, StructureManager structureManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 255));
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;

        if (e.getChunk().getX() == 0 && e.getChunk().getZ() == 0 && !templeGenerated) {
            plugin.getLogger().info("Chunk 0,0 cargado. Generando templo...");
            structureManager.pasteTempleAtSpawn(e.getWorld());
            templeGenerated = true;
        }

        if (e.isNewChunk()) {
            structureManager.tryGenerateAncientCity(e.getChunk());
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (!e.getLocation().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        EntityType type = e.getEntityType();
        if (type != EntityType.ZOMBIE &&
                type != EntityType.CREEPER &&
                type != EntityType.SPIDER &&
                type != EntityType.SKELETON &&
                type != EntityType.BLOCK_DISPLAY &&
                type != EntityType.PLAYER) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBedInteract(PlayerInteractEvent e) {
        if (!e.getPlayer().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock() != null &&
                e.getClickedBlock().getType().name().contains("BED")) {
            if (e.getPlayer().isSneaking() && e.getItem() != null && e.getItem().getType().isBlock()) {
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cNo puedes dormir ni guardar spawn en esta dimensión maldita.");
        }
    }
}