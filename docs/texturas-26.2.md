# Texturas de items (26.2)

Las texturas están en `resourcepack/` con la misma estructura que el resource pack, así que se copia `assets` encima
del pack y listo. Cada item lleva su textura (`textures/item/<id>.png`), su definición (`items/<id>.json`) y su modelo
(`models/item/<id>.json`). Todas son de 16x16 y planas (sin modelos 3D) para que el paso a Bedrock sea fácil.
`resourcepack/preview/` tiene una imagen con cada lote en grande.

## Lote 1 (Version #40)

| Item | ID | Estilo |
|---|---|---|
| Warden Gun | `warden_gun` | Cañón de sculk con la campana del sonic boom y las orejas del Warden |
| Peto de Warden Alado | `peto_warden_alado` | Peto en colores del Warden, el alma brillando en el pecho y alas moradas atrás |
| Bengala de Sculk | `bengala_sculk` | Tubo de sculk con bandas cian y la punta encendida |
| Granada Sónica | `granada_sonica` | Granada de sculk con anillos de onda y la anilla arriba |
| Polvo Silencioso | `polvo_silencioso` | Montón de polvo gris azulado con brillitos de sculk |
| Linterna de Almas | `linterna_almas` | Farol de sculk con el alma cian adentro |
| Cristal de Eco | `cristal_eco` | Cristal de sculk con el núcleo oscuro y destellos |
| EnderKing Pearl | `enderking_pearl` | Perla morada del End con corona dorada |
| Ojo del Rey Ender | `ojo_rey_ender` | Ojo de ender en morado y rosa |
| Espada, Hacha, Lanza, Pico, Pala y Azada de Celestita | `espada_celestita`... `azada_celestita` | La forma de las de Netherite, con la cabeza celeste de celestita y el mango morado del End |

Las herramientas de celestita usan `item/handheld` (se agarran como las de vanilla). La lanza hace como las lanzas
de vanilla: plana en el inventario y con el modelo `item/spear_in_hand` en la mano, las dos con la misma textura.
El Peto de Warden Alado puesto se sigue viendo como el peto de Netherite (la textura es solo la del item).

## Pendientes

Los minerales y materiales van al final: `cristal_celestita`, `lingote_celestita`, `fragmento_astral`,
`esencia_marchita` y `plantilla_celestita`. Faltan también los demás items de la tienda y los papeles de los trabajos.
