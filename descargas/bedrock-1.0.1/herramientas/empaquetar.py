#!/usr/bin/env python3
"""Instalador completo: conserva el lote 1.0.0 y añade el complemento por mundo."""
import hashlib
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

B = Path(__file__).resolve().parent.parent
OLD = B.parent / 'bedrock-1.0.0'
pack = B / 'packs/QuasoWardenCave-1.0.0.mcpack'
pack.parent.mkdir(exist_ok=True)
with ZipFile(pack, 'w', ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted((B / 'resource_pack').rglob('*')):
        if p.is_file(): z.write(p, p.relative_to(B / 'resource_pack'))
files = {
    'plugins/Geyser-Spigot/packs/QuasoTextures-1.0.0.mcpack': OLD / 'packs/QuasoTextures-1.0.0.mcpack',
    'plugins/Geyser-Spigot/packs/QuasoWardenCave-1.0.0.mcpack': pack,
    'plugins/Geyser-Spigot/custom_mappings/quaso_items.json': OLD / 'custom_mappings/quaso_items.json',
    'plugins/Geyser-Spigot/extensions/QuasoWardenCave-1.0.0.jar': B / 'extensions/QuasoWardenCave-1.0.0.jar',
    'opcional_mobs/QuasoMobs-1.0.0.mcpack': OLD / 'opcional_mobs/QuasoMobs-1.0.0.mcpack',
    'README.md': B / 'README.md',
    'VALIDACION.md': B / 'VALIDACION.md',
    'validacion-geyser.txt': B / 'validacion-geyser.txt',
    'UNICODES.md': OLD / 'UNICODES.md',
    'rangos_bedrock.yml': OLD / 'rangos_bedrock.yml',
    'config-geyser-fragmento.yml': OLD / 'config-geyser-fragmento.yml',
}
checks = []
with ZipFile(B / 'Quaso-Bedrock-1.0.1-instalar.zip', 'w', ZIP_DEFLATED, compresslevel=9) as z:
    for destination, source in files.items():
        content = source.read_bytes()
        if destination == 'README.md':
            content = (content.decode().replace('(extensions/', '(plugins/Geyser-Spigot/extensions/')
                       .replace('(packs/', '(plugins/Geyser-Spigot/packs/')
                       .replace('(../bedrock-1.0.0/UNICODES.md)', '(UNICODES.md)')
                       .replace('(extension)', '(https://github.com/CrissyjuanxD/QuasoPlugin/tree/26.2/descargas/bedrock-1.0.1/extension)')
                       .replace('(herramientas)', '(https://github.com/CrissyjuanxD/QuasoPlugin/tree/26.2/descargas/bedrock-1.0.1/herramientas)')).encode()
        z.writestr(destination, content)
        checks.append(hashlib.sha256(content).hexdigest() + '  ' + destination)
    z.writestr('SHA256SUMS.txt', '\n'.join(checks) + '\n')
(B / 'SHA256SUMS.txt').write_text('\n'.join(checks) + '\n')
print(B / 'Quaso-Bedrock-1.0.1-instalar.zip')
