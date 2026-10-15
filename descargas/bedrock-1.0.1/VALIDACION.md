# Validación de Quaso Bedrock 1.0.1

## Entorno

- Geyser Standalone **2.12.0-b1256**, commit `fc2170c`, mismo núcleo de traducción que Geyser-Spigot.
- SHA-256 del Geyser oficial: `e31619e6c0fdd0e4a06d547ff476fe6bed23d0e8e5cc56479846ec80bc2a5e77`.
- Compilación con JDK 25, destino Java 21. No se incluye Geyser dentro de la extensión.
- Pack principal, mappings, pack nuevo y extensión cargados juntos. Servidor de prueba ligado únicamente a `127.0.0.1`.

## Comprobaciones

1. Compilación del JAR contra la versión real de Geyser, no stubs.
2. Arranque real: **314 custom items**, **14 custom blocks** (7 integrados + 7 nuevos), y mensaje de activación de la extensión. Los **199 overrides** y **2 overrides de items de bloque** integrados no aumentan.
3. Al activar la extensión se verifican los **49 estados** (16 de minerales, 6 de froglights y 27 de enredaderas con bayas) en **3 protocolos Bedrock**. Los bloques ajenos permanecen idénticos por referencia; la tabla global no se modifica.
4. Prueba ejecutable con Mockito: entrar, reaparecer dentro, salir, coincidencia exacta del mundo, dos sesiones aisladas y delegación del respawn original una sola vez, después de seleccionar el mapping. Comprueba también enredaderas cosechadas y froglights ajenos.
5. Los siete PNG de color son idénticos a los del CTM Java. Las cuatro máscaras emisivas se convierten a MER, preservando los píxeles sin emisión. Se validan JSON, PNG, atlas, referencias y CRC/hashes del instalador, mcpack y JAR.

Los logs relevantes se guardan en [validacion-geyser.txt](validacion-geyser.txt). Los tests están en `extension/src/test/java`.

## Reproducir

Con `JAVA_HOME` apuntando a un JDK 21 o posterior:

```sh
python herramientas/compilar.py /ruta/Geyser-Standalone-2.12.0-b1256.jar
python herramientas/recursos.py
python herramientas/empaquetar.py
python herramientas/validar.py
```

Los scripts de recursos requieren Pillow. La compilación verifica el hash del Geyser indicado; no descarga ni ejecuta binarios desconocidos. Para `WorldTranslationTest`, compila y ejecuta con el JAR de Geyser, el de la extensión y Mockito 5.20.0 con sus dependencias (Byte Buddy 1.17.7 y Objenesis 3.3) en el classpath.

## Lo que todavía necesita un cliente

No se ejecutó Minecraft Bedrock ni una conexión de jugador Paper→Geyser. Por tanto, el arranque y los tests no confirman el renderizado real, el brillo PBR según dispositivo, la descarga en consola/móvil ni las interacciones de inventario. La guía contiene los pasos de prueba para entrar y salir de Warden Cave, cosechar y romper bloques.

El planeo del peto personalizado tiene el límite de cliente documentado en el README. Las pilas Java de 99 monedas tampoco se transforman en esta entrega visual. No se cambiaron los efectos ni los atributos de los items.
