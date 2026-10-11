# Zombie Floral y Araña Floral 🌸

Modelos **CEM con flores en 3D**, enredaderas y texturas de musgo, inspirados en los modelos corruptos de Blockbench que enviaste. Las flores sustituyen a los cristales y acompañan el movimiento de la parte del cuerpo a la que están unidas.

[**Descargar modelos, texturas y reglas en ZIP**](https://github.com/CrissyjuanxD/QuasoPlugin/raw/refs/heads/26.2/descargas/mobs-florales/mobs_florales.zip) · [Texturas de monedas](../monedas-16x16/)

![Modelos florales, vistas de frente y espalda](vista_previa.png)

La imagen muestra un render de los archivos incluidos sobre fondo claro. Las poses son de presentación; en Minecraft las anima el cliente. Los atlas PNG son de **128 × 128 píxeles**, con detalles pixel art sin suavizado. El tamaño del atlas permite distribuir las caras del cuerpo y las flores; no es el formato de 16 × 16 usado para los items de monedas.

## Qué incluye

| Mob | Modelo floral | Textura | Nombre que activa la variante |
| --- | --- | --- | --- |
| Zombie Floral | [zombie2.jem](assets/minecraft/optifine/cem/zombie2.jem) | [zombie2.png](assets/minecraft/optifine/random/entity/zombie/zombie2.png) | `Zombie Floral` |
| Araña Floral | [spider2.jem](assets/minecraft/optifine/cem/spider2.jem) | [spider2.png](assets/minecraft/optifine/random/entity/spider/spider2.png) | `Spider Floral` o `Araña Floral` |

- **Zombi:** corona de flores, flores en el pecho, hombros y rodilla, hojas y un ramo en la espalda.
- **Araña:** cuerpo y patas anchas basados en la referencia, flores en el abdomen, cabeza y patas, hojas y ojos dorados. Incluye `spider_eyes2.png` para la capa de los ojos.
- Se conservan los nombres de las partes y los puntos de unión de los modelos de referencia para heredar las animaciones del mob.
- No hay cambios en las habilidades, recompensas ni código del plugin.

## Instalación

1. Descarga y descomprime el ZIP.
2. Copia **`assets`** dentro de tu resource pack, al lado de su `pack.mcmeta`. Es un complemento para tu pack; el ZIP no es un pack independiente.
3. Usa **Entity Model Features (EMF) + Entity Texture Features (ETF)**, o **OptiFine** con *Custom Entity Models* y *Random Entities* activados. Debe existir una versión compatible de esos mods con tu cliente. Minecraft sin esos mods no interpreta archivos `.jem`.
4. Activa el pack y recarga los recursos con **F3 + T**.
5. Con permisos de administración en QuasoPlugin, crea ejemplares nuevos con `/spawnqp corruptedzombie` y `/spawnqp corruptedspider`. Los identificadores internos del plugin siguen llamándose así, aunque sus nombres actuales son florales.

Las reglas reconocen los nombres que asigna actualmente QuasoPlugin, incluidos los prefijos de color. No hace falta que el nombre se vea encima del mob. Un mob antiguo que todavía se llame `Corrupted Zombie` o `Corrupted Spider` no coincide con estas reglas: genera uno nuevo para comprobarlo.

## Por qué hay varios `.properties`

```text
assets/minecraft/optifine/
├── cem/
│   ├── zombie.jem            ← base vanilla, sin partes reemplazadas
│   ├── zombie2.jem           ← modelo floral
│   ├── zombie.properties    ← selecciona MODELOS
│   ├── spider.jem            ← base vanilla, sin partes reemplazadas
│   ├── spider2.jem           ← modelo floral
│   └── spider.properties    ← selecciona MODELOS
└── random/entity/
    ├── zombie/
    │   ├── zombie2.png
    │   └── zombie.properties ← selecciona TEXTURAS
    └── spider/
        ├── spider2.png
        ├── spider_eyes2.png
        └── spider.properties ← selecciona TEXTURAS, incluida la capa de ojos
```

**El modelo del zombi floral es `zombie2.jem`.** Los archivos sin número contienen `"models": []`: proporcionan la base sin modificar las partes del modelo vanilla. La regla de nombre selecciona la variante **2**; la última regla reserva la variante **1** para los demás mobs.

Los `.properties` de `cem` usan `models`; los de `random/entity` usan `skins`. Tienen nombres iguales pero funciones y carpetas diferentes: conserva las rutas del ZIP.

### Si tu pack ya tiene otras variantes

Combina las reglas antes de reemplazar tus `.properties`. Este complemento ocupa la variante **2**, igual que la referencia, y no incluye la variante **3** de la araña infernal. Para conservar esa variante en tu pack, mantén sus modelos y texturas y añade su regla **antes de la regla final que selecciona 1**, tanto en modelos como en texturas. No copies una regla que apunte a un archivo que no tienes.

## Edición y comprobación

Los `.jem` se pueden importar con las herramientas CEM de Blockbench. Sus grupos tienen nombres como `corona_rosa`, `flor_pecho`, `rosa_abdomen` y `flor_pata_1` para localizar las flores. Los dos modelos apuntan explícitamente al PNG de su carpeta `random/entity`.

Se han comprobado la estructura JSON, las rutas, las coordenadas UV, las partes de los modelos y las reglas de selección; también se han renderizado la geometría y los atlas incluidos. **Pendiente de comprobación dentro de Minecraft con EMF/ETF u OptiFine**: el render no sustituye esa prueba del cliente.
