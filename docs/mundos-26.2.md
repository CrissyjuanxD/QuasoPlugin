# Mundos configurables (26.2, Version #95)

El server real usa el mundo `Crossiants`, no `world`. Los nombres ahora se leen de `plugins/QuasoPlugin/mundos.yml`,
que se crea solo la primera vez:

```yaml
principal: Crossiants   # el Overworld
eventos: Crossiants     # Hot Potato, Skybattle y Build Battle (si se borra usa el principal)
warden_cave: wardencave # la dimensión de la Warden Cave
```

Después de cambiarlo hay que reiniciar el server. El código usa `Managers.Mundos`, la variable global:

- `Mundos.mundoPrincipal()` y `Mundos.mundoEventos()` dan el mundo. Si el nombre no existe, usan el primero que cargó
  el server y lo avisan una vez en la consola.
- `QuasoPlugin.WORLD_NAME` toma el valor de `warden_cave`.

Ya no queda ningún `getWorld("world")`. Se cambiaron:

- Hot Potato, Skybattle (con los cofres) y Build Battle: usan el mundo de `eventos`.
- La misión 96, el Amuleto de Última Esperanza, MagicTP, el portal y `/wardencave` de la Warden Cave, `/spawnmob`
  desde la consola y la web: usan el mundo `principal`.
- Las tumbas muestran «Overworld» para el mundo `principal` y «Warden Cave» para el de `warden_cave`.
