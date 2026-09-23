package Events.MissionSystem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MissionData {
    private boolean active;
    private boolean completed;
    private boolean rewardClaimed;
    private Map<String, Object> progress;
    private transient boolean dirty = false;

    // El progreso se guarda en la base de datos como JSON
    public MissionData(boolean active, boolean completed, boolean rewardClaimed, String jsonProgress) {
        this.active = active;
        this.completed = completed;
        this.rewardClaimed = rewardClaimed;
        this.progress = new HashMap<>();

        if (jsonProgress != null && !jsonProgress.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> loaded = gson.fromJson(jsonProgress, type);
                if (loaded != null) this.progress = loaded;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public MissionData() {
        this.active = false;
        this.completed = false;
        this.rewardClaimed = false;
        this.progress = new HashMap<>();
    }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isRewardClaimed() { return rewardClaimed; }
    public void setRewardClaimed(boolean rewardClaimed) { this.rewardClaimed = rewardClaimed; }

    public Map<String, Object> getProgress() { return progress; }

    public void setProgressValue(String key, Object value) {
        this.progress.put(key, value);
    }

    public Object getProgressValue(String key) {
        return this.progress.get(key);
    }

    public int getProgressInt(String key) {
        Object val = progress.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    public boolean getProgressBool(String key) {
        Object val = progress.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return false;
    }

    // Listas guardadas en el progreso (estructuras visitadas, tipos de mobs, etc.)
    public List<String> getProgressList(String key) {
        List<String> list = new ArrayList<>();
        if (progress.get(key) instanceof Collection<?> values) {
            for (Object value : values) list.add(String.valueOf(value));
        }
        return list;
    }

    public String getJsonProgress() {
        return new Gson().toJson(this.progress);
    }
}