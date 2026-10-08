# End (26.2)

Todo lo del End se prende con `/changes activar tres` (día 35). Los biomas nuevos se generan siempre (populator), pero
los mobs, las drops, las recetas y el altar del Rey Ender son de la etapa tres (`Dificultades/ThreeChanges.java`).

## Biomas

Las islas de afuera (a más de 1100 bloques del centro) se reparten en regiones de unos 450 bloques
(`EndBiomes/EndBiomeMap.java`): 26% End de siempre, 27% Bosque Prismático, 27% Páramo Marchito y 20% Picos Helados.
Solo cambian los chunks que se generen desde ahora; lo que ya estaba generado queda como estaba (puede quedar un borde
donde se juntan).

- **Bosque Prismático**: árboles de colores como el bioma colorido de Stellarity (hechos de cero, no copiados): árbol
  grande con ramas y bolas de copa, pino en punta y árbol gigante de 2x2. La copa es de lana con vidrio del mismo color
  (el verde de azalea y el rosa de cerezo), así casi no hay hojas que tiren partículas. Cada mancha del bosque tiene más
  árboles de su color, pero salen de todos. Los rojos tienen vides colgando, los blancos varas del End y los grandes
  faroles con cadena. En el suelo: pasto, flores, pétalos, raíces, brotes de amatista (no dan Celestita), rocas de
  diorita y las geodas de siempre. Las partículas de luciérnagas bajaron a un quinto y los arbustos de luciérnagas casi
  no salen (daban 15 FPS).
- **Páramo Marchito**: igual que antes. Los Wither Skeletons del datapack casi no salían: ahora el 12% de los endermans
  que spawnean ahí sale como Wither Skeleton (etapa tres).
- **Picos Helados** (decorativo): nieve con líneas de hielo y manchas peladas de end stone, dunas, picos de hielo
  compacto con vetas de hielo azul, carámbanos debajo de las islas y bolsones de nieve polvo. Salen endermans,
  esqueletos y lepismas como en Stellarity. Niebla celeste pastel.

Las islas chicas que el juego pone desde el chunk de al lado quedaban mitad end stone y mitad bioma: cuando un chunk
nuevo termina de generarse se le pasa al suelo del bioma lo que quedó (`EndIslandFix`).

En el End el fuego no se esparce ni quema bloques (`EndFire`): las bolas de fuego de los blazes ya no prenden el bosque.
Con mechero se puede prender, pero se queda en ese bloque.

El cielo del End no toma el color del bioma porque el juego dibuja la textura del cielo del End encima; la niebla sí
cambia. Para que el cielo tome el color habría que cambiar el tipo de cielo de todo el End (se pierde la textura del End
en todos lados), así que se dejó como está.

## Ender Dragon

Cuando se entra al End no está el dragón: hay que invocarlo poniendo los 4 cristales del End en los costados del portal
de salida, como para revivirlo en vanilla (`EndBiomes/EndDragon.java`). Mientras no haya dragón, los 4 lugares brillan.

- El dragón que el juego crea solo se cancela y la pelea queda como "dragón muerto, nunca matado", así el primero que
  se invoque da el huevo y los 12000 de experiencia. Hasta matar al primero el portal de salida está apagado (como en
  vanilla): hay que llevar los 4 cristales.
- El ritual es el de vanilla (las torres se rearman con sus cristales) con una animación de partículas de colores
  encima: anillos que se cierran hacia el portal cada vez más rápido, espirales en los cristales, una columna de luz,
  arcos y estallidos en cada torre, una hélice doble que sube hasta donde aparece el dragón y una esfera que se cierra.
  Cuando sale: explosión de colores, título para los que están en el End y aviso en el chat.
- Si el End ya se había visitado y el dragón que puso el juego sigue vivo, al prender el server se saca.

## Isla del dragón

Se decora una sola vez al prender el server (`EndIslaPrincipal`; si se regenera el End, poner `end.isla_principal` y
`end.plaza` en 0 en config.yml):

- Suelo con vetas de basalto y basalto liso, manchas de obsidiana y obsidiana llorosa y lomitas de end stone.
- Las 10 torres son las del juego (así el ritual las sigue rearmando), con la base ensanchada de obsidiana, obsidiana
  llorosa y blackstone y vetas de obsidiana llorosa.
- Alrededor del portal, una plaza redonda: ladrillos de blackstone, anillo de obsidiana con obsidiana llorosa en los 8
  puntos, un anillo de vidrio de colores, marcos de pizarra detrás de los 4 lugares de los cristales y 4 obeliscos con
  una vara del End arriba.

## End Cities

Las piezas son las de Better End Cities de IchPhilipp (el zip que se subió), en el datapack como
`data/minecraft/structure/end_city`. Reemplazan a las vanilla en las ciudades que se generen desde ahora.

## Spawns

En las islas de afuera (a más de 500 bloques del centro) el juego spawnea endermans. Parte de ellos se cambian ahí
mismo por un mob del End, uno por uno, así la mobcap cuenta igual (`EndBiomes/EndSpawns.java`).

| Zona | Ender Blaze | Ender Spider | Ender Creeper | Shulker Negro | Wither Skeleton | Sigue enderman |
|---|---|---|---|---|---|---|
| End de siempre y Picos Helados | 9% | 9% | 7% | – | – | 75% |
| Bosque Prismático | 5% | 9% | 6% | – | – | 80% |
| Páramo Marchito | 10% | 7% | 7% | 5% | 12% | 59% |

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

Herramientas: espada, hacha, lanza, pico, pala y azada. Tienen 1 de daño más que las de Netherite (la espada 4 más:
12 de daño) y se quedan los encantamientos. La espada le pega el doble a todos los mobs del End (los que están en el End
y los endermans, endermites y shulkers de cualquier lado) y al Rey Ender; el hacha y la lanza le pegan 50% más al Rey
Ender. El doble funciona también con las espadas que ya estaban hechas (se reconoce por el id), pero esas se quedan con
su daño de 9 y su descripción vieja. La herrería no mejora items custom (sí el Peto de Warden para el alado).

## Rey Ender

- Se invoca con el Ojo del Rey Ender: clic derecho a la vara del End del centro de un Santuario Marchito (la vara sobre
  las dos obsidianas llorosas, en la plataforma de obsidiana). El ojo se gasta y a los 5 segundos sale el boss.
- 5000 de vida. El server no deja pasar de 1024, así que el mob tiene 1000 y recibe todo el daño dividido entre 5; la
  barra muestra la vida de 5000.
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
| Regeneración | Trono del Vacío | Al bajar del 66% y del 33%: flota sobre el altar y 4 cristales lo curan 9 de vida por segundo cada uno, hasta 30 s. Recibe la mitad de daño hasta que se rompan (un golpe o una flecha cada uno) |

Suelta la EnderKing Pearl y 3000 de experiencia; las DinoCoins (25 la primera vez, después 5, una vez al día) las da
`BossRewards`. Para probarlo: `/spawnqp reyender` (la arena queda donde se spawnea).

## Modelos que faltan en el resource pack

`lingote_celestita`, `plantilla_celestita`, `enderking_pearl`, `espada_celestita`, `hacha_celestita`,
`lanza_celestita`, `pico_celestita`, `pala_celestita`, `azada_celestita`, `peto_warden_alado` y `warden_gun` (todos como
`minecraft:<id>`, igual que el resto de los items custom).
