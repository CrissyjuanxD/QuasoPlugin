# Cielo y tormenta de BloodMoon en 26.2

La BloodMoon se activa al llegar la noche (13000 ticks). Al empezar coloca el
mundo en el amanecer de 23000 ticks y pausa únicamente `minecraft:overworld`:
el cielo conserva esa luz durante todo el evento. Un contador independiente
basado en ticks del servidor mantiene la duración restante de la noche (10000
ticks, unos 8 minutos y 20 segundos, si empieza a las 13000). Al agotarse,
termina el evento y reanuda el reloj del día desde ese amanecer. Los fades usan
su propio reloj; no se desactiva `advance_time`, que también los congelaría.

`/bloodmoon start` inicia una noche completa; `/bloodmoon stop` la termina.
Recargar o reiniciar conserva el tiempo restante sin comenzar otra noche entera.
Al descargar el mundo o apagar el plugin se libera la pausa. `PermanentBloodMoon`
significa una BloodMoon cada noche y también termina al agotarse su contador.

`ThunderDuringBloodMoon: true` mantiene lluvia y truenos durante el evento,
también cuando está activado el cielo rojo. Al terminar, Quaso retira su tormenta
y deja 12000 ticks despejados. Las transiciones normales de lluvia de Minecraft
siguen aplicándose. Si se desactiva explícitamente esta opción, el efecto visual
no controla el clima natural.

## Rojo y transiciones

El datapack añade `quaso:bloodmoon_sky` al tag `minecraft:in_overworld`, después
del ciclo visual vanilla. Su reloj independiente `quaso:bloodmoon` usa tres
puntos: 0 sin efecto, 200 con efecto completo y 400 sin efecto. Al empezar corre
hasta 200 y se pausa; al terminar corre hasta 400 y vuelve a quedar pausado en 0.
Cada transición dura 200 ticks, unos 10 segundos a 20 TPS. El cliente interpola
los colores a medida que avanza el reloj, sin saltar directamente al rojo.
Recargar o invertir una transición mantiene su intensidad actual y cancela la
tarea anterior. Descargar un mundo o apagar el plugin restablece inmediatamente
su reloj para que no quede un efecto huérfano.

La capa aplica cielo `#ff1008`, niebla `#ff180c`, nubes `#e82010` y factor visual de
luz 0.30. La niebla empieza a 96 bloques y se extiende hasta 768, con el final de
niebla del cielo a 512 y de nubes a 1024. Así conserva un horizonte rojizo suave
sin la cortina cercana que tapaba el cielo. Al acabar, cada atributo
recupera el valor del mundo y sus biomas, en vez de fijar un color diurno propio.
La capa visual no modifica biomas, chunks ni bloques. El reloj diurno sí queda
en el amanecer elegido durante el evento. La bossbar omite su oscurecimiento adicional cuando funciona este efecto.

**Límite de vanilla:** la tormenta modifica los atributos después de las
timelines: desatura el cielo un 94 % y oscurece la niebla. El datapack refuerza
el rojo con niebla carmesí, que conserva su saturación; no elimina ese filtro
del cielo. Por eso el resultado con tormenta es un rojo oscuro, y no se puede
prometer un cielo rojo luminoso como el de un shader solo con estos atributos.
La textura de la luna tampoco cambia sin un resource pack. La apariencia final
y los shaders instalados deben comprobarse con un cliente 26.2.

## Instalación

`BloodMoonSkyEnabled: true` se configura por mundo en
`plugins/QuasoPlugin/bloodmoon/<mundo>/config.yml`. Se requiere
`time.affects-all-worlds: false`, el valor predeterminado de Paper. Si está en
`true`, Quaso avisa y restablece/pausa su reloj, porque no puede aislar el efecto
por mundo. La lluvia y el contador del evento siguen funcionando, pero no se pausa un
reloj compartido entre mundos.

Después de actualizar el JAR, reinicia otra vez si Quaso avisa de que ha copiado
una nueva versión del datapack: sus registros se leen al arrancar Minecraft.
`/bloodmoon reload` recarga la configuración y los mensajes, pero no sustituye
ese reinicio. Los mundos excluidos conservan su reloj en 0; una BloodMoon
persistida se reanuda en el mismo día, también en su amanecer fijo, con los
ticks pendientes guardados en `estado.yml`.

Las flechas de los mensajes son `►` en gris. Las respuestas privadas a comandos
usan verde lima pastel; los avisos globales y normales conservan naranja, rojo,
melocotón y coral. Los action bars conservan `۞`. Los mensajes predeterminados
anteriores se migran; las ediciones manuales y `%void%` se respetan.

Referencias oficiales:

- [Minecraft Java 1.21.11: atributos, modificadores y timelines](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- [Minecraft Java 26.1: relojes del mundo y comandos de tiempo](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1)

La estructura y el filtro de tormenta también se contrastaron con las clases
incluidas en Paper 26.2 build 129. El comprobador interno verifica los relojes,
los valores ambientales y el clima; no sustituye una inspección visual del cliente.
