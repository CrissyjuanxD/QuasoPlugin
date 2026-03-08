package Dificultades.CustomMobs;

import io.papermc.paper.world.WeatheringCopperState;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Estatua_Reward {

    public static final String STATUE_NAME = ChatColor.of("#FFB347") + "" + ChatColor.BOLD + "Estatua de Recompensas";

    public static void spawn(Location loc) {
        CopperGolem golem = (CopperGolem) loc.getWorld().spawnEntity(loc, EntityType.valueOf("COPPER_GOLEM"));

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

        if (golem.getAttribute(Attribute.SCALE) != null) {
            golem.getAttribute(Attribute.SCALE).setBaseValue(2.0);
        }

        if (golem.getEquipment() != null) {
            golem.getEquipment().setItem(EquipmentSlot.SADDLE, new ItemStack(Material.TORCHFLOWER));
        }

        golem.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 200, false, false, false));

        golem.addScoreboardTag("reward_statue");
    }
}