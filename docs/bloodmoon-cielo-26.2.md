# Cielo y tormenta de BloodMoon en 26.2

La BloodMoon comienza a los 13000 ticks, cuando ya es de noche, y termina a los
23000, al empezar el amanecer. Se mantiene durante ese tramo del ciclo natural;
no congela ni acelera el día. `/bloodmoon start` coloca el mundo al principio de
la noche; `/bloodmoon stop` lo lleva al amanecer. `PermanentBloodMoon` significa
una BloodMoon cada noche, y también termina de día.

`ThunderDuringBloodMoon: true` mantiene lluvia y truenos durante el evento,
también cuando está activado el cielo rojo. Al terminar, Quaso retira su tormenta
y deja 12000 ticks despejados. Las transiciones normales de lluvia de Minecraft
siguen aplicándose. Si se desactiva explícitamente esta opción, el efecto visual
no controla el clima natural.

## Rojo y transiciones

El datapack añade `quaso:bloodmoon_sky` al tag `minecraft:in_overworld`, después
del ciclo visual vanilla. Su reloj independiente `quaso:bloodmoon` usa tres
puntos: 0 sin efecto, 600 con efecto completo y 1200 sin efecto. Al empezar corre
hasta 600 y se pausa; al terminar corre hasta 1200 y vuelve a quedar pausado en 0.
Cada transición dura 600 ticks, unos 30 segundos a 20 TPS. El cliente interpola
los colores a medida que avanza el reloj, sin saltar directamente al rojo.
Recargar o invertir una transición mantiene su intensidad actual y cancela la
tarea anterior. Descargar un mundo o apagar el plugin restablece inmediatamente
su reloj para que no quede un efecto huérfano.

La capa aplica cielo `#ff1008`, niebla `#ff180c`, nubes `#e82010` y factor visual de
luz 0.30. Acerca gradualmente la niebla del horizonte: inicio a 32 bloques, final
a 160, final de niebla del cielo a 48 y de nubes a 64. Al acabar, cada atributo
recupera el valor del mundo y sus biomas, en vez de fijar un color diurno propio.
No se modifican biomas, chunks, bloques ni la luz de las reglas de aparición de
mobs. La bossbar omite su oscurecimiento adicional cuando funciona este efecto.

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
por mundo. La lluvia y el ciclo nocturno siguen funcionando sin este efecto.

Después de actualizar el JAR, reinicia otra vez si Quaso avisa de que ha copiado
una nueva versión del datapack: sus registros se leen al arrancar Minecraft.
`/bloodmoon reload` recarga la configuración y los mensajes, pero no sustituye
ese reinicio. Los mundos excluidos conservan su reloj en 0; una BloodMoon
persistida solo se reanuda si todavía es de noche.

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
