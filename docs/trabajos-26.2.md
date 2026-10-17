# Trabajos: recompensas y encantamientos (26.2)

## DinoCoins por nivel

Las DinoCoins de cada nivel crecen igual que la XP que pide: 1 DinoCoin cada 180 XP
(mínimo 2). El bonus de cada 5 niveles es 10 + la XP del nivel / 40.

| Nivel | DinoCoins | Bonus |
| --- | --- | --- |
| 1 | 2 | - |
| 10 | 2 | 18 |
| 25 | 5 | 31 |
| 50 | 10 | 56 |
| 75 | 16 | 82 |
| 100 | 22 | 110 |

Un trabajo completo (1 a 100) paga unas 2.250 DinoCoins (antes unas 1.230).

## Encantamientos de trabajo

Cada trabajo tiene un encantamiento del datapack (`data/quaso/enchantment`). El
libro I se gana en el nivel 10, el II en el 40 y el III en el 70. Minería tiene
además Veta: el libro I en el nivel 25 y el II en el 55. No salen en la mesa de
encantamientos ni con aldeanos; se ponen en el yunque.
`/giveqp libro_<id>_<nivel>` los da para probar.

| Trabajo | Encantamiento | Va en | Qué hace |
| --- | --- | --- | --- |
| Leñador | Talador (`talador`) | Hacha | Al talar un tronco cae lo de arriba: 24, 64 o 160 troncos. Agachado tala uno. No tumba troncos puestos por jugadores y en el Overworld y el Nether pide hojas naturales |
| Minería | Fervor Minero (`fervor_minero`) | Pico | +5% de velocidad de minado por nivel (antes 10%) |
| Minería | Veta (`veta`), 2 niveles | Pico | Al romper un mineral se rompe la veta pegada (también en diagonal y mezclando deepslate): 12 bloques con el I y 32 con el II. Agachado rompe uno. No toca minerales puestos por jugadores. No va en La Excavadora (el pico 3x3): el yunque no deja ponérselo y si lo tiene no hace nada |
| Pescador | Anzuelo Veloz (`anzuelo_veloz`) | Caña | 2 segundos menos de espera por nivel; se suma a Atracción |
| Granjero | Cosecha (`cosecha`) | Azada | Replanta con una semilla de lo cosechado. II: 3x3. III: 5x5 |
| Constructor | Alcance (`alcance`) | Pantalón | +1 bloque de alcance por nivel |
| Guerrero | Vigor de Batalla (`vigor_de_batalla`) | Armas | Cura 1 corazón por nivel al matar un monstruo |

Talador, Cosecha, Vigor de Batalla y Veta están en `Encantamientos/`. Los otros
tres son solo efectos del datapack. Cada bloque que rompen Talador y Veta cuenta
como roto por el jugador: gasta la herramienta, da la XP del trabajo y respeta
las protecciones.

## Más fuentes de XP

| Trabajo | Nuevo |
| --- | --- |
| Minería | Minerales Profundos de la Warden Cave: 25 XP. Racimos de amatista del End: 5 XP |
| Leñador | Troncos, madera y troncos pelados de la Warden Cave y del End: 1,8 XP |
| Constructor | 50% más en la Warden Cave y el End |
| Granjero | Froglights de los árboles de la Warden Cave: 2 XP. Bayas del Pantano Profundo: 1,5 XP |
| Guerrero | Mobs del plugin: 10 a 30 XP. Wardens: 40 XP |

Los jefes dan XP de menos a más según su dificultad: Wither 400, Abeja Floral 600,
Ender Dragon 900, Infested Warden 1.200, Ultra Warden 1.600 y Rey Ender 2.200. Los
Guerreros se la llevan entera y los demás trabajos la mitad. Las DinoCoins que
pagan los jefes no cambian.

## XP por nivel igual en todos (Version #115)

Antes (Version #78) cada trabajo pedía menos XP según lo rápido que se ganaba, pero Minería llegaba al nivel 29 en un
día. Ahora los 6 trabajos piden la misma XP por nivel que Guerrero (nivel 10: 308 XP, nivel 50: 1.823 XP). Las
DinoCoins, el bonus y la experiencia de cada nivel siguen siendo iguales en todos.

El que ya tiene más nivel no lo pierde; la XP que tenga guardada sigue contando para el próximo nivel, que ahora pide más.
