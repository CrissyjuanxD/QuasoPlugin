package Habilidades;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class HabilidadesEffects {

    private final JavaPlugin plugin;
    // UUID Constante para que el modificador de vida no se duplique infinitamente
    private static final UUID VITALIDAD_MODIFIER_UUID = UUID.fromString("c07bb6b6-3dc9-4a94-81d3-3561937dbf24");

    public HabilidadesEffects(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void playUnlockAnimation(Player player, HabilidadesType type, int level) {
        Location loc = player.getLocation();

        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 100, false, false));

        player.sendMessage("");
        player.sendMessage(ChatColor.of("#C77DFF") + "" + ChatColor.BOLD + "¡Habilidad Desbloqueada!");
        player.sendMessage(ChatColor.of("#E0AAFF") + type.getDisplayName() + " Nivel " + level);
        player.sendMessage("");

        new BukkitRunnable() {
            double y = 0;
            int stage = 0;
            BukkitRunnable magicCircleTask = null;
            BukkitRunnable fadeOutTask = null;

            @Override
            public void run() {
                if (stage == 0) {
                    if (y <= 2) {
                        spawnPoofParticles(player, y);
                        if (magicCircleTask == null) {
                            magicCircleTask = startMagicCircle(player, true);
                            magicCircleTask.runTaskTimer(plugin, 0, 2);
                        }
                        player.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.3f, 1.5f + (float) (y * 0.2));
                        player.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 0.2f, 1.8f + (float) (y * 0.1));

                        for (Player nearbyPlayer : loc.getWorld().getPlayers()) {
                            if (nearbyPlayer.getLocation().distance(loc) <= 20 && !nearbyPlayer.equals(player)) {
                                nearbyPlayer.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.2f, 1.5f + (float) (y * 0.2));
                                nearbyPlayer.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, 0.2f, 1.8f + (float) (y * 0.1));
                            }
                        }

                        y += 0.3;
                    } else {
                        stage = 1;
                        y = 0;
                    }
                } else if (stage == 1) {
                    if (y < 30) {
                        spawnElectricSphereVertical(player.getLocation().clone().add(0, 1, 0), y / 10.0);
                        if (y % 5 == 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.8f);
                            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 0.8f);

                            for (Player nearbyPlayer : loc.getWorld().getPlayers()) {
                                if (nearbyPlayer.getLocation().distance(loc) <= 20 && !nearbyPlayer.equals(player)) {
                                    nearbyPlayer.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 1.8f);
                                    nearbyPlayer.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.4f, 0.8f);
                                }
                            }
                        }
                        y++;
                    } else {
                        stage = 2;
                    }
                } else if (stage == 2) {
                    if (magicCircleTask != null) {
                        magicCircleTask.cancel();
                        magicCircleTask = null;
                    }

                    if (fadeOutTask == null) {
                        fadeOutTask = startMagicCircleFadeOut(player);
                        fadeOutTask.runTaskTimer(plugin, 0, 2);
                    }

                    spawnExpansionExplosion(player.getLocation().clone().add(0, 1, 0));
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.8f);

                    for (Player nearbyPlayer : loc.getWorld().getPlayers()) {
                        if (nearbyPlayer.getLocation().distance(loc) <= 20 && !nearbyPlayer.equals(player)) {
                            nearbyPlayer.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.2f);
                            nearbyPlayer.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.8f);
                        }
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.removePotionEffect(PotionEffectType.SLOWNESS);
                            // Llamamos a la recarga global de todas las habilidades
                            HabilidadesManager manager = new HabilidadesManager(plugin);
                            reapplyAllEffects(player, manager);
                        }
                    }.runTaskLater(plugin, 40);

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    private BukkitRunnable startMagicCircle(Player player, boolean fadeIn) {
        return new BukkitRunnable() {
            double angle = 0;
            double fadeProgress = fadeIn ? 0.0 : 1.0;
            final double radius = 5.0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation();

                if (fadeIn && fadeProgress < 1.0) {
                    fadeProgress += 0.05;
                }

                angle += 0.1;

                spawnMagicCircle(center, radius, angle, fadeProgress);
                spawnMagicStar(center, fadeProgress);

                if (fadeIn && fadeProgress >= 1.0) {
                    fadeProgress = 1.0;
                }
            }
        };
    }

    private BukkitRunnable startMagicCircleFadeOut(Player player) {
        return new BukkitRunnable() {
            double fadeProgress = 1.0;
            double angle = 0;
            final double radius = 5.0;

            @Override
            public void run() {
                if (fadeProgress <= 0) {
                    cancel();
                    return;
                }

                Location center = player.getLocation();

                fadeProgress -= 0.05;
                angle += 0.1;

                spawnMagicCircle(center, radius, angle, fadeProgress);
                spawnMagicStar(center, fadeProgress);
            }
        };
    }

    private void spawnMagicCircle(Location center, double radius, double angle, double alpha) {
        World world = center.getWorld();
        int points = 60;

        for (int i = 0; i < points; i++) {
            double circleAngle = 2 * Math.PI * i / points;
            double x = radius * Math.cos(circleAngle + angle);
            double z = radius * Math.sin(circleAngle + angle);

            Color circleColor = applyAlphaToColor(Color.fromRGB(173, 216, 230), alpha);
            Particle.DustOptions dustOptions = new Particle.DustOptions(circleColor, 1.2f);

            world.spawnParticle(Particle.DUST,
                    center.getX() + x,
                    center.getY(),
                    center.getZ() + z,
                    1, 0, 0, 0, 0, dustOptions);
        }

        for (int i = 0; i < 12; i++) {
            double orbAngle = angle + (2 * Math.PI * i / 12);
            double x = radius * Math.cos(orbAngle);
            double z = radius * Math.sin(orbAngle);

            Color orbColor = applyAlphaToColor(Color.fromRGB(147, 112, 219), alpha);
            Particle.DustOptions orbDust = new Particle.DustOptions(orbColor, 2.0f);

            world.spawnParticle(Particle.DUST,
                    center.getX() + x,
                    center.getY() + 0.2,
                    center.getZ() + z,
                    2, 0.1, 0, 0.1, 0, orbDust);
        }
    }

    private void spawnMagicStar(Location center, double alpha) {
        World world = center.getWorld();
        int starPoints = 8;
        double outerRadius = 1.5;
        double innerRadius = 0.7;

        for (int i = 0; i < starPoints * 2; i++) {
            double angle = Math.PI * i / starPoints;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;

            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            double progress = (double) i / (starPoints * 2);
            Color starColor = interpolateColor(
                    Color.fromRGB(173, 216, 230),
                    Color.fromRGB(147, 112, 219),
                    progress
            );
            starColor = applyAlphaToColor(starColor, alpha);

            Particle.DustOptions dustOptions = new Particle.DustOptions(starColor, 1.8f);

            world.spawnParticle(Particle.DUST,
                    center.getX() + x,
                    center.getY() + 0.1,
                    center.getZ() + z,
                    1, 0, 0, 0, 0, dustOptions);
        }

        Color centerColor = applyAlphaToColor(Color.fromRGB(199, 176, 224), alpha);
        Particle.DustOptions centerDust = new Particle.DustOptions(centerColor, 2.5f);
        world.spawnParticle(Particle.DUST,
                center.getX(), center.getY() + 0.1, center.getZ(),
                5, 0.3, 0.1, 0.3, 0, centerDust);
    }

    private Color applyAlphaToColor(Color color, double alpha) {
        int red = (int)(color.getRed() * alpha);
        int green = (int)(color.getGreen() * alpha);
        int blue = (int)(color.getBlue() * alpha);

        return Color.fromRGB(
                Math.max(0, Math.min(255, red)),
                Math.max(0, Math.min(255, green)),
                Math.max(0, Math.min(255, blue))
        );
    }

    private Color interpolateColor(Color color1, Color color2, double progress) {
        int red = (int)(color1.getRed() + (color2.getRed() - color1.getRed()) * progress);
        int green = (int)(color1.getGreen() + (color2.getGreen() - color1.getGreen()) * progress);
        int blue = (int)(color1.getBlue() + (color2.getBlue() - color1.getBlue()) * progress);

        return Color.fromRGB(
                Math.max(0, Math.min(255, red)),
                Math.max(0, Math.min(255, green)),
                Math.max(0, Math.min(255, blue))
        );
    }

    private void spawnPoofParticles(Player player, double height) {
        World world = player.getWorld();
        Location baseLoc = player.getLocation().clone();

        double radius = 0.8;
        int particlesPerCircle = 15;

        for (int i = 0; i < particlesPerCircle; i++) {
            double angle = 2 * Math.PI * i / particlesPerCircle;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = baseLoc.clone().add(x, height, z);

            Particle.DustOptions dustOptions;
            if (i % 2 == 0) {
                dustOptions = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 1.5f);
            } else {
                dustOptions = new Particle.DustOptions(Color.fromRGB(147, 112, 219), 1.5f);
            }

            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }

        Location centerParticle = baseLoc.clone().add(0, height, 0);
        Particle.DustOptions centerDust = new Particle.DustOptions(Color.fromRGB(255, 182, 193), 2f);
        world.spawnParticle(Particle.DUST, centerParticle, 3, 0.1, 0.1, 0.1, 0, centerDust);
    }

    private void spawnElectricSphereVertical(Location center, double radius) {
        World world = center.getWorld();
        int verticalLines = 12;
        int particlesPerLine = 15;

        for (int line = 0; line < verticalLines; line++) {
            double theta = 2 * Math.PI * line / verticalLines;

            for (int i = 0; i < particlesPerLine; i++) {
                double phi = Math.PI * i / particlesPerLine;

                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);

                world.spawnParticle(Particle.ELECTRIC_SPARK,
                        center.getX() + x,
                        center.getY() + y,
                        center.getZ() + z,
                        1, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < 8; i++) {
            double randomTheta = Math.random() * 2 * Math.PI;
            double randomPhi = Math.random() * Math.PI;

            double x = radius * Math.sin(randomPhi) * Math.cos(randomTheta);
            double y = radius * Math.cos(randomPhi);
            double z = radius * Math.sin(randomPhi) * Math.sin(randomTheta);

            world.spawnParticle(Particle.ELECTRIC_SPARK,
                    center.getX() + x,
                    center.getY() + y,
                    center.getZ() + z,
                    1, 0, 0, 0, 0);
        }
    }

    private void spawnExpansionExplosion(Location center) {
        World world = center.getWorld();

        new BukkitRunnable() {
            double currentRadius = 0;
            final double maxRadius = 3.0;
            final int rings = 5;

            @Override
            public void run() {
                if (currentRadius > maxRadius) {
                    cancel();
                    return;
                }

                for (int ring = 0; ring < rings; ring++) {
                    double ringRadius = currentRadius * (1.0 - (ring * 0.15));
                    if (ringRadius < 0) continue;

                    spawnExplosionRing(center, ringRadius, ring);
                }

                currentRadius += 0.3;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnExplosionRing(Location center, double radius, int ring) {
        World world = center.getWorld();
        int particles = 20;

        Particle.DustOptions dustOptions;
        switch (ring % 3) {
            case 0:
                dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2f);
                break;
            case 1:
                dustOptions = new Particle.DustOptions(Color.fromRGB(147, 112, 219), 2f);
                break;
            default:
                dustOptions = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 2f);
                break;
        }

        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / particles;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            double yVariation = (Math.random() - 0.5) * 1.5;

            world.spawnParticle(Particle.DUST, x, center.getY() + yVariation, z, 1, 0, 0, 0, 0, dustOptions);
        }

        if (ring == 0) {
            for (int i = 0; i < 10; i++) {
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetY = (Math.random() - 0.5) * 1.5;
                double offsetZ = (Math.random() - 0.5) * 1.5;

                world.spawnParticle(Particle.EXPLOSION_EMITTER,
                        center.clone().add(offsetX, offsetY, offsetZ), 1);
            }
        }
    }

    // Calcula cuánta vida máxima en total dan las habilidades
    private double calculateExtraHealth(int level) {
        double extra = 0;
        for (int i = 1; i <= level; i++) {
            if (i <= 4) {
                extra += 5.0; // 2.5 Corazones por nivel (1 al 4)
            } else {
                extra += 8.0; // 4 Corazones por nivel (5 al 8)
            }
        }
        return extra;
    }

    public void reapplyAllEffects(Player player, HabilidadesManager manager) {
        if (!player.isOnline()) return;

        applyAllInternal(player, manager);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                applyAllInternal(player, manager);
            }
        }, 60L);
    }

    private void applyAllInternal(Player player, HabilidadesManager manager) {
        // --- VITALIDAD ---
        int vitLevel = manager.getHighestLevel(player.getUniqueId(), HabilidadesType.VITALIDAD);
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttr != null) {
            healthAttr.getModifiers().stream()
                    .filter(m -> m.getUniqueId().equals(VITALIDAD_MODIFIER_UUID))
                    .forEach(healthAttr::removeModifier);

            if (vitLevel > 0) {
                AttributeModifier modifier = new AttributeModifier(
                        VITALIDAD_MODIFIER_UUID,
                        "Habilidad_Vitalidad",
                        calculateExtraHealth(vitLevel),
                        AttributeModifier.Operation.ADD_NUMBER
                );
                healthAttr.addModifier(modifier);
            }

            if (player.getHealth() > healthAttr.getValue()) {
                player.setHealth(healthAttr.getValue());
            }
        }

        // --- AGILIDAD ---
        int agiLevel = manager.getHighestLevel(player.getUniqueId(), HabilidadesType.AGILIDAD);

        if (agiLevel >= 1) addInfiniteEffect(player, PotionEffectType.HASTE, 0);
        else player.removePotionEffect(PotionEffectType.HASTE);

        if (agiLevel >= 2) addInfiniteEffect(player, PotionEffectType.DOLPHINS_GRACE, 1);
        else player.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);

        // Velocidad II en el Nivel 7, Velocidad I en el Nivel 3
        if (agiLevel >= 7) addInfiniteEffect(player, PotionEffectType.SPEED, 1);
        else if (agiLevel >= 3) addInfiniteEffect(player, PotionEffectType.SPEED, 0);
        else player.removePotionEffect(PotionEffectType.SPEED);

        // Fuerza I en el Nivel 5
        if (agiLevel >= 5) addInfiniteEffect(player, PotionEffectType.STRENGTH, 0);
        else player.removePotionEffect(PotionEffectType.STRENGTH);

        // Salto Alto I en el Nivel 6
        if (agiLevel >= 6) addInfiniteEffect(player, PotionEffectType.JUMP_BOOST, 0);
        else player.removePotionEffect(PotionEffectType.JUMP_BOOST);

        // --- RESISTENCIA ---
        int resLevel = manager.getHighestLevel(player.getUniqueId(), HabilidadesType.RESISTENCIA);

        // Resistencia II en el Nivel 8, Resistencia I en el Nivel 4
        if (resLevel >= 8) addInfiniteEffect(player, PotionEffectType.RESISTANCE, 1);
        else if (resLevel >= 4) addInfiniteEffect(player, PotionEffectType.RESISTANCE, 0);
        else player.removePotionEffect(PotionEffectType.RESISTANCE);
    }

    private void addInfiniteEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect effect = player.getPotionEffect(type);
        if (effect == null || effect.getAmplifier() != amplifier || effect.getDuration() != PotionEffect.INFINITE_DURATION) {
            player.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, false));
        }
    }
}