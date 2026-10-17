# Quaso Bedrock 1.0.1 · Warden Cave

> Actualización disponible: [Quaso Bedrock 1.1.0](../bedrock-1.1.0/README.md), con pack unificado y correcciones de presentación.

Este instalador reúne el port anterior (items, fuentes, rangos, GUIs y armaduras) y añade **las texturas de los cuatro minerales y las tres frutas colocadas**, solo dentro del mundo Warden Cave. Los mobs siguen siendo un complemento opcional.

[Descargar todo para instalar](Quaso-Bedrock-1.0.1-instalar.zip) · [Solo extensión](extensions/QuasoWardenCave-1.0.1.jar) · [Solo pack de bloques](packs/QuasoWardenCave-1.0.0.mcpack)

## Corrección para Geyser-Spigot

La extensión **1.0.1** corrige el `ClassCastException` de fastutil de la 1.0.0. La anterior se había compilado contra Standalone; Geyser-Spigot reubica fastutil y Adventure en otros paquetes, por lo que compartir versión de Geyser no garantizaba compatibilidad binaria.

**Si ya instalaste el lote:** apaga el servidor, elimina `plugins/Geyser-Spigot/extensions/QuasoWardenCave-1.0.0.jar` y coloca allí `QuasoWardenCave-1.0.1.jar`. Conserva la carpeta `quasowardencave`, sus ajustes, los `.mcpack` y los mappings. No dejes las dos versiones del JAR juntas. Inicia el servidor completo.

El instalador de este directorio ya incluye el JAR corregido. El contenido gráfico no ha cambiado.

## Instalación en Paper

1. Apaga el servidor y extrae **Quaso-Bedrock-1.0.1-instalar.zip** en la carpeta principal del servidor. La estructura queda así:

   ```text
   plugins/Geyser-Spigot/
     packs/
       QuasoTextures-1.0.0.mcpack
       QuasoWardenCave-1.0.0.mcpack
     custom_mappings/
       quaso_items.json
     extensions/
       QuasoWardenCave-1.0.1.jar
   ```

   La extensión va en **`plugins/Geyser-Spigot/extensions`**, no directamente en `plugins`. Los `.mcpack` se dejan cerrados, sin descomprimir. Si ya instalaste el lote 1.0.0, basta con añadir la extensión y el pack de Warden Cave. El pack principal mantiene su nombre y UUID porque no ha cambiado.

2. En la sección **existente** `gameplay:` de `plugins/Geyser-Spigot/config.yml`, configura:

   ```yaml
   gameplay:
     enable-custom-content: true
     force-resource-packs: true
   ```

   Conserva las demás opciones; no dupliques la sección. No necesitas un behavior pack, cambiar Floodgate ni actualizar QuasoPlugin para esta extensión.

3. Reinicia el servidor completo. Debes ver un mensaje parecido a:

   ```text
   [quasowardencave] Warden Cave activa para [minecraft:wardencave]: 49 estados por protocolo, 3 protocolos verificados. Fuera de esos mundos se restaura el mapping original.
   ```

   El registro de items sigue siendo **314** con el pack integrado de Geyser. La extensión añade **7 bloques custom** y ningún reemplazo global. Los mensajes `199 custom block overrides` y `2 custom block item overrides` del pack integrado ya existían antes de esta extensión.

4. Entra desde Bedrock, descarga los packs y usa **Perfil de IU → Clásico** para los menús personalizados. Revisa la guía [UNICODES.md](../bedrock-1.0.0/UNICODES.md) para los rangos anchos.

5. Si tus clientes son **Bedrock 26.50 o posterior**, copia también `opcional_mobs/QuasoMobs-1.0.0.mcpack` a `plugins/Geyser-Spigot/packs/`. No lo instales si todavía entran clientes 26.30; el pack principal y Warden Cave sí admiten 26.30+.

Probado con **Geyser-Spigot 2.12.0-b1256 (`fc2170c`) dentro de Paper 26.2-129**, con ViaVersion 5.12.1. Esta extensión se compila para Spigot/Paper; no es un JAR para Standalone ni proxies. La selección por mundo usa clases internas de Geyser: debe volver a verificarse al actualizarlo. Si tu Geyser anuncia Java 26.3 y Paper sigue en 26.2, conserva ViaVersion actualizado para permitir la conexión.

## Si tu mundo tiene otro nombre

El valor por defecto corresponde a `warden_cave: wardencave` en `plugins/QuasoPlugin/mundos.yml`. En el primer arranque la extensión crea:

```text
plugins/Geyser-Spigot/extensions/quasowardencave/config.properties
```

Su contenido relevante es:

```properties
worlds=minecraft:wardencave
```

Si renombraste el mundo, sustituye `wardencave` por su clave Java, en minúsculas, y reinicia. Admite varias claves exactas separadas por comas. No usa coincidencias parciales: `wardencave_copia` no se modifica automáticamente.

## Qué cambia

| Bloque Java dentro de Warden Cave | Apariencia Bedrock |
| --- | --- |
| Terracota esmaltada cian | Mineral cian |
| Terracota esmaltada lima | Mineral verde |
| Terracota esmaltada morada | Mineral morado |
| Terracota esmaltada gris | Mineral gris |
| Froglight verde | Baya Sculk |
| Froglight perlado | Fruta Abisal |
| Enredadera con bayas, cabeza y tramo | Baya Regenerativa |

Se utilizan los PNG originales del CTM. Las frutas conservan el modelo de bloque o enredadera; no se instala el antiguo complemento Java de frutas colgantes. Las enredaderas cosechadas/sin bayas se ven vanilla. El froglight ocre tampoco se sustituye.

La selección en Bedrock es **por mundo completo**, por lo que funciona en sus cuatro biomas y en las fronteras entre ellos. Fuera del mundo configurado todo conserva su mapping anterior, incluso si otro mundo usa los mismos biomas custom. No se cambian bloques guardados, recetas, drops, colisiones del servidor ni datos de inventario. Los jugadores Java siguen usando su pack Java.

Las máscaras emisivas de los cuatro minerales se convirtieron a mapas **MER** para **Vibrant Visuals/RTX compatible con PBR**. Solo emiten los cristales, no el fondo. En gráficos normales se ven los colores, pero no se garantiza emisión visible: el pack no enciende todo el bloque ni añade luz al servidor. Las frutas mantienen la luminosidad de sus materiales originales.

## Peto alado y lógica del plugin

Los efectos, atributos, recompensas y demás comportamiento siguen ejecutándose en QuasoPlugin. No hay que reprogramarlos en un behavior pack.

El planeo es una excepción de compatibilidad del cliente: QuasoPlugin usa `minecraft:glider` sobre un peto de netherita. Geyser 1256 reconoce ese componente para validar el planeo, pero su implementación documenta que **Bedrock no inicia el vuelo si no identifica el equipo como una elytra**. Su registrador de items personalizados tampoco añade un equivalente de `glider`. Las alas y el peto sí están incluidos visualmente. Esta entrega no altera el item ni promete resolver ese límite del cliente mediante texturas.

Referencia técnica: [SessionPlayerEntity.canStartGliding en la versión probada](https://github.com/GeyserMC/Geyser/blob/fc2170c/core/src/main/java/org/geysermc/geyser/entity/type/player/SessionPlayerEntity.java#L620).

Las monedas siguen teniendo su comportamiento Java original. El mapping Bedrock declara máximo 64, mientras QuasoPlugin permite 99; esta entrega **no reduce cantidades ni transforma pilas guardadas**. Comprueba también el movimiento de una pila de 99 en el monedero durante las pruebas. Adaptar ese comportamiento económico sería un cambio del plugin, no una textura.

## Prueba rápida

1. En el mundo normal, comprueba una de esas terracotas y un froglight: deben verse vanilla.
2. Entra en Warden Cave, visita sus cuatro biomas y mira minerales/frutas, tanto chunks nuevos como ya existentes.
3. Rompe un mineral y cosecha una enredadera. Comprueba textura tras el cambio y los drops del plugin.
4. Sal al mundo normal y vuelve a entrar. Repite después de morir y reaparecer en la dimensión.
5. Si puedes, deja otro jugador Bedrock en el mundo normal: sus bloques no deben cambiar cuando tú entras.
6. Prueba armaduras, menús, fuentes y los mobs opcionales desde un cliente real. Para los minerales emisivos, activa Vibrant Visuals/RTX si el dispositivo lo admite.

Se verificaron el arranque real de Geyser, los 49 estados en tres protocolos, el aislamiento de dos sesiones, el orden del cambio de mapping antes del respawn y la restauración al salir. **Se comprobó el arranque real en Paper, pero no se ejecutó un cliente Bedrock ni una sesión de juego Paper→Geyser.** La revisión visual y de interacción la cubren estos pasos. Detalles en [VALIDACION.md](VALIDACION.md).

## Retirar el complemento

Apaga el servidor y retira `QuasoWardenCave-1.0.1.jar` de `extensions` y su `.mcpack` de `packs`. Al arrancar se recupera la traducción original; no hay que regenerar el mundo ni migrar datos. El resto del pack Bedrock puede quedarse instalado.

El código de la extensión está en [extension](extension); los scripts de compilación y empaquetado están en [herramientas](herramientas). No se distribuye Geyser dentro del JAR.
