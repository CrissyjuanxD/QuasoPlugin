# Frutas en los cuatro biomas de Warden Cave · Java

Antes, cada fruta tenía una regla CTM limitada a un solo bioma. Por eso volvía a verse vanilla si aparecía en otro bioma de Warden Cave. Ahora las tres frutas comparten el mismo filtro que los minerales:

```properties
biomes=quaso:caverna_sculk quaso:pantano_profundo quaso:abismo_flotante quaso:ruinas_de_ceniza
```

La Baya Sculk, la Fruta Abisal y las enredaderas con Baya Regenerativa conservan sus texturas en los cuatro biomas. **Fuera de esos biomas, los bloques se ven vanilla.** Los minerales conservan sus reglas anteriores.

## Instalación

1. Descarga **Correccion-frutas-Java.zip** y copia su carpeta `assets` dentro de tu texture pack.
2. Reemplaza los tres `.properties`. El ZIP conserva tus PNG: no incluye ni sustituye imágenes.
3. Recarga con **F3+T**. Si distribuyes el pack desde el servidor, vuelve a empaquetarlo, súbelo y actualiza su SHA-1.

No requiere cambiar el JAR del plugin. Necesitas el mismo soporte CTM de antes (Continuity/OptiFine compatible con tu cliente). El lote completo `descargas/warden-cave-biomas/warden-cave-biomas.zip` también está corregido.

CTM filtra por bioma, no por nombre de mundo. Si otro mundo usa estos mismos cuatro biomas custom, también recibirá las texturas. Los mundos con biomas vanilla no se modifican.

El antiguo `frutas-3d-opcional.zip` cambia los modelos globalmente. Para conservar el aspecto vanilla fuera de Warden Cave, no lo instales; si ya lo aplicaste, retira sus sustituciones de blockstates y restaura los archivos anteriores de tu pack. Este parche solo corrige las reglas CTM.

## Bedrock / Geyser

Este parche es para Java. No se incluye un reemplazo global de texturas Bedrock, porque también modificaría los bloques fuera de Warden Cave.

Geyser admite bloques personalizados, pero sus mappings directos no filtran por bioma ni dimensión. Para aplicar las texturas de frutas y minerales solo en Warden Cave hace falta una integración del servidor que seleccione su representación según la ubicación. Esa integración no está incluida en el port Bedrock 1.0.0; los items de fruta y minerales sí están incluidos.

Se verificaron los filtros de las tres frutas, la conservación de las cuatro reglas de minerales y la integridad de los ZIP. No se ejecutó un cliente de Minecraft para comprobar su renderizado.
