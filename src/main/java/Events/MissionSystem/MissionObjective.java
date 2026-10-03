package Events.MissionSystem;

// Una parte de la misión: un contador, algo que se marca una vez o un tiempo en segundos
public record MissionObjective(String key, String label, int target, Format format) {

    public enum Format { NUMBER, FLAG, TIME }

    public String formatValue(int value) {
        return format == Format.FLAG ? (value >= target ? "✔" : "✖") : formatCurrent(value) + "/" + formatTarget();
    }

    public String formatCurrent(int value) {
        return format == Format.TIME ? time(value) : number(value);
    }

    public String formatTarget() {
        return format == Format.TIME ? time(target) : number(target);
    }

    private static String number(int value) {
        return String.format("%,d", value).replace(',', '.');
    }

    private static String time(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h " + String.format("%02d", (seconds % 3600) / 60) + "m";
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
