# Texturas de items (26.2)

Las texturas están en `resourcepack/` con la misma estructura que el resource pack, así que se copia `assets` encima
del pack y listo. Cada item lleva su textura (`textures/item/custom/<id>.png`), su definición (`items/<id>.json`) y su modelo
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

## Lote 2 (Version #41): lo que se ve del día 1 al 19

| Item | ID | Estilo |
|---|---|---|
| Imán de Botín | `iman_botin` | Imán de herradura rojo con puntas de metal y chispas |
| Red Atrapa-Animales | `red_animales` | Bolsa de red de soga con el cordón arriba |
| Abono Concentrado | `abono_concentrado` | Polvo de hueso verde |
| Brújula del Explorador | `brujula_explorador` | Brújula de bronce con la cara azul (no gira, es fija) |
| Ración de Viaje | `racion_viaje` | Pan atado con dos cordeles |
| Bomba de Humo | `bomba_humo` | Bomba gris con la mecha prendida y humo |
| Incienso Ahuyentador | `incienso_ahuyentador` | Cuenco de barro con dos varitas encendidas y humo |
| Elixir del Minero / Ígneo | `elixir_minero` / `elixir_igneo` | Frasco vanilla con el líquido azul con chispas doradas / naranja con una llamita |
| Galleta de la Fortuna | `galleta_fortuna` | Galleta doblada con el papelito asomando |
| Chatarra | `chatarra` | Engranaje oxidado, un clavo y una chapa doblada |
| Manzana Podrida | `manzana_podrida` | Manzana vanilla marrón con manchas y moho |
| Zanahoria Encantada | `zanahoria_encantada` | Zanahoria azul con brillos |
| Fósiles Pequeños | `fosiles_pequenos` | Amonita en un trozo de piedra |
| Monedero | `monedero` | Monedero rosa con el broche dorado y una moneda |

## Lote 3 (Version #42 y #43): trabajos y habilidades

Los papeles de los trabajos son la herramienta de cada uno, cada una con su propio material y un detalle abajo:

| Trabajo | ID | Herramienta |
|---|---|---|
| Guerrero | `trabajo_guerrero` | Espada de rubí con guarda dorada y mango de cuero |
| Minería | `trabajo_mineria` | Pico de cobalto con remache dorado y una gema rosa |
| Leñador | `trabajo_lenador` | Hacha de cobre con mango de abedul y una hoja |
| Constructor | `trabajo_constructor` | Martillo de latón con mango rojo y un ladrillo |
| Granjero | `trabajo_granjero` | Azada de esmeralda con una espiga de trigo |
| Pescador | `trabajo_pescador` | Caña aguamarina con anzuelo dorado y un pez naranja |

El árbol de habilidades usa `minecraft:habilidad_<rama>_<nivel>_<on|off>` (48 texturas): corazón para Vitalidad,
dos chevrones para arriba con estela (como el efecto de Salto Alto) para Agilidad y escudo de acero con borde dorado y chevrón
morado (como el efecto de Resistencia) para Resistencia. Cada una lleva abajo a la derecha una placa con el nivel
escrito (N1 a N8); del N5 al N8 la placa tiene borde dorado. `on` es a color (ya lo tienes) y `off` en gris (todavía
no). Ya no tienen el brillo de encantamiento.

## Mochilas (opción)

Están en `resourcepack/opciones/mochilas/` (`mochila_nivel_1` a `mochila_nivel_5`), fuera de `assets`, porque las
mochilas usan los modelos vanilla de los bundles (`minecraft:lime_bundle`, `blue_bundle`, `orange_bundle`,
`red_bundle` y `purple_bundle`). Si se usan con esos nombres también cambian los bundles normales de esos colores;
para que no pase, el plugin tendría que pasar a usar `minecraft:mochila_nivel_<n>`. Cada nivel suma algo: el 3 tiene
ribete dorado, el 4 una gema roja y el 5 hebilla de diamante, gema morada y brillitos.

## Pendientes

Los minerales y materiales van al final: `cristal_celestita`, `lingote_celestita`, `fragmento_astral`,
`esencia_marchita`, `plantilla_celestita`, `pepitas_hierro_oxidadas`, `pepitas_diamante`, `fragmentos_ambar` y
`lingote_platino`. Faltan también los items de la tienda de los días 20 y 30 que no están arriba.
