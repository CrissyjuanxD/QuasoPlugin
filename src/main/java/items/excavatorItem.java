package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class excavatorItem implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey excavatorKey;

    private final Set<UUID> activeMiners = new HashSet<>();

    public excavatorItem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.excavatorKey = new NamespacedKey(plugin, "la_excavadora");
    }

    public ItemStack createExcavator() {
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#7d4dac") + ChatColor.BOLD.toString() + "La Excavadora");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#5692d2") + "Con este pico podrás");
            lore.add(ChatColor.of("#5692d2") + "minar " + ChatColor.of("#d06e39") + ChatColor.BOLD + "3 x 3 " + ChatColor.of("#5692d2") + "bloques.");
            lore.add("");
            lore.add(ChatColor.of("#cd5dd5") + "Solo aplica a bloques");
            lore.add(ChatColor.of("#cd5dd5") + "de minería");
            lore.add("");
            meta.setLore(lore);

            AttributeModifier modifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "block_interaction_range",
                    5.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND
            );
            meta.addAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, modifier);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(excavatorKey, PersistentDataType.BYTE, (byte) 1);

            pickaxe.setItemMeta(meta);
        }
        return pickaxe;
    }

    public boolean isExcavator(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) return false;
        if (!item.hasItemMeta()) return false;
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        return data.has(excavatorKey, PersistentDataType.BYTE);
    }

    private boolean isWhitelisted(Material type) {
        return Tag.MINEABLE_PICKAXE.isTagged(type);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (activeMiners.contains(uuid)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isExcavator(item)) return;

        Block centerBlock = event.getBlock();
        if (!isWhitelisted(centerBlock.getType())) return;

        double range = player.getGameMode() == org.bukkit.GameMode.CREATIVE ? 10.0 : 9.5;
        RayTraceResult trace = player.rayTraceBlocks(range, FluidCollisionMode.NEVER);
        BlockFace face = (trace != null && trace.getHitBlockFace() != null) ? trace.getHitBlockFace() : BlockFace.UP;

        activeMiners.add(uuid);
        int blocksBroken = 0;

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;

                int x = 0, y = 0, z = 0;
                switch (face) {
                    case UP:
                    case DOWN:
                        x = a; z = b; break;
                    case NORTH:
                    case SOUTH:
                        x = a; y = b; break;
                    case EAST:
                    case WEST:
                        z = a; y = b; break;
                    default:
                        x = a; y = b; break;
                }

                Block target = centerBlock.getRelative(x, y, z);

                if (isWhitelisted(target.getType())) {

                    BlockBreakEvent fakeEvent = new BlockBreakEvent(target, player);
                    Bukkit.getPluginManager().callEvent(fakeEvent);

                    if (!fakeEvent.isCancelled()) {
                        target.breakNaturally(item);
                        blocksBroken++;
                    }
                }
            }
        }

        if (blocksBroken > 0 && player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            applyDurabilityDamage(player, item, blocksBroken);
        }

        activeMiners.remove(uuid);
    }

    private void applyDurabilityDamage(Player player, ItemStack item, int blocksBroken) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
            int damageToApply = 0;

            for (int i = 0; i < blocksBroken; i++) {
                if (Math.random() < (1.0 / (unbreaking + 1))) {
                    damageToApply++;
                }
            }

            if (damageToApply > 0) {
                damageable.setDamage(damageable.getDamage() + damageToApply);
                item.setItemMeta(damageable);

                if (damageable.getDamage() > item.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }
}