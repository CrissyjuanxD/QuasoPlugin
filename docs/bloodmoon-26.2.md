# BloodMoon dentro de Quaso 26.2

El paquete `BloodMoon` y su clase principal `BloodMoon` integran la lógica de
BloodMoon 0.8.1 de SpectralMemories, recuperada del JAR que había en `libs`.
Conserva el ciclo por mundo, noches permanentes, avisos, barra nocturna, clima,
sonidos, efectos de combate, bloqueo de camas, experiencia y botín configurable,
comandos al inicio/final y hordas. El jefe zombi, sus poderes, configuración y
comandos se han retirado por petición del usuario. La suite de desarrollo del
plugin independiente se sustituye por las pruebas automatizadas de Quaso.

Ya no se necesita `libs/bloodmoon.jar`, una dependencia Maven de BloodMoon ni
el plugin externo en el servidor. **Retirar BloodMoon de `plugins` al instalar
este JAR de Quaso y reiniciar**, para evitar ciclos, mobs y comandos duplicados.
La carpeta antigua `plugins/BloodMoon` puede conservarse: Quaso importa su
configuración por mundo la primera vez y, si Paper dispone de SQLite, lee su
calendario antiguo. No modifica los archivos originales. Los mundos NORMAL
que se carguen posteriormente también se registran.

La configuración nativa queda en `plugins/QuasoPlugin/bloodmoon/<mundo>/config.yml`;
los mensajes en `bloodmoon/mensajes.yml` y el calendario en `bloodmoon/estado.yml`.
Se mantienen las claves originales. Los ajustes de pérdida de inventario y
experiencia, y los del jefe, se descartan al importar/recargar. No se añade
confusión. Las misiones de BloodMoon y el drop existente de fragmentos del Día 1
consultan el estado nativo.

Los valores originales son un intervalo de cinco noches, daño de mobs ×2,
resistencia ×3, experiencia ×4 y hordas de 3–10 mobs. El rango de aparición es
12 bloques. Los tiempos de hordas son ticks: 800 ± 200, es decir, 30–50 segundos.
Estos valores se pueden cambiar por mundo. Los jugadores en creativo o
espectador no se seleccionan como objetivos. Se conserva el formato de
recompensas ponderadas y comandos `;s`, `;p`, `;f` del sistema original.

## Comandos

Todos completan sus argumentos según permisos. Desde un jugador utilizan su
mundo; desde la consola requieren el mundo después del subcomando:

- `/bloodmoon show`: consulta el calendario; permiso `bloodmoon.show`.
- `/bloodmoon start`: inicia la noche; permiso `bloodmoon.start`.
- `/bloodmoon stop`: termina la noche; permiso `bloodmoon.stop`.
- `/bloodmoon reload`: recarga configuración y mensajes; permiso `bloodmoon.reload`.
- `/bloodmoon spawnhorde [jugador]`: genera una horda para un jugador conectado
  en ese mundo; permiso `bloodmoon.spawnhorde`.

Los permisos administrativos son de operador por defecto. `show` es público.
Ejemplo desde consola: `bloodmoon spawnhorde world Crosszy`.
`/quasoreload` también recarga este sistema. El modo permanente se detiene
cambiando `PermanentBloodMoon` a `false` y recargando.

## Spawns y amuleto

Las posiciones se centran y se sitúan **encima del suelo**, comprobando el tamaño
completo del mob, colisiones, líquidos, bloques peligrosos, altura, borde y chunks
cargados. Si no hay espacio seguro, se omite el spawn. Se respetan las cancelaciones
de `CreatureSpawnEvent` y, si está instalado, WorldGuard. No se cargan chunks nuevos
para generar hordas.

El amuleto cancela `BloodMoonHordeEvent` antes de crear cualquier mob. Protege al
portador y su radio de 20 bloques, incluidas posiciones previstas para hordas
que iban dirigidas a otro jugador. **No borra mobs existentes ni cancela spawns
naturales, spawners o mobs de otros sistemas.** Conserva el coste de un diamante
por minuto y un uso cada 7,2 segundos; el progreso parcial se guarda al desactivarlo
para que cambiar su estado no reinicie el consumo. Al morir, desconectarse o terminar
la BloodMoon, la protección se desactiva.

Los mensajes están en español con naranja `#F4B183` y rojo `#EF9292`. Las action bars
comienzan con `۞`, el mismo símbolo de las misiones. El aviso de inicio dice:

> Bloodmoon >
>
> Ha empezado una BloodMoon.
> Los monstruos son más fuertes y las hordas acechan durante la noche.
> Derrota monstruos para conseguir más experiencia, botín y fragmentos de BloodMoon.

## Validación

`mvn clean verify` con Java 25 comprueba calendario y reinicios, importación de
configuración, mobs reforzados, cancelación completa de hordas, spawns junto a
paredes/techos y fuera de límites, argumentos de comandos y consumo del amuleto.
También ejecuta las pruebas existentes de Quaso. Las pruebas usan colaboradores
Bukkit simulados; falta confirmar el comportamiento real en el servidor Paper
con su configuración, terreno, resource pack y otros plugins.

Para comprobarlo allí: `/bloodmoon start`, generar una horda sin amuleto, activar
el amuleto con diamantes y repetir la horda; después `/bloodmoon stop`. Verificar
las misiones, los fragmentos del Día 1 y los mensajes. Probar también reiniciar
con la noche activa y generar hordas en zonas estrechas.
