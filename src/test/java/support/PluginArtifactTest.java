package support;

import imp.crissyjuanxd.bloodmoon.BloodMoon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Detecta JARs que funcionan en Linux pero pierden clases al compilarse en Windows. */
class PluginArtifactTest {
    @TempDir Path directory;
    private final Path compiled = Path.of("target", "classes");

    @Test void outputDirectoriesDoNotCollideOnCaseInsensitiveFilesystems() throws IOException {
        Map<String, String> spellings = new HashMap<>();
        try (var paths = Files.walk(compiled)) {
            for (Path path : paths.filter(Files::isDirectory).toList()) {
                String relative = compiled.relativize(path).toString().replace('\\', '/');
                String previous = spellings.putIfAbsent(relative.toLowerCase(Locale.ROOT), relative);
                if (previous != null) assertEquals(previous, relative,
                        "Dos carpetas se fusionan en Windows y cambian las rutas dentro del JAR");
            }
        }
    }

    @Test void theNativeBloodMoonLoadsFromAJarWithWindowsDirectorySemantics() throws Exception {
        List<Path> resources = new ArrayList<>(), classes = new ArrayList<>();
        try (var paths = Files.walk(compiled)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                (path.toString().endsWith(".class") ? classes : resources).add(path);
            }
        }
        // Maven copia primero los recursos y luego compila. Windows conserva el primer
        // nombre de carpeta, aunque javac intente escribir después con otras mayúsculas.
        resources.addAll(classes);
        Path jar = directory.resolve("QuasoPlugin-windows.jar");
        Map<String, String> directories = new HashMap<>();
        try (var zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            for (Path file : resources) {
                String[] parts = compiled.relativize(file).toString().replace('\\', '/').split("/");
                String parent = "";
                for (int i = 0; i < parts.length - 1; i++) {
                    String candidate = parent + parts[i] + "/";
                    parent = directories.computeIfAbsent(candidate.toLowerCase(Locale.ROOT), ignored -> candidate);
                }
                zip.putNextEntry(new ZipEntry(parent + parts[parts.length - 1]));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
        String binaryName = BloodMoon.class.getName();
        String nativePackage = binaryName.substring(0, binaryName.lastIndexOf('.') + 1);
        // Solo las dependencias vienen del classpath de tests. La clase y sus tipos
        // nativos deben salir del JAR, sin rescatarse de target/classes.
        try (var loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith(nativePackage)) return super.loadClass(name, resolve);
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) type = findClass(name);
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        }) {
            Class<?> type = Class.forName(binaryName, false, loader);
            assertSame(loader, type.getClassLoader());
            assertTrue(type.getDeclaredMethods().length > 0);
        }
    }
}
