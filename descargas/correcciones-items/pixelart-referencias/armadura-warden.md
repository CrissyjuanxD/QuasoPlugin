# Armadura Warden y peto alado · texturas vanilla 2D

**Rama 26.2 · Revisión #60.** Texturas para las piezas equipadas, relacionadas con sus iconos de inventario.

[Descargar solo la armadura](armadura-warden.zip) · [Pack completo de items y armadura](items-pixelart.zip)

![Vistas planas de las caras UV e iconos de referencia](vista_armadura.png)

## Diseño

- **Casco:** base azul casi negra, frente cian y motivos de antenas Warden en ambos laterales.
- **Peto:** placas y costillas cian alrededor de un núcleo de almas; espalda con vetas sculk.
- **Pantalones:** placas azul petróleo, muslos cian, rodillas oscuras y hebilla luminosa en color.
- **Botas:** base oscura y refuerzos cian en empeine y espinilla.
- **Peto alado:** conserva el torso sculk/cian y añade bordes violetas, detalles morados en hombros y espalda, y alas oscuras con venas cian y bordes violetas.

Las antenas son dibujos sobre las caras del casco. Las texturas 2D vanilla no añaden geometría que sobresalga de la cabeza. Las alas utilizan la geometría y la animación normales de las élitras.

## Las dos capas y el alado

Todos los PNG de armadura son **64×32**, con píxeles sin suavizado y transparencia binaria. Se mantiene el mapa UV y la máscara de transparencia de los atlas vanilla de Minecraft 26.2.

| Archivo bajo `assets/minecraft/` | Uso |
| --- | --- |
| `textures/entity/equipment/humanoid/warden.png` | Capa exterior: casco, peto, brazos y botas |
| `textures/entity/equipment/humanoid_leggings/warden.png` | Capa de pantalones y cintura |
| `textures/entity/equipment/humanoid/warden_alado.png` | Torso y brazos del peto alado |
| `textures/entity/equipment/wings/warden_alado.png` | Alas del peto alado |
| `equipment/warden.json` | Une las dos capas de la armadura normal |
| `equipment/warden_alado.json` | Une el peto alado y sus alas |

`humanoid` y `humanoid_leggings` corresponden a las capas que antes se conocían como `layer_1` y `layer_2`. En 26.2 se enlazan mediante los JSON de `equipment` y el componente de equipamiento del item.

![Atlas UV sobre fondo oscuro y claro](vista_armadura_uv.png)

## Instalar y usar

1. Descomprime **armadura-warden.zip** y fusiona su carpeta **assets** con tu resource pack. El ZIP completo de items incluye los mismos archivos.
2. Conserva el `pack.mcmeta` de tu pack de 26.2. Recarga con **F3 + T**; si lo distribuyes desde el servidor, actualiza su ZIP y hash.
3. Compila y utiliza QuasoPlugin con la revisión **#60**. Esta revisión asigna `minecraft:warden` o `minecraft:warden_alado` al componente `equippable` de cada pieza.
4. Las piezas nuevas ya llevan el enlace. Las antiguas se actualizan al iniciar sesión o volver a equiparlas, conservando sus metadatos. El peto alado mantiene su capacidad de planeo.

El icono de inventario sigue usando `casco_warden`, `peto_warden`, `pantalon_warden`, `bota_warden` o `peto_warden_alado`. El modelo de equipo es un componente distinto: cambiar únicamente `item_model` no cambia la apariencia puesta.

## Verificación

Se comprobaron dimensiones, transparencia y UV contra los atlas vanilla, referencias de los JSON y contenido de ambos ZIP. El plugin se compiló con Java 25 y se comprobaron la migración de las cuatro piezas, la variante alada, la conservación de metadatos y la actualización al equipar.

Las vistas previas son montajes planos de las caras UV reales sobre un maniquí de referencia. No son capturas del juego; queda pendiente comprobar el resultado visual en un cliente de Minecraft.
