package Events.AchievementParty;

public interface Achievement {
    String getName();
    String getDescription();
    void initializePlayerData(String playerName);
    void checkCompletion(String playerName);
}