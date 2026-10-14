# Rangos con unicode y kit de bienvenida (26.2)

Los rangos ya no usan etiquetas de texto como `[DinoNugget]`. Ahora usan los íconos del resource pack (`theranks:ranks/...`).
Todo sale de `Handlers/Teams/TeamType.java`.

| Rango | Team | Color del nombre | Chat y arriba de la cabeza | Tab |
|---|---|---|---|---|
| Admin | `Admin` | `#FDC42C` | `` admin.png | `` admin_tab.png |
| Mod | `Mod` | `#5FAEED` | `` mod.png | `` mod_tab.png |
| Helper | `THelper` | `#C241EF` | `` helper.png | `` helper_tab.png |
| DinoNugget | `ZMiembro` | `#FCD869` | `` dinonugget.png | `` dinonugget_tab.png |
| DinoNugget+ | `YMiembro` | `#F3EF8B` | `` dinonugget_plus.png | `` dinonugget_plus_tab.png |
| DinoLeyenda | `XLeyenda` | `#F3A85E` | `` dinoleyenda.png | `` dinoleyenda_tab.png |
| DinoSub | `USub` | `#BF8DF5` | `` dinosub.png | `` dinosub_tab.png |
| DinoVip | `VVip` | `#F38DF5` | `` dinovip.png | `` dinovip_tab.png |

Dónde se ve cada cosa:

- **Chat** (`chat/chatgeneral.java`): el ícono en blanco, el nombre en el color del rango y el mensaje en blanco.
- **Tab** (`list/VHList.java`): el ícono `_tab` en blanco y el nombre en el color del rango.
- **Arriba de la cabeza, logros y mensajes de muerte**: salen del team, que solo acepta los 16 colores de siempre.
  Por eso ahí va el ícono de chat y el nombre en blanco.
- Los equipos de eventos (LavaClash, ItemParty, HotPotato y BuildBattle) siguen con su etiqueta de texto.
- **Fantasma** (`ZFantasma`): no tiene ícono porque los que usaba ahora son de otros rangos. Su nombre sale gris.
- **Survivor** (`TSurvivor`): se quitó. Al arrancar, si alguien seguía en ese team pasa a DinoNugget.

## Rango por defecto

El que entra por primera vez queda como DinoNugget (`ZMiembro`). Si alguien que ya había entrado no tiene ningún
team, también queda como DinoNugget al entrar. Los de los equipos de eventos no se tocan.

## Kit de bienvenida

La primera vez que entras (`Handlers/FirstJoinHandler.java`) recibes:

- 1 bloque protector de oro de 16x16 que protege toda la altura. Lo crea la API de ViciontProtections
  (`createProtectionBlock(ProtectionBlock.square(GOLD_BLOCK, 16), 1)`).
- 20 panes.

Si el inventario está lleno, lo que no cabe cae al suelo.

La API se busca entre los servicios de Bukkit por su nombre y se llama por reflexión. Así QuasoPlugin compila sin el jar
de ViciontProtections. ViciontProtections va en `softdepend`. Si no está activo, el jugador recibe solo los panes y
sale un aviso en la consola.

Para poner el bloque, el jugador necesita el permiso `viciontprotections.user.create`. En ViciontProtections viene solo
para OP, así que hay que darlo al grupo por defecto (por ejemplo `viciontprotections.user.*` en LuckPerms).
