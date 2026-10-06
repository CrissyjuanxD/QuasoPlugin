# BloodMoon dentro de Quaso 26.2

El paquete `BloodMoon` y su clase principal `BloodMoon` integran la lógica de
BloodMoon 0.8.1 de SpectralMemories, recuperada del JAR que había en `libs`.
Conserva el ciclo por mundo, avisos, barra nocturna, clima,
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
Los valores predeterminados se leen desde `bloodmoon-defaults` dentro del JAR;
esa carpeta interna no cambia la ubicación de la configuración del servidor.
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

La BloodMoon comienza de noche y fija el cielo en el amanecer de 23000 ticks.
Su contador independiente conserva la duración restante de la noche y devuelve
el movimiento del tiempo al terminar. `PermanentBloodMoon` la activa cada noche.
El cielo y la niebla carmesí ligera tienen transiciones de 10 segundos; ver [cielo y tormenta](bloodmoon-cielo-26.2.md).

Cuando BloodMoon controla el clima (`ThunderDuringBloodMoon: true`), al terminar
desactiva lluvia y truenos y mantiene el cielo despejado durante 12.000 ticks.
También lo limpia al descargar el mundo o recargar/desactivar el sistema. No
restaura la tormenta anterior: tras un reinicio esa tormenta podía ser la propia
lluvia de BloodMoon y quedarse activa al terminar. Si el control del clima está
desactivado durante toda la noche, se conserva el clima natural del mundo.

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

Al bloquear una horda aparece el aviso con `►` gris, nombre azul
claro, texto azul y título del amuleto rojo. Se anuncia a los jugadores conectados,
suena una selección de baliza para el portador y aparece una espiral de partículas
alrededor de él. El aviso, sonido y animación tienen un límite de una vez cada
cinco segundos; todas las hordas protegidas siguen cancelándose dentro de ese plazo.

El indicador continuo del amuleto usa la cola compartida de `ActionBarHandler`.
Cede mientras aparecen avisos y vuelve a su estado más reciente cuando termina
la cola. Desactivarlo elimina solo su indicador, conservando los mensajes de
misiones visibles o pendientes.

Los mensajes están en español con naranja `#F4B183`, rojo `#EF9292`, melocotón
`#FFD2AE` y coral `#F7AAA1`. Se resaltan nombres, avisos y recompensas dentro de
esa paleta. Las respuestas privadas de comandos usan verde lima pastel y
los separadores `►` son grises. Las action bars comienzan con `۞`, el mismo símbolo de las misiones;
sus textos de inicio/final y horda también se pueden editar en `mensajes.yml`.
El aviso de inicio dice:

> Bloodmoon ►
>
> Ha empezado una BloodMoon.
> Los monstruos son más fuertes y las hordas acechan durante la noche.
> Derrota monstruos para conseguir más experiencia, botín y fragmentos de BloodMoon.

## Mensajes que no aparecen

Se ha corregido la lectura de los valores predeterminados: los mensajes están
disponibles desde el primer arranque y las claves nuevas o ausentes se completan
al recargar, conservando las personalizaciones y `%void%` para silenciar un aviso.
Antes, el archivo se guardaba correctamente, pero los valores añadidos no se
utilizaban hasta volver a leerlo.

En la versión anterior, ejecutar `/bloodmoon reload` después del primer arranque
permite leer los mensajes que ya se han guardado. Para aplicar la corrección
definitiva, reemplazar el JAR y reiniciar el servidor. No hay que borrar ni crear
manualmente `plugins/QuasoPlugin/bloodmoon/mensajes.yml`. Si se personaliza ese
archivo, `/bloodmoon reload` aplica los cambios.

La paleta nueva se aplica también a archivos existentes: únicamente se reemplazan
los textos que coincidan exactamente con los mensajes predeterminados anteriores.
Los mensajes personalizados y `%void%` permanecen intactos; no hace falta borrar
el archivo para recibir los colores nuevos.

## Validación

`mvn clean verify` con Java 25 comprueba calendario y reinicios, importación de
configuración, mobs reforzados, cancelación completa de hordas, spawns junto a
paredes/techos y fuera de límites, argumentos de comandos y consumo del amuleto.
También comprueba los mensajes del primer arranque, archivos incompletos,
recargas y disponibilidad de los valores predeterminados si falla el guardado.
Comprueba la migración de la paleta sin sobrescribir mensajes personalizados,
el despejado al amanecer/final, las recargas del clima y una noche reanudada.
También ejecuta las pruebas existentes de Quaso. La suite completa tiene 117
pruebas correctas. El servidor interno Paper 26.2-129 con MySQL completó dos
arranques y comprobó el clima, los atributos reales de cielo/niebla, la vuelta
al estado normal, el aislamiento entre mundos y la restauración después de
reiniciar. Comprueba también el amanecer fijo, su contador independiente,
los fades de 10 segundos y que el reloj diurno se reanude al terminar. Los diálogos reales de homes también se serializaron correctamente.
El comportamiento con jugadores, su terreno, resource pack y otras integraciones
requiere probarse en el cliente.

Para comprobarlo allí: `/bloodmoon start`, generar una horda sin amuleto, activar
el amuleto con diamantes y repetir la horda; después `/bloodmoon stop`. Verificar
las misiones, los fragmentos del Día 1 y los mensajes. Probar también reiniciar
con la noche activa y generar hordas en zonas estrechas.

## Compilación en Windows e IntelliJ

Las clases están en el paquete `BloodMoon`, al mismo nivel que los demás paquetes,
y los recursos predeterminados del JAR están en `bloodmoon-defaults`.
La antigua separación `BloodMoon` / `bloodmoon` no funcionaba al compilar en
Windows: el sistema fusionaba las carpetas y el JAR podía guardar las clases
como `bloodmoon/BloodMoon.class`, aunque Java buscara `BloodMoon/BloodMoon.class`.

Después de actualizar esta corrección, ejecutar `mvn clean verify` con Java 25 y
usar el JAR de `target`. Si se utiliza el artefacto de IntelliJ, ejecutar antes
**Build → Rebuild Project** y reconstruir el artefacto para descartar las clases
antiguas. Se puede mantener el nombre `QuasoPlugin-26.2.jar` en el servidor;
el nombre del archivo no cambia la carga de clases. Reemplazar el JAR anterior
y reiniciar el servidor.

La prueba `PluginArtifactTest` comprueba que las carpetas compiladas no colisionen
por mayúsculas y carga la clase nativa desde un JAR construido simulando las
rutas de Windows. Esta prueba reprodujo el `ClassNotFoundException` original
antes de separar las rutas de clases y recursos.
