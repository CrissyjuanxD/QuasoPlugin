# Estatua de Recompensas · corrección Java

Copia `assets` dentro de tu pack Java y acepta reemplazar los archivos. Recarga el pack con F3+T. Usa también el plugin del commit de esta corrección y reinicia Paper: las estatuas existentes se actualizan al cargar, sin tener que invocarlas otra vez.

La raíz del modelo de cobre ya está a Y=24. La animación anterior sumaba otros 24 en el cuerpo y enterraba la base. Ahora el cuerpo queda a Y=0 respecto a esa raíz, con la base a la altura de los pies de la entidad.

La escala del gólem pasa de 2 a 4,5 y el CEM se compensa a 4/9: el dinosaurio conserva sus dimensiones, mientras su hitbox alcanza aproximadamente 4,41 bloques de alto (el modelo mide 4,40). La hitbox es rectangular y no sigue exactamente la cola ni los salientes. Actualiza modelo y plugin juntos; con un pack viejo el dino se vería demasiado grande.

Requiere EMF/ETF compatibles con tu versión, igual que el modelo anterior. No cambia los nombres, los clics ni las recompensas.
