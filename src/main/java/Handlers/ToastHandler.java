package Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class ToastHandler {

    private final JavaPlugin plugin;
    // Guardamos las keys para borrarlas SOLO al apagar el server
    private static final Set<NamespacedKey> activeToasts = new HashSet<>();
    private final NamespacedKey rootKey;

    public ToastHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rootKey = new NamespacedKey(plugin, "notification_root");

        // Creamos la "Carpeta" (Root) al iniciar para que la GUI tenga fondo y título
        createRootAdvancement();
    }

    private void createRootAdvancement() {
        // Si ya existe, no lo recreamos
        if (Bukkit.getAdvancement(rootKey) != null) return;

        String json = """
        {
          "display": {
            "icon": { "id": "minecraft:fox_spawn_egg" },
            "title": { "text": "Notificaciones IsManuSMP" },
            "description": { "text": "Registro de eventos del servidor" },
            "background": "minecraft:textures/block/orange_wool.png",
            "frame": "task",
            "show_toast": false,
            "announce_to_chat": false,
            "hidden": true
          },
          "criteria": {
            "trigger": { "trigger": "minecraft:impossible" }
          }
        }
        """;

        try {
            Bukkit.getUnsafe().loadAdvancement(rootKey, json);
            activeToasts.add(rootKey); // Lo añadimos para limpiarlo al reiniciar también
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToast(Player player, String title, String description, String iconMaterial) {
        NamespacedKey key = new NamespacedKey(plugin, "toast_" + System.nanoTime());

        String json = """
        {
          "parent": "%s",
          "display": {
            "icon": { "id": "%s" },
            "title": { "text": "%s" },
            "description": { "text": "%s" },
            "frame": "goal",
            "announce_to_chat": false,
            "show_toast": true,
            "hidden": true
          },
          "criteria": {
            "trigger": {
              "trigger": "minecraft:impossible"
            }
          }
        }
        """.formatted(rootKey.toString(), iconMaterial, title, description);

        try {
            // 1. Cargar
            Bukkit.getUnsafe().loadAdvancement(key, json);
            activeToasts.add(key);

            Advancement adv = Bukkit.getAdvancement(key);
            if (adv == null) return;

            // 2. Otorgar (Visual)
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            if (!progress.isDone()) {
                progress.awardCriteria("trigger");
            }

            // 3. Revocar criterio (Para poder repetirlo)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    AdvancementProgress p = player.getAdvancementProgress(adv);
                    if (p.isDone()) {
                        p.revokeCriteria("trigger");
                    }
                }
            }, 1L);

            // NO BORRAMOS EL ADVANCEMENT (Para evitar el lag spike y bugs visuales)

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanupToasts() {
        if (activeToasts.isEmpty()) return;

        for (NamespacedKey key : activeToasts) {
            try {
                if (Bukkit.getAdvancement(key) != null) {
                    Bukkit.getUnsafe().removeAdvancement(key);
                }
            } catch (Exception ignored) {}
        }
        activeToasts.clear();
    }
}