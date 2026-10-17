#!/usr/bin/env python3
"""Genera artefactos reproducibles. No empaqueta configuración privada del servidor."""
import hashlib
import json
import uuid
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

BASE = Path(__file__).resolve().parent.parent
def archive(path, files):
    path.parent.mkdir(parents=True,exist_ok=True)
    with ZipFile(path,'w',compression=ZIP_DEFLATED) as z:
        for name, data in sorted(files.items()):
            info=ZipInfo(name,(2026,10,17,0,0,0)); info.compress_type=ZIP_DEFLATED
            z.writestr(info,data)
def tree(root):
    return {p.relative_to(root).as_posix():p.read_bytes() for p in root.rglob('*') if p.is_file()}

pack=BASE/'packs/QuasoTextures-1.1.0.mcpack'
archive(pack,tree(BASE/'resource_pack'))
jar=BASE/'extensions/QuasoWardenCave-1.1.0.jar'
if not jar.exists():raise SystemExit('Primero compila la extensión.')

# Opcional: la UI de una descarga no puede venir del mismo pack aún sin descargar.
logo_manifest={'format_version':2,'header':{'name':'Croissants · Logo global (opcional)',
    'description':'Actívalo en Recursos globales para ver el logo antes de descargar los packs del servidor. Cambia también el logo de otros menús del cliente.',
    'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'quaso-logo-global-header')),'version':[1,1,0],'min_engine_version':[1,26,50]},
    'modules':[{'type':'resources','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'quaso-logo-global-module')),'version':[1,1,0]}]}
logo={'manifest.json':json.dumps(logo_manifest,ensure_ascii=False).encode(),
      'ui/_ui_defs.json':json.dumps({'ui_defs':['ui/quaso_loading.json']}).encode()}
for name in ['ui/quaso_loading.json','textures/ui/quaso/croissants.png','pack_icon.png']:
    logo[name]=(BASE/'resource_pack'/name).read_bytes()
optional=BASE/'opcional_cliente/QuasoLogo-global-1.1.0.mcpack'
archive(optional,logo)
files={
    'plugins/Geyser-Spigot/packs/'+pack.name:pack.read_bytes(),
    'plugins/Geyser-Spigot/extensions/'+jar.name:jar.read_bytes(),
    'plugins/Geyser-Spigot/custom_mappings/quaso_items.json':(BASE/'custom_mappings/quaso_items.json').read_bytes(),
    'opcional_cliente/'+optional.name:optional.read_bytes(),
    'LEEME.md':(BASE/'README.md').read_bytes(),
}
archive(BASE/'Quaso-Bedrock-1.1.0-instalar.zip',files)
artifacts=[pack,jar,optional,BASE/'Quaso-Bedrock-1.1.0-instalar.zip']
(BASE/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+str(p.relative_to(BASE))+'\n' for p in artifacts))
print('Pack completo, extensión, logo opcional e instalador empaquetados.')
