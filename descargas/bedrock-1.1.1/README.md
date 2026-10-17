# Quaso Bedrock 1.1.1 · Corrección al entrar

Actualización **sólo de la extensión** para Quaso Bedrock 1.1.0. Conserva `QuasoTextures-1.1.0.mcpack`, los mappings de items y la configuración existente. No necesitas recompilar QuasoPlugin.

[Descargar extensión](extensions/QuasoWardenCave-1.1.1.jar) · [ZIP de actualización](Quaso-Bedrock-1.1.1-actualizar.zip)

## Instalar sobre 1.1.0

1. Apaga completamente el servidor.
2. En `plugins/Geyser-Spigot/extensions/`, retira `QuasoWardenCave-1.1.0.jar` y cualquier otra versión anterior de **esta extensión**. Conserva la carpeta `quasowardencave` con su configuración.
3. Copia `QuasoWardenCave-1.1.1.jar` en esa misma carpeta. También puedes extraer el ZIP y copiar su carpeta `plugins` después de retirar el JAR antiguo.
4. Arranca el servidor. Deben aparecer:

   ```text
   Presentación 1.1.1: rangos, 7 menús, 15 mobs y transición BloodMoon activos.
   4 biomas de Warden Cave registrados en el rango Bedrock 30000–32767.
   Warden Cave activa ... 49 estados ...
   ```

5. Vuelve a entrar desde Bedrock. **El pack no cambió**, así que no tiene que descargarse de nuevo. No uses `/reload` para aplicar esta corrección.

Si todavía usas los tres packs 1.0.0, sigue primero la [instalación del pack unificado 1.1.0](../bedrock-1.1.0/README.md), pero instala directamente la extensión **1.1.1** de esta carpeta.

## Error corregido

La extensión 1.1.0 añadía los cuatro biomas con IDs desde **1024**, fuera del rango utilizado por Bedrock para biomas personalizados. La corrección los asigna desde **30000**, evita los IDs ocupados por otras extensiones y respeta el límite del entero de 16 bits del protocolo. También valida las dos tablas antes de publicarlas para no anunciar un ID sin su definición.

Esto encaja con la desconexión `InitialConnection-90`, palabra clave `Block`, inmediatamente después de descargar el pack y conectar al servidor Java. El código 90 corresponde a `BAD_PACKET`; no identifica por sí solo qué paquete rechazó el cliente. El aviso de Paper sobre `PlayerLoginEvent` no demuestra ser la causa.

Referencias del diagnóstico: [caso equivalente Geyser #6673](https://github.com/GeyserMC/Geyser/issues/6673) y [propuesta de soporte de biomas #6658](https://github.com/GeyserMC/Geyser/pull/6658), cuyo registro utiliza IDs desde 30000. Esa propuesta aún no está integrada en Geyser estable; esta actualización no requiere instalar una compilación experimental de Geyser.

## Comprobaciones

- Compilación contra Geyser-Spigot b1256; pruebas ejecutadas también contra b1262.
- El paquete completo de **92 definiciones** se codifica y decodifica con los codecs reales **1001, 2168 y 2193**. Este último corresponde a Bedrock 26.52. Los cuatro IDs llegan como 30000–30003; se conservan las definiciones vanilla.
- Pruebas de colisiones, registro repetido, límite de IDs, límite de cadenas y errores sin modificaciones parciales.
- Se mantienen las comprobaciones de rangos, menús, mobs, cambio de mundo y transición BloodMoon.
- Arranque verificado en **Paper 26.2 build 129, Java 25 y Geyser-Spigot b1262**: 314 items registrados, los cuatro biomas y 49 estados de Warden Cave activos. Sin errores de la extensión.
- El arranque local **no sustituye una entrada desde el cliente Windows**. El entorno no tiene un cliente Bedrock gráfico; falta confirmar el ingreso real en el servidor del usuario. La prueba local no valida autenticación externa: Authlib no pudo obtener las claves públicas de Mojang en este entorno.

## Si persiste el mismo error

Para aislar el registro de biomas, con el servidor apagado añade esta línea a `plugins/Geyser-Spigot/extensions/quasowardencave/config.properties`:

```properties
custom-biomes=false
```

Arranca y vuelve a entrar. Este modo conserva los biomas que envía Geyser normalmente; se mantienen items, armaduras, frutas, minerales, menús, mobs y la transición BloodMoon. Los colores propios del cielo/agua/niebla de los cuatro biomas quedan desactivados durante esta comprobación. El valor por defecto es `true`; no hace falta añadirlo para instalar la corrección.

Si tampoco entra con `false`, el registro de esos biomas no explica todo el fallo: conserva el nuevo resultado de conexión y el registro de contenido del cliente para revisar el siguiente paquete o recurso. No borres mundos ni datos de jugadores.

## Compilar y probar

Requiere Python 3, JDK 25 para leer la API Paper 26.2 (el JAR de la extensión se genera para Java 21), Geyser-Spigot b1256 o b1262, las librerías de Paper y Mockito 5.20.0 con Byte Buddy/agent 1.17.7 y Objenesis 3.3. Usa las [dependencias del entorno 1.1.0](../bedrock-1.1.0/herramientas/README.md).

Desde esta carpeta, con `JAVA_HOME` apuntando al JDK:

```bash
python herramientas/compilar.py /ruta/Geyser-Spigot.jar /ruta/paper/libraries
python herramientas/probar.py /ruta/Geyser-Spigot.jar /ruta/paper/libraries /ruta/dependencias-pruebas
python herramientas/empaquetar.py
```

El ZIP contiene únicamente la extensión y esta guía. No contiene el pack, configuración del servidor ni datos privados.
