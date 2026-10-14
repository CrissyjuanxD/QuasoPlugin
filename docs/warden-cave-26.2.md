# Warden Cave (26.2)

Cambios de la dimensión (`InfestedCaves/`). La generación solo cambia en los chunks que se generen desde ahora.

## Minerales

Una veta de Mineral Profundo cada tantos bloques de roca:

| Bioma | Antes | Ahora |
|---|---|---|
| Caverna Sculk | 1 cada 900 | igual |
| Pantano Profundo | 1 cada 900 | igual |
| Abismo Flotante | 1 cada 900 | 1 cada 450 (las islas tienen poca roca) |
| Ruinas de Ceniza | 1 cada 900 | 1 cada 650 |

## Abismo Flotante

- Menos ghasts: en el datapack el ghast bajó de peso 60 a 20 y de los zombies que spawnean ahí ahora 1 de cada 4 (antes
  6 de cada 10) pasa a Infested Ghast, y solo si no hay ya 3 ghasts a 48 bloques.
- Árbol nuevo, como el de la Caverna Sculk: tronco de cerezo torcido con raíces de obsidiana llorosa, copa ancha de hojas
  que el bioma tiñe de morado con pétalos de cerezo, y los froglights perlados colgando debajo como frutas (siguen
  dando Frutas Abisales). Al pie, cristales de amatista.

## Ancient City

Arriba de la caverna de la ciudad quedaba una meseta plana. Ahora sobre el techo de roca hay lomas de hasta unos 28
bloques con cuevas, y arriba sigue el bioma con sus crestas, árboles y decoración.

## Infested Warden

Solo cuenta a los jugadores que están dentro de la caverna de la ciudad. A los que están arriba en el terreno (o minando
el techo) no los persigue, no se tepea hasta ellos y no se enoja con ellos aunque hagan ruido; si alguien sube, se olvida
de él.

## Warden Gun

`items/WardenGun.java`. Se da con `/giveqp <jugador> warden_gun`. Desde la Version #82 se fabrica con un Núcleo Sónico
del Ultra Warden (ver más abajo).

- Click derecho: carga 0,6 segundos (sonido de carga del Warden) y dispara un sonic boom.
- 10 de daño a los mobs, 6 a los jefes y 5 a los jugadores. Como el del Warden, atraviesa la armadura.
- Alcance de 16 bloques, se corta en la primera pared y pega a 3 como mucho en línea, con un empuje chico y Lentitud I
  por 3 segundos.
- Recarga de 5 segundos. Se ve en el item (la recarga es solo de la Warden Gun, no de los demás echo shards). No gasta
  Energía de Warden: el límite es la recarga.
- No le pega al que dispara, a sus mascotas ni a los soportes de armadura. Con un cofre o una puerta delante los abre
  normal; agachado dispara.
- Cuenta para las misiones 63 (mobs con la Warden Gun) y 92 (equipo legendario).
- Por debajo es un echo shard que no se apila y no entra en recetas vanilla. Su textura está en `resourcepack/`
  (`minecraft:warden_gun`, CustomModelData 713).

## Ajustes de la Version #66

- Los Minerales Profundos solo se rompen con un pico de Netherite. El Pico
  Floral y el de Celestita también son de Netherite, así que sirven. Con otro
  pico sale un aviso en la action bar y el bloque no se rompe.
- Los mobs suben porque el equipo floral llega antes: los infestados tienen
  120 de vida (antes 100), el Warden Zombie 72 y 8,5 de daño (antes 60 y 7), la
  Infested Cave Spider 9,5 de daño (antes 8) y la explosión de las bolas del
  Infested Ghast pasa de 2,5 a 3.
- El lore de cada pieza de la armadura de Warden va con el color de su bioma,
  igual que su alma: casco cian, peto morado, pantalón gris y botas verde. Las
  piezas que ya existían conservan su lore.
- Al activar la etapa con `/changes activar dos` sale un anuncio en el chat con
  lo que trae la Infested Warden Cave. Las otras etapas también anuncian lo suyo.

## Ajustes de la Version #74

- La armadura de Warden solo sale de la floral: en la herrería la base tiene que ser la pieza floral (Netherite »
  Floral » Warden). Con una pieza de Netherite común no da resultado.
- El Infested Creeper explota más fuerte: radio 3 cargado, potencia 6 (antes 4), como un creeper cargado normal.
- El lore de la armadura de Warden se rehízo con `items/EquipoLore`: historia en el color del bioma, los atributos
  con íconos (◆ armadura y dureza, ❤ +2 corazones, ✚ 30% de no gastarse, ⚓ 10% menos empuje y ✈ planear en el peto
  alado) y la línea de progresión con Warden resaltado. El tooltip de atributos del juego se oculta. Las piezas que
  ya existían conservan su lore.

## Ajustes de la Version #78

- Los libros de Paso Ígneo, Purificación y Visión Abisal salen en los cofres de las Ancient City (40%, antes 25%) y de
  los jefes. Los mobs comunes ya no sueltan libros.
- La zona de la Ancient City ya no queda plana:
  - El piso de su caverna es plano solo en el centro, donde van las piezas. Hacia las paredes sube con lomas y
    montículos de hasta unos 20 bloques. Lo que tape una pieza lo saca la propia estructura (beard_box).
  - En la caverna salen los árboles del bioma, solo donde no tocan ninguna pieza de la ciudad.
  - Las lomas de arriba de la caverna son más altas (hasta unos 50 bloques), con cerros y escalones.

  Solo cambia en los chunks nuevos: las ciudades ya generadas quedan como estaban.

## Ajustes de la Version #81

- Las Ancient City salían pegadas: usaban la colocación vanilla (una cada 24 chunks y a veces a solo 9). Ahora la
  Warden Cave tiene su propia estructura, `quaso:ciudad_warden`, con su `structure_set` `quaso:ciudades_warden`:
  - una ciudad por región de 36x36 chunks, corrida hasta 4 chunks;
  - entre dos ciudades siempre hay al menos 512 bloques y en promedio unos 600, parecido a las Trial Chambers.

  Las piezas y el loot son los vanilla. El mundo normal sigue con la Ancient City vanilla en el Deep Dark: el datapack
  ya no suma los biomas de la Warden Cave al tag vanilla y borra ese archivo al instalarse. Para buscarla se usa
  `/locate structure quaso:ciudad_warden`. Hay que reiniciar una vez para que se cargue el datapack.
- En un mundo ya explorado las ciudades viejas quedan donde están y las nuevas salen con la separación nueva. Cerca
  del borde de lo explorado puede quedar una ciudad vieja a medio generar dentro de la roca. Para que todo quede
  parejo, borra la carpeta `dimensions/minecraft/wardencave` del mundo antes de que entren los jugadores.
- Infested Warden:
  - Las ciudades a menos de 300 bloques comparten una sola guarida y un solo boss. Antes cada ciudad pegada sacaba el
    suyo y salían dos juntos. Las guaridas viejas de `infested_warden.yml` que quedan pegadas se juntan al cargar, y
    si después aparece el boss sobrante, se borra.
  - Tampoco sale un boss si ya hay otro Infested Warden vivo a menos de 300 bloques, por ejemplo uno de `/spawnqp`.
  - Los ataques especiales se avisan en el chat a los de la arena, como la Abeja Floral: «Infested Warden » Rugido
    Sónico», «Pulso de Sculk», «Llamado de la Infestación» y «Erupción de Sculk».
- El Mineral Crudo tarda 3 minutos en el alto horno (antes 4) y su lore lo dice. El alto horno sigue aceptando los
  minerales con el lore viejo. Además, el lore viejo se cambia por el nuevo en el inventario al entrar al server, al
  abrir un cofre o al agarrarlos del suelo, así se apilan con los nuevos. Los que están dentro de un horno no se tocan.

## Version #82: Ultra Warden y su equipo

### Materiales nuevos

| Item | Id | Cómo sale | Para qué sirve |
|---|---|---|---|
| Lingote Profundo Cian, Verde, Morado y Gris | `lingote_profundo_<color>` | 8 Fragmentos Profundos del mismo bioma alrededor de un lingote de oro | La mejora de la pieza de su bioma y el Lingote de Celestita |
| Tejido Sculk | `tejido_sculk` | 40% de los mobs de la Warden Cave (Infested Skeleton, Cave Spider, Ghast, Creeper y Warden Zombie) | La Energía de Warden |
| Núcleo Sónico | `nucleo_sonico` | 1 o 2 a cada jugador que pelea contra el Ultra Warden | Las mejoras de la armadura, la Warden Gun, el Arco de Sculk y las Botas Escaladoras |

- El Tejido Sculk solo cae si el mob lo mató un jugador. No sale de mobs de spawner ni de los que invocan los bosses
  (llevan la marca `boss_minion`). En el servidor de prueba cayó en 84 de 200 Infested Skeletons.
- El Lingote Profundo de antes (el de cualquier color) ya no se fabrica. Los que ya existen siguen sirviendo para el
  Lingote de Celestita y para la misión 208.
- Los items que ya existían no cambian de lore, así siguen entrando en las recetas.

### Recetas nuevas

| Receta | Ingredientes |
|---|---|
| Lingote Profundo de bioma | `FFF` / `FOF` / `FFF`: 8 fragmentos del bioma y 1 lingote de oro |
| Energía de Warden | `FTF` / `TET` / `FTF`: 4 fragmentos de cualquier bioma, 4 Tejidos Sculk y 1 echo shard |
| Mejora de cada pieza | `LAL` / `WTN` / `L L`: 4 lingotes de su bioma, su alma, 1 Energía, la plantilla de Netherite y 1 Núcleo Sónico |
| Pieza de Warden (herrería) | La mejora, la pieza floral y el 5º lingote de su bioma |
| Warden Gun | `ENE` / `W W` / `E E`: 1 Núcleo Sónico, 2 Energías de Warden y 4 echo shards |
| Arco de Sculk (herrería) | Núcleo Sónico, un arco normal o el Arco Mejorado, y 1 Energía de Warden |
| Botas de Warden Escaladoras (herrería) | Núcleo Sónico, Botas de Warden y un bloque de miel |

Cada pieza pide 5 lingotes de su bioma, o sea 40 minerales de ese bioma. Casco cian, peto morado, pantalón gris y
botas verde. Sin el Núcleo Sónico no hay armadura de Warden: hay que matar al Ultra Warden, como pasa con la Esencia
Floral de la Abeja Floral.

### Altar del Ultra Warden

Es un tipo más del sistema de altares (`Dificultades/Features/AltarFunctions`). Se construye parado donde va, con
`/altarvct create ultrawarden`:

- una base de 5x5 de deepslate con sculk y deepslate reforzado al centro;
- deepslate pulido en las esquinas con linternas de almas arriba;
- un catalizador de sculk arriba del centro.

Con un Corazón de Warden Boss en la mano se le da click derecho al catalizador:

- El corazón se gasta.
- Durante 5 segundos de ritual el corazón late, suben almas en espiral y se cierra un anillo de sculk.
- Después el Ultra Warden sale del suelo al lado del altar, con la animación de emerger del warden. Mientras emerge
  no recibe daño.

Solo puede haber un Ultra Warden vivo. Mientras haya uno, el altar no gasta el corazón. Si se descarga su chunk,
vuelve a pelear al cargarse (`Bosses/UltraWardenAltar`).

### Ultra Warden

`Bosses/UltraWardenBoss.java`. Se puede sacar de prueba con `/spawnqp ultrawarden`.

- **Stats:** 2000 de vida, 1.6 de tamaño y 20 de daño base. El mob tiene 1000 y el daño que recibe se divide entre 2,
  como el Rey Ender.
- **Arena:** 34 bloques alrededor de donde sale.
- **Ritmo:** pelea como el Rey Ender: de 1 a 3 ataques cuerpo a cuerpo y después un especial.
- **Avisos:** cada especial se avisa en el chat a los de la arena, por ejemplo «۞ Ultra Warden » Silencio Profundo».
- **Teleports:** se ven con una esfera donde está, un anillo que se cierra donde va a aparecer, un arco de almas entre
  los dos puntos y un sonic boom al llegar.

Los daños de la tabla son antes de la armadura y la dificultad. El sonic boom del Silencio Profundo no depende de la
dificultad.

| Ataque | Tipo | Qué hace |
|---|---|---|
| Embestida Sónica | Cuerpo a cuerpo | Marca en el suelo un camino de 16 bloques hacia su jugador y sale disparado. Todo lo que está en el camino recibe 24 de daño y sale volando hacia arriba. Frena si choca contra una pared |
| Zarpazo Doble | Cuerpo a cuerpo | Se acerca y barre dos veces delante suyo (18 y 18). El segundo zarpazo llega más lejos y da Oscuridad |
| Puño Sísmico | Cuerpo a cuerpo | Golpea el suelo (16 de daño a 3,5 bloques) y una grieta de sculk corre 16 bloques hacia su jugador. El que está sobre la grieta recibe 22 y sale volando |
| Silencio Profundo | Especial | Sale el título «Silencio Profundo · No te muevas» y a los 3 segundos empieza el silencio. Durante 8 segundos el que se mueve (más de 0,4 bloques de costado o 0,6 de alto) recibe un sonic boom que le saca el 90% de la vida, una sola vez. Después queda 4 segundos aturdido y recibe 50% más de daño. Dura 15 segundos |
| Esfera Resonante | Especial | Se queda quieto y crece una esfera de 13 bloques a su alrededor, con cuenta de 3 segundos («¡Sal de la esfera!»). Después explota por dentro durante 3 segundos: 6 de daño sónico cada medio segundo a los que siguen adentro |
| Lluvia Sónica | Especial | Caen del cielo pilares de sonic boom, 3 por jugador y mínimo 5 (hasta 15), uno cerca de cada jugador. Se quedan 1 minuto y el que toca uno recibe 12 de daño sónico por segundo. El boss sigue peleando mientras están y no la repite hasta que se van |
| Llamado de la Infestación | Especial | De la tierra salen Warden Zombies e Infested Skeletons: 2, o 3 con menos de la mitad de vida, hasta 5 vivos. No le pegan al boss y no sueltan Tejido Sculk |
| Cacería en la Oscuridad | Especial | Da Oscuridad a todos, se vuelve invisible y aparece 3 veces detrás de jugadores al azar. Cada vez les pega un zarpazo y los deja ciegos un momento |
| Corazón de Sculk | Regeneración | Al bajar del 50% y del 25% vuelve al centro de la arena y levanta 4 catalizadores de sculk que giran. Cada catalizador lo cura 5 de vida por segundo, con un rayo de almas hacia él. Mientras tanto recibe la mitad de daño y cada 4 segundos empuja a los que se le pegan. Termina cuando se rompen los 4 (un golpe o una flecha cada uno) o a los 30 segundos |

Recompensas (`Bosses/BossRewards`):

- 20 DinoCoins la primera vez y 5 las siguientes, más 1 o 2 Núcleos Sónicos a cada jugador que peleó.
- Se cobra hasta 2 veces por día.
- Suelta 1500 de experiencia y no suelta el catalizador del warden.
- Cuenta para las misiones de jefes, los libros de jefe y la XP de trabajos, como antes.

### Arco de Sculk

Ver [arcos](arcos-26.2.md#arco-de-sculk-version-82).

### Botas de Warden Escaladoras

`Armors/BotasEscaladoras.java`. Son las Botas de Warden con los mismos stats, pero además:

- **Agarre:** en el aire y pegado a una pared, el jugador se queda agarrado y baja despacio, hasta 2,5 segundos. Con
  shift se suelta.
- **Salto de pared:** con salto se impulsa hacia arriba y lejos de la pared, un poco hacia donde mira. Se puede seguir
  de pared en pared como en parkour.
- No funcionan en creativo, volando, planeando ni nadando.
- Sin daño de caída mientras escala.

Se quedan los encantamientos de las botas. El lore suma «⇈ Se pega a las paredes» y «⇈ Salta de pared en pared».
