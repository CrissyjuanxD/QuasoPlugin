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

Todos van como el de misiones: `㈁` (un espacio de −8 en la fuente: `"type": "space"`, `"\u3201": -8`) y después el carácter de la textura en blanco.

| Menú | Filas | Carácter nuevo |
|---|---|---|
| Misiones | 6 | `㈂` (ya estaba) |
| /menu | 6 | `㈃` (U+3203) |
| Trabajos | 3 | `㈄` (U+3204) |
| Habilidades | 6 | `㈅` (U+3205), las dos páginas usan el mismo |
| Tiendas | 6 | `㈈` (U+3208), con el mismo split `㈁`; se reconocen por su holder, no por el título |

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

### Costos (Version #110)

Con la economía nueva se juntan más de 100 DinoCoins en los primeros días, así que las habilidades se encarecieron
para que el árbol tome su tiempo. La XP y los bloques de cada nivel no cambian; las DinoCoins van ~4 veces más caras.

| Nivel | XP | Bloques | DinoCoins antes | DinoCoins ahora |
|---|---|---|---|---|
| 1 | 30 | 12 de Oro | 5 | 20 |
| 2 | 40 | 15 de Diamante | 10 | 35 |
| 3 | 50 | 32 de Esmeralda | 15 | 50 |
| 4 | 60 | 3 de Netherite | 20 | 70 |
| 5 | 70 | 30 de Oro | 10 | 60 |
| 6 | 80 | 40 de Diamante | 20 | 85 |
| 7 | 90 | 64 de Esmeralda | 30 | 110 |
| 8 | 100 | 6 de Netherite | 40 | 140 |

Una habilidad completa pasa de 150 a 570 DinoCoins y las tres de 450 a 1710. El Libro de Habilidades en la Biblioteca
pasa de 1 DinoCoin a 30 más un libro normal (sube un 15% por sección como el resto de la tienda).

Las DinoCoins de un nivel salen primero del inventario y lo que falte de los monederos del jugador (los que lleva y
los registrados a su nombre), igual que al entrar a un trabajo. Si a mitad de la compra ya no le alcanza algo, se le
devuelven al monedero las DinoCoins que se sacaron.

`/proteccion` ya no está en este plugin (era el libro con la guía), así lo usa tu otro plugin.
