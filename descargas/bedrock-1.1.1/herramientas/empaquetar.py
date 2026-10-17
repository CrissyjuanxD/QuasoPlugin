#!/usr/bin/env python3
"""Actualización de la extensión; el pack 1.1.0 permanece idéntico."""
import hashlib
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

BASE = Path(__file__).resolve().parent.parent
jar = BASE / 'extensions/QuasoWardenCave-1.1.1.jar'
if not jar.exists():
    raise SystemExit('Primero compila la extensión.')
with ZipFile(jar) as z:
    assert 'version: 1.1.1' in z.read('extension.yml').decode()
    assert 'studios/viciont/quasogeyser/CustomBiomes.class' in z.namelist()
archive = BASE / 'Quaso-Bedrock-1.1.1-actualizar.zip'
files = {
    'plugins/Geyser-Spigot/extensions/' + jar.name: jar.read_bytes(),
    'LEEME.md': (BASE / 'README.md').read_bytes(),
}
with ZipFile(archive, 'w', compression=ZIP_DEFLATED) as z:
    for name, data in sorted(files.items()):
        info = ZipInfo(name, (2026, 10, 17, 0, 0, 0))
        info.compress_type = ZIP_DEFLATED
        z.writestr(info, data)
with ZipFile(archive) as z:
    assert z.testzip() is None
    assert z.read('plugins/Geyser-Spigot/extensions/' + jar.name) == jar.read_bytes()
(BASE / 'SHA256SUMS.txt').write_text(''.join(
    hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + str(p.relative_to(BASE)) + '\n'
    for p in [jar, archive]), encoding='utf-8')
print('ZIP y JAR 1.1.1 verificados; conserva QuasoTextures-1.1.0.mcpack.')
