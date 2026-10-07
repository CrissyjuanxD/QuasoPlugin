# End (26.2)

Todo lo del End se prende con `/changes activar tres` (día 35). Los biomas nuevos se generan siempre (populator), pero
los mobs, las drops, las recetas y el altar del Rey Ender son de la etapa tres (`Dificultades/ThreeChanges.java`).

## Spawns

En las islas de afuera (a más de 500 bloques del centro) el juego spawnea endermans. Parte de ellos se cambian ahí
mismo por un mob del End, uno por uno, así la mobcap cuenta igual (`EndBiomes/EndSpawns.java`).

| Zona | Ender Blaze | Ender Spider | Ender Creeper | Shulker Negro | Sigue enderman |
|---|---|---|---|---|---|
| End de siempre | 9% | 9% | 7% | – | 75% |
| Bosque Prismático | 5% | 9% | 6% | – | 80% |
| Páramo Marchito | 10% | 7% | 7% | 5% | 71% |

En el Bosque Prismático los endermites (peso 8, antes 5) salen como Ender Insects. Los Shulkers Negros de los
santuarios y agujas no desaparecen; los que salen sueltos en el Páramo sí, como cualquier mob.

## Dificultad

Pensada para la armadura de Warden (24 de armadura, 16 de dureza, +8 corazones) con Protección: se siente, pero no
mata de un golpe.

| Mob | Vida | Lo que hace |
|---|---|---|
| Ender Blaze | 50, Resistencia I | Sus bolas de fuego explotan al pegar (explosión chica que no rompe bloques) |
| Ender Creeper | 40 | Cargado, invisible y con Velocidad I; explota como un creeper cargado normal |
| Ender Spider | 40, Resistencia I | Velocidad I, 6 de daño y el proyectil que te tepea 15 bloques |
| Shulker Negro | 60 | Balas al doble con Wither II y Ceguera, y cada 10 a 14 segundos una ráfaga de 6 balas a la vez |
| Shulker (End Cities) | 30 | 20% de que la bala dé un efecto malo y 10% de que explote; al morir deja una TNT (2,5 s) y 30% de soltar su caja |

Las TNT de los shulkers y las explosiones de sus balas no rompen bloques. Si el shulker murió por una explosión no deja
TNT, así no se encadenan.

## Celestita

| Item | Receta |
|---|---|
| Cristal de Celestita | 12% al romper un racimo de amatista en el End, 18% con el Pico de Celestita |
| Lingote de Celestita | 4 cristales, 4 lingotes de oro y 1 Lingote Profundo |
| Plantilla de Celestita | 4 Fragmentos Astrales, 2 Lingotes de Celestita, 2 ojos de ender y 1 plantilla de Netherite |
| Herramienta de Celestita | Herrería: plantilla, herramienta de Netherite y 1 Lingote de Celestita |
| Peto de Warden Alado | Herrería: EnderKing Pearl, Peto de Warden y unas Elytras |

Herramientas: espada, hacha, lanza, pico, pala y azada. Tienen 1 de daño más que las de Netherite y se quedan los
encantamientos. Las tres armas le pegan 50% más al Rey Ender. La herrería no mejora items custom (sí el Peto de
Warden para el alado).

## Rey Ender

- Se invoca con el Ojo del Rey Ender: clic derecho a la vara del End del centro de un Santuario Marchito (la vara sobre
  las dos obsidianas llorosas, en la plataforma de obsidiana). El ojo se gasta y a los 5 segundos sale el boss.
- 7000 de vida. El server no deja pasar de 1024, así que el mob tiene 1000 y recibe todo el daño dividido entre 7; la
  barra muestra la vida de 7000.
- Es un enderman 1,7 veces más grande. No se tepea solo, las flechas sí le pegan, no recibe daño de caída ni de sus
  invocados, y si cae al vacío vuelve al altar.
- Hace de 1 a 3 ataques cuerpo a cuerpo y después un especial. Con menos del 35% de vida ataca más seguido.

| Tipo | Ataque | Qué hace |
|---|---|---|
| Cuerpo a cuerpo | Zarpazo Abisal | Corre hacia el jugador y barre un arco de 150°: 40 de daño y Debilidad |
| Cuerpo a cuerpo | Pisotón Estelar | Salta y al caer golpea a 6 bloques: 36 de daño, los levanta y Lentitud II |
| Cuerpo a cuerpo | Combo Sombrío | Aparece detrás del jugador 3 veces seguidas: 20 de daño cada golpe |
| Especial | Lluvia Estelar | 3 círculos por jugador (4 con poca vida); 2 segundos después caen estrellas: 38 de daño |
| Especial | Ráfaga Real | 2 balas de shulker por jugador (hasta 10): 14 de daño y levitación corta |
| Especial | Rayo del Vacío | Apunta 1 segundo, se queda quieto y dispara un rayo de 32 bloques: 46 de daño y Wither II |
| Especial | Agujero Negro | Atrae 3 segundos a todos a 18 bloques y explota: 44 de daño a 6 bloques y Oscuridad |
| Especial | Ejército del End | Invoca Ender Spiders, un Ender Blaze y Ender Insects (hasta 4 vivos, no sueltan nada) |
| Especial | Grieta Dimensional | Cambia de lugar con el jugador más lejano (Náusea y Oscuridad) y golpea el suelo |
| Regeneración | Trono del Vacío | Al bajar del 66% y del 33%: flota sobre el altar y 4 cristales lo curan 12 de vida por segundo cada uno, hasta 30 s. Recibe la mitad de daño hasta que se rompan (un golpe o una flecha cada uno) |

Suelta la EnderKing Pearl y 3000 de experiencia; las DinoCoins (25 la primera vez, después 5, una vez al día) las da
`BossRewards`. Para probarlo: `/spawnqp reyender` (la arena queda donde se spawnea).

## Modelos que faltan en el resource pack

`lingote_celestita`, `plantilla_celestita`, `enderking_pearl`, `espada_celestita`, `hacha_celestita`,
`lanza_celestita`, `pico_celestita`, `pala_celestita`, `azada_celestita` y `peto_warden_alado` (todos como
`minecraft:<id>`, igual que el resto de los items custom).
