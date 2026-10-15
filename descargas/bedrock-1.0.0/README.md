# Croissants · Segunda edición — Bedrock 1.0.0

Port del `QuasoTextures.zip` entregado el 15 de octubre de 2026, para **Geyser-Spigot 2.12.0-b1256** en Paper. Conserva los dibujos originales del pack Java. El pack principal requiere Bedrock **26.30 o posterior**; el complemento de mobs usa plantillas de **26.50**.

[Descargar el paquete para instalar](Quaso-Bedrock-1.0.0-instalar.zip) · [Pack principal](packs/QuasoTextures-1.0.0.mcpack) · [Mappings](custom_mappings/quaso_items.json) · [Mobs opcionales](opcional_mobs/QuasoMobs-1.0.0.mcpack)

## Instalación en tu Paper

1. Apaga el servidor. Extrae `Quaso-Bedrock-1.0.0-instalar.zip` en su carpeta principal. El ZIP ya tiene esta estructura:

   ```text
   plugins/
     Geyser-Spigot/
       packs/
         QuasoTextures-1.0.0.mcpack
       custom_mappings/
         quaso_items.json
   ```

   También puedes copiar esos dos archivos manualmente. El `.mcpack` se deja **sin descomprimir**. No pongas el ZIP de instalación dentro de `packs` ni lo configures como el resource pack Java de `server.properties`.

2. Dentro de la sección **existente** `gameplay:` de `plugins/Geyser-Spigot/config.yml`, comprueba estas opciones:

   ```yaml
   gameplay:
     enable-custom-content: true
     force-resource-packs: true
   ```

   Modifica las claves que ya existen; no dupliques `gameplay` ni reemplaces todo tu config. Las otras opciones, Floodgate y tus puertos se conservan. Puedes mantener `enable-integrated-pack: true`.

3. Inicia el servidor completo. Geyser registra los items durante el arranque; `/reload` no basta para instalar mappings nuevos. Con solo este lote y el pack integrado de Geyser, la consola registra **314 custom items**: 313 de Quaso y uno integrado.
4. Entra desde Bedrock y acepta descargar el pack. Para las GUIs de misiones, habilidades, menú, trabajos y tiendas, usa **Ajustes → Vídeo → Perfil de IU → Clásico**. Las cuadrículas mantienen sus slots y clics. La interfaz Pocket conserva sus controles nativos, sin estos fondos personalizados.
5. La estatua de recompensas corregida requiere también QuasoPlugin desde el commit **`0da7623`**. Las estatuas existentes se actualizan al cargar; modelo y plugin deben actualizarse juntos.

Tu `/geyser version` anuncia Java **26.3**, mientras el servidor es Paper **26.2**. Si todavía no puedes entrar desde Bedrock, instala/actualiza **ViaVersion en Paper** para que acepte el protocolo que requiere Geyser. No hace falta cambiar el resource pack Java ni instalar un behavior pack.

## Qué incluye

| Apartado | Contenido |
| --- | --- |
| Items | Los 312 `item_model` personalizados del ZIP y el papel invisible `minecraft:air`: 313 mappings. Cada uno está asociado al material que usa QuasoPlugin. |
| Arcos | Seis modelos, cada uno con reposo y las tres fases de tensado. |
| Equipo | Armaduras floral y Warden, botas escaladoras y peto Warden alado; texturas de las alas y segunda capa de flores. Lanzas y escudo con geometría/animaciones de mano. |
| Estatua protectora | Conserva el modelo 3D y su atlas; el inventario usa una vista del modelo, no el atlas desplegado. |
| Fuentes | Los 23 códigos de iconos, logos y rangos de Java. También hay versiones anchas de los rangos mediante secuencias de caracteres: [UNICODES.md](UNICODES.md). |
| GUIs | Siete fondos reconocidos por el título del inventario. Se utilizan `menu_bedrock`, `habilidades_bedrock` y `trabajos_bedrock` del ZIP cuando están disponibles. |
| Identidad | Icono del pack y logo **Croissants Segunda Edición** en la pantalla de progreso cuando el pack está aplicado. Logo ancho para el título del scoreboard `U+E900`. |

Los nombres visibles, lores, encantamientos y efectos siguen viniendo del plugin. `minecraft:map` y `minecraft:recovery_compass` conservan los recursos vanilla; no les falta un PNG personalizado.

## Mobs opcionales

En [opcional_mobs](opcional_mobs) hay un segundo `.mcpack` con **15 modelos**: Zombie/Araña/Creeper Floral, Abeja Floral y Mini Abeja Floral, cinco variantes de Warden Cave, cuatro de BloodMoon y la Estatua de Recompensas.

Para probarlo, copia `QuasoMobs-1.0.0.mcpack` también a `plugins/Geyser-Spigot/packs/`, junto al principal, y reinicia. **Todos los clientes que lo reciban deben ser Bedrock 26.50 o posterior.** No lo añadas todavía si entran jugadores con 26.30. Con `force-resource-packs` todos reciben los packs instalados; no se asignan automáticamente según la versión del jugador.

La selección usa el nombre que Geyser envía al cliente. Se contemplan nombres sin formato y colores/negritas de Bedrock. Los mobs normales mantienen el modelo vanilla. Si cambias un nombre o le añades un sufijo, habrá que actualizar las condiciones. La mini abeja conserva la variante 3 y la abeja principal la 2. La estatua tiene la base en Y=0 y compensa la nueva escala 4,5 del plugin, manteniendo el tamaño del dinosaurio.

Se adaptaron los cubos, UV, submodelos y movimientos básicos a Bedrock. Los archivos `.jem` y las expresiones de animación de OptiFine no se ejecutan en Bedrock. Las máscaras emisivas se convierten en una capa visible en la oscuridad; no iluminan el entorno ni garantizan bloom. **El complemento necesita una prueba visual con un cliente real antes de dar por cerrados los movimientos y la selección por nombre.**

## Diferencias que debes conocer

- **Unicodes:** Bedrock no interpreta `font/default.json`, `height`, `ascent` ni espacios negativos de Java. Los códigos de iconos se conservan, pero su tamaño depende de la fuente de Bedrock. Los rangos de un solo carácter se muestran compactos; para mantenerlos anchos usa las secuencias de `UNICODES.md` en textos destinados solo a Bedrock. Las pantallas ORE UI no admiten estos glifos. Las GUIs grandes se recrean con JSON UI, no como letras gigantes. No se porta el blanco de pantalla completa `U+E076`.
- **Primera descarga:** el cliente no puede dibujar una imagen del pack antes de haber descargado ese pack. El logo está en el icono y en la pantalla de progreso una vez aplicado; no sustituye el aviso de aceptación de la primera descarga.
- **Monedas:** Java usa pilas de 99; Bedrock permite hasta 64. El mapping fija 64 para el cliente y evita avisos de registro, pero **no transforma las pilas de 99 que ya existen en el servidor**. Hay que comprobar monederos, tiendas y transferencias con Bedrock antes de usarlos con pilas de más de 64; resolver eso requiere adaptar la lógica del plugin, fuera de este port visual.
- **Peto alado:** las alas se incluyen visualmente. Un pack no puede añadir soporte de vuelo a un peto personalizado si Geyser no traduce ese componente; no equivale a garantizar que funcione como una elytra en Bedrock.
- **Animación de iconos:** `rep_hierro` y `rep_oro` usan su primer fotograma. Las cuatro poses de los arcos sí están incluidas.
- **Bloques por bioma:** se excluyen las reglas CTM de Warden Cave. Un reemplazo normal de textura cambiaría esos bloques también fuera del bioma. Para limitarlo a la dimensión haría falta una integración del servidor con estados/identificadores de bloques propios.
- **Otros extras:** no se portan shaders, cielos OptiFine, variaciones CTM, las animaciones especiales de `armor_stand3`, ni los cambios vanilla ajenos a tus prioridades. Tampoco se inventan recursos del End que aún no existen.

## Comprobaciones realizadas

Los scripts validan JSON, PNG, referencias de iconos, mappings únicos, geometrías, capas de armadura, cuadrículas y la altura de la estatua. Se arrancó **Geyser Standalone 2.12.0-b1256**, que comparte el lector de mappings con Geyser-Spigot, para comprobar su registro real. Esto no es una sesión de juego ni una prueba visual en Windows, móvil o consola: **todavía hace falta comprobar allí las interfaces, UV, postura de las alas y mobs**. Detalles en [VALIDACION.md](VALIDACION.md).

## Actualizaciones

Conserva los UUID de los manifiestos y aumenta `header.version` y `modules[].version` cuando publiques 1.0.1, 1.1.0, etc. Así Bedrock detecta la actualización. Si cambias los modelos de items también hay que actualizar `quaso_items.json` y reiniciar Geyser. Retira versiones anteriores del `.mcpack` de `packs` para no servir dos packs con el mismo UUID.

El código fuente del port está en [resource_pack](resource_pack), y los conversores reproducibles en [herramientas](herramientas). Los PNG proceden de tu ZIP; las plantillas de interfaz/attachables proceden de [Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples), con su [licencia](LICENCIA-BEDROCK-SAMPLES.md). La procedencia y SHA-256 del ZIP original están en [procedencia.json](procedencia.json).

Referencias: [custom items de Geyser](https://geysermc.org/wiki/geyser/custom-items/) · [contenido personalizado](https://geysermc.org/wiki/geyser/custom-content/) · [glifos Bedrock](https://wiki.bedrock.dev/text/custom-emojis).
