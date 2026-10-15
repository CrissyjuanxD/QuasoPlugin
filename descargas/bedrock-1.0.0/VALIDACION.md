# Validación de 1.0.0

- Comprobados los 313 mappings, sus materiales, identificadores únicos y texturas del atlas.
- Verificados PNG, JSON sin claves repetidas, referencias de geometría, 30 attachables, capas de flores y alas.
- Comprobados 23 glifos originales, 7 fondos de inventario y cuadrículas de 9×3/9×6.
- En los 15 modelos opcionales, los renderizadores vanilla se conservan para entidades que no coinciden con un nombre personalizado.
- La estatua Bedrock empieza en Y=0; con la escala 4,5 del plugin conserva 4,40 bloques de alto frente a los 4,41 de la hitbox.
- Geyser 2.12.0-b1256 Standalone registra 314 items (313 propios más el integrado) y arranca. El JAR oficial se verificó con SHA-256. El lector de mappings es compartido con Geyser-Spigot.

La comprobación de arranque no incluye un cliente Bedrock ni el servidor de producción. El motor del cliente resuelve JSON UI, materiales, herencia de geometrías, Molang y postura de attachables; aquí no se ejecutó ese motor. Por tanto, los movimientos, alineación final de las GUIs, nombres coloreados, capas de armadura y vuelo necesitan una prueba dentro del juego. Los mobs se entregan como complemento opcional precisamente para poder probarlos por separado.

## Prueba breve en tu servidor

1. Conecta un cliente Bedrock 26.50+ y comprueba el icono/descarga del pack. El primer aviso de aceptación sigue siendo el del cliente.
2. Abre `/giveqp`, comprueba monedas, herramientas, libros y los cuatro estados de un arco al tensarlo.
3. Equipa ambas armaduras y el peto alado. Comprueba tercera persona y ambas manos; ver alas no confirma compatibilidad de vuelo.
4. Usa el perfil de IU Clásico y abre misiones, habilidades, trabajos, menú y tiendas. Comprueba que el dibujo coincide con el botón que recibe el clic.
5. Si instalaste el complemento de mobs, compara un mob normal con cada variante personalizada. Si el nombre coincide pero sigue vanilla, facilita el nombre mostrado por Geyser y el registro de contenido del cliente.
6. Revisa la estatua con el plugin actualizado y el pack Java corregido; su base debe quedar en los pies de la entidad. No hace falta crear una nueva.
7. Prueba monederos y tiendas con cantidades pequeñas. La traducción visual no resuelve automáticamente las pilas de monedas Java de 99; el límite Bedrock sigue siendo 64.

Si otra textura global del cliente reemplaza las fuentes o las mismas pantallas de UI, comprueba primero con los packs del servidor como únicos recursos aplicados. Para retirar los mobs basta quitar su `.mcpack` de `packs` y reiniciar; el principal y sus items siguen funcionando.
