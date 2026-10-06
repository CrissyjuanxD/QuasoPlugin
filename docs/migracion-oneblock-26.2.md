# Ítems y efectos de OneBlock en Quaso 26.2

La migración toma como referencia OneBlockPlugin `fe27648` (master).

## Probar en el servidor

Usar Java 25 y Paper 26.2. Quaso conserva sus integraciones con EliteMobs,
FastAsyncWorldEdit y BloodMoon.

Los nuevos ítems se pueden obtener con `/giveqp <ítem> [cantidad] [jugador]`:

- `keep_inventory_liquido`: protección del inventario y experiencia durante
  dos minutos, conservada al morir y al usar un tótem.
- `estatua_protectora`: protección contra explosiones, fuego y acciones de mobs
  en un área de 18 × 18 × 18; Shift + clic derecho permite recogerla.
- `amuleto_ultima_esperanza`: ritual y regreso al último spawn con efectos de castigo.
- `pluma_levitacion_mejorada`: tres segundos de levitación y protección para esa caída.
- `corrupted_spider_eye`, `bloque_oro_apilado`, `arco_hielo_jugador`.

El amuleto de inmortalidad ahora aplica inmunidad total durante 30 segundos.
Los efectos custom se restauran después del uso de tótems. La corrupción permite
romper spawners.

`/givestatue` sigue entregando una estatua. También admite `give`, `clone` y
`debug`; `/statue` es un alias. Las estatuas incorporan partículas, Anti-Grief,
visualización del rango y recuperación al cargar chunks o pegar schematics.

La GUI de misiones deja vacíos los primeros 18 slots, conserva las misiones y
flechas en sus posiciones y sigue bloqueando clics y arrastres de objetos.

## Item models y resource pack

`ItemModels.load` añade las entradas que falten bajo `modelos` en el `config.yml`
del servidor, sin sustituir los valores configurados. Cada fábrica de ítems
asigna el modelo; también se aplica a drops, recetas, libros, armaduras,
estatuas y objetos de eventos. Los modelos del casino y de logros se incluyen.

El resource pack debe definir los IDs configurados. Por ejemplo,
`minecraft:keep_inv_liquido` corresponde a
`assets/minecraft/items/keep_inv_liquido.json`. Un namespace propio como
`quaso:keep_inv_liquido` se puede asignar en `modelos.keep_inventory_liquido`.
El repositorio no contiene las texturas ni los modelos del resource pack.

Se conservan los IDs ya utilizados en OneBlock:

| Entrada de `modelos` | ID por defecto |
| --- | --- |
| `keep_inventory_liquido` | `minecraft:keep_inv_liquido` |
| `estatua_protectora` | `minecraft:statue_pr` |
| `amuleto_ultima_esperanza` | `minecraft:amuleto_esperanza` |
| `amuleto_inmortalidad` | `minecraft:immunity` |
| `pluma_levitacion` | `minecraft:pluma_levi` |
| `pluma_levitacion_mejorada` | `minecraft:pluma_levi_mejorada` |
| `doubletotem_2` / `doubletotem_1` | `minecraft:totem_doble2` / `minecraft:totem_doble1` |
| `enderbag` | `minecraft:ender_bag` |
| `gancho` | `minecraft:gancho` |
| `artefacto_nivel_1` / `artefacto_nivel_2` | `minecraft:rep_hierro` / `minecraft:rep_oro` |

Las cartas del blackjack usan `minecraft:carta_5000` hasta `minecraft:carta_5051`.
Los IDs de todos los modelos se encuentran en `items/ItemModels.java` y aparecen
en el config del servidor al iniciar el plugin. CustomModelData se conserva
donde el código lo utiliza para reconocer ítems antiguos; el aspecto lo define
`item_model`.

## Validación

`mvn clean verify` con Java 25 compila y ejecuta las pruebas de los modelos y
de la protección de la GUI de misiones. La prueba dentro de Minecraft debe
confirmar interacciones, tumbas, estatuas y la apariencia con el pack.
