# Ítems y efectos de OneBlock en Quaso 26.2

La migración toma como referencia OneBlockPlugin `fe27648` (master).

## Probar en el servidor

Usar Java 25 y Paper 26.2. Quaso conserva sus integraciones con EliteMobs y
FastAsyncWorldEdit. La BloodMoon ahora forma parte de Quaso; ver
[su migración](bloodmoon-26.2.md).

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

Todos los ítems llaman directamente a
`meta.setItemModel(NamespacedKey.minecraft("nombre"))`, incluidas armaduras,
pociones, libros, drops y objetos de las GUIs. Los que ya tenían modelo en
OneBlock conservan su nombre original. Los nombres están fijados en sus clases;
las antiguas entradas `modelos` de `config.yml` ya no se usan.

El resource pack debe definir los IDs asignados. Por ejemplo,
`minecraft:keep_inv_liquido` corresponde a
`assets/minecraft/items/keep_inv_liquido.json`. El monedero usa
`minecraft:monedero`, cuyo archivo es `assets/minecraft/items/monedero.json`.
El repositorio no contiene las texturas ni los modelos del resource pack.
`NamespacedKey.fromString("minecraft:immunity")` y
`NamespacedKey.minecraft("immunity")` dan la misma clave. La clave indica qué
archivo debe cargar el cliente; si ese archivo falta en su pack, Minecraft
mostrará la textura morada incluso usando el prefijo `minecraft:`.

Se conservan los IDs ya utilizados en OneBlock:

| Ítem | ID del modelo |
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
| `manzana_vida` | `minecraft:manzana_vida` |
| `mochila_nivel_1` a `mochila_nivel_5` | `minecraft:lime_bundle`, `blue_bundle`, `orange_bundle`, `red_bundle`, `purple_bundle` |

Las cartas del blackjack usan `minecraft:carta_5000` hasta `minecraft:carta_5051`.
Los ítems ya guardados mantienen su componente hasta que se creen de nuevo;
este cambio afecta a los nuevos ítems que genera el plugin.
CustomModelData se conserva donde el código lo utiliza para reconocer ítems
antiguos; el aspecto lo define `item_model`.

## Validación

`mvn clean verify` con Java 25 compila y ejecuta las pruebas existentes,
incluida la protección de la GUI de misiones. La prueba dentro de Minecraft debe
confirmar interacciones, tumbas, estatuas y la apariencia con el pack.
