# Monederos y registro de monedas

Migración desde `CrissyjuanxD/Viciont-Hardcore-3`, commit
`4173aec1601e1b1c06c3c7842011c77f74ed8d96`:

- `Gui/vithiums/VithiumsManager` y `VithiumsCommand` se adaptan a
  `Gui/dinocoins/DinoCoinsManager` y `DinoCoinsCommand`.
- `EconomyItems.createMonedero()` y las restricciones de
  `EconomyItemsFunctions` reutilizan las mochilas y `player_backpacks`.
- El monedero conserva el identificador 2025 y tiene **18 slots**. En Quaso se
  registra con `item_level = 6`, para distinguirlo de las mochilas incluso
  después de renombrarlo. Tiene UUID y no se apila. Usa directamente el item
  model `minecraft:monedero`, como las demás fábricas de ítems.
- El saldo físico se cuenta una vez por UUID, consultando los monederos
  registrados del usuario, inventario, ender chest, contenido abierto y caché.
  Como en Viciont, los monederos registrados también cuentan cuando están
  guardados en cofres; se conserva la asociación al propietario registrada en
  `player_backpacks`.
- Al iniciar se añaden `players.dinocoins` y `players.dinofichas`, ambas enteras
  y con valor inicial cero. El registro refleja las monedas **dentro de los
  monederos**, sin sumar monedas sueltas del inventario. Se actualiza al entrar,
  modificar/cerrar el monedero, salir y cada 10 segundos.
- No se trasladan los HUD, sus comandos ni la dependencia de ViciontGui.

El monedero admite DinoCoins y DinoFichas; rechaza objetos comunes y otras
mochilas/monederos. Los cofres y las mochilas normales rechazan ambas monedas
por clic, shift, teclas numéricas, intercambio con la segunda mano, arrastre y
tolvas. Se permite retirar monedas antiguas de otros contenedores para pasarlas
al monedero. El inventario personal permite transportarlas, como en Viciont.
Las DinoFichas siguen funcionando en el slot de apuesta del casino; también se
reconocen las fichas antiguas que todavía no tienen la nueva línea del lore.

Ambas monedas incluyen: **«Solo se puede almacenar en un monedero.»**

## Comandos

- `/giveqp monedero [cantidad] [jugador]`: cada monedero entregado se crea
  individualmente. Las tiendas también generan monederos individuales.
- `/dinocoins get <jugador>`: muestra las DinoCoins y DinoFichas de los
  monederos de un jugador conectado.
- `/dinocoins add <jugador> <cantidad>` y
  `/dinocoins remove <jugador> <cantidad>`: añaden/retiran DinoCoins físicas,
  como los comandos de Viciont. Permiso: `dinocoins.admin`. Las operaciones
  pueden ser parciales si falta espacio o saldo; el mensaje lo indica.
- `/mochilas` y `/delmochilas` incluyen los monederos en sus listados existentes.

## Validación

`mvn clean verify`, usando Java 25 y las opciones del entorno, ejecuta los tests
de inventario, capacidad, registro de ambas monedas, UUID duplicados,
actualizaciones asíncronas fuera de orden, conservación del tipo al mover el
objeto de la mano, lore y aspecto del monedero, además de los tests anteriores.

Falta la comprobación en el servidor Paper 26.2 con su MySQL: entregar un
monedero, abrirlo, guardar ambas monedas, consultar el saldo, renombrarlo,
cerrarlo, reconectar y comprobar el contenido y el registro. Probar también
una apuesta con una DinoFicha antigua y otra recién creada. El usuario de
MySQL configurado necesita permiso para añadir las dos columnas. El resource
pack debe incluir `assets/minecraft/items/monedero.json`.

## Arreglos de la Version #77

- **"Error guardando. Items en memoria"**: `ItemSerializer` usaba el `Base64Coder` de SnakeYAML, que no viene en Paper
  26.2. Cada guardado de mochila o monedero tiraba un `NoClassDefFoundError` y por eso no se arreglaba con una base de
  datos nueva. Ahora usa el Base64 de Java, que lee igual lo que se guardó antes. Probado en Paper 26.2 con MariaDB:
  se guardan y cargan mochilas con diamantes, equipo floral, herramientas de Celestita, libros encantados, un shulker
  con items adentro y el monedero.
- Las DinoCoins y las DinoFichas se apilan de a **99** (el juego no deja más de 99 por stack; 100 no se puede). Las
  de antes (de a 64) pasan a las de 99 al entrar al server, al abrir un cofre o el monedero y al levantarlas del
  suelo. Los tradeos guardados de la tienda también se pasan al cargarse, así siguen aceptando las monedas.
- Las mochilas y el monedero ya no brillan; a los que ya existían se les quita al abrirlos.

## Tiendas (Version #80)

En el menú de las tiendas ya no está la primera fila con «Precio #1», «Precio #2» y «Producto». Las flechas (pepitas)
y los espacios «Vacío» (structure void) no se ven, pero al pasar el mouse siguen mostrando su nombre. Todos los slots
que quedaban vacíos tienen un item invisible sin nombre ni lore, así no se puede meter nada.

En la Version #81 la barrera roja del producto vacío también quedó invisible; al pasar el mouse sigue diciendo «Vacío».
