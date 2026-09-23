package Events.AchievementParty;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AchievementCommands implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final AchievementPartyHandler achievementHandler;

    public AchievementCommands(AchievementPartyHandler achievementHandler) {
        this.achievementHandler = achievementHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("achievements.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <nombrelogro> <jugador> [item_lista|all]");
            sender.sendMessage(ChatColor.GOLD + "Logros disponibles:");
            achievementHandler.getAchievements().forEach((id, achievement) -> {
                sender.sendMessage(ChatColor.YELLOW + "- " + id + ChatColor.GRAY + ": " +
                        ChatColor.WHITE + achievement.getName());
            });
            return true;
        }

        String achievementId = args[0];
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null && Bukkit.getOfflinePlayer(playerName).getName() == null) {
            sender.sendMessage(ChatColor.RED + "El jugador " + playerName + " no existe.");
            return true;
        }

        String actualPlayerName = target != null ? target.getName() : Bukkit.getOfflinePlayer(playerName).getName();

        if (command.getName().equalsIgnoreCase("addlogro")) {
            return handleAddAchievement(sender, achievementId, actualPlayerName, args.length > 2 ? args[2] : null);
        } else if (command.getName().equalsIgnoreCase("removelogro")) {
            return handleRemoveAchievement(sender, achievementId, actualPlayerName, args.length > 2 ? args[2] : null);
        }

        return false;
    }

    private boolean handleAddAchievement(CommandSender sender, String achievementId, String playerName, String itemArg) {
        if (!achievementHandler.isEventActive()) {
            sender.sendMessage("§cNo hay ningún evento de logros activo!");
            return true;
        }

        if (!achievementHandler.getAchievementIds().contains(achievementId)) {
            sender.sendMessage(ChatColor.RED + "Logro no válido. Logros disponibles:");
            sender.sendMessage(String.join(", ", achievementHandler.getAchievementIds()));
            return true;
        }

        Achievement achievement = achievementHandler.getAchievements().get(achievementId);

        if (achievement instanceof Achievement2 && itemArg != null) {
            Achievement2 flowerAchievement = (Achievement2) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                for (Material flower : flowerAchievement.getRequiredFlowers()) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), true);
                }

                boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todas las flores del logro han sido marcadas como recolectadas para " + playerName);

                    if (!wasCompleted) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                Material flowerMaterial = null;
                try {
                    flowerMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                }

                if (flowerMaterial == null || !flowerAchievement.getRequiredFlowers().contains(flowerMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Flor no válida. Flores disponibles:");
                    sender.sendMessage(flowerAchievement.getRequiredFlowers().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para marcar todas las flores");
                    return true;
                }

                data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flowerMaterial.name(), true);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Flor " + flowerMaterial.name() + " marcada como recolectada para " + playerName);

                    boolean allCollected = true;
                    for (Material flower : flowerAchievement.getRequiredFlowers()) {
                        if (!data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false)) {
                            allCollected = false;
                            break;
                        }
                    }

                    if (allCollected && !data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        if (achievement instanceof Achievement5 && itemArg != null) {
            Achievement5 blockAchievement = (Achievement5) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                for (Material block : blockAchievement.getRequiredBlocks()) {
                    data.set("players." + playerName + ".achievements.touch_grass.broken." + block.name(), true);
                }

                boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todos los bloques del logro han sido marcados como rotos para " + playerName);

                    if (!wasCompleted) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                Material blockMaterial = null;
                try {
                    blockMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                }

                if (blockMaterial == null || !blockAchievement.getRequiredBlocks().contains(blockMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Bloque no válido. Bloques disponibles:");
                    sender.sendMessage(blockAchievement.getRequiredBlocks().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para marcar todos los bloques");
                    return true;
                }

                data.set("players." + playerName + ".achievements.touch_grass.broken." + blockMaterial.name(), true);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Bloque " + blockMaterial.name() + " marcado como roto para " + playerName);

                    boolean allBroken = true;
                    for (Material block : blockAchievement.getRequiredBlocks()) {
                        if (!data.getBoolean("players." + playerName + ".achievements.touch_grass.broken." + block.name(), false)) {
                            allBroken = false;
                            break;
                        }
                    }

                    if (allBroken && !data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        if (achievement instanceof Achievement8) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
            String path = "players." + playerName + ".achievements.sculk_shrieker.broken";

            if (itemArg != null && itemArg.equalsIgnoreCase("all")) {
                data.set(path, ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Progreso de chilladores completado para " + playerName);

                    if (!data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else if (itemArg != null && itemArg.equalsIgnoreCase("down")) {
                int current = data.getInt(path, 0);
                if (current > 0) {
                    data.set(path, current - 1);

                    try {
                        data.save(achievementHandler.getAchievementsFile());
                        sender.sendMessage(ChatColor.GREEN + "Contador de chilladores reducido a " + (current - 1) + " para " + playerName);
                        return true;
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "El contador ya está en 0");
                    return true;
                }
            } else {
                int current = data.getInt(path, 0);
                data.set(path, current + 1);

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Contador de chilladores incrementado a " + (current + 1) + " para " + playerName);

                    if (current + 1 >= ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS &&
                            !data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        achievementHandler.completeAchievement(playerName, achievementId);
                    }
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        if (itemArg != null) {
            sender.sendMessage(ChatColor.YELLOW + "Este logro no requiere items. Ignorando el parámetro adicional.");
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
        boolean wasCompleted = data.getBoolean("players." + playerName + ".achievements." + achievementId + ".completed", false);

        if (!wasCompleted) {
            achievementHandler.completeAchievement(playerName, achievementId);
            sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " añadido a " + playerName);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "El jugador ya tenía este logro completado.");
        }
        return true;
    }

    private boolean handleRemoveAchievement(CommandSender sender, String achievementId, String playerName, String itemArg) {
        if (!achievementHandler.isEventActive()) {
            sender.sendMessage("§cNo hay ningún evento de logros activo!");
            return true;
        }

        if (!achievementHandler.getAchievementIds().contains(achievementId)) {
            sender.sendMessage(ChatColor.RED + "Logro no válido. Logros disponibles:");
            sender.sendMessage(String.join(", ", achievementHandler.getAchievementIds()));
            return true;
        }

        Achievement achievement = achievementHandler.getAchievements().get(achievementId);

        if (achievement instanceof Achievement2 && itemArg != null) {
            Achievement2 flowerAchievement = (Achievement2) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                for (Material flower : flowerAchievement.getRequiredFlowers()) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flower.name(), false);
                }

                if (data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todas las flores del logro han sido removidas para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                Material flowerMaterial = null;
                try {
                    flowerMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                }

                if (flowerMaterial == null || !flowerAchievement.getRequiredFlowers().contains(flowerMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Flor no válida. Flores disponibles:");
                    sender.sendMessage(flowerAchievement.getRequiredFlowers().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para remover todas las flores");
                    return true;
                }

                data.set("players." + playerName + ".achievements.collect_all_flowers.collected." + flowerMaterial.name(), false);

                if (data.getBoolean("players." + playerName + ".achievements.collect_all_flowers.completed", false)) {
                    data.set("players." + playerName + ".achievements.collect_all_flowers.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Flor " + flowerMaterial.name() + " removida para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        if (achievement instanceof Achievement5 && itemArg != null) {
            Achievement5 blockAchievement = (Achievement5) achievement;
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());

            if (itemArg.equalsIgnoreCase("all")) {
                for (Material block : blockAchievement.getRequiredBlocks()) {
                    data.set("players." + playerName + ".achievements.touch_grass.broken." + block.name(), false);
                }

                if (data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                    data.set("players." + playerName + ".achievements.touch_grass.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Todos los bloques del logro han sido removidos para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                Material blockMaterial = null;
                try {
                    blockMaterial = Material.valueOf(itemArg.toUpperCase());
                } catch (IllegalArgumentException e) {
                }

                if (blockMaterial == null || !blockAchievement.getRequiredBlocks().contains(blockMaterial)) {
                    sender.sendMessage(ChatColor.RED + "Bloque no válido. Bloques disponibles:");
                    sender.sendMessage(blockAchievement.getRequiredBlocks().stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
                    sender.sendMessage(ChatColor.YELLOW + "O usa 'all' para remover todos los bloques");
                    return true;
                }

                data.set("players." + playerName + ".achievements.touch_grass.broken." + blockMaterial.name(), false);

                if (data.getBoolean("players." + playerName + ".achievements.touch_grass.completed", false)) {
                    data.set("players." + playerName + ".achievements.touch_grass.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Bloque " + blockMaterial.name() + " removido para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            }
        }

        if (achievement instanceof Achievement8) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(achievementHandler.getAchievementsFile());
            String path = "players." + playerName + ".achievements.sculk_shrieker.broken";

            if (itemArg != null && itemArg.equalsIgnoreCase("all")) {
                data.set(path, 0);

                if (data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                    data.set("players." + playerName + ".achievements.sculk_shrieker.completed", false);
                    int completed = data.getInt("players." + playerName + ".completed", 0);
                    data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                }

                try {
                    data.save(achievementHandler.getAchievementsFile());
                    sender.sendMessage(ChatColor.GREEN + "Progreso de chilladores reseteado para " + playerName);
                    return true;
                } catch (IOException e) {
                    sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                    return false;
                }
            } else {
                int current = data.getInt(path, 0);
                if (current > 0) {
                    data.set(path, current - 1);

                    if (current >= ((Achievement8) achievement).REQUIRED_SCULK_SHRIEKERS &&
                            data.getBoolean("players." + playerName + ".achievements.sculk_shrieker.completed", false)) {
                        data.set("players." + playerName + ".achievements.sculk_shrieker.completed", false);
                        int completed = data.getInt("players." + playerName + ".completed", 0);
                        data.set("players." + playerName + ".completed", Math.max(0, completed - 1));
                    }

                    try {
                        data.save(achievementHandler.getAchievementsFile());
                        sender.sendMessage(ChatColor.GREEN + "Contador de chilladores reducido a " + (current - 1) + " para " + playerName);
                        return true;
                    } catch (IOException e) {
                        sender.sendMessage(ChatColor.RED + "Error al guardar los cambios: " + e.getMessage());
                        return false;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "El contador ya está en 0");
                    return true;
                }
            }
        }

        if (itemArg != null) {
            sender.sendMessage(ChatColor.YELLOW + "Este logro no requiere items. Ignorando el parámetro adicional.");
        }

        if (achievementHandler.removeAchievement(playerName, achievementId)) {
            sender.sendMessage(ChatColor.GREEN + "Logro " + achievementId + " removido de " + playerName);

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.sendMessage(ChatColor.RED + "Un administrador te ha removido el logro: " +
                        achievementHandler.getAchievements().get(achievementId).getName() + "!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "El jugador no tenía este logro completado.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("achievements.admin")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> achievementIds = new ArrayList<>(achievementHandler.getAchievementIds());
            StringUtil.copyPartialMatches(args[0], achievementIds, completions);
        } else if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();

            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));

            playerNames.addAll(
                    java.util.Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                            .limit(50)
                            .map(OfflinePlayer::getName)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );

            StringUtil.copyPartialMatches(args[1], playerNames, completions);
        } else if (args.length == 3) {
            Achievement achievement = achievementHandler.getAchievements().get(args[0]);

            if (achievement instanceof Achievement2) {
                Achievement2 flowerAchievement = (Achievement2) achievement;
                List<String> flowerNames = flowerAchievement.getRequiredFlowers().stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                flowerNames.add("all");

                StringUtil.copyPartialMatches(args[2], flowerNames, completions);
            }
            else if (achievement instanceof Achievement5) {
                Achievement5 blockAchievement = (Achievement5) achievement;
                List<String> blockNames = blockAchievement.getRequiredBlocks().stream()
                        .map(Enum::name)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                blockNames.add("all");

                StringUtil.copyPartialMatches(args[2], blockNames, completions);
            }
            else if (achievement instanceof Achievement8) {
                List<String> options = new ArrayList<>();
                options.add("one");
                options.add("all");
                StringUtil.copyPartialMatches(args[2], options, completions);
            }
        }

        Collections.sort(completions);

        return completions.size() > 50 ? completions.subList(0, 50) : completions;
    }
}