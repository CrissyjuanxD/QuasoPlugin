#!/usr/bin/env python3
"""Comprueba contratos reales del port; el renderizado final requiere un cliente Bedrock."""
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
BASE=Path(__file__).resolve().parent.parent
PACK=BASE/'resource_pack'
OLD=BASE.parent/'bedrock-1.0.0/resource_pack'
def read(p):return json.loads(p.read_text())
def check(ok,message):
    if not ok:raise AssertionError(message)
jsons=list(PACK.rglob('*.json'))
for p in jsons:read(p)
# Evita regresiones en todo lo que el usuario confirmó que funcionaba.
for folder in ['textures/items','textures/models','attachables']:
    for p in (OLD/folder).rglob('*'):
        if p.is_file():check(p.read_bytes()==(PACK/p.relative_to(OLD)).read_bytes(),f'Se alteró {p}')
check((BASE/'custom_mappings/quaso_items.json').read_bytes()==(BASE.parent/'bedrock-1.0.0/custom_mappings/quaso_items.json').read_bytes(),'Mappings alterados')
for p in (PACK/'ui').glob('*.json'):
    data=p.read_text()
    check('"source_property"' not in data and '"target_property"' not in data,'Binding JSON UI incorrecto: '+str(p))
ui=read(PACK/'ui/chest_screen.json')
for name in ['misiones','misiones_extra','misiones_trabajo','menu','habilidades','trabajos','tiendas']:
    image=ui['quaso_'+name]
    check((PACK/(image['texture']+'.png')).exists(),'Falta imagen GUI')
    check('[quaso:'+name+']' in str(image['bindings']),'Selector incompleto/colisión entre menús')
check(read(PACK/'ui/scoreboards.json')['scoreboard_sidebar_player']['max_size'][0]>=240,'Sidebar truncado')
catalog=read(BASE/'modelos.json');check(len(catalog)==15,'Faltan variantes')
for mob in catalog:
    entity=read(PACK/'entity'/(mob['entity'].split(':')[1]+'.entity.json'))['minecraft:client_entity']['description']
    condition=f"variable.quaso_{mob['model']} = query.mark_variant == {mob['mark_variant']};"
    check(condition in entity['scripts']['pre_animation'],'No se activa '+mob['model'])
    check(not any('is_name_any' in x for x in entity['scripts']['pre_animation']),'Sigue la selección frágil por nombre')
    check((PACK/(entity['textures'][mob['model']]+'.png')).exists(),'Textura de mob ausente')
    geometry=read(PACK/'models/entity'/(mob['model']+'.json'))['minecraft:geometry'][0]
    check(geometry['description']['identifier']==entity['geometry'][mob['model']],'Referencia de geometría rota')
    bones=geometry['bones'];names={b['name'] for b in bones}
    check(len(names)==len(bones),'Huesos duplicados')
    for bone in bones:check('parent' not in bone or bone['parent'] in names,'Padre inexistente')
biomes=list((PACK/'biomes').glob('*.json'));check(len(biomes)==4,'Biomas incompletos')
for p in biomes:
    b=read(p)['minecraft:client_biome'];check(b['description']['identifier'].startswith('quaso:'),'Reemplazo global de biomas')
    check((PACK/'fogs'/p.name.replace('.client_biome','')).exists(),'Falta niebla del bioma')
check(len(list((PACK/'fogs').glob('bloodmoon_*.json')))==20,'Transición incompleta')
with ZipFile(BASE/'extensions/QuasoWardenCave-1.1.0.jar') as z:
    check(b'version: 1.1.0' in z.read('extension.yml'),'Versión jar incorrecta')
    for n in z.namelist():
        if n.endswith('.class'):
            data=z.read(n)
            check(b'Lnet/kyori/adventure/key/Key;' not in data,'Adventure sin reubicar')
            check(b'Lit/unimi/dsi/fastutil/' not in data,'Fastutil sin reubicar')
with ZipFile(BASE/'packs/QuasoTextures-1.1.0.mcpack') as z:
    check(z.testzip() is None,'ZIP corrupto')
    for p in PACK.rglob('*'):
        if p.is_file():check(z.read(str(p.relative_to(PACK)))==p.read_bytes(),'Pack sin reconstruir')
for line in (BASE/'SHA256SUMS.txt').read_text().splitlines():
    digest,name=line.split('  ',1);check(hashlib.sha256((BASE/name).read_bytes()).hexdigest()==digest,'Checksum '+name)
print(f'OK: {len(jsons)} JSON, arte original intacto, 15 mobs, 7 menús, 4 biomas, 20 pasos de fade, JAR Spigot y artefactos verificados.')
