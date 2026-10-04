package InfestedCaves;

import imp.crissyjuanxd.QuasoPlugin;
import items.WardenCaveItems;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class WardenCaveListeners implements Listener {

    private final QuasoPlugin plugin;
    private final PortalManager portalManager;
    private final StructureManager structureManager;
    private final NamespacedKey templeKey;
    private final Random random = new Random();

    public WardenCaveListeners(QuasoPlugin plugin, PortalManager portalManager, StructureManager structureManager) {
        this.plugin = plugin;
        this.portalManager = portalManager;
        this.structureManager = structureManager;
        this.templeKey = new NamespacedKey(plugin, "templo_pegado");
    }

    // Al entrar a la dimensión da resistencia y caída lenta por 10 segundos
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 255));
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 1));
        }
    }

    // Pega el templo cuando carga el chunk 0,0
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        World world = e.getWorld();
        if (!world.getName().equals(QuasoPlugin.WORLD_NAME)) return;

        if (e.getChunk().getX() == 0 && e.getChunk().getZ() == 0) {
            pasteTempleIfNeeded(world);
        }
    }

    // Pega el templo una sola vez y lo marca en el mundo, así no se vuelve a pegar en cada reinicio
    public void pasteTempleIfNeeded(World world) {
        if (world.getPersistentDataContainer().has(templeKey, PersistentDataType.BYTE)) return;
        if (!world.isChunkLoaded(0, 0)) return;
        if (structureManager.pasteTempleAtSpawn(world)) {
            world.getPersistentDataContainer().set(templeKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    // Cada Mineral Profundo suelta el mineral crudo de su bioma (Fortuna no da más)
    @EventHandler(ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!block.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;

        WardenBiome biome = WardenBiome.fromOre(block.getType());
        if (biome == null) return;

        e.setDropItems(false);
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().dropItemNaturally(loc, WardenCaveItems.createRawOre(biome.variant()));
        e.setExpToDrop(2 + random.nextInt(3));
    }

    // Las terracotas que hacen de mineral no se pueden colocar en la dimensión, si no se farmean infinitas
    @EventHandler(ignoreCancelled = true)
    public void onOrePlace(BlockPlaceEvent e) {
        if (!e.getBlock().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (WardenBiome.fromOre(e.getBlock().getType()) == null) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage("§cEse bloque no se puede colocar en la Warden Cave.");
    }

    // En la dimensión solo spawnean los mobs base de cada bioma (TwoChanges los cambia por su versión infestada),
    // los Warden de los chilladores y lo que spawnee el plugin
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {
        if (!e.getLocation().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        EntityType type = e.getEntityType();
        if (type != EntityType.ZOMBIE &&
                type != EntityType.CREEPER &&
                type != EntityType.CAVE_SPIDER &&
                type != EntityType.SKELETON &&
                type != EntityType.GHAST &&
                type != EntityType.WARDEN &&
                type != EntityType.BLOCK_DISPLAY &&
                type != EntityType.PLAYER) {
            e.setCancelled(true);
        }
    }

    // No se pueden usar camas en la dimensión
    @EventHandler
    public void onBedInteract(PlayerInteractEvent e) {
        if (!e.getPlayer().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK &&
                e.getClickedBlock() != null &&
                Tag.BEDS.isTagged(e.getClickedBlock().getType())) {
            if (e.getPlayer().isSneaking() && e.getItem() != null && e.getItem().getType().isBlock()) {
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cNo puedes dormir ni guardar spawn en esta dimensión maldita.");
        }
    }
}
