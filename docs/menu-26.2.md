# /menu (26.2)

`/menu` es para todos (sin permiso) y abre el menú principal (`Gui/MenuPrincipal.java`). El dibujo lo pone el
resource pack con el título; las zonas son items invisibles con nombre, así se puede hacer clic en cualquier parte.

| Zona | Slots | Abre |
|---|---|---|
| Misiones | 1, 2, 3, 10, 11, 12, 19, 20, 21 | El menú de misiones |
| Trabajos | 5, 6, 7, 14, 15, 16, 23, 24, 25 | El menú de trabajos |
| Habilidades | 27, 28, 29, 36, 37, 38, 45, 46, 47 | El árbol de habilidades (solo si ya gastaste el libro) |
| Protecciones | 33, 34, 35, 42, 43, 44, 51, 52, 53 | `/proteccion` (de tu otro plugin) |
| Homes | 39, 40, 41, 48, 49, 50 | `/home list` |

Los slots que sobran (0, 4, 8, 9, 13, 17, 18, 22, 26, 30, 31 y 32) llevan relleno invisible sin tooltip.

## Títulos para el resource pack

Todos van como el de misiones: `㈁㈁` (el espacio negativo de siempre) y después el carácter de la textura en blanco.

| Menú | Filas | Carácter nuevo |
|---|---|---|
| Misiones | 6 | `㈂` (ya estaba) |
| /menu | 6 | `㈃` (U+3203) |
| Trabajos | 3 | `㈄` (U+3204) |
| Habilidades | 6 | `㈅` (U+3205), las dos páginas usan el mismo |

## Items invisibles

Usan el item model `minecraft:air`, que trae el juego, así que no necesitan nada en el resource pack. El relleno además
oculta el tooltip. Están en el menú de misiones (las dos filas de arriba y los slots sin misión), en el de trabajos
(todo lo que no es un trabajo o la guía), en `/menu` y en el árbol de habilidades (los 33 slots que antes tenían tintes
morados, magentas y negros; las pepitas de hierro entre niveles y las flechas de página siguen igual).

**Papel invisible de las tiendas (Version #92):** al cerrar una tienda se devolvía al jugador lo que hubiera en la
columna del medio (los slots 4, 13, 22, 31, 40 y 49). Antes esa columna estaba vacía, pero desde que las tiendas usan
el relleno invisible cada cierre daba 6 papeles invisibles. Ya no se devuelve nada: los clics y arrastres de la tienda
están cancelados, así que ahí no se puede dejar nada. Además, al abrir una tienda se le quita el relleno a quien lo
tenga de antes (`Invisible.esRelleno`: papel con el modelo del aire y sin tooltip). Se reprodujo en el server de
prueba: con el código de antes salían 6 papeles por cierre y con el arreglo 0.

## Libro de Habilidades

Ya no abre el menú: al usarlo se gasta con una animación de partículas moradas y avisa en el chat que el árbol está en
`/menu`, en Habilidades. Queda guardado en `Habilidades.yml` (`acceso.<uuid>`). Si ya tienes acceso el libro no se
gasta. Los que ya habían comprado algún nivel con el libro de antes entran sin gastar otro.

`/proteccion` ya no está en este plugin (era el libro con la guía), así lo usa tu otro plugin.
