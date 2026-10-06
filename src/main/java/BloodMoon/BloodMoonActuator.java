package BloodMoon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/** Lógica del JAR original por mundo, con tareas cancelables y spawns seguros. */
public final class BloodMoonActuator implements Listener {
    private static final Set<EntityType> REWARDED = EnumSet.of(EntityType.ZOMBIE, EntityType.SKELETON,
            EntityType.SPIDER, EntityType.CREEPER, EntityType.HUSK, EntityType.DROWNED, EntityType.WITCH,
            EntityType.ZOMBIE_VILLAGER, EntityType.PHANTOM, EntityType.ENDERMAN);
    private final BloodMoon manager;
    private final World world;
    private final BloodMoonCycle cycle;
    private final Random random = new Random();
    private final SafeSpawnFinder spawnFinder = new SafeSpawnFinder(random);
    private final Map<EntityType, SafeSpawnFinder.Size> sizes = new EnumMap<>(EntityType.class);
    private final Set<UUID> blacklisted = new HashSet<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private BossBar nightBar;
    private boolean active;
    private boolean closed;
    private long activeDay = -1;
    private long generation;
    private int originalSpawnLimit;
    private boolean originalStorm, originalThunder;
    private boolean controlsWeather;
    private int originalWeatherDuration, originalThunderDuration;

    public enum HordeResult { SPAWNED, BLOCKED, NO_SAFE_LOCATION, DISABLED }
    private record PlannedSpawn(EntityType type, Location location, SafeSpawnFinder.Size size) {}
    private record Bonus(String[] parts, int weight) {}

    public BloodMoonActuator(BloodMoon manager, World world, BloodMoonCycle cycle) {
        this.manager = manager;
        this.world = world;
        this.cycle = cycle;
    }
    public static BloodMoonActuator GetActuator(World world) { return BloodMoon.GetInstance() == null ? null : BloodMoon.GetInstance().getActuator(world); }
    public World getWorld() { return world; }
    public BloodMoonCycle getCycle() { return cycle; }
    public long getActiveDay() { return activeDay; }
    public boolean isInProgress() { return active; }
    private ConfigReader config() { return manager.getConfigReader(world); }

    public void checkNight() {
        if (closed) return;
        ConfigReader reader = config();
        long day = world.getFullTime() / 24000, time = world.getTime();
        cycle.observe(day, reader.GetIntervalConfig());
        if (reader.GetPermanentBloodMoonConfig()) {
            if (!active) StartBloodMoon();
            return;
        }
        if (active && (time < 12000 || day != activeDay)) {
            finish(true);
            return;
        }
        if (active) return;
        if (cycle.shouldWarn(day, time)) {
            long nights = cycle.nextNight() - day;
            if (nights <= 0) LocaleReader.MessageAllLocale("BloodMoonTonight", null, null, world);
            else if (nights == 1) LocaleReader.MessageAllLocale("BloodMoonTomorrow", null, null, world);
            else LocaleReader.MessageAllLocale("DaysBeforeBloodMoon", new String[]{"$d"}, new String[]{String.valueOf(nights)}, world);
        }
        if (cycle.isDue(day, time)) StartBloodMoon();
    }

    public boolean StartBloodMoon() {
        if (active || closed) return false;
        active = true;
        generation++;
        activeDay = world.getFullTime() / 24000;
        cycle.started(activeDay, config().GetIntervalConfig());
        originalSpawnLimit = world.getSpawnLimit(SpawnCategory.MONSTER);
        originalStorm = world.hasStorm(); originalThunder = world.isThundering();
        originalWeatherDuration = world.getWeatherDuration(); originalThunderDuration = world.getThunderDuration();
        controlsWeather = config().GetThunderingConfig();
        runCommands(config().GetPreBloodMoonCommands());
        if (!active || closed) return false;
        world.setSpawnLimit(SpawnCategory.MONSTER, config().GetSpawnRateConfig());
        if (!config().GetPermanentBloodMoonConfig()) {
            BarFlag[] flags = config().GetDarkenSkyConfig() ? new BarFlag[]{BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY} : new BarFlag[0];
            nightBar = Bukkit.createBossBar(manager.getLocaleReader().GetLocaleString("BloodMoonTitleBar"), BarColor.RED, BarStyle.SEGMENTED_12, flags);
            for (Player player : world.getPlayers()) nightBar.addPlayer(player);
            repeat(this::updateNightBar, 0, 20);
        }
        for (Player player : world.getPlayers()) warning(player);
        ambient();
        scheduleHorde();
        manager.remember(this);
        return true;
    }

    public void StopBloodMoon() { if (!config().GetPermanentBloodMoonConfig()) finish(true); }
    private void finish(boolean notify) {
        if (!active) return;
        active = false;
        generation++;
        for (BukkitTask task : tasks) task.cancel();
        tasks.clear();
        if (nightBar != null) { nightBar.removeAll(); nightBar = null; }
        blacklisted.clear();
        world.setSpawnLimit(SpawnCategory.MONSTER, originalSpawnLimit);
        if (controlsWeather) {
            world.setStorm(originalStorm); world.setThundering(originalThunder);
            world.setWeatherDuration(originalWeatherDuration); world.setThunderDuration(originalThunderDuration);
        }
        runCommands(config().GetPostBloodMoonCommands());
        if (notify) {
            LocaleReader.MessageAllLocale("BloodMoonEndingMessage", null, null, world);
            for (Player player : world.getPlayers()) {
                LocaleReader.actionBar(player, LocaleReader.RED + "La BloodMoon ha terminado.");
                if (config().GetBloodMoonEndSoundConfig()) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            }
            manager.remember(this);
        }
    }

    public void reload() {
        if (active) {
            finish(false);
            if (config().GetPermanentBloodMoonConfig() || world.getTime() >= 12000) StartBloodMoon();
        } else if (config().GetPermanentBloodMoonConfig()) StartBloodMoon();
        manager.remember(this);
    }
    public void shutdown() { finish(false); closed = true; }

    private void later(Runnable action, long delay) {
        long version = generation;
        BukkitTask[] handle = new BukkitTask[1];
        handle[0] = manager.GetScheduler().runTaskLater(manager.getPlugin(), () -> {
            tasks.remove(handle[0]);
            if (active && !closed && generation == version) action.run();
        }, Math.max(1, delay));
        tasks.add(handle[0]);
    }
    private void repeat(Runnable action, long delay, long period) {
        long version = generation;
        tasks.add(manager.GetScheduler().runTaskTimer(manager.getPlugin(), () -> {
            if (active && !closed && generation == version) action.run();
        }, delay, period));
    }

    private void updateNightBar() {
        if (nightBar != null) nightBar.setProgress(Math.clamp((world.getTime() - 12000) / 12000.0, 0, 1));
    }
    private void ambient() {
        if (config().GetBloodMoonPeriodicSoundConfig()) for (Player player : world.getPlayers()) player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.7f);
        if (config().GetThunderingConfig()) {
            controlsWeather = true;
            world.setStorm(true); world.setThundering(true); world.setThunderDuration(12000); world.setWeatherDuration(12000);
        }
        later(this::ambient, random.nextInt(200) + 320);
    }
    private void scheduleHorde() {
        int base = config().GetHordeSpawnrateBaseline(), variation = Math.min(base, config().GetHordeSpawnrateVariation());
        long delay = Math.max(1, base - variation + random.nextInt(variation * 2 + 1));
        later(() -> {
            try { SpawnHorde(); }
            catch (RuntimeException ex) { manager.getPlugin().getLogger().warning("No se pudo generar una horda de BloodMoon: " + ex.getMessage()); }
            finally { if (active && !closed) scheduleHorde(); }
        }, delay);
    }

    public HordeResult SpawnHorde() {
        List<Player> players = world.getPlayers().stream().filter(this::eligibleTarget).toList();
        if (players.isEmpty()) return HordeResult.DISABLED;
        return SpawnHorde(players.get(random.nextInt(players.size())));
    }
    private boolean eligibleTarget(Player player) {
        return player.isOnline() && !player.isDead() && player.getWorld().equals(world)
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE && player.getGameMode() != org.bukkit.GameMode.SPECTATOR;
    }
    public HordeResult SpawnHorde(Player target) {
        if (closed || !config().GetHordeEnabled() || !eligibleTarget(target)) return HordeResult.DISABLED;
        List<EntityType> types = new ArrayList<>();
        for (String value : config().GetHordeMobWhitelist()) {
            try {
                EntityType type = EntityType.valueOf(value.toUpperCase(Locale.ROOT));
                if (type.isSpawnable() && type.isAlive() && type.getEntityClass() != null && Mob.class.isAssignableFrom(type.getEntityClass())) types.add(type);
            } catch (IllegalArgumentException ex) { manager.getPlugin().getLogger().warning("Mob de horda inválido: " + value); }
        }
        if (types.isEmpty()) return HordeResult.DISABLED;
        int min = Math.min(128, config().GetHordeMinPopulation()), max = Math.min(128, Math.max(min, config().GetHordeMaxPopulation()));
        int amount = min + random.nextInt(max - min + 1);
        List<PlannedSpawn> plan = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            EntityType type = types.get(random.nextInt(types.size()));
            SafeSpawnFinder.Size size = sizeFor(target.getLocation(), type);
            spawnFinder.find(target.getLocation(), config().GetHordeSpawnDistance(), size)
                    .filter(location -> WorldGuardSupport.canSpawn(location))
                    .ifPresent(location -> plan.add(new PlannedSpawn(type, location, size)));
        }
        BloodMoonHordeEvent event = new BloodMoonHordeEvent(target, plan.stream().map(PlannedSpawn::location).toList());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return HordeResult.BLOCKED;
        int spawned = 0;
        for (PlannedSpawn entry : plan) {
            Entity entity = spawnSafe(entry.location(), entry.type(), entry.size());
            if (entity instanceof Mob mob) { mob.setTarget(target); spawned++; world.strikeLightningEffect(entry.location()); }
        }
        if (spawned == 0) return HordeResult.NO_SAFE_LOCATION;
        LocaleReader.MessageAllLocale("HordeArrived", new String[]{"$p"}, new String[]{target.getName()}, world);
        LocaleReader.actionBar(target, LocaleReader.RED + "¡Una horda de BloodMoon te acecha!");
        return HordeResult.SPAWNED;
    }

    SafeSpawnFinder.Size sizeFor(Location origin, EntityType type) { return sizes.computeIfAbsent(type, ignored -> spawnFinder.sizeOf(origin, type)); }
    Optional<Location> findSafe(Location origin, int radius, EntityType type) { return spawnFinder.find(origin, radius, sizeFor(origin, type)).filter(WorldGuardSupport::canSpawn); }
    Entity spawnSafe(Location location, EntityType type, SafeSpawnFinder.Size size) {
        if (!SafeSpawnFinder.isSafe(location, size) || !WorldGuardSupport.canSpawn(location)) return null;
        Entity entity = world.spawnEntity(location, type, CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (!entity.isValid()) return null; // Respeta los listeners que cancelan CreatureSpawnEvent.
        if (!SafeSpawnFinder.isSafe(location, new SafeSpawnFinder.Size(entity.getWidth(), entity.getHeight()))) {
            entity.remove(); // Solo el ejemplar recién creado que no cabe; nunca mobs existentes.
            return null;
        }
        return entity;
    }

    public void AddToBlacklist(LivingEntity entity) { blacklisted.add(entity.getUniqueId()); }

    public ItemStack GetRandomBonus() {
        List<Bonus> bonuses = new ArrayList<>();
        int total = 0;
        for (String entry : config().GetItemListConfig()) {
            try {
                String[] parts = entry.split(":");
                int amount = Integer.parseInt(parts[1]), weight = Integer.parseInt(parts[2]);
                Material material = Material.valueOf(parts[0]);
                if (amount <= 0 || weight <= 0 || material.isAir() || (long) total + weight > Integer.MAX_VALUE) continue;
                bonuses.add(new Bonus(parts, weight)); total += weight;
            } catch (RuntimeException ex) { manager.getPlugin().getLogger().warning("Recompensa de BloodMoon inválida: " + entry); }
        }
        if (total == 0) return null;
        int choice = random.nextInt(total);
        for (Bonus bonus : bonuses) {
            choice -= bonus.weight();
            if (choice >= 0) continue;
            String[] parts = bonus.parts();
            ItemStack item = new ItemStack(Material.valueOf(parts[0]), Math.min(64, Integer.parseInt(parts[1])));
            for (int i = 3; i < parts.length; i++) {
                String value = parts[i];
                var meta = item.getItemMeta();
                if (value.startsWith("$name ")) { meta.setDisplayName(LocaleReader.color(value.substring(6))); item.setItemMeta(meta); }
                else if (value.startsWith("$desc ")) { meta.setLore(Arrays.stream(value.substring(6).split("\\$n")).map(LocaleReader::color).toList()); item.setItemMeta(meta); }
                else if (value.startsWith("$enchant ")) for (String spec : value.substring(9).split(";")) {
                    try {
                        String[] enchant = spec.split(",");
                        Enchantment type = Enchantment.getByKey(NamespacedKey.minecraft(enchant[0].toLowerCase(Locale.ROOT)));
                        if (type != null) item.addUnsafeEnchantment(type, Integer.parseInt(enchant[1]));
                    } catch (RuntimeException ex) { manager.getPlugin().getLogger().warning("Encantamiento de recompensa inválido: " + spec); }
                }
            }
            return item;
        }
        return null;
    }

    private void runCommands(String[] entries) {
        for (String entry : entries) {
            int separator = entry.lastIndexOf(';');
            if (separator < 0) { manager.getPlugin().getLogger().warning("Comando de BloodMoon sin modo ;s, ;p o ;f: " + entry); continue; }
            String command = entry.substring(0, separator).replace("$w", world.getName());
            String mode = entry.substring(separator + 1).trim().toLowerCase(Locale.ROOT);
            if (mode.equals("s")) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            else if (mode.equals("p") || mode.equals("f")) for (Player player : world.getPlayers()) {
                String resolved = command.replace("$p", player.getName());
                if (mode.equals("p")) player.performCommand(resolved);
                else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
            else manager.getPlugin().getLogger().warning("Modo de comando de BloodMoon inválido: " + mode);
        }
    }

    private void warning(Player player) {
        LocaleReader.MessageLocale("BloodMoonWarningTitle", null, null, player);
        LocaleReader.MessageLocale("BloodMoonWarningBody", null, null, player);
        LocaleReader.actionBar(player, LocaleReader.RED + "Ha empezado una BloodMoon.");
    }
    private void syncPlayer(Player player) {
        if (nightBar != null) {
            if (player.getWorld().equals(world) && active) nightBar.addPlayer(player);
            else nightBar.removePlayer(player);
        }
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) { if (active && event.getPlayer().getWorld().equals(world)) { syncPlayer(event.getPlayer()); warning(event.getPlayer()); } }
    @EventHandler public void onChangedWorld(PlayerChangedWorldEvent event) { syncPlayer(event.getPlayer()); if (active && event.getPlayer().getWorld().equals(world)) warning(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { if (nightBar != null) nightBar.removePlayer(event.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) { if (active) later(() -> syncPlayer(event.getPlayer()), 1); }
    @EventHandler(ignoreCancelled = true) public void onSleep(PlayerBedEnterEvent event) {
        if (active && event.getPlayer().getWorld().equals(world) && config().GetPreventSleepingConfig()) { event.setCancelled(true); LocaleReader.MessageLocale("BedNotAllowed", null, null, event.getPlayer()); }
    }
    @EventHandler public void onDeath(PlayerDeathEvent event) {
        if (!active || !event.getEntity().getWorld().equals(world)) return;
        if (config().GetLightningEffectConfig()) world.strikeLightningEffect(event.getEntity().getLocation());
        String suffix = manager.getLocaleReader().GetLocaleString("DeathSuffix");
        String text = event.getDeathMessage();
        if (text != null && !suffix.isEmpty() && !text.contains(suffix)) event.setDeathMessage(text + " " + suffix);
        // La BloodMoon no cambia drops, keepInventory ni experiencia de los jugadores.
    }
    @EventHandler public void onSpawner(SpawnerSpawnEvent event) {
        if (active && event.getEntity().getWorld().equals(world) && config().GetMobsFromSpawnerNoRewardConfig() && event.getEntity() instanceof LivingEntity living) AddToBlacklist(living);
    }
    @EventHandler public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getWorld().equals(world)) return;
        if (!active || !REWARDED.contains(entity.getType()) || blacklisted.remove(entity.getUniqueId())) return;
        event.setDroppedExp(event.getDroppedExp() * config().GetExpMultConfig());
        if (config().GetMobDeathThunderConfig()) world.strikeLightningEffect(entity.getLocation());
        int min = Math.min(128, config().GetMinItemsDropConfig()), max = Math.min(128, Math.max(min, config().GetMaxItemsDropConfig()));
        int count = min + random.nextInt(max - min + 1);
        for (int i = 0; i < count; i++) { ItemStack bonus = GetRandomBonus(); if (bonus != null) event.getDrops().add(bonus); }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH) public void onDamage(EntityDamageByEntityEvent event) {
        if (!active || !event.getEntity().getWorld().equals(world)) return;
        Entity attacker = event.getDamager();
        if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) attacker = shooter;
        if (event.getEntity() instanceof Player player && attacker instanceof LivingEntity mob && REWARDED.contains(mob.getType())) {
            if (event.getFinalDamage() <= 0 && config().GetShieldPreventEffects()) return;
            event.setDamage(event.getDamage() * config().GetMobDamageMultConfig());
            if (WorldGuardSupport.canDamage(player.getLocation())) {
                String name = mob.getType().name();
                for (String spec : config().GetMobEffectConfig(name)) applyEffect(player, spec);
            }
            if (config().GetPlayerDamageSoundConfig()) player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 1.5f);
            if (config().GetPlayerHitParticleConfig()) world.spawnParticle(Particle.FLAME, player.getLocation(), 60);
        } else if (attacker instanceof Player && event.getEntity() instanceof LivingEntity mob && REWARDED.contains(mob.getType())) {
            event.setDamage(Math.ceil(event.getDamage() / config().GetMobHealthMultConfig()));
            if (config().GetMobHitParticleConfig()) world.spawnParticle(Particle.ENCHANTED_HIT, mob.getLocation(), 60);
        }
    }
    private void applyEffect(Player player, String spec) {
        if (spec.equals("LIGHTNING")) { world.strikeLightning(player.getLocation()); return; }
        try {
            String[] parts = spec.split(",");
            String name = parts[0].toLowerCase(Locale.ROOT);
            name = switch (name) { case "slow" -> "slowness"; case "increase_damage" -> "strength"; default -> name; };
            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(name));
            if (type != null) player.addPotionEffect(new PotionEffect(type, Math.max(1, (int) (Double.parseDouble(parts[1]) * 20)), Math.max(0, Integer.parseInt(parts[2]))));
        } catch (RuntimeException ex) { manager.getPlugin().getLogger().warning("Efecto de BloodMoon inválido: " + spec); }
    }
}
