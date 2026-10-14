package Handlers;

import Events.MissionSystem.MissionHandler;
import Handlers.Teams.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class FirstJoinHandler implements Listener {
    private static final String API_PROTECCIONES = "com.viciont.viciontprotections.api.ViciontProtectionsApi";
    private static final String BLOQUE_PROTECCIONES = "com.viciont.viciontprotections.api.ProtectionBlock";

    private final JavaPlugin plugin;
    private final MissionHandler missionHandler;
    private final DatabaseManager databaseManager;
    private final TeamsHandler teamsHandler;

    public FirstJoinHandler(JavaPlugin plugin, MissionHandler missionHandler, DatabaseManager databaseManager, TeamsHandler teamsHandler) {
        this.plugin = plugin;
        this.missionHandler = missionHandler;
        this.databaseManager = databaseManager;
        this.teamsHandler = teamsHandler;
    }

    // La primera vez que entra lo registra en la base de datos, lo mete a DinoNugget y le da el kit de bienvenida.
    // Si ya había entrado pero no tiene ningún team también queda como DinoNugget
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!databaseManager.hasJoinedBefore(player.getUniqueId())) {
            databaseManager.registerPlayer(player.getUniqueId(), player.getName(), TeamType.Z_MIEMBRO.getId());
            teamsHandler.addPlayerToTeam(player, TeamType.Z_MIEMBRO);

            handleWelcomeLogistics(player);
        } else if (Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName()) == null) {
            teamsHandler.addPlayerToTeam(player, TeamType.Z_MIEMBRO);
        }
    }

    // Kit de bienvenida: un bloque protector de oro de 16x16 de ViciontProtections y 20 panes
    private void handleWelcomeLogistics(Player player) {
        List<ItemStack> kit = new ArrayList<>();
        ItemStack protector = bloqueProtector(Material.GOLD_BLOCK, 16);
        if (protector != null) kit.add(protector);
        else plugin.getLogger().warning("ViciontProtections no está activo: " + player.getName() + " no recibió su bloque protector.");
        kit.add(new ItemStack(Material.BREAD, 20));

        for (ItemStack sobra : player.getInventory().addItem(kit.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), sobra);
        }
    }

    // Pide el bloque a la API de ViciontProtections por su servicio de Bukkit. Va por reflexión para que QuasoPlugin
    // compile sin el jar de ViciontProtections; si el plugin no está devuelve null
    static ItemStack bloqueProtector(Material material, int lado) {
        for (Class<?> servicio : Bukkit.getServicesManager().getKnownServices()) {
            if (!servicio.getName().equals(API_PROTECCIONES)) continue;
            try {
                Object api = Bukkit.getServicesManager().load(servicio);
                Class<?> tipo = Class.forName(BLOQUE_PROTECCIONES, true, servicio.getClassLoader());
                // Lado x lado y toda la altura del mundo
                Object bloque = tipo.getMethod("square", Material.class, int.class).invoke(null, material, lado);
                return (ItemStack) servicio.getMethod("createProtectionBlock", tipo, int.class).invoke(api, bloque, 1);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Bukkit.getLogger().warning("No se pudo crear el bloque protector de ViciontProtections: " + e);
                return null;
            }
        }
        return null;
    }
}
