package Pesca;

import Managers.ItemManager;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import imp.crissyjuanxd.QuasoPlugin;
import items.FishingItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.LocalDate;
import java.util.*;

public class FishingListener implements Listener {

    private final QuasoPlugin plugin;
    private final FishingZoneManager zoneManager;
    private final ItemManager itemManager;

    private final Map<UUID, FishingMiniGame> activeGames = new HashMap<>();
    private final Set<UUID> playersInZone = new HashSet<>();
    private final Map<UUID, Long> tutorialCooldowns = new HashMap<>();

    // Premio especial y su peso (suman 100): los raros salen menos y valen más en la tienda
    private static final String[] CUSTOM_LOOT = {
            "chatarra", "manzana_podrida", "zanahoria_encantada", "pepitas_hierro_oxidadas",
            "pepitas_diamante", "fragmentos_ambar", "fosiles_pequenos", "lingote_platino"
    };
    private static final int[] CUSTOM_WEIGHTS = {30, 25, 15, 12, 8, 5, 3, 2};

    private static final int DAILY_CUSTOM_CAP = 60;

    private static final Random RANDOM = new Random();

    public FishingListener(QuasoPlugin plugin, FishingZoneManager zoneManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
        this.itemManager = itemManager;
    }

    // Al entrar a una zona de pesca manda el tutorial (máximo cada 20 segundos)
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        boolean wasInZone = playersInZone.contains(player.getUniqueId());
        boolean isNowInZone = zoneManager.isInFishingZone(to);

        if (isNowInZone && !wasInZone) {
            playersInZone.add(player.getUniqueId());

            long currentTime = System.currentTimeMillis();
            long lastMessageTime = tutorialCooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (currentTime - lastMessageTime >= 20000) {
                sendTutorialMessage(player);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                tutorialCooldowns.put(player.getUniqueId(), currentTime);
            }

        } else if (!isNowInZone && wasInZone) {
            playersInZone.remove(player.getUniqueId());
        }
    }

    // En una zona de pesca, al picar algo se cancela la pesca normal y empieza el minijuego
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        Location hookLoc = event.getHook().getLocation();

        FishingZone zone = zoneManager.getZoneAt(hookLoc);
        if (zone == null) return;

        if (activeGames.containsKey(player.getUniqueId())) return;

        ItemStack vanillaLoot = new ItemStack(Material.COD);
        if (event.getCaught() instanceof org.bukkit.entity.Item droppedItem) {
            vanillaLoot = droppedItem.getItemStack().clone();
            droppedItem.remove();
        }

        event.setCancelled(true);

        FishingMiniGame game = new FishingMiniGame(plugin, player, vanillaLoot, () -> {
            handleGameResult(player, hookLoc);
        });
        activeGames.put(player.getUniqueId(), game);
        game.start();

        player.sendTitle(
                ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "¡Algo ha picado!",
                ChatColor.GRAY + "Usa el botón " + ChatColor.YELLOW + ChatColor.BOLD + "saltar" + ChatColor.GRAY + " o " + ChatColor.YELLOW + ChatColor.BOLD + "agacharse",
                5, 40, 10
        );
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (tryProcessMinigameClick(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            tryProcessMinigameClick(event.getPlayer());
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (tryProcessMinigameClick(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Saltar o agacharse cuenta como el click del minijuego
    private boolean tryProcessMinigameClick(Player player) {
        FishingMiniGame game = activeGames.get(player.getUniqueId());
        if (game != null && !game.isFinished()) {
            game.playerClick();
            return true;
        }
        return false;
    }

    // Según el color donde paró: rojo da el loot normal, naranja 35% de loot especial y verde loot especial seguro
    private void handleGameResult(Player player, Location hookLoc) {
        FishingMiniGame game = activeGames.remove(player.getUniqueId());
        if (game == null) return;

        if (game.isFailedByTime()) {
            player.sendTitle(ChatColor.RED + "¡Tiempo agotado!", ChatColor.GRAY + "El anzuelo se ha roto...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.6f);
            return;
        }

        final int slotType = game.getCurrentSlotType();
        boolean rolledCustom = switch (slotType) {
            case 1 -> RANDOM.nextInt(100) < 35;
            case 2 -> true;
            default -> false;
        };
        boolean capped = rolledCustom && !takeDailyCustom(player);
        final boolean giveCustom = rolledCustom && !capped;
        if (capped) {
            player.sendMessage(ChatColor.of("#4BA3DD") + "✦ Ya pescaste los " + DAILY_CUSTOM_CAP
                    + " premios especiales de hoy, ahora sale loot normal. Mañana se reinicia.");
        }

        ItemStack reward = giveCustom ? getCustomLoot() : game.getVanillaLoot();
        if (reward == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Item dropped = player.getWorld().dropItem(hookLoc, reward);
            dropped.setPickupDelay(0);

            Vector direction = player.getLocation().toVector().subtract(hookLoc.toVector());
            double distance = direction.length();
            direction.normalize();
            direction.multiply(Math.min(distance * 0.12, 1.2));
            direction.setY(direction.getY() + 0.4);
            dropped.setVelocity(direction);

            applyFishingRodDamage(player);

            sendResultMessage(player, slotType, giveCustom, reward);
        });
    }

    // Como se cancela la pesca normal, el desgaste de la caña se hace a mano (respetando Irrompibilidad)
    private void applyFishingRodDamage(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.FISHING_ROD) {
            item = player.getInventory().getItemInOffHand();
        }

        if (item.getType() == Material.FISHING_ROD) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
                if (unbreaking > 0) {
                    if (RANDOM.nextInt(unbreaking + 1) != 0) {
                        return;
                    }
                }

                damageable.setDamage(damageable.getDamage() + 1);
                item.setItemMeta(damageable);

                if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                    item.setAmount(0);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }

    // Suma un premio especial al contador del día; false si ya llegó al tope
    private boolean takeDailyCustom(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "fishing_daily");
        PersistentDataContainer data = player.getPersistentDataContainer();
        String today = LocalDate.now().toString();
        String saved = data.getOrDefault(key, PersistentDataType.STRING, "");
        int count = saved.startsWith(today + ":") ? Integer.parseInt(saved.substring(today.length() + 1)) : 0;
        if (count >= DAILY_CUSTOM_CAP) return false;
        data.set(key, PersistentDataType.STRING, today + ":" + (count + 1));
        return true;
    }

    private ItemStack getCustomLoot() {
        int roll = RANDOM.nextInt(100);
        String key = CUSTOM_LOOT[CUSTOM_LOOT.length - 1];
        for (int i = 0; i < CUSTOM_LOOT.length; i++) {
            roll -= CUSTOM_WEIGHTS[i];
            if (roll < 0) {
                key = CUSTOM_LOOT[i];
                break;
            }
        }
        ItemStack item = itemManager.getItem(key, 1, null);
        if (item != null) return item;

        return switch (key) {
            case "chatarra"               -> FishingItems.createChatarra();
            case "manzana_podrida"        -> FishingItems.createManzanaPodrida();
            case "zanahoria_encantada"    -> FishingItems.createZanahoriaEncantada();
            case "pepitas_hierro_oxidadas"-> FishingItems.createPepitasHierroOxidadas();
            case "pepitas_diamante"       -> FishingItems.createPepitasDiamante();
            case "fragmentos_ambar"       -> FishingItems.createFragmentosAmbar();
            case "fosiles_pequenos"       -> FishingItems.createFosilesP();
            case "lingote_platino"        -> FishingItems.createLingotePlatino();
            default                       -> null;
        };
    }

    private void sendTutorialMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "Zona de Pesca " + ChatColor.GRAY + "▶");
        player.sendMessage("");
        player.sendMessage(ChatColor.of("#4BA3DD") + "En esta zona podrás pescar cosas únicas,");
        player.sendMessage(ChatColor.of("#4BA3DD") + "que se podrán cambiar por " + ChatColor.GOLD + ChatColor.BOLD + "DinoCoins" + ChatColor.of("#4BA3DD") + " en la tienda.");
        player.sendMessage("");
        player.sendMessage(ChatColor.WHITE + "Usa el botón de " + ChatColor.of("#D09039") + ChatColor.BOLD + "saltar" + ChatColor.WHITE + " o " + ChatColor.of("#D09039") + ChatColor.BOLD + "agacharse" + ChatColor.WHITE + " en estos colores:");
        player.sendMessage(ChatColor.GRAY + "(Si estás sentado, debes usar agacharse)");
        player.sendMessage(ChatColor.RED + "Rojo: " + ChatColor.GRAY + "Pescado y loot normal de Minecraft (Vanilla).");
        player.sendMessage(ChatColor.GOLD + "Naranja: " + ChatColor.GRAY + "35% de prob. de objetos especiales.");
        player.sendMessage(ChatColor.GREEN + "Verde: " + ChatColor.GRAY + "100% de prob. de objetos especiales.");
        player.sendMessage("");
    }

    private void sendResultMessage(Player player, int slotType, boolean gotCustom, ItemStack reward) {
        String itemName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : ChatColor.WHITE + formatEnumName(reward.getType().name());

        if (gotCustom) {
            player.sendMessage(ChatColor.of("#61B1F2") + "✦ " + ChatColor.of("#4BA3DD") + "¡Loot especial! Pescaste: " + itemName);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } else {
            player.sendMessage(ChatColor.GRAY + "✦ Pesca normal. Conseguiste: " + itemName);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    private String formatEnumName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}