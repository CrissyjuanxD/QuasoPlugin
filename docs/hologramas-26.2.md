# Hologramas (26.2)

Hologramas con Text Display, todo desde diálogos y solo para admins (permiso `quaso.hologramas`, por defecto OP).

## Comandos

| Comando | Qué hace |
|---|---|
| `/holograma` (`/holo`, `/hologramas`) | Menú con todos los hologramas, crear y guía |
| `/holograma crear` | Diálogo para crear uno |
| `/holograma editar <nombre>` | Menú de ese holograma |
| `/holograma posicion <nombre>` | Directo al diálogo de posición |
| `/holograma guia` | Guía de uso dentro del juego |
| `/holograma recargar` | Vuelve a leer `hologramas.yml` (también desde consola) |

## Cómo funciona

1. **Crear:** nombre (minúsculas, números, `-` y `_`) y texto. Aparece 2 bloques delante del admin como **borrador**: solo lo ven quienes tienen el permiso.
2. **✎ Texto:** cuadro de varias líneas. "Vista previa" muestra cómo queda sin guardar; "Guardar" lo aplica.
3. **⚙ Apariencia:**
   - **Fondo:** el gris de Minecraft, sin fondo o un color hex con su opacidad.
   - **Texto:** opacidad (10–100 %), sombra y verse a través de bloques.
   - **Hacia dónde mira:** siempre al jugador, solo a los lados, solo arriba y abajo, o fijo.
   - **Formato:** alineación, tamaño (0.25–10), brillo (-1 = luz del lugar, 0–15 fijo), ancho de línea y distancia de visión.
4. **✥ Posición:**
   - **Mover:** X, Y y Z con un paso de 0.05 a 2 bloques.
   - **Girar e inclinar:** de 15° en 15°; solo se nota con "Fijo".
   - **Colocar:** "Traer aquí", "Donde miro" (pegado al bloque; en una pared queda mirando hacia afuera), "Mirar hacia mí" o coordenadas exactas.
   - **Ver el cambio:** tras cada cambio el menú vuelve en 1 s, en 3 s o no vuelve, según elijas. Así se ve el holograma sin el menú encima.
5. **Publicar:** lo ven todos. "Volver a borrador" lo esconde otra vez.

Desde el menú de cada holograma también se puede **señalar** (partículas solo para ti), **ir** hasta él, **duplicar** y **eliminar**.

## Texto

- Códigos `&`:
  - Colores: `&0`–`&f`.
  - Formato: `&l` negrita, `&o` cursiva, `&n` subrayado, `&m` tachado, `&k` mágico, `&r` quitar formato.
  - Como en el chat, un color nuevo quita el formato anterior.
- Hex: `&#B79CFF`.
- MiniMessage: `<gradient:#B79CFF:#A8E4FF>texto</gradient>`, `<rainbow>`, `<#FF66AA>`, `<bold>`… Se puede mezclar con `&`.
- Nueva línea: Enter, `\n` o `<br>`.
- Unicodes: se pegan tal cual o como `❤`.

## Guardado

Todo se guarda en `plugins/QuasoPlugin/hologramas.yml`. Los Text Display no se guardan con el mundo: el plugin los crea al cargar cada chunk. Así nunca quedan duplicados aunque el server se caiga, y editar el yml más `/holograma recargar` basta para cambiarlos.
