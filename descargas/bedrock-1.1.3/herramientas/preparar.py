#!/usr/bin/env python3
"""Repara GUIs y abeja floral, añade displays y conserva los demás recursos de 1.1.2."""
import json
import shutil
from pathlib import Path

BASE=Path(__file__).resolve().parent.parent
OLD=BASE.parent/'bedrock-1.1.2'
PACK=BASE/'resource_pack'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def put(p,d):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
shutil.copytree(OLD/'resource_pack',PACK,dirs_exist_ok=True)
(PACK/'quaso-presentation-1.1.2.json').unlink(missing_ok=True)
put(PACK/'quaso-presentation-1.1.3.json',{'version':[1,1,3],'entity_property':'quaso:model'})
m=read(PACK/'manifest.json')
m['header'].update(name='Croissants · Segunda edición | Quaso 1.1.3',version=[1,1,3],
    description='GUIs por título, Abeja Floral y tormenta BloodMoon atenuada. QuasoWardenCave 1.1.3.')
m['modules'][0]['version']=[1,1,3]
put(PACK/'manifest.json',m)

# Patrón utilizado por GeyserIntegratedPack: copiar el título, evaluar requires y
# asignar un booleano real a visible. No comparar texto dentro de la propiedad visible.
menus=['misiones','misiones_extra','misiones_trabajo','menu','habilidades','trabajos','tiendas']
def condition(name):
    return f"(not (($quaso_title_copy - '[quaso:{name}]') = $quaso_title_copy))"
def selector(names,normal=False):
    return {'$quaso_title_copy':'$container_title', '$quaso_visible|default':normal,
            'visible':'$quaso_visible', 'variables':[
                {'requires':condition(n),'$quaso_visible':not normal} for n in names]}
ui={'namespace':'chest', 'chest_label':selector(menus,True),
    'quaso_cell_image_panel@common.cell_image_panel':selector(menus,True),
    'chest_grid_item':{'$background_images':'chest.quaso_cell_image_panel'}}
for name in menus:
    ui['quaso_'+name]={'type':'image','texture':'textures/ui/quaso/'+name,
        'size':[176,81 if name=='trabajos' else 135], 'anchor_from':'top_left',
        'anchor_to':'top_left','offset':[0,3],'layer':3, **selector([name])}
for panel in ['small_chest_panel','large_chest_panel']:
    ui[panel+'/root_panel']={'modifications':[{'array_name':'controls','operation':'insert_back',
        'value':[{f'quaso_{name}@chest.quaso_{name}':{}} for name in menus]}]}
put(PACK/'ui/chest_screen.json',ui)

# bee2.jem fue exportado por Blockbench con invertAxis xy. Se reconstruye su espacio
# de edición y se convierte a Bedrock: X reflejada, rotaciones X/Y invertidas, UV box
# nativo. El conversor anterior reflejaba pivotes pero no los cubos ni las rotaciones.
# Referencia: Blockbench js/formats/optifine/optifine_jem.js y bedrock/bedrock.js.
cem=read(BASE/'herramientas/bee2-original.jem')
bones=[]
def add(part,depth=0,parent=None,parent_origin=None):
    assert part.get('invertAxis')=='xy', 'Este adaptador corresponde al bee2 original de Blockbench'
    name='quaso_'+part.get('id',part.get('part'))
    translation=part.get('translate',[0,0,0])
    if depth==0: origin=[-n for n in translation]
    elif depth==1: origin=translation.copy()
    else: origin=[translation[i]+parent_origin[i] for i in range(3)]
    bone={'name':name, 'pivot':[-origin[0],origin[1],origin[2]]}
    if parent: bone['parent']=parent
    elif name!='quaso_body': bone['parent']='quaso_body'
    if 'rotate' in part:
        rx,ry,rz=part['rotate'];bone['rotation']=[-rx,-ry,rz]
    if part.get('mirrorTexture')=='u': bone['mirror']=True
    cubes=[]
    for box in part.get('boxes',[]):
        x,y,z,w,h,d=box['coordinates']
        if depth>0: x+=origin[0];y+=origin[1];z+=origin[2]
        cube={'origin':[-x-w,y,z],'size':[w,h,d],'uv':box['textureOffset']}
        if box.get('sizeAdd'): cube['inflate']=box['sizeAdd']
        cubes.append(cube)
    if cubes: bone['cubes']=cubes
    bones.append(bone)
    for sub in part.get('submodels',[]): add(sub,depth+1,name,origin)
for part in cem['models']: add(part)
put(PACK/'models/entity/bee2.json',{'format_version':'1.16.0','minecraft:geometry':[{
    'description':{'identifier':'geometry.quaso.bee2','texture_width':64,'texture_height':64,
        'visible_bounds_width':6,'visible_bounds_height':6,'visible_bounds_offset':[0,1,0]},'bones':bones}]})
# Material nativo de la abeja para alas/patas planas. Las cuatro texturas son las Java.
p=PACK/'entity/bee.entity.json';e=read(p);d=e['minecraft:client_entity']['description']
d['materials']['quaso_bee']='bee'
put(p,e)
p=PACK/'render_controllers/quaso_mobs.json';r=read(p)
r['render_controllers']['controller.render.quaso.bee2']['materials']=[{'*':'Material.quaso_bee'}]
put(p,r)
p=PACK/'animations/quaso_mobs.json';a=read(p)
a['animations']['animation.quaso.bee2']['bones']['quaso_body']={
    'position':[0,'math.sin(query.life_time * 180) * 0.15',0]}
a['animations']['animation.quaso.bee2']['bones']['quaso_stinger']={
    'scale':'query.mark_variant == 1 ? 0 : 1'}
put(p,a)
print('Preparado 1.1.3: selectores nativos de GUI, geometría/UV de abeja; demás recursos intactos.')

import subprocess,sys
subprocess.run([sys.executable,str(BASE/"herramientas/displays.py")],check=True)
