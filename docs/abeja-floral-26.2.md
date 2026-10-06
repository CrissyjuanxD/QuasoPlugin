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
