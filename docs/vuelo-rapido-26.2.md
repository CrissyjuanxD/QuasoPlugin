# Vuelo Rápido · Version #77

Vuelo Rápido pasó al sistema de encantamientos: es `quaso:vuelo_rapido` en el datapack, como los de la Warden Cave,
el End y los trabajos. Antes era un sistema aparte que guardaba el nivel en el item y le cambiaba la velocidad al
Happy Ghast cada 5 ticks.

- Va en el arnés (`#minecraft:harnesses`), en el slot del cuerpo del Happy Ghast. Niveles I y II.
- Cada nivel le suma 50% de velocidad de vuelo al ghast que lo lleva (I: +50%, II: +100%). Lo hace el propio juego,
  sin tareas del plugin. Probado en Paper 26.2: con II el ghast pasa de 0,05 a 0,1 de velocidad de vuelo.
- Se pone en el yunque como cualquier libro; dos libros I hacen un II.
- La tienda y las misiones 4, 90 y 112 dan el libro nuevo (`happy_ghast_enchant` y `happy_ghast_enchant_2` siguen
  funcionando en `/giveqp`, y también `libro_vuelo_rapido_1` y `libro_vuelo_rapido_2`). El libro usa el modelo
  `minecraft:happy_ghast_enchant` de siempre.
- Los libros y arneses del sistema viejo se pasan solos al encantamiento: los del inventario y el cofre de ender al
  entrar, y el arnés que lleva puesto un ghast cuando se carga. A los ghasts también se les quita el modificador de
  velocidad viejo, que había quedado guardado en ellos.
