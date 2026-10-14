# Arcos (26.2)

## Arco Mejorado (Version #80)

Va con el cambio uno (`items/ArcoMejorado.java`). Se fabrica en la mesa de crafteo:

```
H N O
H B O
H   O
```

`H` bloque de hierro, `O` bloque de oro, `N` lingote de Netherite y `B` un arco normal. Solo sirven arcos sin
modificar: los arcos custom (explosivos, de hielo, el mismo Arco Mejorado) no entran. Los encantamientos del arco se
quedan en el Arco Mejorado. Está en `/giveqp arco_mejorado`.

Es el único arco que acepta en el yunque:

- **Perforación** (Piercing, hasta IV): la flecha atraviesa mobs como con la ballesta. En el servidor de prueba, con Perforación IV
  y Poder VI una flecha mató 4 zombies en fila.
- **Poder VI**: se junta un Arco Mejorado con Poder V y un libro de Poder V. Un libro de Poder VI también sirve. A los
  demás arcos el yunque les deja Poder V como tope.

El yunque (`Handlers/AnvilOverEnchantHandler`) sigue dejando poner libros por encima del nivel máximo, pero ahora
solo en los items que aceptan ese encantamiento y sin chocar con los que ya tienen (por ejemplo, Infinidad con
Reparación o Filo con Castigo). Antes dejaba poner cualquier libro en cualquier item.

El item model es `minecraft:arco_mejorado`. Ver [texturas](texturas-26.2.md#arco-mejorado-version-80).

## Arco Explosivo (Version #80)

Antes Infinidad solo ahorraba una flecha por disparo (3 → 2, 6 → 5 y 10 → 9) y casi no se notaba. Ahora gasta la
mitad:

| Nivel | Sin Infinidad | Con Infinidad |
|---|---|---|
| 1 | 3 | 1 |
| 2 | 6 | 3 |
| 3 | 10 | 5 |

El lore dice «⚠ Consume N flechas por disparo (con Infinidad, M)». Los arcos que ya existían cambian su lore la primera
vez que disparan. Sin flechas suficientes no dispara y no se pierde ninguna. Lo probé en el servidor de prueba con un
jugador falso.

## Arco de Hielo unificado (Version #83)

Solo existe `arco_hielo` en el catálogo, la tienda y los drops. Se elimina la fábrica de la versión para jugadores; el identificador antiguo `arco_hielo_jugador` queda únicamente como alias de compatibilidad hacia el mismo item, sin entrada duplicada en la GUI ni el autocompletado.

No hay cooldown entre disparos: se puede volver a tensar y disparar normalmente. Jugadores e Iceologer aplican el efecto del arco original: +160 ticks de congelación, Lentitud II durante 5 segundos y Fatiga Minera I durante 4 segundos. La cadencia de ataques del mob conserva su lógica propia. Los disparos y daños cancelados no aplican efectos de hielo.

Los arcos antiguos siguen funcionando porque conservan la misma identificación. Su lore guardado puede seguir indicando 3 segundos o cooldown; para obtener el texto actualizado se puede entregar una copia nueva con `/giveqp arco_hielo`. No se sobrescriben los nombres, lores personalizados ni encantamientos de los arcos existentes.

## Texturas (Version #83)

[Arcos nuevos: ZIP, vista y guía](../descargas/arcos%20nuevos/README.md). El Arco Mejorado usa la forma del explosivo con hierro, oro y Netherite. Los tres niveles del explosivo son rojos. Cada uno incluye reposo y tres fases al tensar, todos a 16×16, además de los JSON. También están integrados en `resourcepack/assets`.

## Arco de Sculk (Version #82)

`items/ArcoSculk.java`. Se hace en la herrería con un Núcleo Sónico del Ultra Warden, un arco normal o el Arco Mejorado
y una Energía de Warden. Se quedan los encantamientos del arco. Los arcos explosivos y los de hielo no sirven.

- Las flechas dejan una estela de sculk y suman 4 de daño sónico al pegar.
- Los Endermen no las esquivan. El arco sirve para el End: la flecha les pega con el mismo daño que hubiera hecho, Poder
  incluido, más los 4 sónicos. Al Rey Ender lo sigue manejando su propio boss.
- En el yunque acepta Perforación y Poder VI, como el Arco Mejorado.

El item model es `minecraft:arco_sculk`. Mientras no tenga textura usa el modelo del arco normal
(`resourcepack/assets/minecraft/items/arco_sculk.json`).
