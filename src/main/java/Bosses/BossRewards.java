package Bosses;

import Events.MissionSystem.BossDefeatedEvent;
import items.CustomPotions;
import items.EconomyItems;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BossRewards implements Listener {

    // coins[i] es lo que paga la vez i+1, la última se repite. dailyCap 0 = sin tope
    private record Reward(String name, int[] coins, int dailyCap) {
        int coinsFor(int kill) {
            return coins[Math.min(kill, coins.length) - 1];
        }
    }

    private static final Map<String, Reward> REWARDS = Map.of(
            "abeja_reina", new Reward("la Abeja Reina", new int[]{15, 10, 5}, 2),
            "ultra_warden", new Reward("el Ultra Warden", new int[]{20, 5}, 1),
            "ender_dragon", new Reward("el Ender Dragon", new int[]{15, 5}, 1),
            "rey_ender", new Reward("el Rey Ender", new int[]{25, 5}, 1),
            "wither", new Reward("un Wither", new int[]{7, 7, 7, 7, 7, 2}, 3),
            "elder_guardian", new Reward("un Elder Guardian", new int[]{5}, 0)
    );

    private static final String COLOR = "#EFDC93";

    private final JavaPlugin plugin;

    public BossRewards(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBossDefeated(BossDefeatedEvent event) {
        Reward reward = REWARDS.get(event.getBossId());
        if (reward == null) return;

        for (Player player : event.getPlayers()) {
            if (!player.isOnline()) continue;
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) continue;
            pay(player, event.getBossId(), reward);
        }
    }

    // Paga según cuántas veces lo cobró el jugador, si todavía no llegó al tope del día
    private void pay(Player player, String bossId, Reward reward) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        NamespacedKey killsKey = killsKey(bossId);
        NamespacedKey dailyKey = new NamespacedKey(plugin, "boss_daily_" + bossId);

        String today = LocalDate.now().toString();
        String daily = data.getOrDefault(dailyKey, PersistentDataType.STRING, "");
        int todayCount = daily.startsWith(today + ":") ? Integer.parseInt(daily.substring(today.length() + 1)) : 0;

        if (reward.dailyCap() > 0 && todayCount >= reward.dailyCap()) {
            player.sendMessage(ChatColor.of(COLOR) + "۞ Ya cobraste " + reward.dailyCap()
                    + (reward.dailyCap() == 1 ? " recompensa" : " recompensas") + " de " + reward.name()
                    + " hoy. Mañana vuelve a pagar.");
            return;
        }

        int kills = data.getOrDefault(killsKey, PersistentDataType.INTEGER, 0) + 1;
        data.set(killsKey, PersistentDataType.INTEGER, kills);
        data.set(dailyKey, PersistentDataType.STRING, today + ":" + (todayCount + 1));

        int coins = reward.coinsFor(kills);
        List<ItemStack> items = new ArrayList<>();
        ItemStack coinItem = EconomyItems.createVithiumCoin();
        coinItem.setAmount(coins);
        items.add(coinItem);
        if (bossId.equals("abeja_reina")) items.addAll(queenBeeExtras(kills));

        if (bossId.equals("abeja_reina") && kills == 1) {
            ItemStack bundle = new ItemStack(Material.BUNDLE);
            BundleMeta meta = (BundleMeta) bundle.getItemMeta();
            items.forEach(meta::addItem);
            bundle.setItemMeta(meta);
            give(player, bundle);
        } else {
            items.forEach(item -> give(player, item));
        }

        String times = kills == 1 ? "por primera vez" : "(" + kills + " veces)";
        player.sendMessage(ChatColor.of(COLOR) + "۞ Derrotaste a " + reward.name() + " " + times + ": "
                + ChatColor.of("#FFCC80") + coins + " DinoCoins" + ChatColor.of(COLOR) + ".");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    // La reina además da manzanas y miel de velocidad, menos cada vez
    private List<ItemStack> queenBeeExtras(int kills) {
        ItemStack speed = CustomPotions.getSpeedHoneyBottle();
        if (kills == 1) {
            speed.setAmount(16);
            return List.of(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 5), speed);
        }
        if (kills == 2) {
            speed.setAmount(8);
            return List.of(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3), speed);
        }
        speed.setAmount(3);
        return List.of(speed);
    }

    // La reina ya guardaba sus muertes con esta llave, se mantiene para no reiniciar la cuenta
    private NamespacedKey killsKey(String bossId) {
        if (bossId.equals("abeja_reina")) return new NamespacedKey(plugin, "queen_bee_kills");
        return new NamespacedKey(plugin, "boss_kills_" + bossId);
    }

    private void give(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }
}
