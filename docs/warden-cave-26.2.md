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

## Ajustes de la Version #67

- La armadura de Warden solo sale de la floral: en la herrería la base tiene que ser la pieza floral (Netherite »
  Floral » Warden). Con una pieza de Netherite común no da resultado.
- El Infested Creeper explota más fuerte: radio 3 cargado, potencia 6 (antes 4), como un creeper cargado normal.
- El lore de la armadura de Warden se rehízo con `items/EquipoLore`: historia en el color del bioma, los atributos
  con íconos (◆ armadura y dureza, ❤ +2 corazones, ✚ 30% de no gastarse, ⚓ 10% menos empuje y ✈ planear en el peto
  alado) y la línea de progresión con Warden resaltado. El tooltip de atributos del juego se oculta. Las piezas que
  ya existían conservan su lore.
