package Events.MissionSystem;

import net.md_5.bungee.api.ChatColor;

public enum MissionDifficulty {
    FACIL("Fácil", "#98FB98", 1),
    MEDIA("Media", "#FFD700", 2),
    DIFICIL("Difícil", "#FF8C42", 3),
    MUY_DIFICIL("Muy difícil", "#FF4F4F", 4);

    private final String label;
    private final String color;
    private final int xpPerSlot;

    MissionDifficulty(String label, String color, int xpPerSlot) {
        this.label = label;
        this.color = color;
        this.xpPerSlot = xpPerSlot;
    }

    public String getLabel() { return label; }
    public String colored() { return ChatColor.of(color) + label; }
    public int getXpPerSlot() { return xpPerSlot; }
}
