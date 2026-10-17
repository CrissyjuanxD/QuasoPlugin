#!/usr/bin/env python3
"""Regresiones del registro real de Bedrock; no simula un renderizador gráfico."""
import ast
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

BASE = Path(__file__).resolve().parent.parent
PACK = BASE/'resource_pack'
OLD = BASE.parent/'bedrock-1.1.0/resource_pack'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def check(ok,message):
    if not ok: raise AssertionError(message)
jsons = list(PACK.rglob('*.json'))
for p in jsons: read(p)

# Todo el arte y los mappings que ya funcionaban deben permanecer iguales.
for folder in ['textures/items','textures/models','attachables']:
    for p in (OLD/folder).rglob('*'):
        if not p.is_file(): continue
        target = PACK/p.relative_to(OLD)
        if p.name == 'peto_warden_alado.json':
            before, after = read(p), read(target)
            check(after.pop('format_version') == '1.10.0','Esquema del peto')
            before.pop('format_version')
            check(before == after,'Se cambió el arte/comportamiento del peto')
        else: check(p.read_bytes()==target.read_bytes(),f'Regresión en {p.name}')
check((BASE/'custom_mappings/quaso_items.json').read_bytes() ==
      (BASE.parent/'bedrock-1.1.0/custom_mappings/quaso_items.json').read_bytes(),'Mappings alterados')
check((PACK/'ui/scoreboards.json').read_bytes()==(OLD/'ui/scoreboards.json').read_bytes(),'Sidebar alterado')

# Esquemas que el cliente 26.52 rechazaba: no basta con que sea JSON válido.
for p in [*(PACK/'entity').glob('*.json'), *(PACK/'attachables').glob('*.json')]:
    data = read(p); version = tuple(map(int,data['format_version'].split('.')))
    d = data.get('minecraft:client_entity',data.get('minecraft:attachable'))['description']
    if 'animate' in d.get('scripts',{}) or any(isinstance(x,dict) for x in d.get('render_controllers',[])):
        check(version >= (1,10,0),f'El esquema no admite animate/condiciones: {p.name}')
    if version >= (1,10,0): check('animation_controllers' not in d, f'Controller antiguo en {p.name}')
    for entry in d.get('scripts',{}).get('animate',[]):
        for alias in [entry] if isinstance(entry,str) else entry:
            check(alias in d.get('animations',{}),f'Animación no declarada {p.name}: {alias}')
check(read(PACK/'ui/quaso_loading.json')['title_image']['type']=='image','Falta tipo del logo')

catalog=read(BASE/'modelos.json'); check(len(catalog)==15,'Faltan variantes')
for mob in catalog:
    entity=read(PACK/'entity'/(mob['entity'].split(':')[1]+'.entity.json'))['minecraft:client_entity']['description']
    condition=f"variable.quaso_{mob['model']} = query.property('quaso:model') == {mob['mark_variant']};"
    check(condition in entity['scripts']['pre_animation'],'No se activa '+mob['model'])
    check((PACK/(entity['textures'][mob['model']]+'.png')).exists(),'Textura de mob ausente')
    geometry=read(PACK/'models/entity'/(mob['model']+'.json'))['minecraft:geometry'][0]
    check(geometry['description']['identifier']==entity['geometry'][mob['model']],'Referencia de geometría rota')
    bones=geometry['bones']; names={b['name'] for b in bones}
    check(len(names)==len(bones),'Huesos duplicados')
    for bone in bones: check('parent' not in bone or bone['parent'] in names,'Padre inexistente')

# Evaluación limitada de las expresiones booleanas de JsonUI contra títulos distintos.
# Usa un AST restringido, sin ejecutar código arbitrario de los recursos.
def visible(expression,title):
    node=ast.parse(expression.replace('$container_title','title').replace(' = ',' == '),mode='eval').body
    def value(n):
        if isinstance(n,ast.Constant) and isinstance(n.value,str): return n.value
        if isinstance(n,ast.Name) and n.id=='title': return title
        if isinstance(n,ast.UnaryOp) and isinstance(n.op,ast.Not): return not value(n.operand)
        if isinstance(n,ast.BoolOp) and isinstance(n.op,ast.And): return all(value(v) for v in n.values)
        if isinstance(n,ast.Compare) and len(n.ops)==1 and isinstance(n.ops[0],ast.Eq): return value(n.left)==value(n.comparators[0])
        raise AssertionError('Expresión UI inesperada: '+ast.dump(n))
    return value(node)
ui=read(PACK/'ui/chest_screen.json')
menus=['misiones','misiones_extra','misiones_trabajo','menu','habilidades','trabajos','tiendas']
titles=['Cofre','Cofre grande','Cofre de Juan','[quaso:desconocido]','[quaso:menu] copia','']
for title in titles+['[quaso:'+n+']' for n in menus]:
    shown=[n for n in menus if visible(ui['quaso_'+n]['visible'],title)]
    expected=[] if title in titles else [title[len('[quaso:'):-1]]
    check(shown==expected,'Fondos superpuestos o visibles fuera del menú: '+title)
    check(visible(ui['chest_label']['visible'],title)==(not shown),'Título oculto en cofre ajeno')
    check(visible(ui['quaso_cell_image_panel@common.cell_image_panel']['visible'],title)==(not shown),'Casillas ajenas alteradas')
check('quaso_title_probe' not in str(ui),'Sigue el binding roto de #text')
for name in menus:
    im=Image.open(PACK/'textures/ui/quaso'/f'{name}.png')
    check(list(im.size)==ui['quaso_'+name]['size'],'GUI escalada de forma accidental')
    check(im.size==(176,81 if name=='trabajos' else 135),'Altura incorrecta del inventario')

# Verifica que el atlas recupera pixel por pixel el rango de 9px original, sin escalado.
atlas=Image.open(PACK/'font/glyph_ED.png').convert('RGBA')
for rank in read(BASE/'rangos.json'):
    original=Image.open(BASE/'herramientas/originales_rangos'/rank['source']).convert('RGBA')
    check(original.height==9,'Altura inesperada del rango')
    rebuilt=Image.new('RGBA',original.size)
    for char,(start,end) in zip(rank['glyphs'],rank['segments']):
        c=ord(char); x=(c%16)*16; y=((c%256)//16)*16+3
        check(0<end-start<=16,'Segmento fuera de celda')
        rebuilt.paste(atlas.crop((x,y,x+end-start,y+9)),(start,0))
        if end not in (12,original.width):
            check(all(original.getpixel((end,yy))==original.getpixel((13,1)) for yy in range(2,7)),
                  'Corte en medio de una letra: '+rank['source'])
    check(rebuilt.tobytes()==original.tobytes(),'Pixeles del rango perdidos: '+rank['source'])

for p in (PACK/'biomes').glob('*.json'):
    b=read(p)['minecraft:client_biome']
    check(b['description']['identifier'].startswith('quaso:'),'Reemplazo global de biomas')
    check('minecraft:foliage_appearance' in b['components'],'Falta color de hojas')
for step in range(1,21):
    distance=read(PACK/'fogs'/f'bloodmoon_{step}.json')['minecraft:fog_settings']['distance']
    for kind in ['air','weather']:
        fog=distance[kind]
        check(fog['render_distance_type']=='render' and 0<=fog['fog_start']<fog['fog_end']<=1,
              'Niebla invisible fuera del renderizado')

with ZipFile(BASE/'extensions/QuasoWardenCave-1.1.2.jar') as z:
    check(b'version: 1.1.2' in z.read('extension.yml'),'Versión jar incorrecta')
    check('studios/viciont/quasogeyser/MobModels.class' in z.namelist(),'Falta el puente de modelos')
    check(z.read('ranks.properties')==(BASE/'extension/src/main/resources/ranks.properties').read_bytes(),'Rangos desincronizados')
    for n in z.namelist():
        if n.endswith('.class'):
            data=z.read(n)
            check(b'Lnet/kyori/adventure/key/Key;' not in data,'Adventure sin reubicar')
            check(b'Lit/unimi/dsi/fastutil/' not in data,'Fastutil sin reubicar')
with ZipFile(BASE/'packs/QuasoTextures-1.1.2.mcpack') as z:
    check(z.testzip() is None,'ZIP corrupto')
    for p in PACK.rglob('*'):
        if p.is_file(): check(z.read(str(p.relative_to(PACK)))==p.read_bytes(),'Pack sin reconstruir')
for line in (BASE/'SHA256SUMS.txt').read_text().splitlines():
    digest,name=line.split('  ',1)
    check(hashlib.sha256((BASE/name).read_bytes()).hexdigest()==digest,'Checksum '+name)
print(f'OK: {len(jsons)} JSON; esquemas, 15 mobs, 7 GUIs aisladas, rangos sin escalado, hojas, niebla y artefactos.')
