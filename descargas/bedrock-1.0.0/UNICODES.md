# Unicodes de Bedrock 1.0.0

Los siguientes códigos mantienen el carácter de Java. Usa blanco (`§f`) antes del icono para conservar sus colores. Bedrock cambia el tamaño y espaciado; no lee `height/ascent` del JSON Java.

| Código | Carácter | Imagen original |
| --- | --- | --- |
| `U+E900` | `` | `minecraft:croissants_scoreboard.png` |
| `U+E906` | `` | `minecraft:croissants_tablist.png` |
| `U+E904` | `` | `minecraft:dinocoins.png` |
| `U+E901` | `` | `minecraft:usuario.png` |
| `U+E902` | `` | `minecraft:rango.png` |
| `U+E903` | `` | `minecraft:misiones.png` |
| `U+E905` | `` | `minecraft:trabajo.png` |
| `U+EB87` | `` | `theranks:ranks/admin.png` |
| `U+EB88` | `` | `theranks:ranks/mod.png` |
| `U+EB89` | `` | `theranks:ranks/helper.png` |
| `U+EB8A` | `` | `theranks:ranks/dinonugget.png` |
| `U+EB8B` | `` | `theranks:ranks/dinonugget_plus.png` |
| `U+EB8C` | `` | `theranks:ranks/dinoleyenda.png` |
| `U+EB8D` | `` | `theranks:ranks/dinosub.png` |
| `U+EB8E` | `` | `theranks:ranks/dinovip.png` |
| `U+EB8F` | `` | `theranks:ranks/admin_tab.png` |
| `U+EB90` | `` | `theranks:ranks/mod_tab.png` |
| `U+EB91` | `` | `theranks:ranks/helper_tab.png` |
| `U+EB92` | `` | `theranks:ranks/dinonugget_tab.png` |
| `U+EB93` | `` | `theranks:ranks/dinonugget_plus_tab.png` |
| `U+EB94` | `` | `theranks:ranks/dinoleyenda_tab.png` |
| `U+EB95` | `` | `theranks:ranks/dinosub_tab.png` |
| `U+EB96` | `` | `theranks:ranks/dinovip_tab.png` |

## Rangos anchos para textos de Bedrock

Los rangos originales ocupan una sola celda y quedan pequeños. Estas alternativas dividen cada insignia en varios caracteres; conservan la proporción del dibujo. Bedrock puede dejar separación entre fragmentos por su espaciado de fuente. Úsalas solamente en prefijos/textos configurados para jugadores Bedrock; el pack Java no define estos caracteres adicionales. No hay una sustitución automática de los prefijos de tu plugin de rangos.

Puedes copiar el valor de `rangos_bedrock.yml` o el texto de esta tabla. El archivo es una lista de valores, no un config que debas pegar entero encima del de otro plugin.

| Rango | Secuencia para copiar | Códigos |
| --- | --- | --- |
| admin | `§f` | U+ED00 U+ED01 U+ED02 U+ED03 U+ED04 U+ED05 |
| mod | `§f` | U+ED06 U+ED07 U+ED08 U+ED09 U+ED0A U+ED0B U+ED0C U+ED0D U+ED0E |
| helper | `§f` | U+ED0F U+ED10 U+ED11 U+ED12 U+ED13 U+ED14 U+ED15 |
| dinonugget | `§f` | U+ED16 U+ED17 U+ED18 U+ED19 U+ED1A U+ED1B U+ED1C U+ED1D U+ED1E |
| dinonugget_plus | `§f` | U+ED1F U+ED20 U+ED21 U+ED22 U+ED23 U+ED24 U+ED25 U+ED26 U+ED27 |
| dinoleyenda | `§f` | U+ED28 U+ED29 U+ED2A U+ED2B U+ED2C U+ED2D U+ED2E U+ED2F U+ED30 |
| dinosub | `§f` | U+ED31 U+ED32 U+ED33 U+ED34 U+ED35 U+ED36 U+ED37 |
| dinovip | `§f` | U+ED38 U+ED39 U+ED3A U+ED3B U+ED3C U+ED3D U+ED3E |
| admin_tab | `§f` | U+ED3F U+ED40 U+ED41 U+ED42 |
| mod_tab | `§f` | U+ED43 U+ED44 U+ED45 U+ED46 |
| helper_tab | `§f` | U+ED47 U+ED48 U+ED49 U+ED4A |
| dinonugget_tab | `§f` | U+ED4B U+ED4C U+ED4D U+ED4E |
| dinonugget_plus_tab | `§f` | U+ED4F U+ED50 U+ED51 U+ED52 |
| dinoleyenda_tab | `§f` | U+ED53 U+ED54 U+ED55 U+ED56 |
| dinosub_tab | `§f` | U+ED57 U+ED58 U+ED59 U+ED5A |
| dinovip_tab | `§f` | U+ED5B U+ED5C U+ED5D U+ED5E |

## GUIs y logos

Los marcadores `U+3202` a `U+3208` se reconocen en el título para mostrar las siete imágenes de interfaz. No se reemplaza toda la página de caracteres asiáticos `glyph_32.png`; así no desaparecen caracteres ajenos al menú. El avance negativo `U+3201` no se reproduce: el título se oculta en la interfaz Clásica reconocida.

El logo `U+E900` del scoreboard se representa mediante una imagen de interfaz para conservar su anchura. `U+E906` sigue siendo el logo compacto en otros textos. El logo de progreso usa la versión con **Segunda Edición**. Los rangos y logos en el chat siguen teniendo las limitaciones de celdas descritas arriba.

Estos glifos corresponden a JSON UI. Las pantallas ORE UI pueden mostrar cuadrados o el carácter original porque no admiten este mecanismo.
