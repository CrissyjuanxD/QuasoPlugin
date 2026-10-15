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

## Misiones extra de los mobs de BloodMoon (Version #92)

Son 6 extras nuevas (de la 213 a la 218) para los 4 mobs de BloodMoon: Zombie, Esqueleto, Araña y Creeper BloodMoon,
los de nombre rojo. Salen con las misiones de BloodMoon que ya había (9, 58, 83 y 96). Si su misión ya estaba activa,
se activan solas al prender el server, como las demás extras. Solo cuentan los mobs de BloodMoon: un zombie normal en
la BloodMoon no suma.

| Extra | Día | Nombre | Qué pide | DinoCoins |
|---|---|---|---|---|
| 213 (`9ex2`) | 9 | Primera sangre | Matar un Zombie, un Esqueleto, una Araña y un Creeper BloodMoon | 13 |
| 214 (`58ex`) | 58 | Cazador de sangre | Matar 30 mobs de BloodMoon (se suman entre noches) | 16 |
| 217 (`58ex2`) | 58 | Telarañas rojas | Matar 15 Arañas BloodMoon | 14 |
| 215 (`83ex`) | 83 | Desactivador | Matar 8 Creepers BloodMoon con un proyectil (flecha, tridente...) | 15 |
| 218 (`83ex2`) | 83 | Duelo de arqueros | Matar 10 Esqueletos BloodMoon con arco o ballesta (el tridente no cuenta) | 15 |
| 216 (`96ex`) | 96 | Herida abierta | Matar 10 mobs de BloodMoon mientras estás sangrando | 18 |

Recompensas:

- 213: 3 fragmentos de BloodMoon y 4 manzanas doradas.
- 214: 5 fragmentos y 2 pociones de Regeneración III arrojadizas.
- 215: 64 flechas y un libro de Poder V.
- 216: 2 pociones de Resistencia II y 2 manzanas de Notch.
- 217: 3 frascos de velocidad y 6 manzanas doradas.
- 218: un libro de Infinidad y 32 flechas espectrales.

Se probó en el server de prueba. Se mataron 10 mobs de BloodMoon y un zombie normal. Así quedó cada misión:

- 213: completa con los 4 tipos.
- 214: llegó a 10.
- 215: llegó a 1 con la flecha. El creeper a melee no sumó.
- 218: llegó a 1 con la flecha. El tridente no sumó.
- 216: solo sumaron las 2 muertes con sangrado.
- 217: llegó a 2.

## Corrección del detector de items (Version #84)

La misión 54 revisa el inventario cada 3 segundos. Los objetos vanilla o sin la etiqueta de item custom devuelven
un identificador nulo; al consultarlo en `Set.of(...).contains(null)` se lanzaba una `NullPointerException`. Esto
interrumpía la revisión incluso con un arma de Celestita en el inventario. El aviso solo mostraba el mensaje de la
excepción, que podía ser `null`.

El detector compartido de las misiones de crafteo ahora descarta esos identificadores antes de consultar los IDs
permitidos, y la misión 54 reutiliza ese detector. Cuenta cualquiera de estas armas: espada, hacha o lanza de
Celestita. Pico, pala y azada pertenecen a la extra 209 y no completan la 54.

No requiere reiniciar misiones, borrar datos ni volver a fabricar el arma. Tras instalar el JAR actualizado y reiniciar
el servidor, la siguiente revisión normal detecta el item si la misión está activa. Se comprobaron inventarios mixtos,
objetos sin ID, cada arma válida y herramientas que no deben contar; compilación y 16 pruebas seleccionadas correctas.
