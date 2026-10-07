package Twitch;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

// Una cuenta de Twitch vinculada a un jugador. Los kits se cuentan por cuenta de Twitch, no por jugador: si la
// cuenta se pasa a otro jugador no vuelve a cobrar el mes
final class TwitchAccount {

    final String twitchId;
    String login;
    UUID player;
    String playerName;
    long linkedAt;

    boolean sub;
    String tier = "";
    boolean gift;
    String gifter = "";
    boolean vip;
    long checkedAt;

    int subKits;
    final Racha subRacha = new Racha();
    final Racha vipRacha = new Racha();

    // Rol que tenía antes de DinoSub/DinoVip, para devolvérselo cuando se le termina
    String previousRole;
    // Cuentas de prueba de /twitchadmin simular: "sub" o "vip" (null en las cuentas de verdad)
    String simulated;

    TwitchAccount(String twitchId) {
        this.twitchId = twitchId;
    }

    boolean isSimulated() {
        return simulated != null;
    }

    String tierName() {
        return switch (tier) {
            case "2000" -> "Tier 2";
            case "3000" -> "Tier 3";
            default -> "Tier 1";
        };
    }

    void save(ConfigurationSection section) {
        section.set("login", login);
        section.set("jugador", player == null ? null : player.toString());
        section.set("nombre", playerName);
        section.set("vinculada", linkedAt);
        section.set("sub", sub);
        section.set("tier", tier);
        section.set("regalo", gift);
        section.set("regalo_de", gifter);
        section.set("vip", vip);
        section.set("revisada", checkedAt);
        section.set("kits_sub", subKits);
        section.set("rol_anterior", previousRole);
        section.set("simulada", simulated);
        subRacha.save(section.createSection("racha_sub"));
        vipRacha.save(section.createSection("racha_vip"));
    }

    static TwitchAccount load(String twitchId, ConfigurationSection section) {
        TwitchAccount account = new TwitchAccount(twitchId);
        account.login = section.getString("login", "");
        String player = section.getString("jugador");
        try {
            account.player = player == null ? null : UUID.fromString(player);
        } catch (IllegalArgumentException e) {
            account.player = null;
        }
        account.playerName = section.getString("nombre", "");
        account.linkedAt = section.getLong("vinculada");
        account.sub = section.getBoolean("sub");
        account.tier = section.getString("tier", "");
        account.gift = section.getBoolean("regalo");
        account.gifter = section.getString("regalo_de", "");
        account.vip = section.getBoolean("vip");
        account.checkedAt = section.getLong("revisada");
        account.subKits = section.getInt("kits_sub");
        account.previousRole = section.getString("rol_anterior");
        account.simulated = section.getString("simulada");
        account.subRacha.load(section.getConfigurationSection("racha_sub"));
        account.vipRacha.load(section.getConfigurationSection("racha_vip"));
        return account;
    }
}
