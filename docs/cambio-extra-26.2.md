# Cambio extra (26.2)

Va entre el cambio uno y el dos, para el día 14: `/changes activar extra` (`Dificultades/ExtraChanges.java`). Con el
nuevo orden los números de `/changes` son 1 uno, 2 extra, 3 dos y 4 tres; por nombre funciona igual que antes.

## Mobs florales

El Corrupted Zombie y la Corrupted Spider ahora son el **Zombie Floral** y la **Spider Floral**, para que tengan que
ver con la Abeja Floral. Cambiaron el nombre y las partículas; lo demás es igual (la wind charge con veneno y debilidad,
la telaraña, la velocidad, la fuerza y la carne corrupta).

- Partículas: polvo pastel (rosa, amarillo, verde y morado, los colores de la Abeja Floral) y pétalos de cerezo
  alrededor del cuerpo cada segundo, una estela floral en la wind charge y un estallido de pétalos al morir y al dejar
  la telaraña.
- La clave PDC sigue siendo la de antes (`corrupted_zombie` y `corruptedspider`), así la misión 17, los spawners y los
  mobs que ya estaban en el mundo siguen contando. En `/spawnqp` y los spawners siguen siendo `corruptedzombie` y
  `corruptedspider`.

## Spawn natural (cambio extra)

En el Overworld (no en la Warden Cave), de los que spawnean solos:

| Mob | Sale floral |
|---|---|
| Zombie (adulto) | 25% pasa a Zombie Floral |
| Araña | 25% pasa a Spider Floral |
| Creeper | 20% pasa a Bombita |

Se convierten ahí mismo, así la mobcap cuenta igual. Los zombies bebé quedan normales. Lo que hacen viene del cambio
uno, que está activo desde el día 1. Si se desactiva el cambio extra, los que ya salieron se quedan.
