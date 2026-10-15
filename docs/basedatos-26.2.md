# /basedatos: la base de datos en diálogos (26.2)

`/basedatos` (o `/bd`) abre toda la base de datos MySQL en diálogos. Es solo para OPs: el permiso `quaso.basedatos`
viene en `op`, y cada botón vuelve a revisarlo antes de hacer algo. Desde la consola solo avisa que se usa en el juego.
El código está en `BaseDatos/` (`BaseDatosDialogos`, `BaseDatosSQL` y `BaseDatosCommand`).

## Menú principal

- Arriba sale el estado: si está conectado, cuánto tarda en responder, la versión de MySQL, cuántas tablas y filas hay
  y cuánto pesa todo.
- Un botón por tabla con su número de filas. Las de QuasoPlugin salen primero, en color crema. Las de otro plugin o de
  la web salen en gris. Al pasar el mouse dice qué pasa en el juego si se borran sus filas.
- **Estado**: servidor y puerto, base, usuario, versión, cuánto tarda en responder, hace cuánto está encendido MySQL,
  conexiones abiertas, filas y tamaño de cada tabla, y las tablas del plugin que falten.
- **Recargar**: hace lo mismo que `/quasoreload`. Recarga el config.yml, el mantenimiento y la BloodMoon, y vuelve a
  conectar MySQL creando las tablas que falten. La conexión se rehace en otro hilo para que el server no se trabe si
  MySQL no responde.
- **Reset total**: lo explica más abajo.
- Si no hay conexión, sale el error con los botones Recargar y Reintentar.

## Una tabla

- Se ven 8 filas por página con Anterior y Siguiente. Arriba salen las columnas con su tipo, y la clave lleva 🔑.
- Cada fila es un botón con el nombre del jugador (si la tabla tiene) y la clave. Al pasar el mouse muestra sus
  primeras columnas.
- **Buscar**: busca un texto en todas las columnas, por ejemplo el nombre o el UUID de un jugador. No busca dentro de
  los inventarios guardados.
- **Añadir fila**: un campo por columna. Si lo dejas vacío toma el valor por defecto, y `NULL` lo deja nulo. Las
  columnas sí/no (BOOLEAN) son un interruptor.
- **Copias de seguridad**: las copias de esa tabla y el botón «Guardar copia ahora».
- **Vaciar tabla**: lo explica más abajo.

## Una fila

Muestra todas sus columnas. Los valores largos se cortan y dicen cuántos caracteres tienen.

- **Editar**: el formulario sale con los valores de ahora y solo se guardan las columnas que cambiaste. Se revisa lo
  que escribes antes de guardar: los números tienen que ser números, los sí/no tienen que ser 1 o 0, NULL solo va
  donde se permite y los varchar tienen su largo máximo. Si algo está mal te dice qué y no guarda nada. Las columnas
  de más de 512 caracteres (inventarios y mochilas en base64) no se editan aquí. Si cambias la clave, la fila se vuelve
  a abrir con la clave nueva.
- **Borrar fila**: pide confirmación.

Si MySQL rechaza algo (por ejemplo una clave repetida), el error sale en el diálogo.

## Vaciar una tabla y reset total

- **Vaciar tabla**: tienes que escribir el nombre exacto de la tabla. Si lo escribes mal no se borra nada.
- **Reset total**: vacía las 7 tablas de QuasoPlugin (`players`, `player_missions`, `global_missions`, `player_jobs`,
  `player_job_state`, `player_backpacks` y `event_inventories`). Pide dos confirmaciones: primero escribir `RESET` y
  después «Sí, borrar todo». Las tablas de otros plugins no se tocan. Después del reset cada jugador cuenta como nuevo
  al volver a entrar (DinoNugget y kit de bienvenida).
- Antes de borrar, cada tabla se guarda en `plugins/QuasoPlugin/basedatos-copias/<tabla>/<fecha>_<filas>filas.json`.
  Si la copia no se puede guardar, no se borra nada. Se quedan las últimas 10 de cada tabla.
- **Restaurar** (en Copias de seguridad): vuelve a poner las filas de la copia. Las que tienen la misma clave se
  reemplazan y las demás filas de la tabla se quedan como están.
- Todo lo que se edita, añade, borra, vacía o restaura queda en la consola con el nombre del OP (`[BaseDatos] ...`).

## Misiones y trabajos con jugadores conectados

Las misiones y los trabajos guardan el progreso en memoria y lo pasan a MySQL cada pocos minutos. Si se editara la
tabla directamente, el siguiente guardado pisaría lo editado. Por eso, al editar, borrar, vaciar o restaurar
`player_missions`, `global_missions`, `player_jobs` o `player_job_state`:

1. Primero se guarda lo pendiente de todos.
2. Se aplica el cambio sin que nadie más guarde mientras tanto.
3. Se vuelven a cargar las misiones activas y los datos de los conectados.

Se probó en el server de prueba: un jugador tenía 11 mobs en la misión 214 sin guardar y se editó a 25. Quedó en 25 y
la siguiente muerte la subió a 26, también en MySQL. Un nivel 40 de minería añadido a mano se vio al instante en el
jugador conectado.

## Probado

En un Paper 26.2 de prueba con MariaDB 10.11 se probaron todas las ventanas:
- Ver tablas, páginas, búsqueda, fila y estado.
- Editar con valores buenos y malos, editar sin cambios, añadir una fila con interruptor y con clave repetida, y borrar.
- Vaciar escribiendo mal y bien, copia, restaurar y guardar copia.
- El reset con `reset` en minúsculas (no borra) y con `RESET` (borra las 7 tablas y deja la de otro plugin).
- Las misiones y los trabajos antes y después del reset, y Recargar.

## Contraseña con @ en config.yml (Version #98)

En YAML un valor no puede empezar con `@`, `` ` `` ni `%` sin comillas. Paper no podía leer `config.yml`, el plugin
seguía con una config vacía y al prender la guardaba encima (por ejemplo el límite de mobs de la Warden Cave), así que
se borraban la base de datos y lo demás.

- Antes de leer `config.yml` el plugin le pone comillas simples a esos valores y avisa en la consola qué clave arregló
  (sin mostrar la contraseña). `password: @abc` queda `password: '@abc'`.
- Si `config.yml` igual tiene un error, el plugin ya no lo guarda hasta que se arregle y se use `/quasoreload`. Así no
  se pierde nada.
