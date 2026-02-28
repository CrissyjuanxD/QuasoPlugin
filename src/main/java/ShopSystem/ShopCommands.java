package ShopSystem;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ShopCommands implements CommandExecutor, TabCompleter {

    private final ShopManager shopManager;
    private final ShopGUI shopGUI;

    public ShopCommands(ShopManager shopManager, ShopGUI shopGUI) {
        this.shopManager = shopManager;
        this.shopGUI = shopGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores.");
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("spawnshop")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /spawnshop <nombre> [x y z] [bioma] [profesion]");
                return true;
            }

            String name = args[0].replace("_", " ");
            Location loc = player.getLocation();
            Villager.Type type = Villager.Type.PLAINS;
            Villager.Profession profession = Villager.Profession.NONE;

            int nextArgIndex = 1;

            if (args.length >= 4 && isDouble(args[1])) {
                try {
                    double x = Double.parseDouble(args[1]);
                    double y = Double.parseDouble(args[2]);
                    double z = Double.parseDouble(args[3]);
                    loc = new Location(player.getWorld(), x, y, z);
                    nextArgIndex = 4;
                } catch (NumberFormatException ignored) {}
            }

            if (args.length > nextArgIndex) {
                try {
                    type = Villager.Type.valueOf(args[nextArgIndex].toUpperCase());
                    nextArgIndex++;
                } catch (IllegalArgumentException ignored) {}
            }

            if (args.length > nextArgIndex) {
                try {
                    profession = Villager.Profession.valueOf(args[nextArgIndex].toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            shopManager.spawnShop(name, loc, type, profession);
            player.sendMessage(ChatColor.GREEN + "Tienda creada: " + type.name() + " / " + profession.name());
            return true;
        }

        if (command.getName().equalsIgnoreCase("removeshop")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;

            RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 5.0,
                    entity -> entity instanceof Villager && entity.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING));

            Villager target = null;

            if (result != null && result.getHitEntity() != null) {
                target = (Villager) result.getHitEntity();
            } else {
                double minDistance = Double.MAX_VALUE;
                for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof Villager) {
                        Villager v = (Villager) e;
                        if (v.getPersistentDataContainer().has(shopManager.shopKey, PersistentDataType.STRING)) {
                            double dist = e.getLocation().distanceSquared(player.getLocation());
                            if (dist < minDistance) {
                                minDistance = dist;
                                target = v;
                            }
                        }
                    }
                }
            }

            if (target != null) {
                String id = target.getPersistentDataContainer().get(shopManager.shopIdKey, PersistentDataType.STRING);
                shopManager.removeShopFromFile(id);
                target.remove();
                player.sendMessage(ChatColor.GREEN + "Tienda eliminada.");
            } else {
                player.sendMessage(ChatColor.RED + "No se encontró ninguna tienda cercana o en tu mira.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("trade")) {
            if (!player.hasPermission("viciont_hardcore3.shop.admin")) return true;

            UUID uuid = player.getUniqueId();
            if (!shopManager.editingShops.containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "No estás editando ninguna tienda.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Uso: /trade <item> <cantidad>");
                return true;
            }
            String itemName = args[0];
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Cantidad inválida.");
                return true;
            }
            ItemStack item = CustomItemRegistry.getCustomItem(itemName, amount);
            if (item == null) {
                try {
                    item = new ItemStack(Material.valueOf(itemName.toUpperCase()), amount);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Item no encontrado: " + itemName);
                    return true;
                }
            }

            String shopId = shopManager.editingShops.get(uuid);
            Integer tradeIndex = shopManager.editingTradeIndex.get(uuid);
            String slotType = shopManager.editingSlotType.get(uuid);

            if (shopId == null || tradeIndex == null || slotType == null) {
                player.sendMessage(ChatColor.RED + "Error: Sesión perdida. Abre la GUI.");
                return true;
            }

            Villager villager = shopManager.getVillagerById(shopId);
            if (villager == null) {
                player.sendMessage(ChatColor.RED + "El aldeano ya no existe.");
                return true;
            }

            // Aplicamos la misma lógica del código antiguo
            shopManager.updateVillagerTrade(villager, tradeIndex, slotType, item);
            shopManager.saveShopTrades(shopId, villager.getRecipes());

            shopManager.editingTradeIndex.remove(uuid);
            shopManager.editingSlotType.remove(uuid);

            player.sendMessage(ChatColor.GREEN + "Tradeo actualizado: " + slotType + " -> " + item.getType());
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

            return true;
        }
        return false;
    }

    private boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("trade")) {
            if (args.length == 1) {
                List<String> suggestions = new ArrayList<>();
                for (Material m : Material.values()) {
                    if (m.isItem()) suggestions.add(m.name().toLowerCase());
                }
                suggestions.addAll(CustomItemRegistry.getAllCustomNames());
                return suggestions.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (command.getName().equalsIgnoreCase("spawnshop")) {
            if (args.length == 1) return null;
            if (args.length >= 2) {
                String lastArg = args[args.length - 1].toUpperCase();
                List<String> suggestions = new ArrayList<>();
                for (Villager.Type t : Villager.Type.values()) suggestions.add(t.name());
                for (Villager.Profession p : Villager.Profession.values()) suggestions.add(p.name());
                return suggestions.stream()
                        .filter(s -> s.startsWith(lastArg))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}