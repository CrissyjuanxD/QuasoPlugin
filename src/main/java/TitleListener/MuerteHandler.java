package TitleListener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class MuerteHandler implements Listener {

    private final JavaPlugin plugin;

    public MuerteHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component original = event.deathMessage();

        event.setDeathMessage(null);

        if (original == null) return;

        Component prefix = Component.text("[", TextColor.color(0xAAAAAA))
                .append(Component.text("☠", TextColor.color(0xAD3C3C)))
                .append(Component.text("] ", TextColor.color(0xAAAAAA)));

        Component formatted;

        if (original instanceof TranslatableComponent translatable) {
            List<Component> coloredArgs = new ArrayList<>();
            List<Component> args = translatable.args();

            for (int i = 0; i < args.size(); i++) {
                Component arg = args.get(i);
                Component colored;

                if (i == 0) {
                    colored = arg;
                } else {
                    colored = forceColor(arg, TextColor.color(0x2B95CC));
                }
                coloredArgs.add(colored);
            }

            formatted = Component.translatable(translatable.key(), coloredArgs)
                    .color(TextColor.color(0xD9632B));
        } else {
            formatted = original.color(TextColor.color(0xD9632B));
        }

        Component finalMessage = prefix.append(formatted);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(finalMessage);
        }
        Bukkit.getConsoleSender().sendMessage(finalMessage);
    }

    private Component forceColor(Component component, TextColor color) {
        Component base = component.color(color);
        if (!component.children().isEmpty()) {
            List<Component> recoloredChildren = new ArrayList<>();
            for (Component child : component.children()) {
                recoloredChildren.add(forceColor(child, color));
            }
            base = base.children(recoloredChildren);
        }
        return base;
    }
}