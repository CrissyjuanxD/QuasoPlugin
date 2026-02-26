package Dificultades.CustomMobs;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Pose; // Import necesario para forzar que no se levante

public class Fox_Statue {

    public static final String STATUE_NAME = ChatColor.of("#FFB347") + "" + ChatColor.BOLD + "Estatua de Recompensas";

    public static void spawn(Location loc) {
        Fox fox = (Fox) loc.getWorld().spawnEntity(loc, EntityType.FOX);

        fox.setSleeping(true);
        fox.setPose(Pose.SLEEPING);

        fox.setAI(false);
        fox.setGravity(false);
        fox.setCollidable(false);

        fox.setInvulnerable(true);
        fox.setSilent(true);

        fox.setAgeLock(true);
        fox.setAdult();

        fox.setCustomName(STATUE_NAME);
        fox.setCustomNameVisible(true);
        fox.setRemoveWhenFarAway(false);

        if (fox.getAttribute(Attribute.SCALE) != null) {
            fox.getAttribute(Attribute.SCALE).setBaseValue(2.1);
        }

        fox.addScoreboardTag("reward_statue");
    }
}