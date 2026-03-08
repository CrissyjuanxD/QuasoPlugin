package items;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifeCampfire implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey campfireKey;
    private final NamespacedKey fuelKey;

    // Registro de todas las fogatas activas en el mundo
    private final Map<Location, ActiveCampfire> activeCampfires = new HashMap<>();

    public LifeCampfire(JavaPlugin plugin) {
        this.plugin = plugin;
        this.campfireKey = new NamespacedKey(plugin, "life_campfire");
        this.fuelKey = new NamespacedKey(plugin, "life_fuel");
    }

    // ========================================================================================
    // CREACIÓN DE LOS ÍTEMS
    // ========================================================================================

    public ItemStack createCampfire() {
        ItemStack item = new ItemStack(Material.CAMPFIRE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#f1cc98") + ChatColor.BOLD.toString() + "Fogata de la Vida");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#dd6446") + "Esta fogata otorga en grupo:");
            lore.add("");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ec6565") + "Regeneración III");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#73b0ce") + "Resistencia III");
            lore.add(ChatColor.GRAY + "> " + ChatColor.of("#ede592") + "Health Boost III");
            lore.add("");
            lore.add(ChatColor.of("#ec947e") + "Su radio es de " + ChatColor.of("#ec947e") + ChatColor.BOLD + "16×16" + ChatColor.of("#ec947e") + " y se reduce");
            lore.add(ChatColor.of("#ec947e") + "1 bloque cada 10 segundos,");
            lore.add(ChatColor.of("#ec947e") + ChatColor.BOLD.toString() + "debilitando los efectos" + ChatColor.of("#ec947e") + ".");
            lore.add("");
            lore.add(ChatColor.of("#ec947e") + "Para avivarla usa " + ChatColor.of("#ee5b2b") + ChatColor.BOLD + "Combustible" + ChatColor.of("#ec947e") + ".");
            lore.add("");
            lore.add(ChatColor.of("#b4b4b1") + "Cuando pierde todo su radio");
            lore.add(ChatColor.of("#b4b4b1") + "o permanece encendida 4 minutos,");
            lore.add(ChatColor.of("#b4b4b1") + "se " + ChatColor.of("#b4b4b1") + ChatColor.BOLD + "romperá" + ChatColor.of("#b4b4b1") + ".");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(campfireKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createFuel() {
        ItemStack item = new ItemStack(Material.TORCHFLOWER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#ee5b2b") + ChatColor.BOLD.toString() + "Combustible");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.of("#c27070") + "Este combustible será el encargado");
            lore.add(ChatColor.of("#c27070") + "de " + ChatColor.of("#c74848") + ChatColor.BOLD + "avivar" + ChatColor.of("#c27070") + " la llama de la:");
            lore.add("");
            lore.add(ChatColor.GRAY + ChatColor.BOLD.toString() + "> " + ChatColor.of("#f1cc98") + ChatColor.BOLD + "Fogata de la Vida");
            lore.add("");
            lore.add(ChatColor.of("#7bb0d1") + "Incrementa en " + ChatColor.of("#7bb0d1") + ChatColor.BOLD + "2 bloques");
            lore.add(ChatColor.of("#7bb0d1") + "el radio de una fogata encendida.");

            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(fuelKey, PersistentDataType.BYTE, (byte) 1);

            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCampfire(ItemStack item) {
        if (item == null || item.getType() != Material.CAMPFIRE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(campfireKey, PersistentDataType.BYTE);
    }

    public boolean isFuel(ItemStack item) {
        if (item == null || item.getType() != Material.TORCHFLOWER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(fuelKey, PersistentDataType.BYTE);
    }

    // ========================================================================================
    // EVENTOS DE COLOCACIÓN E INTERACCIÓN
    // ========================================================================================

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isCampfire(event.getItemInHand())) {
            Location loc = event.getBlockPlaced().getLocation();
            ActiveCampfire campfire = new ActiveCampfire(loc);
            activeCampfires.put(loc, campfire);

            event.getPlayer().sendMessage(ChatColor.of("#f1cc98") + "۞ " + ChatColor.GRAY + "Has encendido la " + ChatColor.of("#f1cc98") + ChatColor.BOLD + "Fogata de la Vida" + ChatColor.GRAY + ".");
            loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();

        if (isFuel(item)) {
            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Block clickedBlock = event.getClickedBlock();

                if (clickedBlock != null && clickedBlock.getType() == Material.CAMPFIRE) {
                    Location loc = clickedBlock.getLocation();

                    if (activeCampfires.containsKey(loc)) {
                        ActiveCampfire campfire = activeCampfires.get(loc);

                        item.setAmount(item.getAmount() - 1);

                        campfire.addFuel();

                        String msg = "[\"\",{\"text\":\"+2 Bloques de Radio\",\"bold\":true,\"color\":\"#ee5b2b\"}]";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + event.getPlayer().getName() + " actionbar " + msg);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (activeCampfires.containsKey(loc)) {
            activeCampfires.get(loc).destroy(false);
            event.setDropItems(false);
        }
    }

    // ========================================================================================
    // LÓGICA INTERNA DE LA FOGATA
    // ========================================================================================

    private class ActiveCampfire {
        private final Location loc;
        private int radius;
        private int ticksLived;
        private BukkitTask task;

        public ActiveCampfire(Location loc) {
            this.loc = loc;
            this.radius = 16;
            this.ticksLived = 0;
            start();
        }

        public void addFuel() {
            radius += 2;
            if (radius > 16) radius = 16;

            loc.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
            loc.getWorld().playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 2f, 1.5f);
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 1.0, 0.5), 30, 0.3, 0.3, 0.3, 0.1);
        }

        private void start() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (loc.getBlock().getType() != Material.CAMPFIRE) {
                        destroy(false);
                        return;
                    }

                    ticksLived += 5;

                    if (ticksLived >= 4800 || radius <= 0) {
                        destroy(true);
                        return;
                    }

                    if (ticksLived % 200 == 0) {
                        radius--;
                        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.4f, 0.8f);
                    }

                    drawRingParticles();

                    if (ticksLived % 20 == 0) {
                        applyEffects();
                    }
                }
            }.runTaskTimer(plugin, 0L, 5L);
        }

        private void applyEffects() {
            int amplifier;

            if (radius <= 6) {
                amplifier = 0; // Nivel I
            } else if (radius <= 12) {
                amplifier = 1; // Nivel II
            } else {
                amplifier = 2; // Nivel III
            }

            PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 60, amplifier, true, true);
            PotionEffect resis = new PotionEffect(PotionEffectType.RESISTANCE, 60, amplifier, true, true);
            PotionEffect healthBoost = new PotionEffect(PotionEffectType.HEALTH_BOOST, 60, amplifier, true, true);

            double cx = loc.getX() + 0.5;
            double cz = loc.getZ() + 0.5;

            for (Player p : loc.getWorld().getPlayers()) {
                double dx = Math.abs(p.getLocation().getX() - cx);
                double dz = Math.abs(p.getLocation().getZ() - cz);

                if (dx <= radius && dz <= radius) {
                    p.addPotionEffect(regen);
                    p.addPotionEffect(resis);
                    p.addPotionEffect(healthBoost);
                }
            }
        }

        private void drawRingParticles() {
            World w = loc.getWorld();

            Particle.DustOptions yellow = new Particle.DustOptions(Color.fromRGB(240, 230, 90), 1.2f);
            Particle.DustOptions green = new Particle.DustOptions(Color.fromRGB(130, 230, 100), 1.2f);
            Particle.DustOptions orange = new Particle.DustOptions(Color.fromRGB(240, 150, 60), 0.8f);

            double[] heights = {0.5, 1.5, 2.5};

            double step = 1.5;

            for (double x = -radius; x <= radius; x += step) {
                spawnParticleColumn(w, loc.clone().add(x + 0.5, 0, -radius + 0.5), heights, yellow, green, orange);
                spawnParticleColumn(w, loc.clone().add(x + 0.5, 0, radius + 0.5), heights, yellow, green, orange);
            }
            for (double z = -radius; z <= radius; z += step) {
                spawnParticleColumn(w, loc.clone().add(-radius + 0.5, 0, z + 0.5), heights, yellow, green, orange);
                spawnParticleColumn(w, loc.clone().add(radius + 0.5, 0, z + 0.5), heights, yellow, green, orange);
            }
        }

        private void spawnParticleColumn(World w, Location basePoint, double[] heights, Particle.DustOptions c1, Particle.DustOptions c2, Particle.DustOptions c3) {
            for (double h : heights) {
                Location pLoc = basePoint.clone().add(0, h, 0);

                w.spawnParticle(Particle.ELECTRIC_SPARK, pLoc, 1, 0, 0, 0, 0);

                double rand = Math.random();
                Particle.DustOptions chosenColor = rand < 0.45 ? c1 : (rand < 0.90 ? c2 : c3);

                w.spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, chosenColor);
            }
        }

        public void destroy(boolean natural) {
            if (task != null) task.cancel();

            if (natural) {
                loc.getBlock().setType(Material.AIR);
                loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.5f);
                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0.5, 0.5, 0.5), 50, 0.5, 1.0, 0.5, 0.05);
            }

            activeCampfires.remove(loc);
        }
    }
}