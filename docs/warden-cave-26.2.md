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
- Por debajo es un echo shard que no se apila y no entra en recetas vanilla. Falta el modelo `minecraft:warden_gun`
  en el resource pack (CustomModelData 713).
