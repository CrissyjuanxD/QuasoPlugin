package Handlers.Teams;

import net.md_5.bungee.api.ChatColor;


public enum TeamType {
    // Rangos con unicode: el ícono va en blanco y el nombre con su color en el chat y el tab. Arriba de la cabeza y en
    // los logros el nombre sale blanco porque el team solo acepta los 16 colores de siempre
    ADMIN("Admin", "#FDC42C", '\uEB87', '\uEB8F', "01_Admin"),
    MOD("Mod", "#5FAEED", '\uEB88', '\uEB90', "02_Mod"),
    T_HELPER("THelper", "#C241EF", '\uEB89', '\uEB91', "03_Helper"),

    // Los pone el sistema de Twitch mientras la sub o el VIP del canal de Crosszy siguen activos
    U_SUB("USub", "#BF8DF5", '\uEB8D', '\uEB95', "95_Sub"),
    V_VIP("VVip", "#F38DF5", '\uEB8E', '\uEB96', "96_Vip"),
    X_LEYENDA("XLeyenda", "#F3A85E", '\uEB8C', '\uEB94', "97_Leyenda"),
    Y_MIEMBRO("YMiembro", "#F3EF8B", '\uEB8B', '\uEB93', "98_Miembro"),
    Z_MIEMBRO("ZMiembro", "#FCD869", '\uEB8A', '\uEB92', "99_Miembro"),

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

    BUILDBATTLE("buildbattle", "#C056E6",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "BUILD" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "BATTLE" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            ChatColor.GRAY + "" + ChatColor.BOLD + "[" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "B" + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "BT" + ChatColor.GRAY + ChatColor.BOLD + "] ",
            org.bukkit.ChatColor.DARK_PURPLE, "08_buildbattle"),

    // Sin ícono propio: los unicodes que usaba ahora son de otros rangos
    Z_FANTASMA("ZFantasma", "#555555", "", "",
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

    TeamType(String id, String hexColor, char chatIcon, char tabIcon, String priority) {
        this(id, hexColor, ChatColor.WHITE + String.valueOf(chatIcon) + " ", ChatColor.WHITE + String.valueOf(tabIcon) + " ",
                org.bukkit.ChatColor.WHITE, priority);
    }

    public String getDisplayName() {
        return switch (this) {
            case ADMIN -> "Prieto";
            case MOD -> "DinoNalgon";
            case T_HELPER -> "Helper";
            case U_SUB -> "DinoSub";
            case V_VIP -> "DinoVip";
            case X_LEYENDA -> "DinoLeyenda";
            case Y_MIEMBRO -> "DinoNugget+";
            case Z_MIEMBRO -> "DinoNugget";
            case LAVACLASH -> "LavaClash";
            case ITEMPARTY -> "ItemParty";
            case HOTPOTATO -> "HotPotato";
            case BUILDBATTLE -> "BuildBattle";
            case Z_FANTASMA -> "Fantasma";
        };
    }

    public String getId() { return id; }
    public ChatColor getBungeeColor() { return ChatColor.of(hexColor); }
    public String getChatPrefix() { return chatPrefix; }
    public String getTabPrefix() { return tabPrefix; }
    public org.bukkit.ChatColor getBukkitColor() { return bukkitColor; }
    public String getPriority() { return priority; }

    // Busca el tipo por el nombre del team (sin importar mayúsculas)
    public static TeamType getById(String id) {
        for (TeamType type : values()) {
            if (type.getId().equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}