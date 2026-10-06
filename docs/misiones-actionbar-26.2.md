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
