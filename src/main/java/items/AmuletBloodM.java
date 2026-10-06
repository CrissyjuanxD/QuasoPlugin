package items;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import imp.crissyjuanxd.bloodmoon.BloodMoon;
import imp.crissyjuanxd.bloodmoon.BloodMoonHordeEvent;
import imp.crissyjuanxd.bloodmoon.LocaleReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AmuletBloodM implements Listener {

    private final JavaPlugin plugin;
    private final BloodMoon bloodMoon;
    private final NamespacedKey amuletIdKey;
    private final NamespacedKey diamondTicksKey;
    private final NamespacedKey usosKey;
    private final NamespacedKey durabilityTicksKey;

    private final Map<UUID, AmuletSession> activeSessions = new HashMap<>();
    private final Map<UUID, Long> hordeMessageCooldown = new HashMap<>();

    private final int MAX_USOS = 250;

    public AmuletBloodM(JavaPlugin plugin, BloodMoon bloodMoon) {
        this.plugin = plugin;
        this.bloodMoon = bloodMoon;
        this.amuletIdKey = new NamespacedKey(plugin, "amulet_bloodmoon");
        this.diamondTicksKey = new NamespacedKey(plugin, "amulet_diamond_ticks");
        this.usosKey = new NamespacedKey(plugin, "amulet_usos");
        this.durabilityTicksKey = new NamespacedKey(plugin, "amulet_durability_ticks");
    }

    public ItemStack createAmulet() {
        ItemStack item = new ItemStack(Material.TORCHFLOWER_SEEDS);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.of("#e17575") + ChatColor.BOLD.toString() + "Amuleto Luna de Sangre");

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(amuletIdKey, PersistentDataType.BYTE, (byte) 1);
            data.set(diamondTicksKey, PersistentDataType.INTEGER, 0);
            data.set(usosKey, PersistentDataType.INTEGER, MAX_USOS);

            updateLore(meta, MAX_USOS);

            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            ItemModels.apply(meta, "amulet_bloodmoon");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateLore(ItemMeta meta, int usosActuales) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#da765d") + "Un amuleto ancestral que impide");
        lore.add(ChatColor.of("#da765d") + "la aparición de hordas de mobs");
        lore.add(ChatColor.of("#da765d") + "cerca de su portador durante");
        lore.add(ChatColor.of("#da765d") + "una " + ChatColor.of("#EF9292") + ChatColor.BOLD + "BloodMoon" + ChatColor.of("#da765d") + ".");
        lore.add("");
        lore.add(ChatColor.of("#EF9292") + ChatColor.BOLD.toString() + "⊗ " + ChatColor.of("#F4B183") + "Consume " + ChatColor.of("#EF9292") + ChatColor.BOLD + "1 uso" + ChatColor.of("#F4B183") + " cada 7.2 segundos.");
        lore.add(ChatColor.of("#EF9292") + ChatColor.BOLD.toString() + "⊗ " + ChatColor.of("#F4B183") + "Consume " + ChatColor.of("#EF9292") + ChatColor.BOLD + "1 diamante" + ChatColor.of("#F4B183") + " por minuto.");
        lore.add("");
        lore.add(ChatColor.of("#999999") + "Si no hay diamantes,");
        lore.add(ChatColor.of("#999999") + "el efecto se cancelará.");
        lore.add("");
        lore.add(ChatColor.of("#e18b75") + ChatColor.BOLD.toString() + "Usos restantes: " + ChatColor.WHITE + usosActuales + ChatColor.GRAY + " / " + MAX_USOS);
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.WHITE + ChatColor.BOLD + "Uso: " + ChatColor.WHITE + "Click Derecho usar o cancelar");

        meta.setLore(lore);
    }

    public boolean isAmulet(ItemStack item) {
        if (item == null || item.getType() != Material.TORCHFLOWER_SEEDS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(amuletIdKey, PersistentDataType.BYTE);
    }

    private boolean consumeDiamond(Player player) {
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.getType() == Material.DIAMOND && content.getAmount() > 0) {
                content.setAmount(content.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!isAmulet(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            event.setCancelled(true);
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);

            if (player.hasCooldown(Material.TORCHFLOWER_SEEDS)) {
                return;
            }

            UUID uuid = player.getUniqueId();

            if (activeSessions.containsKey(uuid)) {
                deactivateAmulet(player, getActiveAmulet(player), true);
                return;
            }

            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                LocaleReader.amuletMessage(player, "El amuleto solo funciona en el Overworld.");
                return;
            }

            if (!bloodMoon.isActive(player.getWorld())) {
                LocaleReader.amuletMessage(player, "El amuleto solo se puede activar durante una BloodMoon.");
                return;
            }

            activateAmulet(player, item);
        }
    }

    private void activateAmulet(Player player, ItemStack amulet) {
        ItemMeta meta = amulet.getItemMeta();
        if (meta == null) return;

        int savedDiamondTicks = Math.clamp(meta.getPersistentDataContainer().getOrDefault(diamondTicksKey, PersistentDataType.INTEGER, 0), 0, 1199);
        int usosActuales = meta.getPersistentDataContainer().getOrDefault(usosKey, PersistentDataType.INTEGER, MAX_USOS);

        if (usosActuales <= 0) {
            LocaleReader.amuletMessage(player, "¡El amuleto está gastado y ya no tiene usos!");
            amulet.setAmount(0);
            return;
        }

        boolean justPaid = false;
        if (savedDiamondTicks == 0) {
            if (!consumeDiamond(player)) {
                LocaleReader.amuletMessage(player, "¡No tienes diamantes para activar el amuleto!");
                return;
            }
            justPaid = true;
        }

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        amulet.setItemMeta(meta);

        int savedDurabilityTicks = Math.clamp(meta.getPersistentDataContainer().getOrDefault(durabilityTicksKey, PersistentDataType.INTEGER, 0), 0, 143);
        AmuletSession session = new AmuletSession(player, amulet, justPaid ? 1 : savedDiamondTicks, savedDurabilityTicks);
        if (justPaid) {
            session.triggerDiamondMessage();
        }
        activeSessions.put(player.getUniqueId(), session);

        player.setCooldown(Material.TORCHFLOWER_SEEDS, 60);
        playAuraAnimation(player, true);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        LocaleReader.amuletMessage(player, "Has activado el Amuleto Luna de Sangre.");

        sendActionBar(player, session.showDiamondMessageTicks > 0);
    }

    private void deactivateAmulet(Player player, ItemStack amulet, boolean notify) {
        AmuletSession session = activeSessions.remove(player.getUniqueId());
        hordeMessageCooldown.remove(player.getUniqueId());
        if (session != null) {
            session.cancelTask();
            if (amulet == null) amulet = session.originalAmulet;

            if (amulet != null && amulet.hasItemMeta()) {
                ItemMeta meta = amulet.getItemMeta();
                meta.getPersistentDataContainer().set(diamondTicksKey, PersistentDataType.INTEGER, session.getDiamondTicks());
                meta.getPersistentDataContainer().set(durabilityTicksKey, PersistentDataType.INTEGER, session.durabilityTicks);
                meta.removeEnchant(Enchantment.UNBREAKING);
                amulet.setItemMeta(meta);
            }
        }

        if (notify) {
            playAuraAnimation(player, false);
            player.setCooldown(Material.TORCHFLOWER_SEEDS, 80);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
            LocaleReader.amuletMessage(player, "Has desactivado el Amuleto Luna de Sangre.");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    private void sendActionBar(Player player, boolean isDiamondPaid) {
        String state = isDiamondPaid ? "-1 diamante" : "Activado";
        LocaleReader.actionBar(player, LocaleReader.ORANGE + "Amuleto Luna de Sangre: " + LocaleReader.RED + state);
    }

    private void playAuraAnimation(Player player, boolean isActivation) {
        new BukkitRunnable() {
            double yOffset = isActivation ? 0.0 : 2.2;
            final double step = 0.15;
            final double radius = 1.0;

            Particle.DustOptions color1 = new Particle.DustOptions(isActivation ? org.bukkit.Color.fromRGB(244, 177, 131) : org.bukkit.Color.fromRGB(239, 146, 146), 1.2f);
            Particle.DustOptions color2 = new Particle.DustOptions(org.bukkit.Color.WHITE, 1.2f);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                org.bukkit.Location loc = player.getLocation().add(0, yOffset, 0);
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    loc.add(x, 0, z);

                    Particle.DustOptions dust = (i % 2 == 0) ? color1 : color2;
                    player.getWorld().spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);

                    loc.subtract(x, 0, z);
                }

                if (isActivation) {
                    yOffset += step;
                    if (yOffset > 2.2) this.cancel();
                } else {
                    yOffset -= step;
                    if (yOffset < 0.0) this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private class AmuletSession {
        private final Player player;
        private final ItemStack originalAmulet;
        private int diamondTicks;
        private int durabilityTicks;
        private int missingTicks;
        public int showDiamondMessageTicks;
        private BukkitTask task;

        public AmuletSession(Player player, ItemStack amulet, int savedDiamondTicks, int savedDurabilityTicks) {
            this.player = player;
            this.originalAmulet = amulet;
            this.diamondTicks = savedDiamondTicks;
            this.durabilityTicks = savedDurabilityTicks;
            this.missingTicks = 0;
            this.showDiamondMessageTicks = 0;
            startTask();
        }

        public void triggerDiamondMessage() {
            this.showDiamondMessageTicks = 40;
        }

        private void startTask() {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (activeSessions.get(player.getUniqueId()) != AmuletSession.this) return;
                    ItemStack amulet = getActiveAmulet(player);

                    if (amulet == null) {
                        missingTicks += 4;
                        if (missingTicks >= 80) {
                            deactivateAmulet(player, null, true);
                        }
                        return;
                    } else {
                        missingTicks = 0;
                    }

                    if (!bloodMoon.isActive(player.getWorld()) || player.isDead()) {
                        deactivateAmulet(player, amulet, true);
                        LocaleReader.amuletMessage(player, "La BloodMoon ha terminado. El amuleto se apagó.");
                        return;
                    }

                    durabilityTicks += 4;
                    diamondTicks += 4;

                    if (showDiamondMessageTicks > 0) {
                        showDiamondMessageTicks -= 4;
                        if (showDiamondMessageTicks % 20 == 0 || showDiamondMessageTicks <= 0) {
                            sendActionBar(player, showDiamondMessageTicks > 0);
                        }
                    } else if (durabilityTicks % 20 == 0) {
                        sendActionBar(player, false);
                    }

                    if (durabilityTicks >= 144) {
                        durabilityTicks = 0;
                        if (!consumeVirtualDurability(player, amulet)) {
                            deactivateAmulet(player, null, false);
                            LocaleReader.amuletMessage(player, "¡Tu Amuleto Luna de Sangre se ha desintegrado por falta de usos!");
                            return;
                        }
                    }

                    if (diamondTicks >= 1200) {
                        diamondTicks = 0;
                        if (!consumeDiamond(player)) {
                            LocaleReader.amuletMessage(player, "¡No tienes diamantes! El amuleto se ha desactivado.");
                            deactivateAmulet(player, amulet, true);
                            return;
                        }
                        triggerDiamondMessage();
                    }
                }
            }.runTaskTimer(plugin, 4L, 4L);
        }

        public void cancelTask() {
            if (task != null) task.cancel();
        }

        public int getDiamondTicks() {
            return diamondTicks;
        }

        private boolean consumeVirtualDurability(Player player, ItemStack amulet) {
            ItemMeta meta = amulet.getItemMeta();
            if (meta == null) return false;

            int usos = meta.getPersistentDataContainer().getOrDefault(usosKey, PersistentDataType.INTEGER, MAX_USOS);
            usos -= 1;

            if (usos <= 0) {
                amulet.setAmount(0);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                return false;
            }

            meta.getPersistentDataContainer().set(usosKey, PersistentDataType.INTEGER, usos);
            updateLore(meta, usos);
            amulet.setItemMeta(meta);
            return true;
        }
    }

    private ItemStack getActiveAmulet(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isAmulet(item) && item.containsEnchantment(Enchantment.UNBREAKING)) {
                return item;
            }
        }

        ItemStack cursor = player.getOpenInventory().getCursor();
        if (cursor != null && isAmulet(cursor) && cursor.containsEnchantment(Enchantment.UNBREAKING)) {
            return cursor;
        }

        return null;
    }

    public boolean isProtecting(Player player) {
        return player.isOnline() && !player.isDead() && bloodMoon.isActive(player.getWorld())
                && activeSessions.containsKey(player.getUniqueId()) && getActiveAmulet(player) != null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHordeSpawn(BloodMoonHordeEvent event) {
        for (UUID uuid : List.copyOf(activeSessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !isProtecting(player) || !player.getWorld().equals(event.getOrigin().getWorld())) continue;
            org.bukkit.Location position = player.getLocation();
            boolean nearby = player.equals(event.getTarget()) || position.distanceSquared(event.getOrigin()) <= 400
                    || event.getLocations().stream().anyMatch(location -> position.distanceSquared(location) <= 400);
            if (!nearby) continue;
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            if (now - hordeMessageCooldown.getOrDefault(uuid, 0L) >= 5000) {
                hordeMessageCooldown.put(uuid, now);
                for (Player viewer : player.getWorld().getPlayers()) {
                    LocaleReader.amuletMessage(viewer, player.getName() + " ha bloqueado una horda con su Amuleto Luna de Sangre.");
                }
                LocaleReader.actionBar(player, LocaleReader.RED + "Horda bloqueada por tu amuleto.");
            }
            return;
        }
    }

    public void shutdown() {
        for (AmuletSession session : List.copyOf(activeSessions.values())) {
            deactivateAmulet(session.player, getActiveAmulet(session.player), false);
        }
        hordeMessageCooldown.clear();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getType() == InventoryType.CRAFTING) return;

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            if (event.isShiftClick() && isAmulet(clicked)) {
                event.setCancelled(true);
                LocaleReader.amuletMessage(player, "No puedes guardar el amuleto mientras esté activado.");
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
            if (isAmulet(cursor) || isAmulet(clicked)) {
                event.setCancelled(true);
                LocaleReader.amuletMessage(player, "No puedes guardar el amuleto mientras esté activado.");
            }

            if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (isAmulet(hotbarItem)) {
                    event.setCancelled(true);
                    LocaleReader.amuletMessage(player, "No puedes guardar el amuleto mientras esté activado.");
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        if (isAmulet(event.getOldCursor()) || isAmulet(event.getCursor())) {
            Inventory topInv = event.getView().getTopInventory();
            if (topInv.getType() == InventoryType.CRAFTING) return;

            for (int slot : event.getRawSlots()) {
                if (slot < topInv.getSize()) {
                    event.setCancelled(true);
                    LocaleReader.amuletMessage(player, "No puedes guardar el amuleto mientras esté activado.");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();

        if (isAmulet(dropped) && activeSessions.containsKey(player.getUniqueId())) {
            LocaleReader.amuletMessage(player, "Has soltado tu amuleto activo. Se desactivará si no lo recuperas.");

            AmuletSession droppedSession = activeSessions.get(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (activeSessions.get(player.getUniqueId()) == droppedSession) {
                    if (getActiveAmulet(player) == null) {
                        deactivateAmulet(player, dropped, true);
                    }
                }
            }, 50L);
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        deactivateAmulet(player, getActiveAmulet(player), false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            deactivateAmulet(player, getActiveAmulet(player), false);
        }
    }
}
