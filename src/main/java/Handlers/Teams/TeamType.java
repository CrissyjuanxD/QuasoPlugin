package Handlers.Teams;

import net.md_5.bungee.api.ChatColor;


public enum TeamType {
    // Orden: ID_INTERNO, HEX, PREFIJO_CHAT/CABEZA, PREFIJO_TAB, COLOR_BUKKIT, PRIORIDAD_TAB

    ADMIN("Admin", "#F89130",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_GRAY + ChatColor.BOLD + "PRIETO" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_GRAY + ChatColor.BOLD + "PRT" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.GOLD, "01_Admin"),

    MOD("Mod", "#C056E6",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "DinoNalgon" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "DNG" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_PURPLE, "02_Mod"),

    T_HELPER("THelper", "#68E3BA",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#00AAAA") + ChatColor.BOLD + "Helper" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.of("#00AAAA") + ChatColor.BOLD + "HLP" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.AQUA, "03_Helper"),

    T_SURVIVOR("TSurvivor", "#9455ED", "\uEB8A ", "\uEB8F ",
            org.bukkit.ChatColor.LIGHT_PURPLE, "04_TSurvivor"),

    Y_MIEMBRO("YMiembro", "#F7A1F0",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "DinoNugget" + ChatColor.GOLD + ChatColor.BOLD + "+" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "DNT" + ChatColor.GOLD + ChatColor.BOLD + "+" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.LIGHT_PURPLE, "98_Miembro"),

    Z_MIEMBRO("ZMiembro", "#FDCDA0",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "DinoNugget" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "DNT" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.YELLOW, "99_Miembro"),

    LAVACLASH("LavaClash", "#FFD294",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "Lava" + ChatColor.YELLOW + ChatColor.BOLD + "Clash" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.GOLD + ChatColor.BOLD + "Lv" + ChatColor.YELLOW + ChatColor.BOLD + "C" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_AQUA, "05_LavaClash"),

    ITEMPARTY("Itemparty", "#C056E6",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_AQUA + ChatColor.BOLD + "ITEM" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "PARTY" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_AQUA + ChatColor.BOLD + "I" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "PT" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_PURPLE, "06_Itemparty"),

    HOTPOTATO("HotPotato", "#FCA37D",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "HOT" + ChatColor.RED + ChatColor.BOLD + "POTATO" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_RED + ChatColor.BOLD + "H" + ChatColor.RED + ChatColor.BOLD + "PO" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.GOLD, "07_HotPotato"),

    Z_FANTASMA("ZFantasma", "#555555", "\uEB8C ", "\uEB91 ",
            org.bukkit.ChatColor.DARK_GRAY, "99_Fantasma");

    private final String id;
    private final String hexColor;
    private final String chatPrefix;
    private final String tabPrefix;
    private final org.bukkit.ChatColor bukkitColor;
    private final String priority;

    TeamType(String id, String hexColor, String chatPrefix, String tabPrefix, org.bukkit.ChatColor bukkitColor, String priority) {
        this.id = id;
        this.hexColor = hexColor;
        this.chatPrefix = chatPrefix;
        this.tabPrefix = tabPrefix;
        this.bukkitColor = bukkitColor;
        this.priority = priority;
    }

    public String getId() { return id; }
    public ChatColor getBungeeColor() { return ChatColor.of(hexColor); }
    public String getChatPrefix() { return chatPrefix; }
    public String getTabPrefix() { return tabPrefix; }
    public org.bukkit.ChatColor getBukkitColor() { return bukkitColor; }
    public String getPriority() { return priority; }

    public static TeamType getById(String id) {
        for (TeamType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}