# Quaso para Bedrock · 1.1.3

Actualización completa para **Geyser como plugin de Paper**. Corrige las interfaces, la Abeja Floral y la intensidad visual de la tormenta durante BloodMoon. Añade los displays de tumbas y ataques. Incluye también los items, armaduras, rangos, otros mobs y Warden Cave de la versión 1.1.2.

## Instalar desde 1.1.2 o versiones anteriores

1. **Apaga el servidor.**
2. En `plugins/Geyser-Spigot/packs/`, retira los packs anteriores de **QuasoTextures**, **QuasoMobs** y **QuasoWardenCave**. Conserva los packs de otros proyectos.
3. En `plugins/Geyser-Spigot/extensions/`, retira los JAR antiguos de **QuasoWardenCave**. Conserva su carpeta de configuración.
4. Descomprime `Quaso-Bedrock-1.1.3-instalar.zip` y copia su carpeta `plugins` sobre la del servidor. Deben quedar estos archivos:

   ```text
   plugins/Geyser-Spigot/packs/QuasoTextures-1.1.3.mcpack
   plugins/Geyser-Spigot/extensions/QuasoWardenCave-1.1.3.jar
   plugins/Geyser-Spigot/custom_mappings/quaso_items.json
   ```

5. Conserva `enable-custom-content: true` y la configuración de Geyser que ya permite conectarse.
6. Arranca el servidor. La consola debe indicar `Presentación 1.1.3`. No uses `/reload`.
7. Cierra Minecraft Bedrock completamente y vuelve a entrar para descargar el pack nuevo. Si conserva la versión anterior, elimina **sólo el pack Quaso en caché** desde Ajustes → Almacenamiento → Datos en caché y reconecta.

No necesitas reemplazar QuasoPlugin: esta actualización modifica el pack y la extensión de Geyser. Los modelos de Java permanecen como están.

## Correcciones

### Interfaces

Los siete fondos se seleccionan con `variables.requires` y una copia del título, siguiendo el mecanismo del pack integrado de Geyser. La extensión conserva también su prefijo de filas. Esto permite reconocer el menú aunque el título lleve códigos de formato.

Cada menú activa sólo su imagen; los cofres normales conservan su interfaz. Los títulos técnicos y el fondo gris de las casillas se ocultan únicamente en los menús Quaso. Los items, botones y transacciones siguen siendo los del inventario nativo.

Se conservan las imágenes ya convertidas del pack Java, incluidas sus variantes Bedrock: **176 píxeles de ancho y casillas de 18 píxeles**. Trabajos usa 27 casillas; los demás fondos, 54. La corrección está destinada al perfil de IU **Clásico**.

### Abeja Floral

Se reconstruye el modelo Bedrock desde el `bee2.jem` original. Se corrigen el reflejo horizontal de los cubos, los pivotes y las rotaciones del abdomen y las patas, y se usan UV de caja nativos y el material de abeja para las superficies planas.

Conserva las cuatro texturas de sus estados y la animación de vuelo. La Abeja Floral sigue siendo el modelo **2**; la mini abeja floral conserva el **3**. Los otros modelos y texturas permanecen iguales a los de 1.1.2.

### BloodMoon

La lluvia y la tormenta siguen activas en el servidor. Para Bedrock, su intensidad visual baja gradualmente durante los **10 segundos** de entrada:

- Lluvia: **30 %** de la intensidad recibida.
- Tormenta: **8 %**, para reducir el oscurecimiento del cielo.
- Se mantiene la niebla y el horizonte rojo de BloodMoon.

Al terminar, la intensidad visual vuelve gradualmente al clima real. Al salir del mundo se restaura inmediatamente. Los cambios de tiempo que lleguen durante el evento siguen respetando esta reducción; fuera del evento, Geyser muestra el clima normal.

Bedrock no reproduce exactamente los atributos visuales del datapack Java. Esta corrección reduce la oscuridad que tapa el rojo; el aspecto final depende del renderizador del cliente.

### Displays: tumbas y ataques

Se registra una entidad visual Bedrock para los `block_display` usados por Quaso y los `item_display` que forman sus tumbas. Incluye **30 tipos de bloque**, sus variantes y las **siete cabezas originales de la tumba**, además de su losa. El catálogo tiene 62 modelos, contando la raíz invisible.

Se muestran los panales giratorios de curación de la Abeja Floral, sus aguijones, el hielo del Iceologer, los proyectiles de la Ender Spider, los catalizadores del Ultra Warden, los spawners y el portal de Warden Cave. Se conservan traslación, rotación izquierda/derecha, escala independiente por eje y escala negativa; la curación interpola sus cambios de dos ticks. Los textos montados en las tumbas recuperan su desplazamiento.

La representación sigue los paquetes de creación, movimiento y eliminación de Geyser. No crea entidades adicionales en Java ni cambia el daño, la curación, el inventario de la tumba o las hitboxes de interacción.

El soporte cubre los modelos actuales de Quaso incluidos en `extension/src/main/resources/displays.json`. Los modelos nuevos de otros plugins, las cabezas de perfiles ajenos a estas tumbas y otras modalidades de `item_display` necesitan su propia adaptación. Los contornos luminosos de Java y el texto pegado a una superficie no tienen una reproducción exacta en este puente: los textos conservan la representación de Geyser.

## Comprobación

Pruebas automatizadas con **Geyser-Spigot 2.12.0 b1256 y b1262**: títulos con prefijos, selección exclusiva de menús, pivotes y UV de la abeja, conservación de los recursos anteriores, transición y restauración del clima, catálogo y ciclo de vida de los displays, y las integraciones ya existentes de bloques, biomas y mobs. Se comprueba además el arranque con **Paper 26.2 build 129 y Geyser b1262**, incluida la nueva entidad visual. El entorno no pudo consultar las claves públicas de Mojang; no se verificó una conexión Xbox autenticada.

El entorno de compilación no dispone de un cliente gráfico de Bedrock. La comprobación visual pendiente es en **Windows 26.52, IU Clásico**:

1. Abrir un cofre normal y los menús Quaso: cada menú debe mostrar sólo su fondo, con los items encima y las casillas alineadas.
2. Observar una Abeja Floral por ambos lados, en reposo y volando.
3. Activar y terminar BloodMoon: la lluvia debe continuar, con menos oscuridad y una transición de diez segundos. Salir del mundo durante el evento debe restaurar su clima normal.
4. Visitar una tumba y recogerla; activar la curación de la Abeja Floral y romper un panal. Las piezas deben aparecer y desaparecer junto a la entidad original, y las interacciones deben seguir funcionando.

Se conservan formatos estables y la API de propiedades de Geyser para los mobs. La adaptación de paquetes todavía utiliza integraciones internas de Geyser; sus futuras versiones y los cambios de JsonUI pueden requerir revisión.

## Logo opcional del cliente

`opcional_cliente/QuasoLogo-global-1.1.3.mcpack` se importa en Minecraft y se activa en **Recursos globales** para mostrar el logo antes de descargar los recursos del servidor. Sustituye el pack global antiguo si lo instalaste. No lo coloques en la carpeta `packs` de Geyser.

Un pack del servidor aún sin descargar no puede dibujar su propia pantalla inicial de descarga. El pack global cambia también el logo de otros menús del cliente.

## Reconstruir

Desde la raíz del repositorio, con Python/Pillow y `JAVA_HOME` apuntando a JDK 25 o posterior:

```bash
python descargas/bedrock-1.1.3/herramientas/preparar.py
python descargas/bedrock-1.1.3/herramientas/compilar.py /ruta/Geyser-Spigot.jar /ruta/servidor/libraries
python descargas/bedrock-1.1.3/herramientas/probar.py /ruta/Geyser-Spigot.jar /ruta/servidor/libraries /ruta/dependencias-test
python descargas/bedrock-1.1.3/herramientas/empaquetar.py
python descargas/bedrock-1.1.3/herramientas/validar.py
```

`preparar.py` conserva los recursos de `../bedrock-1.1.2/`, convierte el original `herramientas/bee2-original.jem` y ejecuta `displays.py`. Este último usa los modelos/texturas originales guardados en `herramientas/display-originals`; `sources.json` registra su procedencia y huellas. Los bloques provienen de los recursos Minecraft publicados por Mojang y las cabezas son los perfiles ya usados por Quaso; no se incluye código de extensiones externas.

Las dependencias de prueba son Mockito 5.20.0, Byte Buddy/agent 1.17.7 y Objenesis 3.3. La compilación verifica las huellas de los JAR Geyser-Spigot b1256/b1262; no acepta Standalone. El instalador contiene recursos y la extensión, sin configuración privada del servidor.
