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

`items/WardenGun.java`. Se da con `/giveqp <jugador> warden_gun`; la soltará el Ultra Warden cuando esté hecho.

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
