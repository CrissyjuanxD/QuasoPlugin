# Pesca en las zonas (26.2)

## Minijuego

- Cuando algo pica dentro de una zona de pesca, se cancela la pesca normal y el
  corcho se queda en el agua. Durante el minijuego no hay otra picada.
- La barra sale en el subtítulo: `≈ ▬▬▬▬ … ▬▬▬▬ ≈`. Tiene 24 casillas: 2 verdes,
  4 naranjas a cada lado y el resto rojas. El verde cambia de lugar en cada
  partida.
- Un marcador blanco recorre la barra, acelerando y frenando como si el pez
  tirara.
- Para tirar hay **que volver a usar la caña**; saltar o agacharse ya no hace
  nada. El primer momento (4 ticks) no cuenta, para que el doble clic con el
  que picó no tire por error.
- Se juzga la casilla donde estaba el marcador cuando el jugador lo vio: se
  mira atrás según su ping, hasta 300 ms.
- La bossbar azul muestra el tiempo: 6 segundos, y se pone amarilla y luego
  roja. Si no tira a tiempo, el pez se escapa sin premio. Si guarda la caña,
  también.
- En el agua salen estela y tirones (el corcho se hunde con salpicaduras) y
  suena una campanita cuando el marcador entra al verde.

## Probabilidades

| Color | Resultado | Premio especial |
|---|---|---|
| Verde | Pesca perfecta | 100%, con la tabla de suerte |
| Naranja | Buena pesca | 40% (+5% por nivel de Suerte marina, hasta 55%), con la tabla normal |
| Rojo | Pesca normal | 0% (sale el loot de Minecraft) |

Tope: 60 premios especiales por jugador al día. Después solo sale pesca normal.

| Premio | Rareza | Tabla normal | Tabla de suerte | La Pescadería paga 1 DinoCoin por |
|---|---|---|---|---|
| Chatarra | Común | 30% | 16% | 64 |
| Manzana Podrida | Común | 25% | 14% | 48 |
| Zanahoria Encantada | Poco común | 15% | 18% | 24 |
| Pepitas de Hierro Oxidadas | Poco común | 12% | 16% | 16 |
| Pepitas de Diamante | Raro | 8% | 14% | 8 |
| Fragmentos de Ámbar | Raro | 5% | 10% | 4 |
| Fósiles Pequeños | Épico | 3% | 7% | 2 |
| Lingote de Platino | Épico | 2% | 5% | 1 |

Los precios son los de la Pescadería del catálogo (`/tienda`, ver `ShopSystem/CatalogoTienda.java`). Así, un premio
de la tabla normal vale unas 0,08 DinoCoins y uno de la tabla de suerte unas 0,15. Con el tope de 60 al día, la pesca
da de 5 a 9 DinoCoins diarias según cuántos verdes se acierten: lo que pedía el itinerario (unas 5), con un poco más
para el que juega bien.

Para bajar lo que da la pesca sin tocar la tienda, se baja `FishingLoot.TOPE_DIARIO`.

Los épicos se anuncian a todo el server. Cada premio especial deja una línea en
el chat con lo que lleva el jugador ese día.
