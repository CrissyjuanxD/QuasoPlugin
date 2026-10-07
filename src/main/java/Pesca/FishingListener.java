package Pesca;

import Handlers.ActionBarHandler;
import Managers.ItemManager;
import Trabajos.TrabajosXp;
import imp.crissyjuanxd.QuasoPlugin;
import items.FishingItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

public class FishingListener implements Listener {

    static final TextColor AGUA = TextColor.color(0x61B1F2);
    static final TextColor AGUA_OSCURA = TextColor.color(0x3E7FB5);
    static final TextColor TEXTO = TextColor.color(0xCFE9F7);
    static final TextColor GRIS = TextColor.color(0x9AA9B5);
    static final TextColor DORADO = TextColor.color(0xF2C46B);
    static final TextColor VERDE = TextColor.color(0x7BE38E);
    static final TextColor NARANJA = TextColor.color(0xEFA94A);
    static final TextColor ROJO = TextColor.color(0xE0707A);
    static final Component PREFIJO = Component.text("Pesca ", AGUA, TextDecoration.BOLD).append(Component.text("» ", AGUA_OSCURA));

    private static final long AVISO_ZONA = 20_000;
    private static final long TUTORIAL = 30 * 60_000;
    private static final Title.Times TIEMPOS_RESULTADO = Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500));

    private final QuasoPlugin plugin;
    private final FishingZoneManager zoneManager;
    private final ItemManager itemManager;

    private final Map<UUID, FishingMiniGame> activeGames = new HashMap<>();
    private final Set<UUID> playersInZone = new HashSet<>();
    private final Map<UUID, Long> avisos = new HashMap<>();
    private final Map<UUID, Long> tutoriales = new HashMap<>();

    private static final Random RANDOM = new Random();

    public FishingListener(QuasoPlugin plugin, FishingZoneManager zoneManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
        this.itemManager = itemManager;
    }

    // Al entrar a una zona: la explicación completa cada 30 minutos y si no, solo un aviso corto
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean estaba = playersInZone.contains(uuid);
        boolean esta = zoneManager.isInFishingZone(event.getTo());
        if (esta == estaba) return;
        if (!esta) {
            playersInZone.remove(uuid);
            return;
        }
        playersInZone.add(uuid);

        long ahora = System.currentTimeMillis();
        if (ahora - tutoriales.getOrDefault(uuid, 0L) >= TUTORIAL) {
            tutoriales.put(uuid, ahora);
            avisos.put(uuid, ahora);
            sendTutorialMessage(player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.2f);
        } else if (ahora - avisos.getOrDefault(uuid, 0L) >= AVISO_ZONA) {
            avisos.put(uuid, ahora);
            ActionBarHandler.get(plugin).sendNotification(player, "pesca:zona", ChatColor.of("#61B1F2") + "≈ "
                    + ChatColor.of("#CFE9F7") + "Entraste a la Zona de Pesca" + ChatColor.of("#61B1F2") + " ≈");
        }
    }

    // En una zona, al picar algo empieza el minijuego. Mientras dura, volver a usar la caña es tirar: se cancela para
    // que no recoja el anzuelo ni pesque otra cosa
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        FishingMiniGame game = activeGames.get(player.getUniqueId());
        if (game != null) {
            if (!usoLaCana(event.getState())) return;
            event.setCancelled(true);
            if (event.getCaught() instanceof Item caught && event.getState() == PlayerFishEvent.State.CAUGHT_FISH) caught.remove();
            game.tirar();
            return;
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (zoneManager.getZoneAt(event.getHook().getLocation()) == null) return;

        ItemStack vanillaLoot = new ItemStack(Material.COD);
        if (event.getCaught() instanceof Item droppedItem) {
            vanillaLoot = droppedItem.getItemStack().clone();
            droppedItem.remove();
        }
        event.setCancelled(true);

        FishingMiniGame nuevo = new FishingMiniGame(plugin, player, event.getHook(), vanillaLoot,
                resultado -> handleGameResult(player, resultado));
        activeGames.put(player.getUniqueId(), nuevo);
        nuevo.start();
    }

    // Lo que hace el jugador con la caña (no lo que hace el pez: picar, acercarse o escaparse)
    private static boolean usoLaCana(PlayerFishEvent.State state) {
        return switch (state) {
            case FISHING, CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND, REEL_IN -> true;
            default -> false;
        };
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        FishingMiniGame game = activeGames.remove(uuid);
        if (game != null) game.cancelar();
        playersInZone.remove(uuid);
        avisos.remove(uuid);
    }

    private void handleGameResult(Player player, FishingMiniGame.Resultado resultado) {
        FishingMiniGame game = activeGames.remove(player.getUniqueId());
        if (game == null) return;

        if (resultado == FishingMiniGame.Resultado.ESCAPO || resultado == FishingMiniGame.Resultado.SOLTO) {
            player.showTitle(Title.title(Component.text("¡Se escapó!", ROJO, TextDecoration.BOLD),
                    Component.text(resultado == FishingMiniGame.Resultado.SOLTO ? "Guardaste la caña antes de tirar"
                            : "No volviste a usar la caña a tiempo", GRIS), TIEMPOS_RESULTADO));
            player.playSound(player.getLocation(), Sound.ENTITY_COD_FLOP, SoundCategory.PLAYERS, 1f, 0.8f);
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 0.8f, 0.6f);
            return;
        }

        ItemStack cana = cana(player);
        int suerte = cana == null ? 0 : cana.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
        boolean especial = RANDOM.nextInt(100) < FishingLoot.chanceEspecial(resultado, suerte);
        int hoy = especial ? takeDailyCustom(player) : 0;
        if (hoy < 0) {
            especial = false;
            player.sendMessage(PREFIJO.append(Component.text("Ya pescaste los " + FishingLoot.TOPE_DIARIO
                    + " premios especiales de hoy: hasta mañana solo sale pesca normal.", TEXTO)));
        }

        FishingLoot.Premio premio = especial ? FishingLoot.elegir(RANDOM, resultado == FishingMiniGame.Resultado.PERFECTA) : null;
        ItemStack reward = premio != null ? createCustomLoot(premio.clave()) : game.getVanillaLoot();
        if (reward == null) {
            premio = null;
            especial = false;
            reward = game.getVanillaLoot();
        }

        Location anzuelo = game.getAnzuelo();
        lanzar(player, anzuelo, reward);
        applyFishingRodDamage(player, cana);
        efectos(player, anzuelo, resultado);
        sendResultMessage(player, resultado, reward, premio, hoy);
        TrabajosXp.pescoEnZona(player, reward, especial);
    }

    // El premio sale volando del anzuelo hacia el jugador
    private void lanzar(Player player, Location anzuelo, ItemStack reward) {
        Item dropped = player.getWorld().dropItem(anzuelo, reward);
        dropped.setPickupDelay(0);
        Vector direction = player.getLocation().toVector().subtract(anzuelo.toVector());
        double distance = direction.length();
        if (distance > 0) direction.normalize();
        direction.multiply(Math.min(distance * 0.12, 1.2));
        direction.setY(direction.getY() + 0.4);
        dropped.setVelocity(direction);
    }

    private void efectos(Player player, Location anzuelo, FishingMiniGame.Resultado resultado) {
        anzuelo.getWorld().spawnParticle(Particle.SPLASH, anzuelo, 30, 0.3, 0.1, 0.3, 0);
        switch (resultado) {
            case PERFECTA -> {
                anzuelo.getWorld().spawnParticle(Particle.END_ROD, anzuelo.clone().add(0, 0.3, 0), 14, 0.25, 0.25, 0.25, 0.04);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.4f);
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.2f);
            }
            case BUENA -> {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.1f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.6f, 1.4f);
            }
            default -> player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1f, 1f);
        }
    }

    // Como se cancela la pesca normal, el desgaste de la caña se hace a mano (respetando Irrompibilidad)
    private void applyFishingRodDamage(Player player, ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof Damageable damageable)) return;
        int unbreaking = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && RANDOM.nextInt(unbreaking + 1) != 0) return;
        damageable.setDamage(damageable.getDamage() + 1);
        item.setItemMeta(damageable);
        if (damageable.getDamage() >= item.getType().getMaxDurability()) {
            item.setAmount(0);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
        }
    }

    private static ItemStack cana(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.FISHING_ROD) return item;
        item = player.getInventory().getItemInOffHand();
        return item.getType() == Material.FISHING_ROD ? item : null;
    }

    // Suma un premio especial al contador del día y devuelve cuántos lleva; -1 si ya llegó al tope
    private int takeDailyCustom(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "fishing_daily");
        PersistentDataContainer data = player.getPersistentDataContainer();
        String today = LocalDate.now().toString();
        String saved = data.getOrDefault(key, PersistentDataType.STRING, "");
        int count = saved.startsWith(today + ":") ? Integer.parseInt(saved.substring(today.length() + 1)) : 0;
        if (count >= FishingLoot.TOPE_DIARIO) return -1;
        data.set(key, PersistentDataType.STRING, today + ":" + (count + 1));
        return count + 1;
    }

    private ItemStack createCustomLoot(String key) {
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
        String segmento = "▬▬ ";
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("≈≈ ", AGUA).append(Component.text("Zona de Pesca", AGUA, TextDecoration.BOLD)).append(Component.text(" ≈≈", AGUA)));
        player.sendMessage(Component.text("Aquí se pescan premios especiales que se cambian por ", TEXTO)
                .append(Component.text("DinoCoins", DORADO, TextDecoration.BOLD)).append(Component.text(" en la tienda.", TEXTO)));
        player.sendMessage(Component.text("Cuando algo pica sale una barra con un marcador: ", TEXTO)
                .append(Component.text("vuelve a usar la caña", AGUA, TextDecoration.BOLD))
                .append(Component.text(" cuando esté encima del color que quieras.", TEXTO)));
        player.sendMessage(Component.text(" " + segmento, VERDE).append(Component.text("Verde", VERDE, TextDecoration.BOLD))
                .append(Component.text(" · pesca perfecta: premio especial seguro y más suerte con los raros.", GRIS)));
        player.sendMessage(Component.text(" " + segmento, NARANJA).append(Component.text("Naranja", NARANJA, TextDecoration.BOLD))
                .append(Component.text(" · buena pesca: " + FishingLoot.CHANCE_BUENA + "% de premio especial (+"
                        + FishingLoot.CHANCE_POR_SUERTE + "% por nivel de Suerte marina).", GRIS)));
        player.sendMessage(Component.text(" " + segmento, ROJO).append(Component.text("Rojo", ROJO, TextDecoration.BOLD))
                .append(Component.text(" · pesca normal de Minecraft.", GRIS)));
        player.sendMessage(Component.text("Tienes " + FishingMiniGame.DURACION / 20 + " segundos antes de que se escape. Premios especiales por día: ", TEXTO)
                .append(Component.text(String.valueOf(FishingLoot.TOPE_DIARIO), DORADO)).append(Component.text(".", TEXTO)));
        player.sendMessage(Component.empty());
    }

    // Título con el resultado y, si fue premio especial, una línea en el chat (los épicos se anuncian a todos)
    private void sendResultMessage(Player player, FishingMiniGame.Resultado resultado, ItemStack reward, FishingLoot.Premio premio, int hoy) {
        Component nombre = reward.effectiveName();
        Component titulo = switch (resultado) {
            case PERFECTA -> Component.text("✦ ¡Pesca perfecta! ✦", VERDE, TextDecoration.BOLD);
            case BUENA -> Component.text("¡Buena pesca!", NARANJA, TextDecoration.BOLD);
            default -> Component.text("Pesca normal", TEXTO);
        };
        Component subtitulo = premio == null ? nombre.colorIfAbsent(TEXTO)
                : nombre.append(Component.text(" · " + premio.rareza().nombre(), premio.rareza().color()));
        player.showTitle(Title.title(titulo, subtitulo, TIEMPOS_RESULTADO));
        if (premio == null) return;

        TextColor rareza = premio.rareza().color();
        player.sendMessage(PREFIJO.append(Component.text("✦ ", rareza)).append(nombre)
                .append(Component.text(" · " + premio.rareza().nombre(), rareza))
                .append(Component.text("  (" + hoy + "/" + FishingLoot.TOPE_DIARIO + " hoy)", GRIS)));
        if (premio.rareza() == FishingLoot.Rareza.EPICO) {
            Bukkit.broadcast(PREFIJO.append(Component.text(player.getName(), TEXTO, TextDecoration.BOLD))
                    .append(Component.text(" pescó ", TEXTO)).append(nombre).append(Component.text(" ✦", rareza)));
        }
    }
}
