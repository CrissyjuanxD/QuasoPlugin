# Anuncios, mantenimiento, scoreboard y homes

Se trasladan `Handlers/AutoAnnouncer`, `MantenimientoHandler` y
`MainScoreboard` desde `CrissyjuanxD/IsManuSMP`, commit
`cd024fe4b087a3244b60c7ebb3aeb65242b87d7c`, a QuasoPlugin `26.2`.

## Anuncios

Se conservan los siete mensajes originales, incluido su texto sobre
ManuCoins, el formato y el sonido. El intervalo normal sigue siendo de ocho
minutos. `/autoanuncio` pausa ese ciclo, recorre la lista cada treinta segundos
y reanuda el intervalo normal al terminar.

El fallo se debía al retorno anticipado cuando no había jugadores: aumentaba
el índice sin aplicar el reinicio de la lista. Tras suficientes ciclos vacíos,
el siguiente anuncio con jugadores producía un `IndexOutOfBoundsException` y
Bukkit detenía la tarea. Ahora el índice vuelve al inicio también con el
servidor vacío. Se cancela la tarea al apagar el plugin.

Permiso: `viciont_hardcore3.command.autoanuncio`, tanto en el comando como en el
handler, sin exigir además el permiso de IsManuSMP.

## Mantenimiento

`/mantenimiento on` guarda `mantenimiento.activo: true`, expulsa a los jugadores
que no son operadores y rechaza sus próximas conexiones. Los operadores pueden
entrar y reciben el recordatorio del modo activo. `/mantenimiento off` vuelve
a permitir la entrada. El estado se conserva al reiniciar y se actualiza con
`/quasoreload`. Se mantiene el mensaje de expulsión de IsManuSMP.

Permiso: `viciont_hardcore3.mantenimiento`.

## Scoreboard

Muestra Usuario, Rango, Misiones completadas/registradas, DinoCoins,
`Trabajo: Pendiente` y `croissant.holy.gg`. Trabajo es solamente un texto:
no se añade un sistema de trabajos. Las DinoCoins proceden de la caché del
registro de monederos, sin ejecutar consultas MySQL en cada actualización del
scoreboard. Las misiones y rangos utilizan los sistemas existentes de Quaso.

El scoreboard se actualiza cada segundo, sincroniza las propiedades y los
miembros de los equipos principales y cede el lateral cuando hay un scoreboard
de evento. Al finalizar el evento y restaurar el principal, vuelve a mostrarse.

El título y los iconos son glifos de uso privado para reemplazar con los del
resource pack. Se añaden las claves que falten sin sobrescribir la
configuración existente. Cambia sus valores en `config.yml` y usa
`/quasoreload`:

| Clave | Unicode inicial |
| --- | --- |
| `main-scoreboard.titulo` | `\uE900` |
| `main-scoreboard.iconos.usuario` | `\uE901` |
| `main-scoreboard.iconos.rango` | `\uE902` |
| `main-scoreboard.iconos.misiones` | `\uE903` |
| `main-scoreboard.iconos.dinocoins` | `\uE904` |
| `main-scoreboard.iconos.trabajo` | `\uE905` |
| `main-scoreboard.iconos.ip` | `\uE906` |

En YAML puedes usar, por ejemplo, `titulo: "\uE900"`. El resource pack define
la apariencia de cada glifo; el plugin solamente muestra el carácter.

## Homes

Los jugadores normales mantienen la espera de cinco segundos. Un cambio de
posición o mundo, daño real, desconexión o apagado del plugin cancela la tarea
pendiente. Girar la cámara y el daño cancelado no interrumpen la espera.

Además de escuchar los eventos, cada tick de la cuenta atrás comprueba que la
petición siga vigente y que el jugador siga en el punto inicial. Un callback
antiguo o cancelado no puede ejecutar el TP. Pedir otro `/home` reemplaza la
petición anterior. Los operadores se teletransportan inmediatamente, sin
programar la cuenta atrás.

## Comprobaciones

`mvn clean verify` con Java 25 ejecuta los tests de los ciclos vacíos y la ráfaga
de anuncios, el mantenimiento y su persistencia, el contenido y configuración
del scoreboard, la convivencia con los eventos, el tiempo del TP, los
operadores y todas las cancelaciones de homes. También conserva los tests de
monederos, item models y GUI de misiones.

La comprobación visual con el resource pack y los comandos en Paper 26.2 sigue
pendiente en el servidor. Para reproducir el fallo de anuncios ya no hace falta
esperar horas: el test simula veintiún ejecuciones sin jugadores y vuelve a
conectar uno. Para homes, los tests ejecutan incluso el callback ya cancelado
y comprueban que no teletransporte al jugador.
