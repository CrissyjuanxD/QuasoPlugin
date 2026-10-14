# Abeja Floral en Quaso 26.2

Combate trasladado de `Bosses/QueenBeeHandler.java` de OneBlockPlugin,
commit `fe27648a398f1e8d63c078b937191cd85bef6150`.

- 600 puntos de vida, ataques cuerpo a cuerpo y combos de teletransporte.
- Aguijones venenosos y explosivos, refuerzos florales y nube tóxica.
- Polen regenerador desde 200 HP: cuatro panales giratorios que se rompen
  de un golpe o flechazo. Cada panal vivo cura 2 HP cada tres segundos.
- Avisos de especiales y regeneración por chat, con colores pastel.
- Bossbar nativa rosa para el nombre y blanca para la vida, como la Reina.

Se mantienen el altar `queen_bee`, el spawn `queenbee`, `/bosstp` y la clase
`QueenBeeHandler`. El ID de misiones y recompensas sigue siendo `abeja_reina`;
las cuentas de muertes, el límite diario y los objetivos guardados no se reinician.
Las Reinas guardadas adoptan el nombre y los 600 HP al cargar, conservando su
porcentaje de vida.

## Recompensas conservadas

| Victoria cobrada | DinoCoins | Manzanas encantadas | Miel de velocidad |
| --- | --- | --- | --- |
| Primera | 15 | 5 | 16 |
| Segunda | 10 | 3 | 8 |
| Siguientes | 5 | 0 | 3 |

El límite sigue siendo dos recompensas diarias; la primera llega en el mismo
bundle de antes. La muerte conserva los 3500 puntos de experiencia. No se
importan la Estatua Protectora ni la cinemática de derrota de OneBlock.
Los refuerzos no cuentan como jefes para las misiones ni para cobrar recompensas.

El combate, el chat y las barras no necesitan ViciontMedia ni ViciontGUI.
El modelo visual por nombre y la música `minecraft:custom.abeja_floral_music`
usan los recursos de OneBlock; el repositorio no contiene ese resource pack.

## Validación

Las pruebas de `QueenBeeHandlerTest` cubren los golpes cancelados, la inmunidad
a explosiones, el daño de flechas, la conversión de vida y la distinción entre
el jefe y sus refuerzos. El servidor interno usa Paper 26.2 y Java 25 para
comprobar entidades, barras, paquetes de chat, panales y recompensas. Pasaron
130 pruebas automáticas y el combate interno completo, incluida la entrega de
15 DinoCoins, cinco manzanas encantadas, 16 mieles y 3500 XP en la primera victoria.
La apariencia con el resource pack debe revisarse dentro del cliente.

## Refuerzos florales y equipo floral (Version #66)

El ataque de refuerzos ya no crea abejas vanilla: salen de 6 a 10 mobs florales,
40% Mini Abejas Florales y 20% cada uno de Zombie Floral, Araña Floral y Creeper
Floral. Llevan la marca `invocado_abeja_floral`, no sueltan drops florales y se
van a los 30 segundos. Los que caminan bajan al suelo de la arena.

La Corrupted Bee ahora es la **Mini Abeja Floral**: escala 0,8, nombre rosa,
partículas florales y 30% de soltar el **Aguijón Floral** (`aguijon_floral`).
El id del mob sigue siendo `corrupted_bee`, así que la misión 14 y los spawners
siguen igual.

Cada recompensa cobrada de la Abeja Floral (respeta el tope de dos por día)
también da **1 Esencia Floral** (`esencia_floral`).

| Receta | Dónde | Ingredientes |
| --- | --- | --- |
| Mejora Floral | Mesa de crafteo | 16 Carne Podrida Floral, 16 Ojo de Araña Floral, 16 Pólvora Floral, 16 Aguijón Floral, 1 Esencia Floral y 1 plantilla de mejora de Netherite |
| Pieza floral | Herrería | Mejora Floral + pieza de Netherite (o escudo) + Bloque de Miel |

Desde la Version #74 se gastan 16 de cada drop (con menos de 16 en alguna
casilla no sale nada) y la Mejora Floral **no se puede copiar**: hace falta
una por pieza, así que el equipo completo (11 piezas) son 11 Esencias, 11
plantillas de Netherite y 176 de cada drop. La herrería no acepta items custom
como base (Warden, Celestita ni otra pieza floral). Las recetas están en
`Dificultades/FloralForja` y se registran con la etapa uno.

| Pieza | Floral | Netherite |
| --- | --- | --- |
| Espada | 9 de daño | 8 |
| Hacha | 8 de daño, 1,2 ataques/s, pica 20% más rápido | 10 y 1,0 |
| Lanza | 5,5 de daño | 5 |
| Pico | 1,3 ataques/s, pica 20% más rápido; rompe los minerales de la Warden Cave | 1,2 |
| Pala | 1,1 ataques/s, pica 20% más rápido | 1,0 |
| Azada | Pica 20% más rápido | Igual |
| Escudo | Irrompible; bloquea como un escudo normal | Se gasta |
| Armadura | 3/8/6/3 de armadura, 3,5 de dureza por pieza, +1 corazón por pieza y 15% de no gastarse | 3/8/6/3, 3 de dureza |

La armadura de Warden sigue siendo mejor (24 de armadura, 4 de dureza, +2
corazones por pieza y 30%). La línea es **Netherite » Floral » Warden o
Celestita**: la herrería solo hace Celestita con una herramienta floral y
armadura de Warden con una pieza floral (ya no desde Netherite).

### Lore del equipo

Floral, Warden y Celestita usan el mismo lore (`items/EquipoLore`): la
historia de la pieza, los atributos con íconos (⚔ daño, ⚡ golpes por
segundo, ◆ armadura, ❤ corazones, ✚ no gastarse, ⚓ empuje, ✦ especial) en
vez del tooltip del juego y la línea de progresión con la pieza actual
resaltada. Las piezas que ya existían conservan su lore viejo.
