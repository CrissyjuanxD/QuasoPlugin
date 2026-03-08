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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
import org.spectralmemories.bloodmoon.BloodmoonActuator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AmuletBloodM implements Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey amuletIdKey;
    private final NamespacedKey diamondTicksKey;
    private final NamespacedKey usosKey; // Nueva llave para usos virtuales

    private final Map<UUID, AmuletSession> activeSessions = new HashMap<>();
    private final Map<UUID, Long> hordeMessageCooldown = new HashMap<>();

    private final int MAX_USOS = 250;

    public AmuletBloodM(JavaPlugin plugin) {
        this.plugin = plugin;
        this.amuletIdKey = new NamespacedKey(plugin, "amulet_bloodmoon");
        this.diamondTicksKey = new NamespacedKey(plugin, "amulet_diamond_ticks");
        this.usosKey = new NamespacedKey(plugin, "amulet_usos");
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
        lore.add(ChatColor.of("#da765d") + "una " + ChatColor.DARK_RED + ChatColor.BOLD + "BloodMoon" + ChatColor.of("#da765d") + ".");
        lore.add("");
        lore.add(ChatColor.of("#c23d3d") + ChatColor.BOLD.toString() + "⊗ " + ChatColor.of("#488bad") + "Consume " + ChatColor.of("#81b9d5") + ChatColor.BOLD + "1 uso" + ChatColor.of("#488bad") + " cada 7.2 segundos.");
        lore.add(ChatColor.of("#c23d3d") + ChatColor.BOLD.toString() + "⊗ " + ChatColor.of("#488bad") + "Consume " + ChatColor.of("#81b9d5") + ChatColor.BOLD + "1 diamante" + ChatColor.of("#488bad") + " por minuto.");
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

    private boolean isBasicHordeMob(EntityType type) {
        switch (type) {
            case ZOMBIE:
            case ZOMBIE_VILLAGER:
            case HUSK:
            case DROWNED:
            case SPIDER:
            case CAVE_SPIDER:
            case CREEPER:
            case SKELETON:
            case WITHER_SKELETON:
            case STRAY:
            case WITCH:
            case PHANTOM:
                return true;
            default:
                if (type.name().equals("BOGGED")) return true;
                return false;
        }
    }

    // ========================================================================================
    // INTERACCIÓN Y LÓGICA DE ACTIVACIÓN
    // ========================================================================================

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
                deactivateAmulet(player, item, true);
                return;
            }

            if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
                player.sendMessage("§cEl amuleto solo funciona en el Overworld.");
                return;
            }

            BloodmoonActuator actuator = BloodmoonActuator.GetActuator(player.getWorld());
            if (actuator == null || !actuator.isInProgress()) {
                player.sendMessage("§cEl amuleto solo se puede activar durante una BloodMoon.");
                return;
            }

            activateAmulet(player, item);
        }
    }

    private void activateAmulet(Player player, ItemStack amulet) {
        ItemMeta meta = amulet.getItemMeta();
        if (meta == null) return;

        int savedDiamondTicks = meta.getPersistentDataContainer().getOrDefault(diamondTicksKey, PersistentDataType.INTEGER, 0);
        int usosActuales = meta.getPersistentDataContainer().getOrDefault(usosKey, PersistentDataType.INTEGER, MAX_USOS);

        if (usosActuales <= 0) {
            player.sendMessage("§c¡El amuleto está gastado y ya no tiene usos!");
            amulet.setAmount(0);
            return;
        }

        boolean justPaid = false;
        if (savedDiamondTicks == 0) {
            if (!consumeDiamond(player)) {
                player.sendMessage("§c¡No tienes diamantes para activar el amuleto!");
                return;
            }
            justPaid = true;
        }

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        amulet.setItemMeta(meta);

        AmuletSession session = new AmuletSession(player, savedDiamondTicks);
        if (justPaid) {
            session.triggerDiamondMessage();
        }
        activeSessions.put(player.getUniqueId(), session);

        player.setCooldown(Material.TORCHFLOWER_SEEDS, 60);
        playAuraAnimation(player, true);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        player.sendMessage("§aHas activado el Amuleto Luna de Sangre.");

        sendActionBar(player, session.showDiamondMessageTicks > 0);
    }

    private void deactivateAmulet(Player player, ItemStack amulet, boolean notify) {
        AmuletSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancelTask();

            if (amulet != null && amulet.hasItemMeta()) {
                ItemMeta meta = amulet.getItemMeta();
                meta.getPersistentDataContainer().set(diamondTicksKey, PersistentDataType.INTEGER, session.getDiamondTicks());
                meta.removeEnchant(Enchantment.UNBREAKING);
                amulet.setItemMeta(meta);
            }
        }

        if (notify) {
            playAuraAnimation(player, false);
            player.setCooldown(Material.TORCHFLOWER_SEEDS, 80);
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1.5f);
            player.sendMessage("§cHas desactivado el Amuleto Luna de Sangre.");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    private void sendActionBar(Player player, boolean isDiamondPaid) {
        String baseName = ChatColor.of("#e18b75") + "Amul. Luna de Sangre" + ChatColor.WHITE + ": ";
        String state = isDiamondPaid ? ChatColor.RED + "-1 Diamante" : ChatColor.WHITE + "Activado";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(baseName + state));
    }

    private void playAuraAnimation(Player player, boolean isActivation) {
        new BukkitRunnable() {
            double yOffset = isActivation ? 0.0 : 2.2;
            final double step = 0.15;
            final double radius = 1.0;

            Particle.DustOptions color1 = new Particle.DustOptions(isActivation ? org.bukkit.Color.LIME : org.bukkit.Color.RED, 1.2f);
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

    // ========================================================================================
    // MANEJADOR DE TIEMPO DEL AMULETO
    // ========================================================================================

    private class AmuletSession {
        private final Player player;
        private int diamondTicks;
        private int durabilityTicks;
        private int missingTicks;
        public int showDiamondMessageTicks;
        private BukkitTask task;

        public AmuletSession(Player player, int savedDiamondTicks) {
            this.player = player;
            this.diamondTicks = savedDiamondTicks;
            this.durabilityTicks = 0;
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

                    BloodmoonActuator actuator = BloodmoonActuator.GetActuator(player.getWorld());
                    if (actuator == null || !actuator.isInProgress()) {
                        deactivateAmulet(player, amulet, true);
                        player.sendMessage("§cLa BloodMoon ha terminado. El amuleto se apagó.");
                        return;
                    }

                    for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
                        if (isBasicHordeMob(entity.getType())) {
                            if (entity.getCustomName() != null) {
                                if (!entity.getCustomName().contains("§")) {
                                    continue;
                                }
                            }
                            entity.getWorld().spawnParticle(Particle.SMOKE, entity.getLocation(), 10, 0.2, 0.5, 0.2, 0.05);
                            entity.remove();
                        }
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

                    // Desgaste virtual (Cada 7.2s = 144 ticks)
                    if (durabilityTicks >= 144) {
                        durabilityTicks = 0;
                        if (!consumeVirtualDurability(player, amulet)) {
                            deactivateAmulet(player, null, false);
                            player.sendMessage("§c¡Tu Amuleto Luna de Sangre se ha desintegrado por falta de usos!");
                            return;
                        }
                    }

                    if (diamondTicks >= 1200) {
                        diamondTicks = 0;
                        if (!consumeDiamond(player)) {
                            player.sendMessage("§c¡No tienes diamantes! El amuleto se ha desactivado.");
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

        // LÓGICA VIRTUAL DE USOS
        private boolean consumeVirtualDurability(Player player, ItemStack amulet) {
            ItemMeta meta = amulet.getItemMeta();
            if (meta == null) return false;

            int usos = meta.getPersistentDataContainer().getOrDefault(usosKey, PersistentDataType.INTEGER, MAX_USOS);
            usos -= 1;

            if (usos <= 0) {
                amulet.setAmount(0); // Se rompe el ítem
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

    // ========================================================================================
    // BLOQUEO DE MOBS Y HORDAS (SINERGIA CON BLOODMOON)
    // ========================================================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (activeSessions.isEmpty()) return;
        if (!isBasicHordeMob(event.getEntityType())) return;

        World world = event.getLocation().getWorld();

        for (UUID uuid : activeSessions.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getWorld().equals(world)) {

                if (p.getLocation().distanceSquared(event.getLocation()) <= 400) {
                    event.setCancelled(true);

                    if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
                        long lastMsg = hordeMessageCooldown.getOrDefault(uuid, 0L);
                        if (System.currentTimeMillis() - lastMsg > 5000) {
                            hordeMessageCooldown.put(uuid, System.currentTimeMillis());

                            String tellraw = "[\"\",{\"text\":\"→\",\"bold\":true,\"color\":\"white\"},{\"text\":\" \",\"bold\":true},{\"text\":\"" + p.getName() + "\",\"bold\":true,\"color\":\"#89bfe1\"},\" \",{\"text\":\"ha b\",\"color\":\"#53b6f3\"},{\"text\":\"loqueado la horda con su\",\"color\":\"#53b6f3\"},\" \",{\"text\":\"Amuleto de Luna de Sangre\",\"bold\":true,\"color\":\"#e17575\"}]";
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw @a " + tellraw);
                        }
                    }
                    break;
                }
            }
        }
    }

    // ========================================================================================
    // ANTI-EXPLOITS Y EVENTOS EXTERNOS
    // ========================================================================================

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
                player.sendMessage("§cNo puedes guardar el amuleto mientras esté activado.");
            }
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(topInv)) {
            if (isAmulet(cursor) || isAmulet(clicked)) {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes guardar el amuleto mientras esté activado.");
            }

            if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) {
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                if (isAmulet(hotbarItem)) {
                    event.setCancelled(true);
                    player.sendMessage("§cNo puedes guardar el amuleto mientras esté activado.");
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
                    player.sendMessage("§cNo puedes guardar el amuleto mientras esté activado.");
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
            player.sendMessage("§eHas soltado tu amuleto activo. Se desactivará si no lo recuperas.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (activeSessions.containsKey(player.getUniqueId())) {
                    if (getActiveAmulet(player) == null) {
                        deactivateAmulet(player, dropped, true);
                    }
                }
            }, 50L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeSessions.containsKey(player.getUniqueId())) {
            deactivateAmulet(player, getActiveAmulet(player), false);
        }
    }
}