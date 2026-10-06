# Cielo de BloodMoon en 26.2

Minecraft 26.2 permite colorear el cielo y la niebla mediante atributos ambientales
y timelines del datapack. Quaso añade `quaso:bloodmoon_sky` al final del tag
`minecraft:in_overworld`, después del ciclo visual vanilla. Así el cielo permanece
rojo de noche, en lugar de multiplicarse por el negro nocturno.

El reloj independiente `quaso:bloodmoon` queda pausado: el tick 0 conserva los
valores originales con una mezcla de alfa cero; el tick 1 aplica cielo rojo pastel,
niebla roja, nubes naranjas y una iluminación visual nocturna más visible. No se
modifican biomas, chunks, bloques, la hora del mundo ni la iluminación utilizada
por las reglas de aparición de mobs. Paper mantiene el reloj por mundo y
sincroniza sus cambios con los jugadores de ese mundo.

Se requiere `time.affects-all-worlds: false`, el valor predeterminado de Paper.
Con `true`, Paper comparte los relojes entre todos los mundos; Quaso desactiva
solo este efecto y emite un aviso una vez. La comprobación utiliza la API pública
de configuración de Paper, que resuelve también su ruta de configuración
personalizada, y no modifica esa configuración.
El reloj propio sí se pausa y se pone en 0 en este caso: impedir la activación
sin detener un reloj recién registrado dejaría aplicada su capa roja de forma
permanente. Ningún otro reloj se modifica.

`BloodMoonSkyEnabled: true` se configura por mundo en
`plugins/QuasoPlugin/bloodmoon/<mundo>/config.yml`. Cuando el efecto está disponible
y activo, se omiten `DARKEN_SKY` y la niebla de la bossbar, y se mantiene el cielo
despejado aunque `ThunderDuringBloodMoon` esté activado. La tormenta vanilla
desatura casi todo el rojo; la lluvia también oscurece y atenúa la niebla. Al
terminar, el reloj vuelve a 0 y se conserva el intervalo de clima despejado. Si se
desactiva el efecto o no está cargado el datapack, se utiliza el clima configurado
habitual de BloodMoon.

La instalación conserva el resto del datapack QuasoPlugin. Después de actualizar
el JAR, hace falta reiniciar una vez tras el aviso de actualización del datapack:
los registros de relojes y timelines se cargan al iniciar Minecraft. Hasta entonces
el plugin detecta que falta el reloj y continúa sin ejecutar comandos inválidos.
`/bloodmoon reload` aplica la opción por mundo; no sustituye ese reinicio inicial.
Los mundos excluidos y los que cargan después también reciben el reloj pausado en
0, para que no hereden un cielo rojo guardado. Al reanudar una BloodMoon persistida,
se activa de nuevo únicamente su mundo.

El efecto usa las capacidades visuales de clientes Minecraft 26.2. El color de la
textura de la luna en sí sigue siendo el del cliente; para cambiar esa textura
haría falta un resource pack. Los clientes anteriores conectados mediante
traductores de protocolo pueden no mostrar estos nuevos atributos.

Referencias oficiales:

- [Minecraft Java 1.21.11: atributos, modificadores y timelines](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)
- [Minecraft Java 26.1: relojes del mundo y comandos de tiempo](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-1)

La estructura también se comprobó contra las definiciones y las clases incluidas
en Paper 26.2 build 129.

La prueba interna sobre ese Paper confirma reloj pausado en 1, cielo `#ef9292`,
niebla `#d46c64` y factor de luz visual `0.45` durante BloodMoon. Al finalizar
vuelve a 0 y recupera los atributos normales. Dos arranques comprueban la
restauración del evento; `wardencave` mantiene su reloj pausado en 0.
La apariencia final y las transiciones se deben comprobar con un cliente 26.2.
