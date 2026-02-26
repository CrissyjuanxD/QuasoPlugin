package Handlers;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ActionBarHandler {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;

    public ActionBarHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendActionBar(Player player, String message) {
        // CORRECCIÓN: Usar fromLegacyText para que lea los colores Hex y códigos §
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}