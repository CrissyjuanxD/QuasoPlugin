package Trabajos;

import Handlers.DatabaseManager;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// Lo que tiene guardado un jugador: el nivel y la XP de cada trabajo (se conservan al cambiar) y el trabajo actual
final class DatosTrabajo {

    Trabajo activo;
    long desde;
    final Map<Trabajo, Integer> niveles = new EnumMap<>(Trabajo.class);
    final Map<Trabajo, Double> xp = new EnumMap<>(Trabajo.class);
    boolean sucio;

    // Ventana de una hora para el tope de XP
    long inicioHora;
    double xpEnLaHora;

    int nivel(Trabajo trabajo) {
        return niveles.getOrDefault(trabajo, 0);
    }

    double xp(Trabajo trabajo) {
        return xp.getOrDefault(trabajo, 0.0);
    }

    static DatosTrabajo desde(DatabaseManager.JobsData data) {
        DatosTrabajo datos = new DatosTrabajo();
        datos.activo = Trabajo.porId(data.activeJob());
        datos.desde = data.joinedAt();
        for (Map.Entry<String, DatabaseManager.JobProgress> entry : data.progress().entrySet()) {
            Trabajo trabajo = Trabajo.porId(entry.getKey());
            if (trabajo == null) continue;
            datos.niveles.put(trabajo, Math.max(0, Math.min(Trabajo.NIVEL_MAXIMO, entry.getValue().level())));
            datos.xp.put(trabajo, Math.max(0, entry.getValue().xp()));
        }
        return datos;
    }

    // Copia para guardar desde otro hilo
    DatabaseManager.JobsData copia() {
        Map<String, DatabaseManager.JobProgress> progreso = new HashMap<>();
        for (Trabajo trabajo : Trabajo.values()) {
            if (!niveles.containsKey(trabajo) && !xp.containsKey(trabajo)) continue;
            progreso.put(trabajo.id(), new DatabaseManager.JobProgress(nivel(trabajo), xp(trabajo)));
        }
        return new DatabaseManager.JobsData(activo == null ? null : activo.id(), desde, progreso);
    }
}
