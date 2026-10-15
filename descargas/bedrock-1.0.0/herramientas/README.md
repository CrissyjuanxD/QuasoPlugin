# Reconstruir el port

Requiere Python 3.10+, Pillow y NumPy. `base_bedrock` contiene las plantillas utilizadas, para que la reconstrucción no dependa de cambios futuros de Mojang. No incluye ejecutables ni credenciales.

Extrae tu `QuasoTextures.zip` en una carpeta cuya raíz contenga `assets/`. Para la estatua Java, aplica también la corrección de `descargas/correccion-estatua-recompensas` al pack original.

Desde la raíz de `QuasoPlugin`:

```bash
python descargas/bedrock-1.0.0/herramientas/convertir.py --java /ruta/QuasoTextures --vanilla descargas/bedrock-1.0.0/herramientas/base_bedrock
python descargas/bedrock-1.0.0/herramientas/acabados.py --java /ruta/QuasoTextures --vanilla descargas/bedrock-1.0.0/herramientas/base_bedrock
python descargas/bedrock-1.0.0/herramientas/mobs.py --java /ruta/QuasoTextures --vanilla descargas/bedrock-1.0.0/herramientas/base_bedrock
python descargas/bedrock-1.0.0/herramientas/validar.py
python descargas/bedrock-1.0.0/herramientas/empaquetar.py
```

`catalogo_items.json` une cada item model con el material Java. Se obtuvo de las factorías `ItemManager` y revisión de los menús/factorías adicionales. Si el plugin cambia materiales o añade items, actualiza ese catálogo antes de reconstruir: copiar solamente un PNG no registra un item de Geyser.

La validación incluye los recuentos de esta versión. Al añadir contenido actualiza esos valores conscientemente. No ejecuta Minecraft Bedrock ni afirma que haya pasado una prueba visual.

`convertir.py` genera el pack principal, `acabados.py` adapta particularidades y `mobs.py` crea el complemento opcional. El orden importa. Borra los archivos generados de modelos retirados antes de reconstruir, para no arrastrar recursos obsoletos entre versiones.
