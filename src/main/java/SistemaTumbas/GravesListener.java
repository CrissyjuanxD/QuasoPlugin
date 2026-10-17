package SistemaTumbas;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravesListener implements Listener {
    private final GravesManager manager;
    private final MarcadorTumba marcador;

    public GravesListener(GravesManager manager, MarcadorTumba marcador) {
        this.manager = manager;
        this.marcador = marcador;
    }

    // Si no tiene keepInventory los drops van a una tumba en vez de caer al suelo, y la tumba nueva queda marcada en la barra
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // KeepInventoryEffect se ejecuta después (HIGHEST); no duplicar sus objetos en una tumba.
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LUCK)) return;

        if (!event.getKeepInventory() && !event.getDrops().isEmpty()) {
            List<ItemStack> dropsToSave = new ArrayList<>(event.getDrops());
            event.getDrops().clear();

            marcador.marcar(player, manager.createGrave(player, dropsToSave));
            player.sendMessage(TumbaMessages.error("Has muerto. Tus cosas quedaron en una tumba en X:" + player.getLocation().getBlockX()
                    + " Y:" + player.getLocation().getBlockY() + " Z:" + player.getLocation().getBlockZ() + ". " + TumbaMessages.accent("/muertes")));
        }
    }

    // Click derecho a la tumba abre sus items: el dueño y los admins siempre, los demás según el modo de las tumbas
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (abrir(event.getPlayer(), event.getRightClicked())) event.setCancelled(true);
    }

    // Pegarle también la abre: desde Bedrock (Geyser) tocar la tumba suele llegar como golpe y no como click derecho
    @EventHandler
    public void onAttackEntity(PrePlayerAttackEntityEvent event) {
        if (abrir(event.getPlayer(), event.getAttacked())) event.setCancelled(true);
    }

    // Abre la tumba si la entidad es su cuadro para clickear; devuelve true si era una tumba
    private boolean abrir(Player player, Entity entity) {
        if (!(entity instanceof Interaction interaction)) return false;

        for (String tag : interaction.getScoreboardTags()) {
            if (!tag.startsWith("grave_")) continue;

            Grave grave = manager.getGraveById(UUID.fromString(tag.replace("grave_", "")));
            if (grave == null) return true;

            if (!manager.canOpen(grave, player)) {
                long untilOpen = manager.millisUntilOpen(grave);
                player.sendMessage(TumbaMessages.error("Esta tumba es de " + grave.getOwnerName()
                        + (untilOpen > 0 ? ". Se abre para todos en " + ModoTumba.reloj(untilOpen) + "." : " y solo la puede abrir su dueño.")));
                return true;
            }

            player.openInventory(grave.getInventory());
            return true;
        }
        return false;
    }

    // Si la tumba quedó vacía se borra, si no se guarda como quedó
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith("Tumba de ")) {
            UUID emptyGraveId = null;

            for (Grave grave : manager.getGraves()) {
                if (grave.getInventory().equals(event.getInventory())) {
                    if (grave.isEmpty()) {
                        emptyGraveId = grave.getId();
                    }
                    break;
                }
            }

            if (emptyGraveId != null) {
                manager.removeGrave(emptyGraveId);
                event.getPlayer().sendMessage("§aLa tumba se ha vaciado por completo y ha desaparecido.");
            } else {
                manager.saveData();
            }
        }
    }
}