package Dificultades;

import Bosses.QueenBeeHandler;
import Dificultades.CustomMobs.*;
import Dificultades.Features.AltarActivateEvent;
import Handlers.DayHandler;
import items.CorruptedGoldenApple;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.spectralmemories.bloodmoon.BloodmoonActuator;

import java.util.*;

public class DayOneChanges implements Listener {
    private final DayHandler dayHandler;
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private boolean isApplied = false;
    private final CorruptedZombies corruptedZombies;
    private final CorruptedSpider corruptedSpider;
    private final Map<UUID, Long> cooldownPlayers = new HashMap<>();

    // FEATURES DEL DÍA 2
    private final Map<LivingEntity, Long> trackedMobs = new HashMap<>();
    private BukkitTask targetTask;
    // FEATURES DEL DÍA 4
    private final Map<Location, Long> altarCooldowns = new HashMap<>();
    private final GuardianBlaze blazespawmer;
    private final GuardianCorruptedSkeleton guardianCorruptedSkeleton;
    private final CorruptedInfernalSpider corruptedInfernalSpider;
    private final CorruptedBee corruptedBee;
    private final Bombita bombitaSpawner;;
    private final Iceologer iceologerSpawner;
    private final NamespacedKey uuidKey;
    private final NamespacedKey upgradeKey;

    public DayOneChanges(JavaPlugin plugin, DayHandler handler) {
        this.plugin = plugin;
        this.dayHandler = handler;
        this.corruptedZombies = new CorruptedZombies(plugin);
        this.corruptedSpider = new CorruptedSpider(plugin, handler);

        //INICIALIZACIÓN DE LOS FEATURES DEL DÍA 4
        this.blazespawmer = new GuardianBlaze(plugin);
        this.guardianCorruptedSkeleton = new GuardianCorruptedSkeleton(plugin);
        this.corruptedInfernalSpider = new CorruptedInfernalSpider(plugin);
        this.corruptedBee = new CorruptedBee(plugin);
        this.bombitaSpawner = new Bombita(plugin);
        this.iceologerSpawner = new Iceologer(plugin);
        this.uuidKey = new NamespacedKey(plugin, "creator_uuid");
        this.upgradeKey = new NamespacedKey(plugin, "is_upgrade");
    }

    public void apply() {
        if (!isApplied) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            isApplied = true;
            corruptedZombies.apply();
            corruptedSpider.apply();
            registerCustomRecipe();
            //APPLYS DEL DIA 2
            bombitaSpawner.apply();
            iceologerSpawner.apply();
            startTargetTask();
            //APPLYS DEL DIA 4
            blazespawmer.apply();
            guardianCorruptedSkeleton.apply();
            corruptedInfernalSpider.apply();
            corruptedBee.apply();
        }
    }

    public void revert() {
        if (isApplied) {
            corruptedZombies.revert();
            corruptedSpider.revert();
            NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");
            Bukkit.removeRecipe(key);
            //REVERTS DEL DIA 2
            bombitaSpawner.revert();
            iceologerSpawner.revert();
            if (targetTask != null && !targetTask.isCancelled()) {
                targetTask.cancel();
                targetTask = null;
            }
            trackedMobs.clear();
            //rEVERTS DEL DIA 4
            blazespawmer.revert();
            guardianCorruptedSkeleton.revert();
            corruptedInfernalSpider.revert();
            corruptedBee.revert();
            // Desregistrar eventos
            HandlerList.unregisterAll(this);

            isApplied = false;
        }
    }

    public static ItemStack corruptedSteak() {
        ItemStack item = new ItemStack(Material.COOKED_BEEF);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Carne Corrupta");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#ffcc99") + "Esta carne te otorga estos");
        lore.add(ChatColor.of("#ffcc99") + "efectos" + ChatColor.GRAY + ":");
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#99cc33") + "Náuseas 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "10 s" + ChatColor.GRAY + ")");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#cc3300") + "Saturación 1" + ChatColor.GRAY + " (" + ChatColor.of("#0099cc") + "1.5 s" + ChatColor.GRAY + ")");
        lore.add("");
        meta.setLore(lore);
        meta.setCustomModelData(2);
        meta.setRarity(ItemRarity.EPIC);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack improvedPumpkinPie() {
        ItemStack item = new ItemStack(Material.PUMPKIN_PIE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.of("#FF8C00") + "" + ChatColor.BOLD + "Tarta de Calabaza Mejorada");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.of("#FFCC99") + "Esta tarta te otorga estos");
        lore.add(ChatColor.of("#FFCC99") + "efectos" + ChatColor.GRAY + ":");
        lore.add("");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#FFA500") + "Lentitud 1" + ChatColor.GRAY + " (" + ChatColor.of("#FFD700") + "10 s" + ChatColor.GRAY + ")");
        lore.add(ChatColor.GRAY + "> " + ChatColor.of("#FF4500") + "Saturación 1" + ChatColor.GRAY + " (" + ChatColor.of("#FFD700") + "2.5 s" + ChatColor.GRAY + ")");
        lore.add("");

        meta.setLore(lore);
        meta.setCustomModelData(3);
        meta.setRarity(ItemRarity.EPIC);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        item.setItemMeta(meta);
        return item;
    }

    public void registerCustomRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "corrupted_steak");

        if (Bukkit.getRecipe(key) != null) {
            return;
        }

        ShapedRecipe customRecipe = new ShapedRecipe(key, corruptedSteak());
        customRecipe.shape(" F ", "FSF", " F ");
        customRecipe.setIngredient('F', Material.ROTTEN_FLESH);
        customRecipe.setIngredient('S', Material.COOKED_BEEF);

        plugin.getServer().addRecipe(customRecipe);
    }


    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        if (!isApplied) return;

        ItemStack item = event.getItem();
        if (item.getType() != Material.COOKED_BEEF) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 2) return;

        Player player = event.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 30, 0, false, false, true));
    }

    @EventHandler
    public void onPlayerEatPie(PlayerItemConsumeEvent event) {
        if (!isApplied) return;

        ItemStack item = event.getItem();
        if (item.getType() != Material.PUMPKIN_PIE) return;

        ItemMeta meta = item.getItemMeta();
        // Verificamos que sea nuestra tarta (CustomModelData 3)
        if (meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != 3) return;

        Player player = event.getPlayer();

        // Lentitud 1 por 10 segundos (200 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, false, false, true));

        // Saturación 1 por 2.5 segundos (50 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 50, 0, false, false, true));
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (!isApplied) return;
        if (event.getItem().isSimilar(CorruptedGoldenApple.createCorruptedGoldenApple())) {
            CorruptedGoldenApple.applyEffects(event.getPlayer());
        }
    }

    @EventHandler
    public void onHoneyConsume(PlayerItemConsumeEvent event) {
       ItemStack item = event.getItem();

        if (item.getType() == Material.HONEY_BOTTLE && item.hasItemMeta()) {
            if (item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 8001) {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 7200, 3));
            }
        }
    }

    @EventHandler
    public void onPlayerVoidDamage(EntityDamageEvent event) {
        if (!isApplied) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) return;

        long currentTime = System.currentTimeMillis();
        if (cooldownPlayers.containsKey(player.getUniqueId())) {
            long lastDamageTime = cooldownPlayers.get(player.getUniqueId());
            if (currentTime - lastDamageTime < 5000) {
                event.setCancelled(true);
                return;
            }
        }

        boolean tieneTotem = player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING ||
                player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;

        if (tieneTotem) {
            cooldownPlayers.put(player.getUniqueId(), currentTime);
            event.setCancelled(true);
            player.setNoDamageTicks(0);

            player.damage(player.getHealth() + 500.0);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isValid() && !player.isDead()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 3 * 20, 100, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 50 * 20, 0, false, false));
                    player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1.0f, 2.0f);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onBloodMoonMobKill(EntityDeathEvent event) {
        if (!isApplied) return;

        if (event.getEntity().getWorld().getEnvironment() != World.Environment.NORMAL) return;

        if (!(event.getEntity() instanceof org.bukkit.entity.Monster)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        BloodmoonActuator actuator = BloodmoonActuator.GetActuator(event.getEntity().getWorld());
        if (actuator != null && actuator.isInProgress()) {

            int chance = random.nextInt(100);
            int amount = 0;

            if (chance < 5) {
                amount = 2;
            } else if (chance < 25) {
                amount = 1;
            } else {
                amount = 0;
            }

            if (amount > 0) {
                ItemStack tokens = items.EconomyItems.createBloodFragment();
                tokens.setAmount(amount);
                event.getDrops().add(tokens);
            }
        }
    }

    //----------------------
    //SISTEMA DE ALTARES
    //----------------------

    @EventHandler
    public void onAltarActivate(AltarActivateEvent event) {
        if (!isApplied) return;

        if (event.getAltarType().equals("queen_bee")) {
            Player player = event.getPlayer();
            Location loc = event.getLocation();

            if (player.getPotionEffect(PotionEffectType.BAD_OMEN) != null) {

                if (isQueenBeeSpawned(loc)) {
                    player.sendMessage(net.md_5.bungee.api.ChatColor.RED + "۞ Ya hay una Reina viva cerca.");
                    return;
                }

                spawnQueenBee(loc);
                player.removePotionEffect(PotionEffectType.BAD_OMEN);
                event.setCooldownSeconds(10800);

            } else {
                player.sendMessage(net.md_5.bungee.api.ChatColor.RED + "۞ Necesitas Bad Omen para activar este altar.");
            }
        }
    }

    private boolean isQueenBeeSpawned(Location altarLocation) {
        for (Entity entity : Objects.requireNonNull(altarLocation.getWorld()).getNearbyEntities(altarLocation, 50, 50, 50)) {
            if (entity instanceof Bee bee && "Abeja Reina".equals(bee.getCustomName())) {
                return true;
            }
        }
        return false;
    }

    private void spawnQueenBee(Location altarLocation) {
        World world = altarLocation.getWorld();
        if (world == null) return;

        Location center = altarLocation.clone().add(0.5, 0.5, 0.5);
        int totalSteps = 100;
        int finalY = 4;

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (step <= totalSteps) {
                    double progress = (double) step / totalSteps;
                    double yOffset = progress * finalY;

                    Location particleLoc = center.clone().add(0, yOffset, 0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 5, 0.1, 0, 0.1, 0.01);

                    if (step % 20 == 0) {
                        world.playSound(center, Sound.BLOCK_HONEY_BLOCK_SLIDE, 3.0f, 0.5f + (float)progress);
                    }

                    step++;
                } else if (step <= totalSteps + 40) {
                    double angle = (step - totalSteps) * (Math.PI / 20);
                    for (int i = 0; i < 360; i += 10) {
                        double radians = Math.toRadians(i);
                        double radius = 1.5 + Math.sin(angle) * 1.5;
                        double x = Math.cos(radians) * radius;
                        double z = Math.sin(radians) * radius;
                        Location loc = center.clone().add(0, finalY, 0).add(x, 0, z);
                        world.spawnParticle(Particle.END_ROD, loc, 1, 0, 0, 0, 0);
                    }

                    if ((step - totalSteps) % 10 == 0) {
                        world.playSound(center.clone().add(0, finalY, 0), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 3.0f, 1.5f);
                    }

                    step++;
                } else {
                    Location spawnLocation = center.clone().add(0, finalY, 0);
                    world.spawnParticle(Particle.EXPLOSION, spawnLocation, 1);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, spawnLocation, 100, 0.5, 1, 0.5, 0.1);
                    world.playSound(spawnLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 5.0f, 1.0f);

                    QueenBeeHandler.spawn(plugin, spawnLocation);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    //----------------------
    // SISTEMA DE RAIDS
    //----------------------

    private void startTargetTask() {
        targetTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (trackedMobs.isEmpty()) return;
                updateTargets();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onRaidWaveSpawn(RaidSpawnWaveEvent event) {
        if (!isApplied) return;

        int currentWave = event.getRaid().getSpawnedGroups();

        if (random.nextDouble() <= 0.16) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isApplied) return;

                sendRaidWarning(event);
                spawnCorruptedMobs(event, "corruptedzombie", 10);
                spawnCorruptedMobs(event, "corruptedspider", 8);
            }, 40L);
        }

        // 2. Reemplazo seguro de mobs por Bombitas
        // Esperamos 1 tick para que la Raid se genere completamente antes de tocarla
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!isApplied) return;

            int replacedCount = 0;
            // Evitamos reemplazar a los bosses de la raid (Evokers/Ravagers) si no es necesario, preferimos a los Vindicators/Pillagers
            for (Entity entity : event.getRaiders()) {
                if (entity instanceof Raider && entity.isValid() && !entity.isDead()) {
                    if (replacedCount < currentWave) {
                        Location spawnLocation = entity.getLocation();
                        Creeper bombita = bombitaSpawner.spawnBombita(spawnLocation);
                        trackedMobs.put(bombita, System.currentTimeMillis());
                        entity.remove();
                        replacedCount++;
                    } else {
                        break;
                    }
                }
            }
        }, 1L);

        // 3. Iceologers a partir de la oleada 2
        if (currentWave >= 2) {
            int iceologerCount = random.nextInt(2) + 1;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!isApplied) return;

                int spawned = 0;
                for (Entity entity : event.getRaiders()) {
                    if (entity instanceof Pillager && entity.isValid() && !entity.isDead()) {
                        if (spawned < iceologerCount) {
                            Location spawnLocation = entity.getLocation();
                            iceologerSpawner.spawnIceologer(spawnLocation);
                            entity.remove();
                            spawned++;
                        } else {
                            break;
                        }
                    }
                }
            }, 20L);
        }
    }

    // Actualiza los objetivos dinámicamente
    private void updateTargets() {
        Iterator<Map.Entry<LivingEntity, Long>> iterator = trackedMobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = iterator.next();
            LivingEntity mob = entry.getKey();

            // Limpieza automática si el mob murió
            if (mob == null || !mob.isValid() || mob.isDead()) {
                iterator.remove();
                continue;
            }

            // Solo forzamos target si es un Mob con IA
            if (mob instanceof Mob activeMob) {
                LivingEntity currentTarget = activeMob.getTarget();

                if (currentTarget != null && currentTarget.isValid() && !currentTarget.isDead()
                        && (currentTarget instanceof Player || currentTarget instanceof Villager)) {
                    continue;
                }

                LivingEntity newTarget = findTarget(mob);
                if (newTarget != null) {
                    activeMob.setTarget(newTarget);
                }
            }
        }
    }

    private LivingEntity findTarget(Entity mob) {
        World world = mob.getWorld();

        // 1. Prioridad: Jugadores (Búsqueda rápida)
        Player closestPlayer = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                double distSq = p.getLocation().distanceSquared(mob.getLocation());
                if (distSq < minDistanceSq && distSq <= 2500) {
                    minDistanceSq = distSq;
                    closestPlayer = p;
                }
            }
        }

        if (closestPlayer != null) {
            return closestPlayer;
        }

        // 2. Secundaria: Aldeanos (Búsqueda lenta - solo si no hay jugadores)
        return world.getNearbyEntities(mob.getLocation(), 50, 50, 50).stream()
                .filter(e -> e instanceof Villager && !e.isDead())
                .map(e -> (LivingEntity) e)
                .min(Comparator.comparingDouble(v -> v.getLocation().distanceSquared(mob.getLocation())))
                .orElse(null);
    }

    private void spawnCorruptedMobs(RaidSpawnWaveEvent event, String mobType, int count) {
        List<Location> spawnLocations = getSpawnLocations(event, count);

        for (Location location : spawnLocations) {
            LivingEntity corruptedMob = null;

            if (mobType.equals("corruptedzombie")) {
                corruptedMob = corruptedZombies.spawnCorruptedZombie(location);
            } else if (mobType.equals("corruptedspider")) {
                corruptedMob = corruptedSpider.spawnCorruptedSpider(location);
            }

            if (corruptedMob != null) {
                trackedMobs.put(corruptedMob, System.currentTimeMillis());
            }
        }
    }

    // Obtener ubicaciones de spawn cerca de los Raiders
    private List<Location> getSpawnLocations(RaidSpawnWaveEvent event, int count) {
        List<Location> locations = new ArrayList<>();
        List<Entity> raiders = new ArrayList<>(event.getRaiders());

        if (raiders.isEmpty()) return locations;

        for (int i = 0; i < count; i++) {
            Entity raider = raiders.get(random.nextInt(raiders.size()));
            Location spawnLocation = raider.getLocation().clone();

            // Añade un pequeño desplazamiento aleatorio para dispersar los mobs
            spawnLocation.add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);

            // Validar que no spawnee en bloque solido
            int y = spawnLocation.getWorld().getHighestBlockYAt(spawnLocation);
            spawnLocation.setY(y + 1);

            locations.add(spawnLocation);
        }

        return locations;
    }

    private void sendRaidWarning(RaidSpawnWaveEvent event) {
        Sound sound = Sound.ENTITY_ENDER_DRAGON_GROWL;

        String jsonMessage = "[\"\",{\"text\":\"\\u06de\",\"bold\":true,\"color\":\"#C17CE5\"}," +
                "{\"text\":\" Ha aparecido una oleada de\",\"color\":\"#E28761\"}," +
                "{\"text\":\" Corrupted Mobs \",\"bold\":true,\"color\":\"dark_purple\"}," +
                "{\"text\":\"\\u26a0\",\"bold\":true,\"color\":\"dark_red\"}]";

        Location raidLoc = event.getRaid().getLocation();
        for (Player player : raidLoc.getWorld().getPlayers()) {
            if (raidLoc.distanceSquared(player.getLocation()) <= 10000) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tellraw " + player.getName() + " " + jsonMessage);

                player.playSound(player.getLocation(), sound, 2.0f, 0.1f);
            }
        }
    }
}