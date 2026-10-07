package items.tienda;

import Events.MissionSystem.MissionUtils;
import Handlers.ActionBarHandler;
import InfestedCaves.DarknessShield;
import InfestedCaves.WardenBiome;
import ShopSystem.CustomItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

// Cómo se usan los items de la tienda: click derecho, comerlos o tomarlos y los que se tiran
public final class UsoItemsTienda implements Listener {

    private static final NamespacedKey ANIMAL = new NamespacedKey("quasoplugin", "red_animal");
    private static final NamespacedKey ANIMAL_TIPO = new NamespacedKey("quasoplugin", "red_animal_tipo");
    private static final NamespacedKey LUGAR = new NamespacedKey("quasoplugin", "eco_lugar");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    // Los efectos malos que quitan el Botiquín y el Antídoto
    private static final Set<PotionEffectType> MALOS = Set.of(PotionEffectType.POISON, PotionEffectType.WITHER,
            PotionEffectType.HUNGER, PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS, PotionEffectType.MINING_FATIGUE,
            PotionEffectType.NAUSEA, PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS, PotionEffectType.LEVITATION,
            PotionEffectType.UNLUCK, PotionEffectType.INSTANT_DAMAGE, PotionEffectType.INFESTED, PotionEffectType.OOZING,
            PotionEffectType.WEAVING, PotionEffectType.WIND_CHARGED);

    private final JavaPlugin plugin;
    private final EfectosTienda efectos;
    private final Random random = new Random();
    private final Map<String, Long> esperas = new HashMap<>();
    private final Set<UUID> viajando = new HashSet<>();

    public UsoItemsTienda(JavaPlugin plugin, EfectosTienda efectos) {
        this.plugin = plugin;
        this.efectos = efectos;
    }

    // ---------------------------------------------------------------- Click derecho

    @EventHandler(priority = EventPriority.HIGH)
    public void onUsar(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = ItemsTienda.idOf(event.getItem());
        if (id == null || event.getHand() == null) return;
        Player player = event.getPlayer();
        EquipmentSlot mano = event.getHand();
        Block bloque = event.getClickedBlock();

        // Los que se comen, se toman o se tiran los maneja el juego
        switch (id) {
            case "racion_viaje", "galleta_fortuna", "caldo_profundo", "bomba_humo", "bengala_sculk", "granada_sonica",
                 "elixir_minero", "elixir_igneo", "elixir_abisal", "elixir_agilidad", "elixir_coloso", "elixir_furia",
                 "tonico_antilevitacion", "antidoto", "frasco_sabiduria" -> {
                return;
            }
            default -> { }
        }
        // Con un item que no necesita un bloque se puede seguir abriendo puertas y cofres
        boolean usaBloque = id.equals("abono_concentrado") || id.equals("red_animales");
        if (bloque != null && !usaBloque && !player.isSneaking() && bloque.getType().isInteractable()) return;
        event.setCancelled(true);

        switch (id) {
            case "red_animales" -> soltarAnimal(player, mano, bloque, event.getBlockFace());
            case "abono_concentrado" -> abonar(player, mano, bloque);
            case "brujula_explorador" -> brujula(player, mano);
            case "incienso_ahuyentador" -> {
                efectos.activar(player, EfectosTienda.Buff.INCIENSO);
                consumir(player, mano);
                player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.01);
                player.playSound(player.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 1f, 1.2f);
            }
            case "polvo_silencioso" -> {
                efectos.activar(player, EfectosTienda.Buff.SILENCIO);
                consumir(player, mano);
                player.getWorld().spawnParticle(Particle.WHITE_ASH, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.01);
                player.playSound(player.getLocation(), Sound.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 1f, 0.6f);
            }
            case "talisman_botin" -> {
                efectos.activar(player, EfectosTienda.Buff.BOTIN);
                consumir(player, mano);
                player.getWorld().spawnParticle(Particle.WAX_OFF, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.5);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.6f);
            }
            case "botiquin" -> botiquin(player, mano);
            case "radar_minerales" -> radar(player, mano);
            case "cristal_eco" -> cristalEco(player, mano);
            case "propulsor_estelar" -> propulsor(player, mano);
            case "estandarte_guerra" -> estandarte(player, mano);
            case "caja_misteriosa" -> caja(player, mano);
            default -> { }
        }
    }

    // Ninguno de los que tienen forma de bloque se puede poner (linterna, estandarte y caja)
    @EventHandler(ignoreCancelled = true)
    public void onPoner(BlockPlaceEvent event) {
        if (ItemsTienda.isItem(event.getItemInHand())) event.setCancelled(true);
    }

    // ---------------------------------------------------------------- Comer y tomar

    @EventHandler(ignoreCancelled = true)
    public void onConsumir(PlayerItemConsumeEvent event) {
        String id = ItemsTienda.idOf(event.getItem());
        if (id == null) return;
        Player player = event.getPlayer();
        switch (id) {
            case "racion_viaje" -> Bukkit.getScheduler().runTask(plugin, () -> {
                player.setFoodLevel(20);
                player.setSaturation(Math.min(20f, player.getSaturation() + 10f));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 0));
            });
            case "caldo_profundo" -> Bukkit.getScheduler().runTask(plugin, () -> {
                player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 0));
            });
            case "galleta_fortuna" -> galleta(player);
            case "elixir_abisal" -> DarknessShield.protect(player, 5 * 60_000L);
            case "tonico_antilevitacion" -> {
                efectos.activar(player, EfectosTienda.Buff.ANTILEVITACION);
                player.removePotionEffect(PotionEffectType.LEVITATION);
            }
            case "antidoto" -> {
                int quitados = 0;
                for (PotionEffect efecto : player.getActivePotionEffects()) {
                    if (MALOS.contains(efecto.getType())) {
                        player.removePotionEffect(efecto.getType());
                        quitados++;
                    }
                }
                ActionBarHandler.get(plugin).sendNotification(player, "tienda:antidoto", ChatColor.of("#a6e3a1") + "✦ "
                        + (quitados == 0 ? "No tenías efectos malos" : "Se fueron " + quitados + (quitados == 1 ? " efecto malo" : " efectos malos")));
            }
            case "frasco_sabiduria" -> efectos.activar(player, EfectosTienda.Buff.SABIDURIA);
            default -> { }
        }
    }

    private static final String[] FORTUNAS = {
            "Hoy el cofre que abras tendrá algo bueno.", "Un creeper menos es un día mejor.", "La paciencia pesca los mejores premios.",
            "Lo que siembras hoy lo cosechas mañana.", "El que mina profundo encuentra diamantes.", "Una BloodMoon se acerca... o no.",
            "Tus amigos te van a necesitar pronto.", "La suerte favorece al que se arriesga.", "Guarda tus DinoCoins para algo grande.",
            "Hoy no es día de morir."
    };

    private void galleta(Player player) {
        PotionEffect[] premios = {
                new PotionEffect(PotionEffectType.SPEED, 1200, 1), new PotionEffect(PotionEffectType.HASTE, 1200, 1),
                new PotionEffect(PotionEffectType.STRENGTH, 1200, 0), new PotionEffect(PotionEffectType.REGENERATION, 1200, 0),
                new PotionEffect(PotionEffectType.JUMP_BOOST, 1200, 1), new PotionEffect(PotionEffectType.RESISTANCE, 1200, 0),
                new PotionEffect(PotionEffectType.NIGHT_VISION, 1200, 0), new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 0),
                new PotionEffect(PotionEffectType.ABSORPTION, 1200, 1), new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0)
        };
        PotionEffect premio = premios[random.nextInt(premios.length)];
        Bukkit.getScheduler().runTask(plugin, () -> player.addPotionEffect(premio));
        player.sendMessage(ChatColor.of("#e8b96a") + "✧ " + ChatColor.ITALIC + FORTUNAS[random.nextInt(FORTUNAS.length)]);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.6f);
    }

    // ---------------------------------------------------------------- Los que se tiran

    @EventHandler(ignoreCancelled = true)
    public void onTirar(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball bola) || !(bola.getShooter() instanceof Player player)) return;
        if (!"bomba_humo".equals(ItemsTienda.idOf(bola.getItem()))) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 6, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 6, 1));
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
    }

    @EventHandler
    public void onCaer(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball bola)) return;
        String id = ItemsTienda.idOf(bola.getItem());
        if (id == null) return;
        Location lugar = bola.getLocation();
        World world = lugar.getWorld();
        Player tirador = bola.getShooter() instanceof Player p ? p : null;
        switch (id) {
            case "bomba_humo" -> {
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, lugar, 60, 2, 1, 2, 0.02);
                world.spawnParticle(Particle.LARGE_SMOKE, lugar, 40, 2, 1, 2, 0.02);
                world.playSound(lugar, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1f, 0.7f);
                for (Entity entity : world.getNearbyEntities(lugar, 5, 5, 5)) {
                    if (!(entity instanceof Mob mob)) continue;
                    if (tirador != null && tirador.equals(mob.getTarget())) mob.setTarget(null);
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                }
            }
            case "bengala_sculk" -> {
                world.spawnParticle(Particle.SCULK_SOUL, lugar, 30, 1.5, 1, 1.5, 0.05);
                world.spawnParticle(Particle.GLOW, lugar, 40, 2, 1, 2, 0.1);
                world.playSound(lugar, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.PLAYERS, 0.4f, 1.6f);
                for (Entity entity : world.getNearbyEntities(lugar, 16, 16, 16)) {
                    if (entity instanceof LivingEntity vivo && !(entity instanceof Player)) {
                        vivo.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 15, 0));
                    }
                }
            }
            case "granada_sonica" -> {
                world.spawnParticle(Particle.SONIC_BOOM, lugar.clone().add(0, 0.5, 0), 1);
                world.playSound(lugar, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1f, 1.2f);
                for (Entity entity : world.getNearbyEntities(lugar, 5, 5, 5)) {
                    if (!(entity instanceof LivingEntity vivo) || !(entity instanceof Enemy)) continue;
                    if (tirador != null) vivo.damage(8, tirador);
                    else vivo.damage(8);
                    Vector empuje = vivo.getLocation().toVector().subtract(lugar.toVector());
                    if (empuje.lengthSquared() < 0.01) empuje = new Vector(0, 1, 0);
                    vivo.setVelocity(empuje.normalize().multiply(1.1).setY(0.45));
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Red Atrapa-Animales

    @EventHandler(priority = EventPriority.HIGH)
    public void onAtrapar(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (!"red_animales".equals(ItemsTienda.idOf(item))) return;
        event.setCancelled(true);
        Entity entity = event.getRightClicked();
        if (item.getItemMeta().getPersistentDataContainer().has(ANIMAL, PersistentDataType.BYTE_ARRAY)) {
            avisar(player, ChatColor.of("#d9a5a0") + "La red ya tiene un animal: suéltalo con click derecho a un bloque");
            return;
        }
        if (!(entity instanceof Animals animal) || !entity.getPassengers().isEmpty() || MissionUtils.bossId(entity) != null) {
            avisar(player, ChatColor.of("#d9a5a0") + "La red solo atrapa animales");
            return;
        }
        if (animal instanceof Tameable domado && domado.isTamed() && !player.getUniqueId().equals(domado.getOwnerUniqueId())) {
            avisar(player, ChatColor.of("#d9a5a0") + "Ese animal es de otro jugador");
            return;
        }

        byte[] datos = Bukkit.getUnsafe().serializeEntity(entity);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ANIMAL, PersistentDataType.BYTE_ARRAY, datos);
        meta.getPersistentDataContainer().set(ANIMAL_TIPO, PersistentDataType.STRING, entity.getType().translationKey());
        meta.lore(loreConLinea("red_animales", Component.text("Contiene: ", NamedTextColor.GRAY)
                .append(Component.translatable(entity.getType().translationKey(), TextColor.color(0xF2C46B)))));
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        player.getInventory().setItem(event.getHand(), item);

        Location lugar = entity.getLocation().add(0, 0.5, 0);
        entity.remove();
        lugar.getWorld().spawnParticle(Particle.CLOUD, lugar, 15, 0.4, 0.4, 0.4, 0.02);
        lugar.getWorld().playSound(lugar, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1f, 0.6f);
    }

    private void soltarAnimal(Player player, EquipmentSlot mano, Block bloque, BlockFace cara) {
        ItemStack item = player.getInventory().getItem(mano);
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        byte[] datos = pdc.get(ANIMAL, PersistentDataType.BYTE_ARRAY);
        if (datos == null) {
            avisar(player, ChatColor.GRAY + "La red está vacía: click derecho a un animal para atraparlo");
            return;
        }
        if (bloque == null) return;
        Location lugar = bloque.getRelative(cara == null ? BlockFace.UP : cara).getLocation().add(0.5, 0, 0.5);
        Entity animal = Bukkit.getUnsafe().deserializeEntity(datos, lugar.getWorld());
        if (!animal.spawnAt(lugar)) {
            avisar(player, ChatColor.of("#d9a5a0") + "No se pudo soltar aquí");
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(ANIMAL);
        meta.getPersistentDataContainer().remove(ANIMAL_TIPO);
        meta.lore(loreConLinea("red_animales", null));
        meta.setEnchantmentGlintOverride(null);
        item.setItemMeta(meta);
        player.getInventory().setItem(mano, item);
        lugar.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, lugar.clone().add(0, 0.5, 0), 12, 0.4, 0.4, 0.4, 0);
        lugar.getWorld().playSound(lugar, Sound.ENTITY_CHICKEN_EGG, SoundCategory.PLAYERS, 1f, 1f);
    }

    // ---------------------------------------------------------------- Uno por uno

    private void abonar(Player player, EquipmentSlot mano, Block centro) {
        if (centro == null) return;
        int crecieron = 0;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 1; y++) {
                    Block bloque = centro.getRelative(x, y, z);
                    if (seAbona(bloque.getType()) && bloque.applyBoneMeal(BlockFace.UP)) crecieron++;
                }
            }
        }
        if (crecieron == 0) {
            avisar(player, ChatColor.GRAY + "No hay cultivos para abonar cerca");
            return;
        }
        consumir(player, mano);
        centro.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, centro.getLocation().add(0.5, 1, 0.5), 40, 2, 0.5, 2, 0);
        centro.getWorld().playSound(centro.getLocation(), Sound.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 1f, 0.9f);
    }

    private static boolean seAbona(Material tipo) {
        return Tag.CROPS.isTagged(tipo) || Tag.SAPLINGS.isTagged(tipo) || tipo == Material.SWEET_BERRY_BUSH || tipo == Material.COCOA
                || tipo == Material.MELON_STEM || tipo == Material.PUMPKIN_STEM || tipo == Material.BAMBOO_SAPLING
                || tipo == Material.BAMBOO || tipo == Material.CAVE_VINES || tipo == Material.CAVE_VINES_PLANT;
    }

    // Aldea en el Overworld, fortaleza en el Nether y ciudad en el End. La aguja apunta ahí
    @SuppressWarnings("deprecation")
    private void brujula(Player player, EquipmentSlot mano) {
        if (enEspera(player, "brujula", 5000)) return;
        World world = player.getWorld();
        org.bukkit.StructureType tipo;
        String nombre;
        switch (world.getEnvironment()) {
            case NETHER -> { tipo = org.bukkit.StructureType.NETHER_FORTRESS; nombre = "una fortaleza"; }
            case THE_END -> { tipo = org.bukkit.StructureType.END_CITY; nombre = "una ciudad del End"; }
            default -> { tipo = org.bukkit.StructureType.VILLAGE; nombre = "una aldea"; }
        }
        Location destino = world.locateNearestStructure(player.getLocation(), tipo, 64, false);
        if (destino == null) {
            avisar(player, ChatColor.of("#d9a5a0") + "No hay " + nombre.substring(nombre.indexOf(' ') + 1) + " cerca");
            return;
        }
        destino.setY(player.getLocation().getY());
        int distancia = (int) Math.round(destino.distance(player.getLocation()));
        player.sendMessage(ChatColor.of("#e8c07a") + "➤ " + ChatColor.GRAY + "Hay " + nombre + " a " + ChatColor.WHITE + distancia
                + " bloques" + ChatColor.GRAY + " hacia el " + ChatColor.WHITE + rumbo(player.getLocation(), destino)
                + ChatColor.GRAY + " (" + destino.getBlockX() + ", " + destino.getBlockZ() + ").");
        player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1f, 1f);

        ItemStack item = player.getInventory().getItem(mano);
        if (item.getItemMeta() instanceof CompassMeta compass) {
            compass.setLodestone(destino);
            compass.setLodestoneTracked(false);
            item.setItemMeta(compass);
            player.getInventory().setItem(mano, item);
        }
        gastarUso(player, mano);
    }

    private static String rumbo(Location desde, Location hasta) {
        double angulo = Math.toDegrees(Math.atan2(hasta.getX() - desde.getX(), desde.getZ() - hasta.getZ()));
        String[] rumbos = {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};
        return rumbos[(int) Math.round(((angulo % 360) + 360) % 360 / 45) % 8];
    }

    private void botiquin(Player player, EquipmentSlot mano) {
        if (enEspera(player, "botiquin", 20_000)) return;
        double maxima = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(maxima, player.getHealth() + 10));
        for (PotionEffect efecto : player.getActivePotionEffects()) {
            if (MALOS.contains(efecto.getType()) && efecto.getType() != PotionEffectType.LEVITATION) player.removePotionEffect(efecto.getType());
        }
        consumir(player, mano);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.8, 0), 6, 0.4, 0.3, 0.4, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 2f);
    }

    // Marca los minerales a 8 bloques con un contorno de su color que solo ve el que lo usó
    private void radar(Player player, EquipmentSlot mano) {
        if (enEspera(player, "radar", 10_000)) return;
        Location centro = player.getLocation();
        List<BlockDisplay> marcas = new ArrayList<>();
        for (int x = -8; x <= 8 && marcas.size() < 40; x++) {
            for (int y = -8; y <= 8 && marcas.size() < 40; y++) {
                for (int z = -8; z <= 8 && marcas.size() < 40; z++) {
                    Block bloque = centro.getBlock().getRelative(x, y, z);
                    Color color = colorMineral(bloque.getType());
                    if (color == null) continue;
                    BlockDisplay marca = bloque.getWorld().spawn(bloque.getLocation(), BlockDisplay.class, display -> {
                        display.setBlock(bloque.getBlockData());
                        display.setGlowing(true);
                        display.setGlowColorOverride(color);
                        display.setPersistent(false);
                        display.setVisibleByDefault(false);
                        display.setBrightness(new Display.Brightness(15, 15));
                        display.setTransformation(new Transformation(new Vector3f(-0.002f), new AxisAngle4f(),
                                new Vector3f(1.004f), new AxisAngle4f()));
                    });
                    player.showEntity(plugin, marca);
                    marcas.add(marca);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> marcas.forEach(Entity::remove), 200L);
        avisar(player, ChatColor.of("#b0e0ff") + "✦ " + (marcas.isEmpty() ? "No hay minerales cerca" : marcas.size() + " minerales marcados por 10 s"));
        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, 1f, 1.4f);
        gastarUso(player, mano);
    }

    static Color colorMineral(Material tipo) {
        if (Tag.DIAMOND_ORES.isTagged(tipo)) return Color.fromRGB(0x5DE8E0);
        if (Tag.EMERALD_ORES.isTagged(tipo)) return Color.fromRGB(0x3CDC6A);
        if (Tag.GOLD_ORES.isTagged(tipo)) return Color.fromRGB(0xF2C94C);
        if (Tag.IRON_ORES.isTagged(tipo)) return Color.fromRGB(0xD8B89A);
        if (Tag.COPPER_ORES.isTagged(tipo)) return Color.fromRGB(0xE07A4E);
        if (Tag.REDSTONE_ORES.isTagged(tipo)) return Color.fromRGB(0xE03A3A);
        if (Tag.LAPIS_ORES.isTagged(tipo)) return Color.fromRGB(0x3A5BE0);
        if (Tag.COAL_ORES.isTagged(tipo)) return Color.fromRGB(0x555555);
        if (tipo == Material.NETHER_QUARTZ_ORE) return Color.fromRGB(0xEDE6DD);
        if (tipo == Material.ANCIENT_DEBRIS) return Color.fromRGB(0x8A5A44);
        for (WardenBiome bioma : WardenBiome.values()) {
            if (bioma.ore() == tipo) return Color.fromRGB(0x9F7BFF);
        }
        return null;
    }

    // Shift + click marca el lugar; click viaja después de 3 segundos sin moverse ni recibir daño
    private void cristalEco(Player player, EquipmentSlot mano) {
        ItemStack item = player.getInventory().getItem(mano);
        ItemMeta meta = item.getItemMeta();
        if (player.isSneaking()) {
            Location aqui = player.getLocation();
            meta.getPersistentDataContainer().set(LUGAR, PersistentDataType.STRING, aqui.getWorld().getName() + ";" + aqui.getX() + ";"
                    + aqui.getY() + ";" + aqui.getZ() + ";" + aqui.getYaw() + ";" + aqui.getPitch());
            meta.lore(loreConLinea("cristal_eco", Component.text("Marcado en: ", NamedTextColor.GRAY).append(Component.text(
                    aqui.getBlockX() + " " + aqui.getBlockY() + " " + aqui.getBlockZ() + " (" + aqui.getWorld().getName() + ")", TextColor.color(0x5AD1C5)))));
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
            player.getInventory().setItem(mano, item);
            avisar(player, ChatColor.of("#5ad1c5") + "✦ Lugar guardado en el Cristal de Eco");
            player.playSound(aqui, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.2f);
            return;
        }
        String guardado = meta.getPersistentDataContainer().get(LUGAR, PersistentDataType.STRING);
        if (guardado == null) {
            avisar(player, ChatColor.GRAY + "Primero marca un lugar con shift + click derecho");
            return;
        }
        String[] partes = guardado.split(";");
        World mundo = Bukkit.getWorld(partes[0]);
        if (mundo == null || !mundo.equals(player.getWorld())) {
            avisar(player, ChatColor.of("#d9a5a0") + "El lugar marcado está en otra dimensión");
            return;
        }
        if (!viajando.add(player.getUniqueId())) return;
        Location destino = new Location(mundo, Double.parseDouble(partes[1]), Double.parseDouble(partes[2]), Double.parseDouble(partes[3]),
                Float.parseFloat(partes[4]), Float.parseFloat(partes[5]));
        Location inicio = player.getLocation();
        double vida = player.getHealth();
        new BukkitRunnable() {
            int segundos = 3;

            @Override
            public void run() {
                if (!player.isOnline() || player.getLocation().distanceSquared(inicio) > 0.25 || player.getHealth() < vida) {
                    viajando.remove(player.getUniqueId());
                    if (player.isOnline()) avisar(player, ChatColor.of("#d9a5a0") + "Viaje cancelado");
                    cancel();
                    return;
                }
                if (segundos > 0) {
                    avisar(player, ChatColor.of("#5ad1c5") + "Viajando en " + segundos + "...");
                    player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 20, 0.4, 0.8, 0.4, 0.05);
                    segundos--;
                    return;
                }
                viajando.remove(player.getUniqueId());
                cancel();
                if (!quitarCristal(player, guardado)) return;
                player.teleport(destino);
                mundo.playSound(destino, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1.2f);
                mundo.spawnParticle(Particle.REVERSE_PORTAL, destino.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.1);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // El cristal se gasta al final del viaje, si sigue en el inventario
    private boolean quitarCristal(Player player, String lugar) {
        ItemStack[] contenido = player.getInventory().getContents();
        for (int slot = 0; slot < contenido.length; slot++) {
            ItemStack item = contenido[slot];
            if (!"cristal_eco".equals(ItemsTienda.idOf(item))) continue;
            if (!lugar.equals(item.getItemMeta().getPersistentDataContainer().get(LUGAR, PersistentDataType.STRING))) continue;
            player.getInventory().setItem(slot, null);
            return true;
        }
        avisar(player, ChatColor.of("#d9a5a0") + "Ya no tienes el cristal");
        return false;
    }

    private void propulsor(Player player, EquipmentSlot mano) {
        if (!player.isGliding()) {
            avisar(player, ChatColor.GRAY + "Úsalo mientras planeas con Elytra");
            return;
        }
        if (enEspera(player, "propulsor", 1000)) return;
        Vector impulso = player.getLocation().getDirection().multiply(2.2).add(player.getVelocity().multiply(0.3));
        player.setVelocity(impulso);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 30, 0.3, 0.3, 0.3, 0.1);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 15, 0.3, 0.3, 0.3, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 0.8f);
        gastarUso(player, mano);
    }

    private void estandarte(Player player, EquipmentSlot mano) {
        int cuantos = 0;
        for (Player cerca : player.getWorld().getPlayers()) {
            if (cerca.getLocation().distanceSquared(player.getLocation()) > 144) continue;
            cerca.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 0));
            cerca.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 0));
            if (cerca != player) avisar(cerca, ChatColor.of("#d94a4a") + "⚔ " + player.getName() + " alzó el Estandarte de Guerra");
            cuantos++;
        }
        consumir(player, mano);
        Particle.DustOptions rojo = new Particle.DustOptions(Color.fromRGB(0xD94A4A), 1.6f);
        Location centro = player.getLocation();
        for (int i = 0; i < 48; i++) {
            double angulo = Math.PI * 2 * i / 48;
            centro.getWorld().spawnParticle(Particle.DUST, centro.clone().add(Math.cos(angulo) * 4, 0.2, Math.sin(angulo) * 4), 1, rojo);
        }
        centro.getWorld().playSound(centro, Sound.ITEM_GOAT_HORN_SOUND_0, SoundCategory.PLAYERS, 1.5f, 1f);
        avisar(player, ChatColor.of("#d94a4a") + "⚔ Estandarte alzado: " + cuantos + (cuantos == 1 ? " jugador" : " jugadores") + " con Fuerza y Resistencia");
    }

    private void caja(Player player, EquipmentSlot mano) {
        Object[] premio = ItemsTienda.premioCaja(random.nextInt(100));
        ItemStack item = CustomItemRegistry.getCustomItem((String) premio[1], (int) premio[2]);
        if (item == null) return;
        consumir(player, mano);
        player.getInventory().addItem(item).values().forEach(sobra -> player.getWorld().dropItem(player.getLocation(), sobra));
        boolean grande = (int) premio[0] <= 6;
        player.sendMessage(ChatColor.of("#f5d76e") + "✦ " + ChatColor.GRAY + "La Caja Misteriosa traía: "
                + LEGACY.serialize(item.effectiveName()) + ChatColor.GRAY + " x" + item.getAmount() + (grande ? ChatColor.of("#f5d76e") + " ¡Qué suerte!" : ""));
        player.getWorld().spawnParticle(grande ? Particle.TOTEM_OF_UNDYING : Particle.FIREWORK, player.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.2);
        player.playSound(player.getLocation(), grande ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, grande ? 0.7f : 0.6f, 1.4f);
    }

    // ---------------------------------------------------------------- Ayudas

    // Quita uno del stack de esa mano (en creativo no gasta)
    private void consumir(Player player, EquipmentSlot mano) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = player.getInventory().getItem(mano);
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItem(mano, item.getAmount() > 0 ? item : null);
    }

    // Los que tienen usos (brújula, radar y propulsor) se rompen al llegar a 0
    private void gastarUso(Player player, EquipmentSlot mano) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemStack item = player.getInventory().getItem(mano);
        String id = ItemsTienda.idOf(item);
        ItemMeta meta = item.getItemMeta();
        int maximo = ItemsTienda.usosMaximos(id);
        int quedan = meta.getPersistentDataContainer().getOrDefault(ItemsTienda.USOS, PersistentDataType.INTEGER, maximo) - 1;
        if (quedan <= 0) {
            player.getInventory().setItem(mano, null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1f, 1f);
            return;
        }
        meta.getPersistentDataContainer().set(ItemsTienda.USOS, PersistentDataType.INTEGER, quedan);
        List<String> lore = meta.getLore();
        if (lore != null && !lore.isEmpty()) {
            lore.set(lore.size() - 1, ItemsTienda.lineaUsos(quedan, maximo));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        player.getInventory().setItem(mano, item);
    }

    // La descripción del item recién creado más una línea propia (lo que tiene la red o el lugar del cristal)
    private static List<Component> loreConLinea(String id, Component extra) {
        List<Component> lore = new ArrayList<>();
        for (String linea : ItemsTienda.crear(id, 1).getItemMeta().getLore()) lore.add(LEGACY.deserialize(linea));
        if (extra != null) {
            lore.add(Component.empty());
            lore.add(extra);
        }
        return lore;
    }

    private boolean enEspera(Player player, String que, long millis) {
        String clave = player.getUniqueId() + ":" + que;
        long ahora = System.currentTimeMillis();
        Long hasta = esperas.get(clave);
        if (hasta != null && hasta > ahora) {
            avisar(player, ChatColor.GRAY + "Espera " + ((hasta - ahora) / 1000 + 1) + " s");
            return true;
        }
        esperas.put(clave, ahora + millis);
        return false;
    }

    private void avisar(Player player, String mensaje) {
        ActionBarHandler.get(plugin).sendNotification(player, "tienda:" + mensaje.hashCode(), mensaje);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String prefijo = event.getPlayer().getUniqueId() + ":";
        esperas.keySet().removeIf(clave -> clave.startsWith(prefijo));
    }
}
