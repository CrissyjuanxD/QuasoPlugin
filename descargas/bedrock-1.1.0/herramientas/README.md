# Reconstrucción

Requiere Python 3.10+ y JDK 25. Ejecutar desde la raíz de QuasoPlugin:

```bash
python descargas/bedrock-1.1.0/herramientas/preparar.py
python descargas/bedrock-1.1.0/herramientas/compilar.py /ruta/Geyser-Spigot.jar /ruta/paper/libraries
python descargas/bedrock-1.1.0/herramientas/empaquetar.py
python descargas/bedrock-1.1.0/herramientas/validar.py
```

El compilador verifica el SHA-256 de Geyser-Spigot. **No compilar con Standalone:** Spigot reubica Adventure y fastutil; un binario compilado con Standalone puede compilar y luego fallar en Paper. El directorio `libraries` se genera al arrancar Paper. La extensión se emite con bytecode Java 21 y se prueba en Paper 26.2/Java 25.

Para las pruebas Java, usa en el classpath el JAR compilado, Geyser-Spigot, las bibliotecas de Paper y Mockito 5.20.0 con ByteBuddy 1.17.7/Objenesis 3.3. Compila `extension/src/test/java` y ejecuta los métodos `main` de `studios.viciont.quasogeyser.PresentationTest` y `studios.viciont.quasogeyser.WorldTranslationTest` y `studios.viciont.quasogeyser.AtmosphereTest`. También puedes ejecutar el runner:

```bash
python descargas/bedrock-1.1.0/herramientas/probar.py /ruta/Geyser-Spigot.jar /ruta/paper/libraries /ruta/dependencias-de-pruebas
```

Repite con el Geyser nuevo antes de declararlo compatible.

`validar.py` comprueba los archivos que recibe el cliente, mantiene intactos los mappings y texturas de equipo aprobados, revisa modelos/biomas/selectores y verifica los checksums del instalador. No simula el motor gráfico de Minecraft.

Referencias:

- [Modificaciones parciales JSON UI](https://wiki.bedrock.dev/json-ui/best-practices)
- [Propiedades de bindings](https://wiki.bedrock.dev/json-ui/json-ui-documentation#data-binding)
- [Biomas del cliente Bedrock](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/clientbiomesreference/examples/clientbiomesoverview)
- [Geyser Spigot b1256](https://download.geysermc.org/v2/projects/geyser/versions/2.12.0/builds/1256)
- [Geyser Spigot b1262](https://download.geysermc.org/v2/projects/geyser/versions/2.12.0/builds/1262)
