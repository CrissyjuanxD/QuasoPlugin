package mobcap.optimization;

import org.bukkit.Bukkit;

public class PerformanceMonitor {
    private long lastCheck = System.currentTimeMillis();
    private double averageTPS = 20.0;
    private long usedMemory = 0;
    private double cpuUsage = 0.0;

    public PerformanceData getPerformanceData() {
        updatePerformanceMetrics();
        return new PerformanceData(averageTPS, usedMemory, cpuUsage);
    }

    private void updatePerformanceMetrics() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastCheck;
        if (timeDiff > 1000) {
            try {
                Object server = Bukkit.getServer();
                if (server.getClass().getName().contains("CraftServer")) {
                    averageTPS = Math.min(20.0, 20.0 * (1000.0 / Math.max(timeDiff, 50)));
                }
            } catch (Exception e) {
                averageTPS = 20.0;
            }
            lastCheck = currentTime;
        }

        Runtime runtime = Runtime.getRuntime();
        usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        cpuUsage = Math.min(100.0, (usedMemory / (double) (runtime.maxMemory() / (1024 * 1024))) * 100);
    }
}