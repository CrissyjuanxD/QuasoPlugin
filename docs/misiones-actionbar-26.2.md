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
