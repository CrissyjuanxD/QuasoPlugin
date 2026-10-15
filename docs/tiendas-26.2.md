# Tiendas automáticas (26.2, Version #95)

El catálogo está en `ShopSystem/CatalogoTienda.java`. Las secciones se abren a mano con `/tienda dia <n>`, igual que
antes.

## Sin anuncio en el chat

`/tienda dia` y `/tienda seccion` ya no mandan el mensaje global «¡La tienda del spawn se actualizó!». Al admin le
sigue saliendo cuántos aldeanos se actualizaron. El anuncio lo hace el admin a mano.

## Lo que ya no se vende

No se vende nada que se farmee en las dimensiones o de los jefes para conseguir equipo. Si no, los jugadores que
entren después no tendrían motivo para farmear. Se quitaron:

| Tienda | Items quitados |
|---|---|
| Pociones | Baya Sculk, Fruta Abisal y Baya Luminosa (frutas de la Warden Cave) |
| Explorador | Las 4 almas infestadas (van en las mejoras de la armadura de Warden), Fragmento Astral y Esencia Marchita (End: Celestita y Ojo del Rey) |
| Legendario | Energía de Warden, Corazón de Warden Boss (invoca al Ultra Warden) y Ojo del Rey Ender (invoca al Rey Ender) |

Una prueba revisa que ninguno de estos ni las armaduras, herramientas o minerales vuelvan al catálogo.

## Segundo lugar del tradeo

La mayoría de los tradeos piden DinoCoins en el primer lugar y un item en el segundo (lo de `.con(...)` en el
catálogo). Algunos ejemplos:

- La mochila pide bundles.
- El tótem doble pide un tótem de la inmortalidad.
- Los tragos del bar piden una botella vacía.
- El amuleto de la BloodMoon pide fragmentos de BloodMoon.

El item extra se queda en todas las secciones y no sube de precio: solo suben las DinoCoins. Se quita únicamente
cuando el precio pasa de 64 (por la subida de 15% por sección, un ajuste o un precio a mano), porque entonces las
DinoCoins ocupan los dos lugares (Version #97).

Si se pide un item vanilla (un bundle, un tótem, un arco...), no cuenta un item custom del mismo material ni uno
encantado o con nombre. Así no se cobra una mochila (que es un echo shard) ni un tótem custom por error.
`/tienda precios` muestra el extra al lado del precio.

## Mochilas

Minecraft guarda poco por jugador, así que las mochilas salen antes:

| Mochila | Sección | Precio | Extra |
|---|---|---|---|
| Nivel 1 | 1 (día 1) | 15 | 1 bundle |
| Nivel 2 | 1 (día 1) | 30 | 2 bundles |
| Nivel 3 | 1 (día 1) | 45 | 4 bundles |
| Nivel 4 | 2 (día 20) | 60 | 6 bundles |
| Nivel 5 | 3 (día 30) | 64 | 2 shulker shells |
