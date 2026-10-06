# Cielo y tormenta de BloodMoon en 26.2

La BloodMoon se activa al llegar la noche (13000 ticks). Al empezar coloca el
mundo en la noche de 19000 ticks y pausa únicamente `minecraft:overworld`:
el cielo conserva esa hora durante todo el evento. Un contador independiente
basado en ticks del servidor mantiene la duración restante de la noche (10000
ticks, unos 8 minutos y 20 segundos, si empieza a las 13000). Al agotarse,
termina el evento, lleva el mundo al amanecer de 23000 ticks y reanuda el reloj.
Los fades usan
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

La capa mezcla al 80 % cielo `#ff1008`, niebla `#ff180c`, nubes `#e82010` y
factor visual de luz 0.30. Toma como referencia la intensidad que tenía el fade
anterior unos dos segundos después de detener el evento. Con los valores
normales del mundo, la niebla empieza a 76,8 bloques y se extiende hasta 819,2;
el final de niebla del cielo queda a 512 y el de nubes a 1228,8. Al acabar, cada
atributo recupera el valor del mundo y sus biomas. La capa visual no modifica
biomas, chunks ni bloques. La bossbar omite su oscurecimiento adicional.

## Lluvia, brillo y luna

Minecraft aplica el filtro de tormenta después de las timelines: a intensidad
máxima desatura el cielo un 94 %. Además, `SkyRenderer` usa `1 - rainLevel` como
brillo del sol y la luna, por lo que lluvia a 1 hace desaparecer la luna aunque
se aleje toda la niebla. Al detener BloodMoon, el filtro se retiraba junto con
la lluvia; por eso el cielo se veía más rojo y luminoso durante el fade.

Quaso conserva la tormenta real y ajusta únicamente sus paquetes visuales para
los jugadores del mundo afectado. Con el efecto completo, los niveles visuales
de lluvia y truenos se multiplican por 0,60: sigue lloviendo, el filtro gris es
menor y la luna deja de tener brillo cero (pasa a 0,40 por ese factor). Los rayos,
las reglas del clima y el comportamiento de los mobs conservan el clima real.
El brillo final también depende de la fase lunar, la posición y el cliente.

La intensidad del ajuste acompaña los fades de 10 segundos. Se refresca al
entrar, reaparecer y cambiar de mundo; se retira al acabar o apagar el plugin.
No modifica otros paquetes ni sobrescribe el clima personal de otros plugins.
No reenvía paquetes repetidos durante la fase estable. El puente de paquetes
está aislado para Paper 26.2 y usa el Netty que ya proporciona el servidor; no
requiere instalar otro plugin. Si no está disponible, avisa una vez y conserva
el clima normal.

El datapack por sí solo no evita la desaparición de la luna con lluvia visual
al máximo. La textura de la luna tampoco cambia sin un resource pack. La
apariencia final y los shaders instalados deben comprobarse con un cliente 26.2.

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
persistida se reanuda en el mismo día, también en su hora fija, con los
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
