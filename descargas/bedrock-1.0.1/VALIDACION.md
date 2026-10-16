# Validación de la extensión QuasoWardenCave 1.0.1

## Entorno probado

- **Paper 26.2-129**, Java 25, ViaVersion **5.12.1** y **Geyser-Spigot 2.12.0-b1256**, commit `fc2170c`.
- SHA-256 del Geyser-Spigot oficial: `c61720bbea714a704d9b76a2f1e9a7ac91271e48101ec745ccd668fda9514015`.
- SHA-256 del Paper oficial: `b1d8f6bfa1b6101fa8e947b53041cb3bdf5540e7b83b6547ca19ba7edefeb083`.
- Compilación con JDK 25, destino Java 21. No se incluyen Geyser ni sus librerías dentro de la extensión.
- Servidor local de prueba ligado a `127.0.0.1`, con el pack principal, los mappings y el pack de Warden Cave. No se cargó QuasoPlugin: esta prueba verifica la extensión y su entorno de carga real.

## Reproducción y corrección

1. Se arrancó Paper con la extensión anterior **1.0.0** y se reprodujo exactamente el `ClassCastException` de `Object2IntOpenHashMap` en `selectedStates`.
2. Se detuvo ese mismo servidor y se sustituyó únicamente la extensión por **1.0.1**, compilada contra **Geyser-Spigot**. También se corrigieron las referencias a Adventure `Key` que intervienen en login/respawn.
3. Al arrancar de nuevo, la extensión activó **49 estados en 3 protocolos** sin `ClassCastException`, `NoSuchMethodError` ni `NoClassDefFoundError`. Geyser registró **314 custom items** y **14 custom blocks**; los **199 overrides** y **2 overrides de items de bloque** integrados no aumentaron.
4. Se ejecutó `WorldTranslationTest` con el JAR de **Geyser-Spigot**, las librerías de Paper y Mockito: entrada, reaparición, salida, nombre exacto del mundo, dos sesiones aisladas, orden del cambio de mapping y delegación una sola vez. También se comprueban enredaderas cosechadas y froglights ajenos.
5. El script de compilación rechaza el JAR Standalone por hash/plataforma, sin sobrescribir el artefacto corregido. La validación del bytecode exige las referencias reubicadas de Spigot y rechaza referencias sin reubicar a fastutil o Adventure.
6. Se verifican JSON, PNG, máscaras MER, CRC y hashes del instalador; el ZIP incluye únicamente la extensión nueva. Las texturas y mappings conservan su contenido anterior.

El registro antes/después está en [validacion-geyser.txt](validacion-geyser.txt). Las pruebas de transiciones usan sesiones simuladas; el arranque Paper/Geyser es real.

La validación anterior solo utilizaba Standalone. Eso no detectaba los paquetes internos que Spigot reubica y no era suficiente para afirmar compatibilidad con Paper. Esta corrección reemplaza esa comprobación.

## Reproducir

Con `JAVA_HOME` apuntando a un JDK 21 o posterior:

```sh
python herramientas/compilar.py /ruta/Geyser-Spigot-2.12.0-b1256.jar
python herramientas/empaquetar.py
python herramientas/validar.py
```

Para regenerar las imágenes a partir de los PNG CTM originales, ejecuta `python herramientas/recursos.py` antes de empaquetar. Los scripts gráficos requieren Pillow. La compilación comprueba el hash oficial del JAR Spigot; no sirve el de Standalone.

Para `WorldTranslationTest`, compila y ejecuta con Geyser-Spigot, la extensión corregida, los JAR de la carpeta `libraries` de un Paper iniciado y Mockito 5.20.0 con Byte Buddy 1.17.7 y Objenesis 3.3 en el classpath.

Para repetir el arranque, usa un Paper 26.2-129 local con ViaVersion 5.12.1, instala el contenido del ZIP, conserva `enable-custom-content: true` y `force-resource-packs: true`, y verifica el mensaje `Warden Cave activa`. Esta comprobación se realiza tras arrancar el servidor completo; no mediante `/reload`.

## Alcance de la prueba

No se ejecutó un cliente Bedrock ni una sesión de juego Paper→Geyser. El arranque local en modo offline emitió un error independiente al consultar las claves públicas de Mojang; la autenticación no se comprobó. Eso no impidió reproducir el error original ni validar la carga y los mappings de la extensión corregida.

La revisión visual, las máscaras PBR según dispositivo y la interacción real con bloques e inventarios siguen necesitando el cliente. Las limitaciones conocidas del peto alado y las pilas de 99 monedas no cambian con esta corrección.
