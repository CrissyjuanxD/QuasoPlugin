#!/usr/bin/env python3
"""Reutiliza el port aprobado; no requiere volver a descargar el ZIP Java privado."""
import json
import shutil
from pathlib import Path
from PIL import Image

BASE = Path(__file__).resolve().parent.parent
OLD = BASE.parent / 'bedrock-1.1.0'
PACK = BASE / 'resource_pack'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def put(p, data):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

shutil.copytree(OLD/'resource_pack', PACK, dirs_exist_ok=True)
shutil.copytree(OLD/'custom_mappings', BASE/'custom_mappings', dirs_exist_ok=True)
manifest = read(PACK/'manifest.json')
manifest['header'].update(name='Croissants · Segunda edición | Quaso 1.1.2', version=[1,1,2],
    description='Presentación Bedrock con QuasoWardenCave 1.1.2: mobs, menús, rangos y atmósfera.')
manifest['modules'][0]['version'] = [1,1,2]
put(PACK/'manifest.json', manifest)
put(PACK/'quaso-presentation-1.1.2.json', {'version':[1,1,2], 'entity_property':'quaso:model'})

# $container_title es la variable nativa de cada pantalla. Un label con text no crea #text.
# Comparación exacta y control local: los cofres ajenos conservan sus casillas y título.
menus = ['misiones','misiones_extra','misiones_trabajo','menu','habilidades','trabajos','tiendas']
normal = '(' + ' and '.join(f"(not ($container_title = '[quaso:{name}]'))" for name in menus) + ')'
ui = {'namespace':'chest', 'chest_label':{'visible':normal},
      'quaso_cell_image_panel@common.cell_image_panel':{'visible':normal},
      'chest_grid_item':{'$background_images':'chest.quaso_cell_image_panel'}}
for name in menus:
    height = 81 if name == 'trabajos' else 135
    ui['quaso_'+name] = {'type':'image', 'texture':'textures/ui/quaso/'+name,
        'size':[176,height], 'anchor_from':'top_left', 'anchor_to':'top_left',
        'offset':[0,3], 'layer':3, 'visible':f"($container_title = '[quaso:{name}]')"}
    # La cuadrícula Java comienza en y=18; la de Bedrock en y=21. Mantiene 18px por casilla.
    image = Image.open(OLD/'resource_pack/textures/ui/quaso'/f'{name}.png').convert('RGBA')
    image.crop((0,0,176,height)).save(PACK/'textures/ui/quaso'/f'{name}.png')
for panel in ['small_chest_panel','large_chest_panel']:
    ui[panel+'/root_panel'] = {'modifications':[{'array_name':'controls','operation':'insert_back',
        'value':[{f'quaso_{name}@chest.quaso_{name}':{}} for name in menus]}]}
put(PACK/'ui/chest_screen.json', ui)

# Propiedad registrada en la API de Geyser. No reutiliza MARK_VARIANT de las abejas.
catalog = read(BASE/'modelos.json')
for p in (PACK/'entity').glob('*.json'):
    data = read(p); d = data['minecraft:client_entity']['description']
    if 'scripts' not in d: continue
    # 1.8 sólo admite render_controllers de tipo string y no scripts.animate.
    # Los templates vanilla mezclaban animation_controllers antiguos con animate.
    if tuple(map(int,data['format_version'].split('.'))) < (1,10,0):
        data['format_version'] = '1.10.0'
    animations = d.setdefault('animations', {})
    animate = d['scripts'].setdefault('animate', [])
    for controller in d.pop('animation_controllers', []):
        for alias, identifier in controller.items():
            # Los templates modernos ya tenían estos controllers: no ejecutarlos dos veces.
            if identifier in animations.values(): continue
            alias = 'quaso_native_'+alias+'_controller'
            animations[alias] = identifier
            animate.insert(0, {alias:'!variable.quaso_custom'})
    pre = d['scripts'].get('pre_animation', [])
    for mob in catalog:
        prefix = 'variable.quaso_'+mob['model']+' = '
        pre = [prefix+f"query.property('quaso:model') == {mob['mark_variant']};"
               if line.startswith(prefix) else line for line in pre]
    d['scripts']['pre_animation'] = pre
    put(p, data)

# El peto alado también ejecuta scripts.animate, que no existe en el esquema 1.8.
p = PACK/'attachables/peto_warden_alado.json'
data = read(p); data['format_version'] = '1.10.0'; put(p,data)

# Un control redefinido desde otro archivo necesita su tipo: no hereda title_image.
put(PACK/'ui/quaso_loading.json', {'namespace':'common_art', 'title_image':{
    'type':'image', 'layer':1, 'texture':'textures/ui/quaso/croissants', 'size':[256,69]}})

# Colores de las hojas aplicados sólo a los cuatro biomas custom, no a los vanilla.
foliage = {'caverna_sculk':'#1F8F8A', 'pantano_profundo':'#577C49',
           'ruinas_de_ceniza':'#82828B', 'abismo_flotante':'#A35EE8'}
for name, color in foliage.items():
    p = PACK/'biomes'/f'{name}.client_biome.json'
    data = read(p)
    data['minecraft:client_biome']['components']['minecraft:foliage_appearance'] = {'color':color}
    put(p, data)

# La niebla fija empezaba a 80 bloques: podía quedar fuera de la distancia visible.
# Una distancia relativa llega al horizonte incluso con pocos chunks de renderizado.
for step in range(1,21):
    t = step/20
    color = '#'+''.join(f'{round(a+(b-a)*t):02x}' for a,b in zip((24,27,42),(186,30,48)))
    air = {'fog_start':round(.92-.50*t,4), 'fog_end':1.0,
           'fog_color':color, 'render_distance_type':'render'}
    put(PACK/'fogs'/f'bloodmoon_{step}.json', {'format_version':'1.16.100','minecraft:fog_settings':{
        'description':{'identifier':f'quaso:bloodmoon_{step}'}, 'distance':{'air':air,'weather':air}}})

# Altura original de 9px, sin ampliar a 16. Los cortes se sitúan entre las letras,
# nunca dentro de sus trazos. Bedrock añade espacio entre glifos: no permite el avance
# negativo de Java, así que los distintivos conservan una separación pequeña.
unicode = read(BASE.parent/'bedrock-1.0.0/unicodes.json')
atlas = Image.new('RGBA',(256,256))
code = 0xED00
aliases = []; audit = []
for glyph in unicode['originales']:
    if not glyph['source'].startswith('theranks:'): continue
    name = Path(glyph['source']).name
    image = Image.open(BASE/'herramientas/originales_rangos'/name).convert('RGBA')
    # La fila 1 es el relleno del distintivo; las filas 2–6 contienen las letras.
    background = image.getpixel((13,1))
    spaces = [x for x in range(12,image.width-1)
              if all(image.getpixel((x,y)) == background for y in range(2,7))]
    x = 0; chars = []; segments = []
    while x < image.width:
        end = min(12 if x == 0 else x+16,image.width)
        if x > 0 and end < image.width:
            candidates = [s for s in spaces if x < s <= end]
            if not candidates: raise ValueError("No hay espacio seguro para cortar " + name)
            end = max(candidates)
        tile = image.crop((x,0,end,image.height))
        atlas.alpha_composite(tile,((code%16)*16,((code%256)//16)*16+3))
        chars.append(chr(code)); segments.append([x,end]); code += 1; x=end
    aliases.append(glyph['code'][2:]+'='+''.join(chars))
    audit.append({'source':name, 'size':list(image.size), 'segments':segments, 'glyphs':''.join(chars)})
assert code <= 0xEE00, 'Los rangos exceden la página reservada ED'
atlas.save(PACK/'font/glyph_ED.png')
(BASE/'extension/src/main/resources/ranks.properties').write_text('\n'.join(aliases)+'\n',encoding='utf-8')
put(BASE/'rangos.json',audit)
print('Preparado 1.1.2: propiedad de mobs, menús aislados, rangos de 9px, hojas y BloodMoon.')
