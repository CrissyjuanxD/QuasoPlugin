#!/usr/bin/env python3
"""Port incremental: conserva el arte aprobado y reconstruye 1.1.0 sin el ZIP privado."""
import json
import re
import shutil
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
OLD = BASE.parent / 'bedrock-1.0.0'
WC = BASE.parent / 'bedrock-1.0.1'
PACK = BASE / 'resource_pack'

def read(path):
    text = path.read_text(encoding='utf-8')
    text = re.sub(r'"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\r\n]*',
                  lambda m: m[0] if m[0].startswith('"') else '', text)
    return json.loads(re.sub(r',\s*([}\]])', r'\1', text))

def put(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

for source in [OLD/'resource_pack', WC/'resource_pack', OLD/'opcional_mobs/resource_pack']:
    for path in source.rglob('*'):
        if path.is_file() and path.name not in ('manifest.json', 'pack_icon.png'):
            dest = PACK/path.relative_to(source)
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, dest)
shutil.copy2(OLD/'resource_pack/pack_icon.png', PACK/'pack_icon.png')
manifest = read(OLD/'resource_pack/manifest.json')
manifest['header'].update(name='Croissants · Segunda edición | Quaso 1.1.0', version=[1,1,0],
    min_engine_version=[1,26,50], description='Items, armaduras, menús, rangos, mobs y Warden Cave. Geyser + QuasoWardenCave 1.1.0.')
manifest['modules'][0]['version'] = [1,1,0]
manifest['capabilities'] = ['pbr']
put(PACK/'manifest.json', manifest)
shutil.copytree(OLD/'custom_mappings', BASE/'custom_mappings', dirs_exist_ok=True)

# Los fondos se activan con títulos ASCII enviados por la extensión. Se enlaza al texto
# del control, no a una variable evaluada una sola vez al construir la pantalla cacheada.
guis = dict(zip(['misiones','misiones_extra','misiones_trabajo','menu','habilidades','trabajos','tiendas'], range(0x3202,0x3209)))
chest = {'namespace':'chest'}
normal = '(' + ' and '.join(f"((#text - '[quaso:{name}]') = #text)" for name in guis) + ')'
def title_binding(expression):
    return [{'binding_type':'view', 'source_control_name':'quaso_title_probe',
             'source_property_name':expression, 'target_property_name':'#visible'}]
chest['chest_label'] = {'bindings':title_binding(normal)}
chest['quaso_title_probe'] = {'type':'label','text':'$container_title', 'localize':False,
    'size':[0,0], 'alpha':0, 'layer':0}
chest['quaso_cell_image_panel@common.cell_image_panel'] = {'bindings':title_binding(normal)}
chest['chest_grid_item'] = {'$background_images':'chest.quaso_cell_image_panel'}
for name in guis:
    chest['quaso_'+name] = {'type':'image','texture':'textures/ui/quaso/'+name,
        'size':[256,256], 'anchor_from':'top_left','anchor_to':'top_left',
        'offset':[0,0], 'layer':3,
        'bindings':title_binding(f"(not ((#text - '[quaso:{name}]') = #text))")}
controls = [{'quaso_title_probe@chest.quaso_title_probe':{}}] + [
    {f'quaso_{name}@chest.quaso_{name}':{}} for name in guis]
for panel in ['small_chest_panel','large_chest_panel']:
    chest[panel+'/root_panel'] = {'modifications':[
        {'array_name':'controls','operation':'insert_back','value':controls}]}
put(PACK/'ui/chest_screen.json', chest)

# Conserva las factorías y controles nativos. Elimina sólo el número del sidebar Quaso.
is_quaso = "(not ((#objective_sidebar_name - '\ue900') = #objective_sidebar_name))"
bindings = [{'binding_name':'#objective_sidebar_name','binding_type':'global'},
    {'binding_type':'view','source_property_name':'(not '+is_quaso+')','target_property_name':'#visible'}]
score = {'namespace':'scoreboard',
    'scoreboard_sidebar_player':{'max_size':[280,10]},
    'scoreboard_sidebar_score':{'modifications':[{'array_name':'bindings','operation':'insert_back','value':bindings}]},
    'scoreboard_sidebar/main/displayed_objective':{'modifications':[
        {'array_name':'bindings','operation':'insert_back','value':bindings[1:]}]},
    'scoreboard_sidebar/main':{'modifications':[{'array_name':'controls','operation':'insert_back','value':[
        {'quaso_logo':{'type':'image','texture':'textures/ui/quaso/croissants','size':[128,34.5],
            'anchor_from':'top_middle','anchor_to':'bottom_middle','offset':[0,-2],'layer':4,
            'bindings':[bindings[0], {'binding_type':'view','source_property_name':is_quaso,'target_property_name':'#visible'}]}}]}]}}
put(PACK/'ui/scoreboards.json',score)

# El pack ya debe estar activado: no puede cambiar la pantalla de su primera descarga.
# Sustituye el título de las pantallas de carga sin copiar la pantalla completa de Mojang.
put(PACK/'ui/progress_screen.json', {'namespace':'progress'})
put(PACK/'ui/quaso_loading.json',{'namespace':'common_art',
    'title_image':{'texture':'textures/ui/quaso/croissants', 'size':[256,69]}})
put(PACK/'ui/_ui_defs.json', {'ui_defs':['ui/chest_screen.json','ui/scoreboards.json','ui/progress_screen.json','ui/quaso_loading.json']})

# Selección por metadato suministrado por Geyser. El número nunca altera la entidad Java.
catalog = read(OLD/'opcional_mobs/modelos_convertidos.json')
for index, mob in enumerate(catalog, 1):
    mob['mark_variant'] = 9300 + index
    mob['entity'] = 'minecraft:' + re.sub(r'\d+$','',mob['model'])
for p in (PACK/'entity').glob('*.json'):
    data = read(p); d = data['minecraft:client_entity']['description']
    if 'scripts' not in d: continue
    pre = d['scripts'].get('pre_animation',[])
    for mob in catalog:
        prefix = 'variable.quaso_' + mob['model'] + ' = '
        pre = [prefix+f"query.mark_variant == {mob['mark_variant']};" if line.startswith(prefix) else line for line in pre]
    d['scripts']['pre_animation'] = pre
    put(p,data)
put(BASE/'modelos.json',catalog)

# Biomas Bedrock auténticos: identidad propia, sin recolorear biomas vanilla.
for name in ['caverna_sculk','pantano_profundo','ruinas_de_ceniza','abismo_flotante']:
    source = read(BASE.parents[1]/'src/main/resources/datapack/QuasoPlugin/data/quaso/worldgen/biome'/f'{name}.json')
    a = source['attributes']; fog = 'quaso:'+name
    def distance(color,start,end):
        return {'fog_start':start,'fog_end':end,'fog_color':color,'render_distance_type':'fixed'}
    air = distance(a['minecraft:visual/fog_color'],a['minecraft:visual/fog_start_distance'],a['minecraft:visual/fog_end_distance'])
    put(PACK/'fogs'/f'{name}.json', {'format_version':'1.16.100','minecraft:fog_settings':{
        'description':{'identifier':fog},'distance':{'air':air,'weather':air,
        'water':distance(a['minecraft:visual/water_fog_color'],0,24)}}})
    put(PACK/'biomes'/f'{name}.client_biome.json', {'format_version':'1.21.70','minecraft:client_biome':{
        'description':{'identifier':fog},'components':{
        'minecraft:fog_appearance':{'fog_identifier':fog},
        'minecraft:sky_color':{'sky_color':a['minecraft:visual/sky_color']},
        'minecraft:water_appearance':{'surface_color':source['effects']['water_color']}}}})

# Transición de horizonte rojo de 10 s; el cielo de Java usa atributos que Geyser no transmite.
for step in range(1,21):
    t = step/20
    color = '#'+''.join(f'{round(a+(b-a)*t):02x}' for a,b in zip((24,27,42),(150,25,43)))
    air = {'fog_start':round(160-80*t),'fog_end':round(320-112*t),
           'fog_color':color,'render_distance_type':'fixed'}
    put(PACK/'fogs'/f'bloodmoon_{step}.json',{'format_version':'1.16.100','minecraft:fog_settings':{
        'description':{'identifier':f'quaso:bloodmoon_{step}'},'distance':{'air':air,'weather':air}}})

# La misma tabla de alias que las páginas de fuente aprobadas; activación automática por sesión.
unicode = read(OLD/'unicodes.json')
properties = []
for glyph in unicode['originales']:
    if glyph['source'].startswith('theranks:'):
        alias = unicode['rangos_anchos_bedrock'][Path(glyph['source']).stem]
        properties.append(glyph['code'][2:]+'='+alias)
resources = BASE/'extension/src/main/resources'
(resources/'ranks.properties').write_text('\n'.join(properties)+'\n',encoding='utf-8')
put(resources/'mobs.json',catalog)
print('Preparado Quaso 1.1.0: pack único, 15 variantes, 4 biomas, UI y rangos automáticos.')
