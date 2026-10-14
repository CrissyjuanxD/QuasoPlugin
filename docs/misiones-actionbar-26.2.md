# Avisos de objetivos y amuleto

`/misiones` conserva la etiqueta «Misión extra», sin el texto «sale con la #…».
Los requisitos para que una misión extra se active siguen siendo los mismos.

`Handlers.ActionBarHandler` mantiene una cola por jugador compartida entre
misiones, avisos de BloodMoon y el indicador continuo del amuleto. Los objetivos
completados aparecen por orden, durante cuatro segundos cada uno, incluido el
último objetivo de una misión. Los avances parciales duran tres segundos: los
cambios repetidos del mismo objetivo actualizan su texto sin alargar su turno
ni añadir copias a la cola. Completarlo sustituye su avance pendiente.

El amuleto mantiene un indicador de fondo y cede mientras se leen los avisos.
Después reaparece con su estado actual. Desactivarlo no borra objetivos en
pantalla. Las desconexiones eliminan la cola de ese jugador y el apagado del
plugin cancela la tarea compartida. El límite de 64 avisos pendientes evita
acumular mensajes indefinidamente durante actividad intensa.

Las pruebas comprueban tres objetivos simultáneos, sus duraciones, progreso
repetido, el fondo del amuleto, desactivación, límites y limpieza. También cubren
el último objetivo, el lore de misiones extras y que los avisos de BloodMoon
compartan el coordinador. La apariencia necesita comprobarse con un cliente.

## Tipos de misión (26.2, Version #32)

Cada tipo tiene dos colores pastel: el fuerte va en el ۞ y la etiqueta de los
avisos, en el título de los anuncios y en la sección del menú; el suave en el
objetivo y en el nombre de la misión.

| Tipo | Números | En los comandos | Colores | Recompensa |
|---|---|---|---|---|
| Misión | 1 a 100 | `1` | morado `#C9A7EB` y rosa `#F7B8D2` | Ficha para el cofre de la Estatua |
| Extra | 101 a 140 | `1ex` (la extra de la misión 1) | celeste `#9ED8F5` y cyan `#8FE8E2` | Solo DinoCoins, directo al monedero |
| Trabajo | 141 a 200 | `170tra` | café `#C8A27C` y dorado `#F2D58A` | Ficha para el cofre de la Estatua |

- El menú de `/misiones` va en tres secciones, cada una desde una página nueva:
  las 100 misiones, después las 40 extras (ordenadas por su misión) y al final
  las de trabajo.
- El anuncio global de una misión completada sale con los colores de su tipo.
  Con una extra, debajo le sale al jugador «Has obtenido N Dinocoins.» y las
  monedas van a sus monederos (lo que no entra va al inventario).
- `/missions activar|desactivar|complete|remove` aceptan `1`, `1ex` y `170tra`
  (y el número de siempre). El tab muestra las tres formas.

## Misiones extra de crafteos (Version #80)

Una misión puede tener varias extras. En los comandos la primera es `12ex`, la segunda `12ex2`, la tercera `12ex3`
(también vale `12ex1`). Las extras nuevas van de la 201 a la 212 (de la 141 a la 200 son las de trabajo) y salen con
su misión, igual que las de antes. Si su misión ya estaba activa cuando se agregaron, se activan solas al prender el
server, sin anuncio. Esto pasa solo con las que nunca estuvieron en la base de datos: si el staff desactivó una, se
queda así.

Todas se cumplen teniendo el item en el inventario o puesto (se revisa cada 3 segundos), así cuenta aunque se haga
con shift, en la herrería o en el alto horno. Una pieza que ya se mejoró también cuenta: la de Warden vale por la
floral y la de Celestita por la floral.

| Extra | Día | Nombre | Qué pide | DinoCoins |
|---|---|---|---|---|
| 201 (`1ex2`) | 1 | Puntería reforzada | Fabricar el Arco Mejorado | 14 |
| 202 (`2ex2`) | 2 | Botín floral | Conseguir un drop floral | 11 |
| 203 (`8ex`) | 8 | Cocina corrupta | Fabricar una Carne Corrupta | 10 |
| 204 (`12ex2`) | 12 | Armadura floral | Forjar las 4 piezas de la armadura floral | 20 |
| 205 (`12ex3`) | 12 | Herramientas florales | Forjar la espada, el hacha, el pico y la pala florales | 20 |
| 206 (`14ex`) | 14 | Escudo floral | Forjar el Escudo Floral | 15 |
| 207 (`20ex`) | 20 | Armadura de Warden | Forjar las 4 piezas de la armadura de Warden | 20 |
| 208 (`23ex`) | 23 | Fundición profunda | Fundir un Mineral Crudo y fabricar un Lingote Profundo | 13 |
| 209 (`35ex`) | 35 | Herramientas de celestita | Forjar el pico, la pala y la azada de Celestita | 20 |
| 210 (`35ex2`) | 35 | Armas de celestita | Forjar la espada, el hacha y la lanza de Celestita | 20 |
| 211 (`54ex`) | 54 | El ojo del rey | Fabricar el Ojo del Rey Ender | 15 |
| 212 (`55ex`) | 55 | Alas de Warden | Forjar el Peto de Warden Alado | 20 |

El mensaje de las DinoCoins de las extras sale con el ícono de las DinoCoins (``, el mismo del scoreboard):
«Has obtenido [ícono] 14 Dinocoins.». También lo llevan el mensaje de los jefes y los de subir de nivel y bonus de los
trabajos.
