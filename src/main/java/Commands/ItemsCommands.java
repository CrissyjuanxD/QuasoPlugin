package Commands;

import Dificultades.CustomMobs.CustomBoat;
import Dificultades.DayOneChanges;
import Habilidades.HabilidadesBook;
import imp.crissyjuanxd.QuasoPlugin;
import items.*;
import items.IceBow.IceBowItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsCommands implements CommandExecutor, TabCompleter {

    private final QuasoPlugin plugin;
    private final DoubleLifeTotem doubleLifeTotem;
    private final EconomyIceTotem economyIceTotem;
    private final EconomyFlyTotem economyFlyTotem;
    private final excavatorItem ExcavatorItem;
    private final AmuletBloodM amuletBloodM;
    private final AmuletInmortal amuletInmortal;
    private final LifeCampfire lifeCampfire;
    private final IceBowItem iceBowItem;
    private final HappyGhastEnchant happyGhastEnchant;

    public ItemsCommands(QuasoPlugin plugin) {
        this.plugin = plugin;
        this.doubleLifeTotem = new DoubleLifeTotem(plugin);
        this.economyIceTotem = new EconomyIceTotem(plugin);
        this.economyFlyTotem = new EconomyFlyTotem(plugin);
        this.ExcavatorItem = new excavatorItem(plugin);
        this.amuletBloodM = new AmuletBloodM(plugin);
        this.amuletInmortal = new AmuletInmortal(plugin);
        this.lifeCampfire = new LifeCampfire(plugin);
        this.iceBowItem = new IceBowItem(plugin);
        this.happyGhastEnchant = new HappyGhastEnchant(plugin);
        plugin.getCommand("giveqp").setExecutor(this);
        plugin.getCommand("giveqp").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /giveqp <item> [cantidad] [jugador]");
            return true;
        }

        String itemName = args[0].toLowerCase();
        int cantidad = 1;
        Player target = null;

        if (args.length > 1) {
            try {
                cantidad = Integer.parseInt(args[1]);
                if (cantidad <= 0) {
                    sender.sendMessage("§cLa cantidad debe ser mayor a 0.");
                    return true;
                }
            } catch (NumberFormatException e) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cEl jugador '" + args[1] + "' no está en línea.");
                    return true;
                }
            }
        }

        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage("§cEl jugador '" + args[2] + "' no está en línea.");
                return true;
            }
        }

        if (target == null) {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage("§cDebes especificar un jugador si ejecutas el comando desde la consola.");
                return true;
            }
        }

        ItemStack item;
        switch (itemName) {
            case "doubletotem":
                item = doubleLifeTotem.createDoubleLifeTotem();
                item.setAmount(cantidad);
                break;
            case "corrupted_steak":
                item = DayOneChanges.corruptedSteak();
                item.setAmount(cantidad);
                break;
            case "corrupted_golden_apple":
                item = CorruptedGoldenApple.createCorruptedGoldenApple();
                item.setAmount(cantidad);
                break;
            case "libro_habilidades":
                item = HabilidadesBook.createHabilidadesBook();
                item.setAmount(cantidad);
                break;
            case "dinocoins":
                item = EconomyItems.createVithiumCoin();
                item.setAmount(cantidad);
                break;
            case "dinofichas":
                item = EconomyItems.createVithiumToken();
                item.setAmount(cantidad);
                break;
            case "mochila_nivel_1":
                item = EconomyItems.createNormalMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_nivel_2":
                item = EconomyItems.createGreenMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_nivel_3":
                item = EconomyItems.createRedMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_nivel_4":
                item = EconomyItems.createBlueMochila();
                item.setAmount(cantidad);
                break;
            case "mochila_nivel_5":
                item = EconomyItems.createPurpleMochila();
                item.setAmount(cantidad);
                break;
            case "enderbag":
                item = EconomyItems.createEnderBag();
                item.setAmount(cantidad);
                break;
            case "gancho":
                item = EconomyItems.createGancho();
                item.setAmount(cantidad);
                break;
            case "panic_apple":
                item = EconomyItems.createManzanaPanico();
                item.setAmount(cantidad);
                break;
            case "artefacto_nivel_1":
                item = EconomyItems.createYunqueReparadorNivel1();
                item.setAmount(cantidad);
                break;
            case "artefacto_nivel_2":
                item = EconomyItems.createYunqueReparadorNivel2();
                item.setAmount(cantidad);
                break;
            case "misiones":
                item = Misionesitem.createMisiones();
                item.setAmount(cantidad);
                break;
            case "icetotem":
                item = economyIceTotem.createIceTotem();
                item.setAmount(cantidad);
                break;
            case "flytotem":
                item = economyFlyTotem.createFlyTotem();
                item.setAmount(cantidad);
                break;
            case "excavator_pickaxe":
                item = ExcavatorItem.createExcavator();
                item.setAmount(cantidad);
                break;
            case "potion_resistance_2":
                item = CustomPotions.getResistanceIIPotion();
                item.setAmount(cantidad);
                break;
            case "splash_resistance_3":
                item = CustomPotions.getSplashResistanceIIIPotion();
                item.setAmount(cantidad);
                break;
            case "potion_slow_falling":
                item = CustomPotions.getSlowFallingPotion();
                item.setAmount(cantidad);
                break;
            case "splash_regeneration_3":
                item = CustomPotions.getSplashRegenerationIIIPotion();
                item.setAmount(cantidad);
                break;
            case "potion_haste_3":
                item = CustomPotions.getHasteIIIPotion();
                item.setAmount(cantidad);
                break;
            case "potion_haste_2":
                item = CustomPotions.getHasteIIPotion();
                item.setAmount(cantidad);
                break;
            case "splash_absorption_10":
                item = CustomPotions.getSplashAbsorptionXPotion();
                item.setAmount(cantidad);
                break;
            case "frasco_de_velocidad":
                item = CustomPotions.getSpeedHoneyBottle();
                item.setAmount(cantidad);
                break;
            case "amulet_bloodmoon":
                item = amuletBloodM.createAmulet();
                item.setAmount(cantidad);
                break;
            case "amuleto_inmortalidad":
                item = amuletInmortal.createAmulet();
                item.setAmount(cantidad);
                break;
            case "life_campfire":
                item = lifeCampfire.createCampfire();
                item.setAmount(cantidad);
                break;
            case "fuel_campfire":
                item = lifeCampfire.createFuel();
                item.setAmount(cantidad);
                break;
            case "special_totem":
                item = ItemsTotems.createSpecialTotem();
                item.setAmount(cantidad);
                break;
            case "cristal_hielo":
                item = ItemsTotems.createIceCrystal();
                item.setAmount(cantidad);
                break;
            case "arco_hielo":
                item = iceBowItem.createIceBow();
                item.setAmount(cantidad);
                break;
            case "happy_ghast_enchant":
                item = happyGhastEnchant.createFastFlightBook(1);
                item.setAmount(cantidad);
                break;
            default:
                sender.sendMessage("§cEse item no existe.");
                return true;
        }

        target.getInventory().addItem(item);
        sender.sendMessage("§aHas dado " + cantidad + "x " + itemName + " a " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("doubletotem");
            completions.add("corrupted_steak");
            completions.add("corrupted_golden_apple");
            completions.add("libro_habilidades");
            completions.add("dinocoins");
            completions.add("dinofichas");
            completions.add("mochila_nivel_1");
            completions.add("mochila_nivel_2");
            completions.add("mochila_nivel_3");
            completions.add("mochila_nivel_4");
            completions.add("mochila_nivel_5");
            completions.add("enderbag");
            completions.add("gancho");
            completions.add("panic_apple");
            completions.add("artefacto_nivel_1");
            completions.add("artefacto_nivel_2");
            completions.add("misiones");
            completions.add("icetotem");
            completions.add("flytotem");
            completions.add("excavator_pickaxe");
            completions.add("potion_resistance_2");
            completions.add("splash_resistance_3");
            completions.add("potion_slow_falling");
            completions.add("splash_regeneration_3");
            completions.add("potion_haste_3");
            completions.add("potion_haste_2");
            completions.add("splash_absorption_10");
            completions.add("frasco_de_velocidad");
            completions.add("amulet_bloodmoon");
            completions.add("amuleto_inmortalidad");
            completions.add("life_campfire");
            completions.add("fuel_campfire");
            completions.add("special_totem");
            completions.add("cristal_hielo");
            completions.add("arco_hielo");
            completions.add("happy_ghast_enchant");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
