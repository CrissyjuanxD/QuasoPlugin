# Quaso para Bedrock · 1.1.2

Actualización completa para Geyser **como plugin de Paper**. Incluye los items, armaduras, mobs, interfaces y Warden Cave en un solo pack. Sustituye tanto el pack 1.1.0 como la extensión 1.1.1.

## Instalar

1. Apaga el servidor.
2. En `plugins/Geyser-Spigot/packs/`, retira las versiones anteriores de **QuasoTextures**, **QuasoMobs** y **QuasoWardenCave**. Conserva los packs de otros proyectos.
3. En `plugins/Geyser-Spigot/extensions/`, retira los JAR antiguos de **QuasoWardenCave**. Conserva su carpeta de configuración.
4. Descomprime `Quaso-Bedrock-1.1.2-instalar.zip` y copia su carpeta `plugins` sobre la del servidor. Las rutas finales son:

   ```text
   plugins/Geyser-Spigot/packs/QuasoTextures-1.1.2.mcpack
   plugins/Geyser-Spigot/extensions/QuasoWardenCave-1.1.2.jar
   plugins/Geyser-Spigot/custom_mappings/quaso_items.json
   ```

5. Conserva `enable-custom-content: true` y la configuración de Geyser que ya permite conectarse. No cambies IP, puerto ni Floodgate.
6. Arranca el servidor. Debe indicar `Presentación 1.1.2` y mantener los biomas Quaso en **30000–30003**. No uses `/reload`.
7. Cierra Minecraft Bedrock completamente y vuelve a entrar para descargar la versión nueva. Si sigue mostrando el pack antiguo, elimina **sólo el pack Quaso en caché** desde Ajustes → Almacenamiento → Datos en caché y reconecta.

No necesitas recompilar ni reemplazar QuasoPlugin para esta corrección. La extensión consulta su sistema BloodMoon existente.

### Logo antes de descargar

`opcional_cliente/QuasoLogo-global-1.1.2.mcpack` se importa en Minecraft y se activa en **Recursos globales**, si deseas ver el logo antes de la descarga del servidor. Sustituye la versión global antigua si la instalaste. No se coloca en `plugins/Geyser-Spigot/packs/`.

Un pack del servidor aún sin descargar no puede dibujar su propia pantalla inicial de descarga. El logo global también cambia el título en otros menús del cliente.

## Qué cambia

| Parte | Corrección |
| --- | --- |
| Mobs | Se elimina la mezcla de esquemas 1.8 y modernos que el registro de contenido rechazaba. Se migran los controllers antiguos a `animations` y `scripts.animate`. Las 15 variantes se seleccionan con la propiedad registrada `quaso:model`, leyendo la entidad Paper aunque su nombre esté oculto. |
| Abejas | Su estado nativo `MARK_VARIANT` queda libre para el aguijón. La mini abeja sigue usando el modelo 3 y la floral el 2. |
| Interfaces | Cada fondo requiere su título exacto; se elimina el enlace al `#text` inexistente. Sólo las casillas de los menús Quaso pierden el fondo gris. Se conservan botones, inventario, hover y transacciones nativas. Trabajos utiliza 27 casillas; los otros fondos, 54. |
| Rangos | Se conserva la altura original de 9px y se separan los fragmentos entre las letras, sin ampliación ni cortes en sus trazos. Bedrock añade espacio entre glifos: el borde del distintivo no es idéntico al de Java. |
| Hojas | Los cuatro biomas registran su color de follaje: cyan, verde, gris y morado. Afecta las hojas que usan tinte de bioma; las hojas con color fijo propio conservan su textura. No recolorea los biomas vanilla. |
| BloodMoon | Niebla/horizonte rojo en aire y tormenta, con entrada y salida de 10 segundos y distancia relativa al renderizado. Se añade búsqueda del jugador por nombre Java como respaldo para Floodgate. Se retira al salir del mundo y al acabar el evento. |
| Logo y peto alado | Se corrige el tipo ausente del control `title_image` y el esquema del attachable que rechazaba `scripts.animate`. |

El cielo completo de BloodMoon en Java usa un reloj y atributos visuales que Geyser no traduce. En Bedrock esta versión ofrece **niebla y horizonte rojo**, no una reproducción exacta del cielo, luna y nubes de Java. La intensidad final depende del renderizador del cliente.

## Compatibilidad y comprobación

Destinado a Bedrock Windows 26.52, perfil de IU **Clásico**, con Geyser-Spigot 2.12.0 b1256/b1262. Se usan formatos de recursos estables y la API de propiedades de Geyser para los mobs. Los bloques por dimensión y la adaptación de paquetes aún necesitan integración interna: no se garantiza que cualquier actualización futura de Geyser o de JsonUI sea compatible sin revisión.

Se comprueban los archivos de recursos, referencias, selectores exclusivos de GUI, conservación de items/armaduras, variantes con nombres ocultos, restauración vanilla y transición de niebla. Las pruebas automatizadas pasan con Geyser b1256 y b1262. Se verifican los paquetes de biomas de los protocolos 1001, 2168 y 2193 y el arranque local con Paper 26.2 build 129/Geyser b1262 (314 items registrados, cuatro biomas y la extensión habilitada). La consulta de claves públicas de Mojang falló por la conectividad del entorno; no se probó una sesión Xbox autenticada. **No hay un cliente gráfico de Bedrock en el entorno de compilación:** estas pruebas no sustituyen la comprobación visual en Windows.

Para probar: abre un cofre normal y luego cada menú Quaso; observa un Zombie Floral, una abeja y un mob BloodMoon; entra y sal de Warden Cave y activa/termina una BloodMoon. Si un modelo sigue sin aparecer, el registro de contenido debe revisarse de nuevo: no deberían repetirse los errores `animate not valid here`, `animation_controllers not valid here` de estos ocho archivos ni `Type not specified` del logo.

El error de `panda.entity.json` no corresponde a un archivo incluido en este pack. Las advertencias de comandos Java (`displayName`, `friendlyFire`, etc.) pertenecen a la traducción de comandos de Geyser. El aviso de registro VoxelShape requiere comprobar si persiste con esta versión y Geyser actualizado; este registro no basta para atribuirle una causa.

## Reconstruir

Desde la raíz del repositorio, con Python/Pillow y `JAVA_HOME` apuntando a JDK 25 o posterior:

```bash
python descargas/bedrock-1.1.2/herramientas/preparar.py
python descargas/bedrock-1.1.2/herramientas/compilar.py /ruta/Geyser-Spigot.jar /ruta/servidor/libraries
python descargas/bedrock-1.1.2/herramientas/probar.py /ruta/Geyser-Spigot.jar /ruta/servidor/libraries /ruta/dependencias-test
python descargas/bedrock-1.1.2/herramientas/empaquetar.py
python descargas/bedrock-1.1.2/herramientas/validar.py
```

Las dependencias de prueba son Mockito 5.20.0, Byte Buddy/agent 1.17.7 y Objenesis 3.3. El script de compilación verifica las huellas de los JAR Geyser-Spigot b1256/b1262; no acepta Standalone. El instalador contiene recursos y la extensión, sin configuración privada del servidor.
