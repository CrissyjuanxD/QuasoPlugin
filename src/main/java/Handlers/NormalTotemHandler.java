package Handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class NormalTotemHandler implements Listener {

    private final Plugin plugin;

    public NormalTotemHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTotemUse(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        boolean isMainHandTotem = mainHandItem.getType() == Material.TOTEM_OF_UNDYING;
        boolean isOffHandTotem = offHandItem.getType() == Material.TOTEM_OF_UNDYING;

        if (isMainHandTotem || isOffHandTotem) {
            ItemStack totem = isMainHandTotem ? mainHandItem : offHandItem;
            ItemMeta meta = totem.getItemMeta();

            Component totemNameComp;
            if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
                totemNameComp = meta.displayName();
            } else {
                totemNameComp = Component.text("Tótem", TextColor.color(0xB684E4), TextDecoration.BOLD);
            }

            broadcastTotemMessage(player, totemNameComp);
        }
    }

    private void broadcastTotemMessage(Player player, Component totemName) {
        Component message = Component.text("\n")
                .append(Component.text("۞ ", TextColor.color(0xB684E4)))
                .append(Component.text(player.getName(), TextColor.color(0xF7AD62), TextDecoration.BOLD))
                .append(Component.text(" ha consumido un ", TextColor.color(0xB684E4)))
                .append(totemName)
                .append(Component.text(".\n", TextColor.color(0xB684E4)));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), "item.trident.return", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer.getLocation(), "custom.noti", SoundCategory.VOICE, 2f, 2f);
                onlinePlayer.playSound(onlinePlayer.getLocation(), "entity.allay.item_thrown", SoundCategory.VOICE, 2f, 0.5f);
            }
        }
    }
}