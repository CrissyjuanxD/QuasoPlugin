# Spawners custom (26.2)

## Block display que gira (Version #80)

`Handlers/CustomSpawnerHandler`. Al poner un spawner de `/givespawner` el bloque pasa a ser una **barrera** (invisible y
con colisión) y encima sale un **block display** de spawner que gira (15° cada 3 ticks, una vuelta cada 3,6 segundos)
mientras hay alguien a 48 bloques. Mide 0,7 bloques para no meterse en los bloques de al lado. Toda la configuración
(mob, modo, valores, si es rompible y la vida que le queda) se guarda en el display, así vuelve a funcionar cuando se
carga el chunk.

Los 4 modos siguen igual. El modo Vanilla ahora también lo maneja el plugin y, como un spawner normal, solo saca mobs
donde la luz de bloques es 11 o menos. Spawn Custom no mira la luz.

### Romperlo

En el menú del spawner (shift + clic derecho con el item) está la opción nueva **Rompible**, en el slot 9, que se
cambia con un clic. Se guarda en el lore como «Rompible: Sí» o «Rompible: No» y los items de antes son rompibles.

- **Rompible**: solo con un pico, cualquiera. Cada golpe cuenta una vez por segundo y a los **5 golpes** se rompe, como
  las estatuas. Arriba sale la vida que le queda (por ejemplo «4/5»), que sube y se desvanece. Es solo texto, sin
  unicode. Al romperse suelta experiencia como un spawner normal (15 a 43).
- **No rompible**: los golpes no hacen nada.
- En creativo un admin lo saca de un golpe, sea rompible o no.

Si alguien saca la barrera o el display (WorldEdit, `/kill`), el plugin quita lo otro.

### Spawners de antes

Los que ya estaban puestos (el bloque de spawner con la configuración adentro) se pasan solos al cargar su chunk: el
bloque pasa a ser barrera, sale el display con la misma configuración y quedan rompibles. En la consola sale una línea
por cada uno.

Se probó en el servidor de prueba:

- un spawner viejo se pasó con su configuración;
- con espada no le pasa nada;
- dos golpes seguidos cuentan uno;
- a los 5 golpes se rompe y suelta experiencia;
- el irrompible aguanta;
- si se saca la barrera se va el display.

También se arreglaron dos cosas del menú. Un clic en el inventario propio cambiaba un valor del spawner, y los slots 9
a 18 tiraban un error.
