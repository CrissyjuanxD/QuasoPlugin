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
