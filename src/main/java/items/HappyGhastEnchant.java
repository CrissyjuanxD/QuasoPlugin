package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HappyGhast;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HappyGhastEnchant implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey enchantKey;
    private static final UUID FAST_FLIGHT_UUID = UUID.fromString("11a11a11-22b2-33c3-44d4-55e55e55e55e");

    public HappyGhastEnchant(JavaPlugin plugin) {
        this.plugin = plugin;
        this.enchantKey = new NamespacedKey(plugin, "fast_flight_level");

        iniciarControlDeVuelo();
    }

    public ItemStack createFastFlightBook(int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#AEECEF") + "Libro Encantado");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Vuelo Rápido " + (level == 1 ? "I" : "II"));
            lore.add("");
            lore.add(ChatColor.of("#6BD2D6") + "Combínalo con un Harness");
            lore.add(ChatColor.of("#6BD2D6") + "en un Yunque.");
            lore.add(ChatColor.of("#6BD2D6") + "Sirve para mejorar la velocidad de vuelo ");
            lore.add(ChatColor.of("#6BD2D6") + "de los Happy Ghasts.");

            meta.setLore(lore);
            meta.getPersistentDataContainer().set(enchantKey, PersistentDataType.INTEGER, Math.min(level, 2));
            book.setItemMeta(meta);
        }
        return book;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack leftItem = event.getInventory().getItem(0);
        ItemStack rightItem = event.getInventory().getItem(1);

        if (leftItem == null || rightItem == null) return;

        if (leftItem.getType() == Material.ENCHANTED_BOOK && rightItem.getType() == Material.ENCHANTED_BOOK) {
            int leftLevel = getCustomEnchantLevel(leftItem);
            int rightLevel = getCustomEnchantLevel(rightItem);

            if (leftLevel > 0 && rightLevel > 0) {
                if (leftLevel == 1 && rightLevel == 1) {
                    ItemStack result = createFastFlightBook(2);
                    event.setResult(result);

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        event.getInventory().setRepairCost(5);
                    });
                }
            }
            return;
        }

        if (leftItem.getType().name().endsWith("_HARNESS")) {
            if (rightItem.getType() == Material.ENCHANTED_BOOK && rightItem.hasItemMeta()) {
                int bookLevel = getCustomEnchantLevel(rightItem);

                if (bookLevel > 0) {
                    int currentLevel = getCustomEnchantLevel(leftItem);

                    if (bookLevel > currentLevel) {
                        ItemStack result = leftItem.clone();
                        ItemMeta resultMeta = result.getItemMeta();

                        resultMeta.getPersistentDataContainer().set(enchantKey, PersistentDataType.INTEGER, bookLevel);

                        List<String> lore = resultMeta.hasLore() ? resultMeta.getLore() : new ArrayList<>();
                        lore.removeIf(line -> ChatColor.stripColor(line).contains("Vuelo Rápido"));
                        lore.add(0, ChatColor.GRAY + "Vuelo Rápido " + (bookLevel == 1 ? "I" : "II"));
                        resultMeta.setLore(lore);

                        resultMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
                        resultMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                        result.setItemMeta(resultMeta);
                        event.setResult(result);

                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            int cost = (event.getInventory().getRepairCost() > 0) ? event.getInventory().getRepairCost() : 0;
                            event.getInventory().setRepairCost(cost + (bookLevel * 5));
                        });
                    }
                }
            }
        }
    }

    private void iniciarControlDeVuelo() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (HappyGhast ghast : world.getEntitiesByClass(HappyGhast.class)) {

                        ItemStack harness = getHarnessFromGhast(ghast);
                        int level = getCustomEnchantLevel(harness);

                        AttributeInstance speedAttr = ghast.getAttribute(Attribute.FLYING_SPEED);
                        if (speedAttr == null) {
                            speedAttr = ghast.getAttribute(Attribute.MOVEMENT_SPEED);
                        }

                        if (speedAttr != null) {
                            speedAttr.getModifiers().stream()
                                    .filter(m -> m.getUniqueId().equals(FAST_FLIGHT_UUID))
                                    .forEach(speedAttr::removeModifier);

                            if (level > 0) {
                                double bonus = (level == 1) ? 0.5 : 1.0;
                                AttributeModifier modifier = new AttributeModifier(
                                        FAST_FLIGHT_UUID,
                                        "fast_flight_bonus",
                                        bonus,
                                        AttributeModifier.Operation.ADD_SCALAR
                                );
                                speedAttr.addModifier(modifier);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private ItemStack getHarnessFromGhast(HappyGhast ghast) {
        if (ghast.getEquipment() == null) return null;

        ItemStack item = ghast.getEquipment().getItem(EquipmentSlot.SADDLE);
        if (item == null || item.getType() == Material.AIR) {
            item = ghast.getEquipment().getItem(EquipmentSlot.BODY);
        }
        return item;
    }

    private int getCustomEnchantLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(enchantKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }
}