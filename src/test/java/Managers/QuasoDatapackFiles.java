package Managers;

import java.util.List;

// Para que los tests de otros paquetes lean la lista de archivos del datapack
public final class QuasoDatapackFiles {
    private QuasoDatapackFiles() {}

    public static List<String> files() {
        return QuasoDatapack.files();
    }
}
