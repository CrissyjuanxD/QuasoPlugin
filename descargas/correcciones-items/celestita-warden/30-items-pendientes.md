# Los siguientes 30 items sin textura propia

Revisión de la rama `26.2`, sobre la versión #55. Orden del catálogo `/giveqp`, omitiendo los modelos que ya tienen PNG y los modelos vanilla usados a propósito. Se revisaron `resourcepack/` y las entregas de `descargas/`. Estos son pendientes, no texturas incluidas en este paquete.

Los nombres de la última columna son los del modelo, dentro del namespace `minecraft`. Para un pack completo cada uno necesita su `items/<modelo>.json`, `models/item/custom/<modelo>.json` y `textures/item/custom/<modelo>.png`.

| # | Item | Clave de `/giveqp` | Modelo / nombre del PNG |
|---|---|---|---|
| 1 | Tótem de Doble Vida | `doubletotem` | `totem_doble2 / totem_doble1` |
| 2 | Carne Corrupta | `corrupted_steak` | `corrupted_steak` |
| 3 | Corrupted Golden Apple | `corrupted_golden_apple` | `corrupted_golden_apple` |
| 4 | Libro de Habilidades | `libro_habilidades` | `libro_habilidades` |
| 5 | Fragmento de BloodMoon | `blood_fragment` | `blood_fragment` |
| 6 | Ender Bag | `enderbag` | `ender_bag` |
| 7 | Gancho | `gancho` | `gancho` |
| 8 | Manzana del Pánico | `panic_apple` | `panic_apple` |
| 9 | Artefacto de Hierro Reparador | `artefacto_nivel_1` | `rep_hierro` |
| 10 | Artefacto de Oro Reparador | `artefacto_nivel_2` | `rep_oro` |
| 11 | Ice Totem | `icetotem` | `icetotem` |
| 12 | Fly Totem | `flytotem` | `flytotem` |
| 13 | La Excavadora | `excavator_pickaxe` | `excavator_pickaxe` |
| 14 | Poción de Resistencia II | `potion_resistance_2` | `potion_resistance_2` |
| 15 | Poción arrojadiza de Resistencia III | `splash_resistance_3` | `splash_resistance_3` |
| 16 | Poción de Caída Lenta | `potion_slow_falling` | `potion_slow_falling` |
| 17 | Poción arrojadiza de Regeneración III | `splash_regeneration_3` | `splash_regeneration_3` |
| 18 | Poción de Prisa Minera III | `potion_haste_3` | `potion_haste_3` |
| 19 | Poción de Prisa Minera II | `potion_haste_2` | `potion_haste_2` |
| 20 | Poción arrojadiza de Absorción X | `splash_absorption_10` | `splash_absorption_10` |
| 21 | Frasco de Velocidad | `frasco_de_velocidad` | `frasco_de_velocidad` |
| 22 | Amuleto Luna de Sangre | `amulet_bloodmoon` | `amulet_bloodmoon` |
| 23 | Amuleto de la Inmortalidad | `amuleto_inmortalidad` | `immunity` |
| 24 | Fogata de la Vida | `life_campfire` | `life_campfire` |
| 25 | Combustible de la fogata | `fuel_campfire` | `fuel_campfire` |
| 26 | Tótem Especial | `special_totem` | `special_totem` |
| 27 | Cristal de Hielo | `cristal_hielo` | `cristal_hielo` |
| 28 | Arco de Hielo | `arco_hielo` | `arco_hielo` |
| 29 | Libro de Vuelo Rápido (I y II) | `happy_ghast_enchant / happy_ghast_enchant_2` | `happy_ghast_enchant` |
| 30 | Perla Infinita | `perla_infinita` | `perla_infinita` |

**Variantes:** el tótem necesita dos imágenes (dos vidas y una vida). Los libros de Vuelo Rápido I y II comparten modelo. Son 30 items agrupados y 31 PNG pendientes contando ambos estados del tótem.

`misiones` usa `minecraft:map` y `retorno_warden` usa `minecraft:recovery_compass`; no se cuentan como texturas custom ausentes. Las pociones de esta lista sí asignan modelos custom: el color vanilla del líquido no sustituye esos archivos.

También quedan otros pendientes fuera de estos 30, como los materiales de celestita, los minerales y fragmentos de Warden Cave, otras piezas de armadura y varios consumibles de las tiendas de día 20/30.
