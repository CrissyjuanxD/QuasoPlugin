package Dificultades.CustomMobs;

import io.papermc.paper.world.WeatheringCopperState;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Estatua_Reward implements Listener {

    public static final String STATUE_NAME = ChatColor.of("#FFB347") + "" + ChatColor.BOLD + "Estatua de Recompensas";
    // Hitbox de 4,41 bloques de alto. El CEM compensa con escala 4/9 para conservar el tamaño del dino.
    static final double STATUE_SCALE = 4.5;

    // Golem de cobre gigante, quieto e invulnerable que hace de estatua de recompensas
    public static void spawn(Location loc) {
        CopperGolem golem = (CopperGolem) loc.getWorld().spawnEntity(loc, EntityType.COPPER_GOLEM);

        golem.setAI(false);
        golem.setGravity(false);
        golem.setCollidable(false);
        golem.setInvulnerable(true);
        golem.setSilent(true);
        golem.setRemoveWhenFarAway(false);

        golem.setWeatheringState(WeatheringCopperState.WEATHERED);

        golem.setOxidizing(CopperGolem.Oxidizing.waxed());

        golem.setCustomName(STATUE_NAME);
        golem.setCustomNameVisible(true);

        golem.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 200, false, false, false));

        golem.addScoreboardTag("reward_statue");
        refresh(golem);
    }

    // Actualiza también las estatuas colocadas antes del cambio, sin tocar los gólems normales.
    public static void refresh(Entity entity) {
        if (!(entity instanceof CopperGolem golem)) return;
        if (!golem.getScoreboardTags().contains("reward_statue")
                && !"Estatua de Recompensas".equals(ChatColor.stripColor(golem.getCustomName()))) return;
        var scale = golem.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(STATUE_SCALE);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        event.getEntities().forEach(Estatua_Reward::refresh);
    }
}
