# Quaso Bedrock 1.1.0

> **Corrección al entrar:** utiliza [QuasoWardenCave 1.1.1](../bedrock-1.1.1/README.md) en lugar del JAR 1.1.0. El pack de esta carpeta se conserva. Corrige los IDs del registro de biomas relacionados con `InitialConnection-90 / Block`.

Actualización del port para **Minecraft Bedrock Windows 26.52, IU clásica**, Paper **26.2** y Geyser-Spigot **2.12.0**. Integra los tres packs anteriores en uno. Conserva las texturas y mappings de items, armaduras, frutas y minerales que ya funcionaban.

[Descargar instalador](Quaso-Bedrock-1.1.0-instalar.zip) · [Pack](packs/QuasoTextures-1.1.0.mcpack) · [Extensión](extensions/QuasoWardenCave-1.1.0.jar)

## Actualizar desde 1.0.0 / 1.0.1

1. **Apaga el servidor.** Guarda una copia de `plugins/Geyser-Spigot`.
2. De `plugins/Geyser-Spigot/packs/`, retira únicamente estos archivos antiguos, si existen:
   - `QuasoTextures-1.0.0.mcpack`
   - `QuasoMobs-1.0.0.mcpack`
   - `QuasoWardenCave-1.0.0.mcpack`
3. De `plugins/Geyser-Spigot/extensions/`, retira `QuasoWardenCave-1.0.0.jar` o `QuasoWardenCave-1.0.1.jar`. **Conserva** la carpeta `extensions/quasowardencave` con su configuración.
4. Extrae el instalador y copia su carpeta `plugins` sobre la del servidor. El resultado será:

   ```text
   plugins/Geyser-Spigot/
   ├── packs/QuasoTextures-1.1.0.mcpack
   ├── custom_mappings/quaso_items.json
   └── extensions/QuasoWardenCave-1.1.0.jar
   ```

5. En el apartado `gameplay` de la configuración actual de Geyser conserva:

   ```yaml
   gameplay:
     enable-custom-content: true
     force-resource-packs: true
   ```

   No reemplaces todo el archivo: conserva tu puerto, autenticación, Floodgate y demás ajustes.
6. Arranca el servidor. Deben aparecer `Presentación 1.1.0` y `Warden Cave activa ... 49 estados`. No necesitas recompilar ni sustituir QuasoPlugin para esta actualización.
7. Cierra Minecraft Bedrock y vuelve a entrar aceptando el pack actualizado. Si conserva el aspecto anterior, elimina **sólo la copia de Quaso** en Ajustes → Almacenamiento → Datos en caché, y reconecta.

No mantengas copias antiguas de estos packs en otras carpetas de packs: sus definiciones de entidades y UI pueden pisarse. Los packs de otros plugins se conservan.

## Qué cambia

| Parte | Adaptación |
|---|---|
| Rangos | La extensión convierte los 16 caracteres Java en secuencias de glifos anchos de Bedrock. Conserva el dibujo y restaura el color del texto posterior. No hay que editar prefijos ni permisos. |
| Scoreboard | Amplía el límite de anchura de 100 a 280 píxeles, oculta los números del sidebar Quaso y muestra el logo como imagen. Conserva los números internamente para ordenar las filas. |
| Menús | Traduce los siete títulos del plugin a selectores de interfaz: misiones, extras, trabajo, menú, habilidades, trabajos y tiendas. Los fondos usan bindings JSON UI correctos y se conservan los botones nativos. |
| Mobs | La extensión reconoce nombre y tipo después del spawn y envía una variante de modelo. Ya no depende de una expresión Molang enorme con cada combinación de colores. Conserva la abeja floral 2 y la mini abeja 3. |
| Warden Cave | Registra cuatro biomas Bedrock con sus propios colores de cielo, agua y niebla. No cambia los biomas vanilla. Los bloques siguen limitados al mundo configurado. |
| BloodMoon | Añade horizonte/niebla rojos por jugador con entrada y salida de 10 segundos. Se retira al acabar, cambiar de mundo o desconectar. La tormenta y la hora siguen siendo responsabilidad de QuasoPlugin. |

El cielo de BloodMoon de Java usa atributos visuales de su datapack que Geyser no transmite. Esta versión **aproxima la ambientación con niebla y horizonte rojos**; no reproduce exactamente el cielo completo ni las nubes de Java. El resultado cambia también con el modo gráfico y la distancia de renderizado de Bedrock. Los fondos de inventario están adaptados al perfil **Clásico**; Pocket conserva su interfaz nativa.

## Logo al entrar y descargar

El logo se aplica a las pantallas de carga **después de activar el pack**. Un pack del servidor no puede modificar por sí solo la pantalla de su **primera descarga**, cuando todavía no está disponible en el cliente.

Si quieres verlo desde esa primera descarga en tu Windows, abre [QuasoLogo-global-1.1.0.mcpack](opcional_cliente/QuasoLogo-global-1.1.0.mcpack) y actívalo en **Ajustes → Recursos globales → Mis packs** antes de entrar. Es opcional y se instala en el cliente, no en Geyser. Al ser global también cambia el logo de otras pantallas/servidores mientras esté activo.

## Compatibilidad y comprobaciones

- Pack mínimo: **Bedrock 26.50**, sin límite máximo artificial. La versión probada por el usuario es 26.52.
- Arranque comprobado con **Geyser-Spigot b1256 y b1262**, en Paper 26.2 build 129 y Java 25. Los nuevos parches de Geyser pueden admitir clientes posteriores sin regenerar todo el arte.
- 57 comprobaciones de paquetes y canal de salida: rangos, títulos, aparición/renombrado de mobs, abeja 2/3 y restauración de metadatos vanilla. También se comprueban entrada/salida de Warden Cave y aislamiento entre dos sesiones.
- La UI usa cambios parciales sobre los controles nativos, no copias completas de las pantallas de una versión. La selección de modelos usa un metadato estable. Los formatos de recursos se conservan en versiones reconocidas.
- La extensión aún necesita algunas clases internas de Geyser para los bloques por mundo y la presentación. No se puede garantizar compatibilidad con todas las actualizaciones futuras. Si Geyser cambia esas clases, habrá que actualizar esa parte. Los fallos de adaptación de paquetes se registran conservando el paquete original.
- **Pendiente de verificación visual real en Windows 26.52:** renderizado, alineación y aspecto final. Las pruebas automáticas y el arranque no sustituyen esa comprobación. En este entorno no hay cliente gráfico Bedrock. La prueba local se hizo sin autenticación externa; su advertencia de conexión a las claves públicas de Mojang es ajena al pack.

Para revisar dentro del juego: abre misiones y trabajos, comprueba un rango en chat y sobre un jugador, mira la abeja floral y mini abeja, entra/sal de Warden Cave y activa/finaliza una BloodMoon. Comprueba también un cofre y un mob normales. Si ves un fallo, el **registro de contenido de Bedrock** en Ajustes → Creador ayuda a distinguir un selector que no se activa de una geometría o interfaz inválida.

## Desarrollo

El código de la extensión está en `extension/`. `herramientas/preparar.py` reconstruye los recursos usando los packs 1.0.0/1.0.1 conservados en el repositorio. Las fuentes y texturas aprobadas se mantienen; no se requiere el ZIP original del usuario.

Consulta [herramientas/README.md](herramientas/README.md) para compilar, probar y empaquetar. El instalador no incluye Geyser, ViaVersion, contraseñas ni configuración privada. Las plantillas originales conservan la [licencia de Mojang](../bedrock-1.0.0/LICENCIA-BEDROCK-SAMPLES.md).
