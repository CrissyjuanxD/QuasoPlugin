package Twitch;

import org.bukkit.configuration.ConfigurationSection;

// Cuánto tiempo seguido se vio activa una sub (o un VIP). Solo cuenta el tiempo que el server la vio: si estuvo
// apagado no suma. El kit del mes N se destraba cuando la racha llega al mes N, así el segundo kit pide que la sub
// siga activa pasado el mes, o sea que se haya renovado. Si se la ve sin sub la racha se corta y vuelve a empezar
final class Racha {

    long inicio;
    long observado;
    long ultimaVez;
    int mesReclamado;

    boolean activa() {
        return inicio != 0;
    }

    // Cada revisión de Twitch (o la simulada) pasa por acá. Un hueco más largo que maxHueco no se suma
    void observar(boolean activa, long ahora, long maxHueco) {
        if (!activa) {
            cortar();
            return;
        }
        if (inicio == 0) {
            inicio = ahora;
            observado = 0;
            ultimaVez = ahora;
            mesReclamado = 0;
            return;
        }
        long hueco = ahora - ultimaVez;
        if (hueco > 0 && hueco <= maxHueco) observado += hueco;
        if (ahora > ultimaVez) ultimaVez = ahora;
    }

    void cortar() {
        inicio = 0;
        observado = 0;
        ultimaVez = 0;
        mesReclamado = 0;
    }

    int mesActual(long mes) {
        return inicio == 0 ? 0 : 1 + (int) (observado / mes);
    }

    boolean puedeReclamar(long mes) {
        return inicio != 0 && mesActual(mes) > mesReclamado;
    }

    // Los meses que no se reclamaron se pierden: se marca el mes en el que está la racha
    void reclamar(long mes) {
        mesReclamado = mesActual(mes);
    }

    // Tiempo de sub activa que falta para el kit siguiente
    long falta(long mes) {
        return Math.max(0, (long) mesReclamado * mes - observado);
    }

    void save(ConfigurationSection section) {
        section.set("inicio", inicio);
        section.set("observado", observado);
        section.set("ultima_vez", ultimaVez);
        section.set("mes_reclamado", mesReclamado);
    }

    void load(ConfigurationSection section) {
        if (section == null) return;
        inicio = section.getLong("inicio");
        observado = section.getLong("observado");
        ultimaVez = section.getLong("ultima_vez");
        mesReclamado = section.getInt("mes_reclamado");
    }
}
