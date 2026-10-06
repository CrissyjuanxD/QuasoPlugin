package imp.crissyjuanxd.bloodmoon;

/** Calendario persistente basado en días del mundo, independiente de tareas de Bukkit. */
public final class BloodMoonCycle {
    private long nextNight;
    private long warnedDay = Long.MIN_VALUE;
    private long lastDay;

    public BloodMoonCycle(long currentDay, int interval, long nextNight) {
        this.lastDay = currentDay;
        this.nextNight = nextNight >= 0 ? nextNight : currentDay + Math.max(1, interval) - 1;
    }

    public long nextNight() { return nextNight; }
    public long remaining(long day) { return Math.max(1, nextNight - day + 1); }
    public boolean isDue(long day, long time) { return day >= nextNight && time >= 12000; }
    public boolean shouldWarn(long day, long time) {
        if (time < 11000 || warnedDay == day) return false;
        warnedDay = day;
        return true;
    }
    public void started(long day, int interval) { nextNight = day + Math.max(1, interval); }
    public void stopped(long day, int interval) { nextNight = day + Math.max(1, interval) - 1; warnedDay = day; }
    public void observe(long day, int interval) {
        if (day < lastDay) { nextNight = day + Math.max(1, interval) - 1; warnedDay = Long.MIN_VALUE; }
        lastDay = day;
    }
}
