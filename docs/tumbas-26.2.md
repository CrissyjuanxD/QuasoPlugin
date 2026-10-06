# Carga de tumbas en 26.2

Una tumba guardada en `wardencave` podía impedir que Quaso arrancase después
de reiniciar. El sistema leía `tumbas_data.yml` antes de crear esa dimensión.
Bukkit intentaba deserializar una `Location` cuyo mundo todavía no existía,
registraba `IllegalArgumentException: unknown world` y devolvía `null`.
Después `cleanupVisuals` usaba esa ubicación y provocaba el segundo error.

Ahora los registros se leen primero como datos YAML sin resolver mundos.
Las tumbas de mundos disponibles se restauran; las restantes esperan a
`WorldLoadEvent`, conservando todos sus objetos. Si un mundo se descarga,
sus tumbas vuelven a quedar pendientes. Sus objetos no se descartan cuando
vence el tiempo mientras el mundo está ausente: la expiración se procesa
cuando se puede volver a acceder al mundo.

La reconstrucción de displays espera al siguiente tick y carga las entidades
del chunk antes de limpiar las anteriores. Así no se añaden copias de una tumba
cuyas entidades todavía no eran visibles durante `WorldLoadEvent`.

Las ubicaciones nuevas se guardan como nombre y UUID del mundo más coordenadas,
sin el marcador serializable de `Location`. Un mundo distinto con el mismo
nombre no toma tumbas de otro UUID. Se admiten los archivos antiguos, que solo
tenían el nombre. Los registros incompletos se conservan y se avisa de ellos;
no bloquean el resto de tumbas ni se eliminan al guardar otras. Tampoco se
convierte una tumba con objetos ilegibles en una tumba vacía.

Antes del primer guardado que migra ubicaciones antiguas se conserva una copia
original en `tumbas_data.yml.pre-26.2.bak`; los guardados se escriben primero en
un archivo temporal y luego sustituyen el archivo de datos. Un YAML con errores
de sintaxis se rechaza sin sobrescribirlo.

Para actualizar, reemplazar el JAR y reiniciar. No borrar `tumbas_data.yml`.
Una advertencia «espera al mundo …; sus objetos se conservan» indica que falta
cargar ese mundo. No se crea ni se sustituye un mundo por tener tumbas pendientes.

La regresión se reprodujo en Paper 26.2 build 129 con una tumba antigua en
`wardencave` y tres diamantes: aparecieron los dos errores exactos antes de la
corrección. Las pruebas automatizadas cubren carga tardía, descarga/recarga,
mundos ausentes, objetos/metadata, registros inválidos, borrado independiente,
copias originales, identidad del mundo y fallos de deserialización de objetos.
