package Events.MissionSystem;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public interface Mission {
    String getName();
    String getDescription();
    void initializePlayerData(String playerName);
    void checkCompletion(String playerName);
    List<ItemStack> getRewards();
    int getMissionNumber();

    // Las misiones extra devuelven la misión normal con la que se activan (0 si no es extra)
    default int getParentMission() {
        return 0;
    }
}
