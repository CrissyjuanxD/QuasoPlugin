# Texturas y modelos de monedas · 16 × 16

[Descargar PNG y JSON en ZIP](https://github.com/CrissyjuanxD/QuasoPlugin/raw/refs/heads/26.2/descargas/monedas-16x16/texturas_monedas_16x16.zip)

También disponibles en esta sección de descargas: [Zombie, Araña y Creeper Floral — modelos, animaciones y texturas de sus drops](../mobs-florales/).

![Vista ampliada sobre fondo claro](vista_previa_16x16.png)

Los tres PNG tienen **16 × 16 píxeles reales**, fondo transparente y una **D** central. La vista previa está ampliada sin suavizado sobre un fondo claro; ese fondo no forma parte de las texturas.

- [DinoCoins](dinocoins.png): morado `#3f2163`, azul `#284a78` y letra `#6fa2e7`.
- [DinoFicha](dinofichas.png): amarillo dorado y café pastel.
- [Ficha de misión](ficha_mision.png): rojo y naranja.

## Instalación en tu resource pack

Descarga el ZIP y copia su carpeta **`assets`** dentro de tu resource pack, junto a su `pack.mcmeta`. Después vuelve a cargar el pack. Este ZIP complementa tu pack existente y contiene las tres texturas y sus seis JSON.

Se utiliza la misma estructura que en `resourcepack/`: definiciones en `items`, modelos en `models/item/custom` y texturas en `textures/item/custom`.

| Item | Identificador utilizado por el plugin | Definición del item | Modelo y textura |
| --- | --- | --- | --- |
| DinoCoins | `minecraft:dinocoins` | [items/dinocoins.json](assets/minecraft/items/dinocoins.json) | `item/custom/dinocoins` |
| DinoFicha | `minecraft:dinofichas` | [items/dinofichas.json](assets/minecraft/items/dinofichas.json) | `item/custom/dinofichas` |
| Ficha de misión | `minecraft:ficha_mision` | [items/ficha_mision.json](assets/minecraft/items/ficha_mision.json) | `item/custom/ficha_mision` |

La ficha de misión utiliza **`minecraft:ficha_mision`** en el plugin. Su definición, modelo y textura se llaman **`ficha_mision`**.

Si instalaste el ZIP anterior, elimina `assets/minecraft/items/popped_chorus_fruit.json` de tu pack. Actualiza también el plugin para que las fichas nuevas reciban el identificador `ficha_mision`; las fichas ya entregadas conservan el modelo anterior y deben recrearse para mostrar esta textura.

```text
assets/minecraft/
├── items/
│   ├── dinocoins.json
│   ├── dinofichas.json
│   └── ficha_mision.json
├── models/item/custom/
│   ├── dinocoins.json
│   ├── dinofichas.json
│   └── ficha_mision.json
└── textures/item/custom/
    ├── dinocoins.png
    ├── dinofichas.png
    └── ficha_mision.png
```

Esta carpeta contiene las descargas por separado. Para descargar un PNG o JSON individual, abre su archivo y pulsa **Download raw file**.
