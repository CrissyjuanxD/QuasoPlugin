package Pesca;

import Managers.ItemManager;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import imp.crissyjuanxd.QuasoPlugin;
import items.FishingItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.util.Vector;

import java.util.*;

public class FishingListener implements Listener {

    private final QuasoPlugin plugin;
    private final FishingZoneManager zoneManager;
    private final ItemManager itemManager;

    /** UUID de jugadores con minijuego activo */
    private final Map<UUID, FishingMiniGame> activeGames = new HashMap<>();
    /** Set de UUIDs de jugadores que están actualmente FÍSICAMENTE dentro de una zona */
    private final Set<UUID> playersInZone = new HashSet<>();
    /** Cooldown de 20 segundos para el mensaje del tutorial */
    private final Map<UUID, Long> tutorialCooldowns = new HashMap<>();

    // Loot custom (intercambiable por DinoCoins)
    private static final String[] CUSTOM_LOOT = {
            "zanahoria_encantada", "pepitas_hierro_oxidadas", "pepitas_diamante",
            "fragmentos_ambar", "fosiles_pequenos", "lingote_platino"
    };

    private static final Random RANDOM = new Random();

    public FishingListener(QuasoPlugin plugin, FishingZoneManager zoneManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
        this.itemManager = itemManager;
    }

    // ─── Evento: Entrar a la zona (Tutorial) ─────────────────────────────────

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Optimización: Solo verificamos si cambió de bloque entero
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        boolean wasInZone = playersInZone.contains(player.getUniqueId());
        boolean isNowInZone = zoneManager.isInFishingZone(to);

        if (isNowInZone && !wasInZone) {
            playersInZone.add(player.getUniqueId());

            long currentTime = System.currentTimeMillis();
            long lastMessageTime = tutorialCooldowns.getOrDefault(player.getUniqueId(), 0L);

            // Verificamos si pasaron 20 segundos (20,000 ms) desde el último mensaje
            if (currentTime - lastMessageTime >= 20000) {
                sendTutorialMessage(player);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                tutorialCooldowns.put(player.getUniqueId(), currentTime);
            }

        } else if (!isNowInZone && wasInZone) {
            playersInZone.remove(player.getUniqueId());
        }
    }

    // ─── Evento: pez atrapado ────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        Location hookLoc = event.getHook().getLocation();

        // ¿Está la caña en una zona de pesca?
        FishingZone zone = zoneManager.getZoneAt(hookLoc);
        if (zone == null) return;

        if (activeGames.containsKey(player.getUniqueId())) return;

        // Capturamos el botín Vanilla original antes de cancelar el evento
        ItemStack vanillaLoot = new ItemStack(Material.COD); // Fallback por defecto
        if (event.getCaught() instanceof org.bukkit.entity.Item droppedItem) {
            vanillaLoot = droppedItem.getItemStack().clone();
            droppedItem.remove(); // Eliminamos la entidad nativa para que no se suelte sola
        }

        // Cancelamos el evento nativo para retener la física y ejecutar el minijuego
        event.setCancelled(true);

        // Inicia el minijuego, pasando el loot vanilla y la ubicación del anzuelo
        FishingMiniGame game = new FishingMiniGame(plugin, player, vanillaLoot, () -> {
            handleGameResult(player, hookLoc);
        });
        activeGames.put(player.getUniqueId(), game);
        game.start();

        // Títulos
        player.sendTitle(
                ChatColor.of("#61B1F2") + "" + ChatColor.BOLD + "¡Algo ha picado!",
                ChatColor.GRAY + "Usa el botón " + ChatColor.YELLOW + ChatColor.BOLD + "saltar" + ChatColor.GRAY + " o " + ChatColor.YELLOW + ChatColor.BOLD + "agacharse",
                5, 40, 10
        );
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2f);
    }

    // ─── Eventos de Acción del Minijuego (Compatibilidad Total) ──────────────

    // 1. Saltar (Estando de pie)
    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (tryProcessMinigameClick(player)) {
            event.setCancelled(true); // Cancela el salto físico
        }
    }

    // 2. Agacharse (Estando de pie)
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) { // Solo cuenta cuando presiona la tecla hacia abajo
            tryProcessMinigameClick(event.getPlayer());
        }
    }

    // 3. Agacharse estando sentado (Intentar bajarse de monturas/sillas)
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (tryProcessMinigameClick(player)) {
                event.setCancelled(true); // Evitamos que se baje de la vagoneta/silla
            }
        }
    }

    /**
     * Método centralizado para procesar la acción del jugador.
     * Retorna 'true' si el jugador tenía un minijuego activo.
     */
    private boolean tryProcessMinigameClick(Player player) {
        FishingMiniGame game = activeGames.get(player.getUniqueId());
        // Gracias a game.isFinished() evitamos que se ejecute dos veces si ocurren dos eventos simultáneos
        if (game != null && !game.isFinished()) {
            game.playerClick();
            return true;
        }
        return false;
    }

    // ─── Lógica de resultado y físicas Vanilla ───────────────────────────────

    private void handleGameResult(Player player, Location hookLoc) {
        FishingMiniGame game = activeGames.remove(player.getUniqueId());
        if (game == null) return;

        // Verificar si perdió por tiempo
        if (game.isFailedByTime()) {
            player.sendTitle(ChatColor.RED + "¡Tiempo agotado!", ChatColor.GRAY + "El anzuelo se ha roto...", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.6f);
            return;
        }

        final int slotType = game.getCurrentSlotType();
        final boolean giveCustom = switch (slotType) {
            case 1 -> RANDOM.nextInt(100) < 35;
            case 2 -> true;
            default -> false;
        };

        ItemStack reward = giveCustom ? getCustomLoot() : game.getVanillaLoot();
        if (reward == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            // FÍSICA VANILLA: Hacemos spawn del ítem en el anzuelo y lo lanzamos al jugador
            Item dropped = player.getWorld().dropItem(hookLoc, reward);
            dropped.setPickupDelay(0); // Para que se pueda recoger al instante al chocar con el jugador

            // Matemáticas para el arco de vuelo del ítem hacia el jugador
            Vector direction = player.getLocation().toVector().subtract(hookLoc.toVector());
            double distance = direction.length();
            direction.normalize();
            // Multiplicamos según la distancia para que no vuele al infinito si está cerca
            direction.multiply(Math.min(distance * 0.12, 1.2));
            direction.setY(direction.getY() + 0.4); // Arco curvo hacia arriba
            dropped.setVelocity(direction);

            // Desgaste natural de la caña de pescar
            applyFishingRodDamage(player);

            // Enviar mensaje en el chat
            sendResultMessage(player, slotType, giveCustom, reward);
        });
    }

    // Desgaste de la caña de pescar soportando Unbreaking
    private void applyFishingRodDamage(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.FISHING_ROD) {
            item = player.getInventory().getItemInOffHand();
        }

        if (item.getType() == Material.FISHING_ROD) {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable damageable) {
                // Cálculo del encantamiento irrompibilidad
                int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
                if (unbreaking > 0) {
                    // Probabilidad de desgaste = 100% / (Nivel + 1)
                    if (RANDOM.nextInt(unbreaking + 1) != 0) {
                        return; // Evitó el desgaste
                    }
                }

                damageable.setDamage(damageable.getDamage() + 1);
                item.setItemMeta(damageable);

                // Si se rompe
                if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                    item.setAmount(0);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }

    // ─── Loot ────────────────────────────────────────────────────────────────

    private ItemStack getCustomLoot() {
        String key = CUSTOM_LOOT[RANDOM.nextInt(CUSTOM_LOOT.length)];
        // Intentamos primero el ItemManager
        ItemStack item = itemManager.getItem(key, 1, null);
        if (item != null) return item;

        // Fallback: FishingItems
        return switch (key) {
            case "zanahoria_encantada"    -> FishingItems.createZanahoriaEncantada();
            case "pepitas_hierro_oxidadas"-> FishingItems.createPepitasHierroOxidadas();
            case "pepitas_diamante"       -> FishingItems.createPepitasDiamante();
            case "fragmentos_ambar"       -> FishingItems.createFragmentosAmbar();
            case "fosiles_pequenos"       -> FishingItems.createFosilesP();
            case "lingote_platino"        -> FishingItems.createLingotePlatino();
            default                       -> null;
        };
    }

    // ─── Mensajes y Formateo ─────────────────────────────────────────────────

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