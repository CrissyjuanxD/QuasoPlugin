#!/usr/bin/env python3
"""Convierte los recursos de Quaso ya dibujados. No genera texturas nuevas.
python convertir.py --java RUTA_PACK_EXTRAIDO --vanilla RUTA_BEDROCK_SAMPLES_RESOURCE_PACK
"""
import argparse, copy, hashlib, json, math, re, shutil, uuid, zipfile
from pathlib import Path
from PIL import Image, ImageDraw

BASE = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument('--java', required=True, type=Path)
parser.add_argument('--vanilla', required=True, type=Path)
args = parser.parse_args()
SRC, VAN = args.java, args.vanilla
PACK = BASE/'resource_pack'
PACK.mkdir(parents=True, exist_ok=True)

def read(p):
    text = Path(p).read_text(encoding='utf-8-sig')
    # Quita comentarios sin tocar cadenas, URLs o escapes.
    text = re.sub(r'"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\r\n]*', lambda m: m[0] if m[0].startswith('"') else '', text)
    text = re.sub(r',\s*([}\]])', r'\1', text)
    return json.loads(text)

def put(path, value):
    path = Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def cp(src, dest):
    dest = Path(dest); dest.parent.mkdir(parents=True, exist_ok=True); shutil.copy2(src, dest)

def java_path(key, kind='textures', extension='.png'):
    namespace, key = key.split(':',1) if ':' in key else ('minecraft',key)
    return SRC/'assets'/namespace/kind/(key if key.endswith(extension) else key+extension)

def model_data(key):
    data=read(java_path(key,'models','.json'))
    if data.get('parent') and java_path(data['parent'],'models','.json').exists():
        parent=model_data(data['parent']); parent.update(data); data=parent
    return data

def texture(data):
    ts=data.get('textures',{}); t=ts.get('layer0', ts.get('0',ts.get('particle')))
    while t and t.startswith('#'): t=ts[t[1:]]
    if not t: raise ValueError('Modelo sin textura: '+str(data))
    return java_path(t)

def leaf(node):
    typ=node.get('type','').removeprefix('minecraft:')
    if typ=='model':return node['model']
    if typ=='condition':return leaf(node['on_false'])
    if typ=='select':
        for case in node['cases']:
            if 'gui' in case['when']:return leaf(case['model'])
        return leaf(node['fallback'])
    if typ=='range_dispatch':return leaf(node['fallback'])
    raise ValueError('Nodo no contemplado: '+str(node))

def all_leaves(node):
    if isinstance(node,dict):
        if node.get('type','').removeprefix('minecraft:')=='model':yield node['model']
        else:
            for v in node.values():yield from all_leaves(v)
    elif isinstance(node,list):
        for n in node:yield from all_leaves(n)

def save_image(im,path):
    path.parent.mkdir(parents=True,exist_ok=True); im.save(path)

def rc(geometry='default',tex='default',material='default',enchanted=True):
    return {'geometry':'Geometry.'+geometry,'materials':[{'*':f'variable.is_enchanted ? Material.enchanted : Material.{material}' if enchanted else 'Material.'+material}], 'textures':['Texture.'+tex]+(['Texture.enchanted'] if enchanted else [])}

manifest={'format_version':2,'header':{'name':'Croissants · Segunda edición | Quaso 1.0.0','description':'Items, armaduras, iconos e interfaces de Quaso para Geyser.','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'https://github.com/CrissyjuanxD/QuasoPlugin/bedrock/header')),'version':[1,0,0],'min_engine_version':[1,26,30]},'modules':[{'type':'resources','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'https://github.com/CrissyjuanxD/QuasoPlugin/bedrock/resources')),'version':[1,0,0]}]}
put(PACK/'manifest.json',manifest)
controllers={'controller.render.quaso.armor':rc(), 'controller.render.quaso.flowers':rc('flowers','flowers','default',False), 'controller.render.quaso.wings':rc('wings','wings','wings',False)}
icons={}; mappings={}; audit=[]; lang=[]
catalog=read(BASE/'catalogo_items.json')
for item in catalog:
    name=item['model']; base=item['java_item']; identifier='quaso:'+name
    key='quaso.'+name; dst='textures/items/quaso/'+name
    putname=item.get('display_name',name.replace('_',' ').capitalize())
    data=None; node=None
    if name=='air': save_image(Image.new('RGBA',(16,16)),PACK/(dst+'.png'))
    else:
        node=read(SRC/'assets/minecraft/items'/f'{name}.json')['model']; model=leaf(node); data=model_data(model)
        cp(texture(data),PACK/(dst+'.png'))
    icons[key]={'textures':dst}
    opts={'icon':key,'display_handheld':bool(data and ('handheld' in data.get('parent','') or 'spear' in data.get('parent',''))),'allow_offhand':True,'creative_category':'items'}
    entry={'type':'definition','model':'minecraft:'+name,'bedrock_identifier':identifier,'display_name':putname,'bedrock_options':opts}
    if 'max_stack_size' in item: entry['components']={'minecraft:max_stack_size':item['max_stack_size']}
    # Hereda las propiedades del material real (no solo el nombre del modelo).
    mappings.setdefault(base,[]).append(entry)
    lang.append(f'item.{identifier}={putname}')
    audit.append({'model':name,'java_item':base,'bedrock_identifier':identifier,'source_texture':str(texture(data).relative_to(SRC)) if data else None})
    if base=='minecraft:bow' and node:
        variants={Path(k).name:k for k in all_leaves(node)}
        a=read(VAN/'attachables/bow.json'); desc=a['minecraft:attachable']['description'];desc['identifier']=identifier
        desc['textures']['default']=dst
        for i in range(3):
            k=next(k for n,k in variants.items() if n.endswith('_pulling_'+str(i)))
            td=dst+'_pulling_'+str(i);cp(texture(model_data(k)),PACK/(td+'.png'));desc['textures']['bow_pulling_'+str(i)]=td
        put(PACK/'attachables'/f'{name}.json',a)
    if base.endswith(('_helmet','_chestplate','_leggings','_boots')) and ('warden' in name or 'floral' in name):
        slot=base.split('_')[-1]; family='floral' if 'floral' in name else 'warden_alado' if name=='peto_warden_alado' else 'warden'
        equip=read(SRC/'assets/minecraft/equipment'/f'{family}.json')['layers']; layers=equip['humanoid_leggings' if slot=='leggings' else 'humanoid']
        opts['protection_value']={'helmet':3,'chestplate':8,'leggings':6,'boots':3}[slot]+(1 if family.startswith('warden') else 0)
        opts['creative_category']='equipment'
        for player in [False,True]:
            suffix='.player' if player else ''
            a=read(VAN/'attachables'/f'netherite_{slot}{suffix}.json');d=a['minecraft:attachable']['description'];d['identifier']=identifier+suffix
            if player:d['item']={identifier:"query.owner_identifier == 'minecraft:player'"}
            kind='humanoid_leggings' if slot=='leggings' else 'humanoid'
            for index,layer in enumerate(layers):
                tex=layer['texture'].split(':')[-1]; dest=f'textures/models/armor/quaso/{kind}/{tex}'
                cp(SRC/'assets/minecraft/textures/entity/equipment'/kind/(tex+'.png'),PACK/(dest+'.png'))
                d['textures']['default' if index==0 else 'flowers']=dest
            d['render_controllers']=['controller.render.quaso.armor']
            if len(layers)>1:
                # Segunda capa independiente ligeramente elevada sobre la armadura.
                geo=d['geometry']['default']; namegeo=f'geometry.quaso.flowers.{slot}'+suffix
                put(PACK/'models/entity'/f'flowers_{slot}{suffix}.json',{'format_version':'1.8.0',namegeo+':'+geo:{'bones':[{'name':b,'inflate':1.2 if slot!='leggings' else .65} for b in {'helmet':['head','hat'],'chestplate':['body','leftArm','rightArm'],'leggings':['body','leftLeg','rightLeg'],'boots':['leftLeg','rightLeg']}[slot]]}})
                d['geometry']['flowers']=namegeo;d['render_controllers'].append('controller.render.quaso.flowers')
            if family=='warden_alado':
                wings='textures/models/armor/quaso/warden_alado_wings';cp(SRC/'assets/minecraft/textures/entity/equipment/wings/warden_alado.png',PACK/(wings+'.png'))
                el=read(VAN/'attachables/elytra.json')['minecraft:attachable']['description'];d['textures']['wings']=wings;d['geometry']['wings']='geometry.elytra';d['materials']['wings']='elytra';d.setdefault('animations',{}).update(el['animations']);d.setdefault('scripts',{}).setdefault('animate',[]).append('default_controller');d['render_controllers'].append('controller.render.quaso.wings')
            put(PACK/'attachables'/f'{name}{suffix}.json',a)
    if name=='escudo_floral':
        a=read(VAN/'attachables/shield.entity.json');d=a['minecraft:attachable']['description'];d['identifier']=identifier;d['textures']['default']=dst;d['geometry']['default']='geometry.quaso.shield'
        d['scripts']['pre_animation']=[x.replace('minecraft:shield',identifier) for x in d['scripts']['pre_animation']]
        put(PACK/'attachables'/f'{name}.json',a)
        # La silueta del sprite se extruye; no se usa el atlas vanilla incompatible.
        put(PACK/'models/entity/quaso_shield.json',{'format_version':'1.16.0','minecraft:geometry':[{'description':{'identifier':'geometry.quaso.shield','texture_width':16,'texture_height':16},'bones':[{'name':'shield','binding':'q.item_slot_to_bone_name(c.item_slot)','texture_meshes':[{'texture':'default','local_pivot':[0,.5,0],'use_pixel_depth':True}]}]}]})
    if base.endswith('_spear'):
        handheld=model_data(leaf(node['fallback'])); hdst=dst+'_in_hand';cp(texture(handheld),PACK/(hdst+'.png'))
        # Geometría nativa de lanzas: malla plana generada a partir del sprite 32×32.
        a={'format_version':'1.10.0','minecraft:attachable':{'description':{'identifier':identifier,'materials':{'default':'entity_alphatest','enchanted':'entity_alphatest_glint'},'textures':{'default':hdst,'enchanted':'textures/misc/enchanted_item_glint'},'geometry':{'default':'geometry.spear'},'render_controllers':['controller.render.item_default']}}}
        put(PACK/'attachables'/f'{name}.json',a)

put(PACK/'textures/item_texture.json',{'resource_pack_name':'quaso','texture_name':'atlas.items','texture_data':icons})
put(BASE/'custom_mappings/quaso_items.json',{'format_version':2,'items':mappings})
put(BASE/'inventario_conversion.json',audit)
put(PACK/'render_controllers/quaso_armor.json',{'format_version':'1.8.0','render_controllers':controllers})
put(PACK/'texts/languages.json',['es_ES','es_MX','en_US'])
for language in ['es_ES','es_MX','en_US']:
    (PACK/'texts'/f'{language}.lang').write_text('\n'.join(lang)+'\n',encoding='utf-8')

# Fuentes: las celdas Bedrock no admiten height/ascent Java ni avances negativos.
fonts=read(SRC/'assets/minecraft/font/default.json')['providers']; pages={}; glyphs=[]; rankstrings={}; next_rank=0xED00
for p in fonts:
    if p.get('type')!='bitmap':continue
    f=p['file']; code=ord(p['chars'][0][0]); ns=f.split(':')[0]
    if ns=='along' or code==0xE076 or code>=0xEFF0:continue
    im=Image.open(java_path(f)).convert('RGBA'); crop=im.getbbox()
    if crop:im=im.crop(crop)
    cell=Image.new('RGBA',(16,16)); thumb=im.copy();thumb.thumbnail((16,16),Image.Resampling.NEAREST);cell.alpha_composite(thumb,((16-thumb.width)//2,(16-thumb.height)//2))
    page=code>>8; atlas=pages.setdefault(page,Image.new('RGBA',(256,256)));atlas.alpha_composite(cell,((code%16)*16,((code%256)//16)*16))
    glyphs.append({'code':f'U+{code:04X}','character':chr(code),'source':f,'java_height':p.get('height'),'java_ascent':p.get('ascent')})
    if ns=='theranks':
        im=im.resize((round(im.width*16/im.height),16),Image.Resampling.NEAREST)
        chars=[]
        for x in range(0,im.width,16):
            c=next_rank;next_rank+=1;tile=im.crop((x,0,min(x+16,im.width),min(im.height,16)));atlas=pages.setdefault(c>>8,Image.new('RGBA',(256,256)));atlas.alpha_composite(tile,((c%16)*16,((c%256)//16)*16));chars.append(chr(c))
        rankstrings[Path(f).stem]=''.join(chars)
for page,atlas in pages.items():save_image(atlas,PACK/'font'/f'glyph_{page:02X}.png')
put(BASE/'unicodes.json',{'originales':glyphs,'rangos_anchos_bedrock':rankstrings})
logo=Image.open(java_path('minecraft:croissants_scoreboard.png')).convert('RGBA');logo=logo.crop(logo.getbbox());save_image(logo,PACK/'textures/ui/quaso/croissants.png')
icon=Image.new('RGBA',(256,256),(28,17,48,255));thumb=logo.copy();thumb.thumbnail((240,220),Image.Resampling.NEAREST);icon.alpha_composite(thumb,((256-thumb.width)//2,(256-thumb.height)//2));save_image(icon,PACK/'pack_icon.png')
# Interfaces de cofres clásicas: se conserva la cuadrícula y sus botones.
chest=read(VAN/'ui/chest_screen.json'); gui_codes={}
for p in fonts:
    if p.get('type')=='bitmap' and p.get('file','').startswith('along:'):
        name=Path(p['file']).stem;code=p['chars'][0];gui_codes[name]=code
        source=java_path(p['file']);bedrock=source.with_stem(source.stem+'_bedrock');source=bedrock if bedrock.exists() else source
        cp(source,PACK/'textures/ui/quaso'/f'{name}.png')
        chest['quaso_'+name]={'type':'image','texture':f'textures/ui/quaso/{name}','size':[256,256],'anchor_from':'top_left','anchor_to':'top_left','offset':[0,0],'layer':3,'visible':f"(not (($container_title - '{code}') = $container_title))"}
for panel in ['large_chest_panel','small_chest_panel']:
    root=next(c for c in chest[panel]['controls'] if 'root_panel@common.root_panel' in c)['root_panel@common.root_panel']
    for name in gui_codes:root['controls'].insert(1,{f'quaso_{name}@chest.quaso_{name}':{}})
chest['chest_label']['visible']='('+' and '.join(f"(($container_title - '{code}') = $container_title)" for code in gui_codes.values())+')'
put(PACK/'ui/chest_screen.json',chest)
progress=read(VAN/'ui/progress_screen.json')
progress['quaso_logo']={'type':'image','texture':'textures/ui/quaso/croissants','size':[128,round(128*logo.height/logo.width)],'anchor_from':'top_middle','anchor_to':'top_middle','offset':[0,12],'layer':20}
progress['progress_panel']['controls'].append({'quaso_logo@progress.quaso_logo':{}})
put(PACK/'ui/progress_screen.json',progress)
if (VAN/'LICENSE.md').exists():cp(VAN/'LICENSE.md',BASE/'LICENCIA-BEDROCK-SAMPLES.md')
print(f'Convertidos {len(catalog)} registros de items, {len(glyphs)} glifos, {len(gui_codes)} interfaces; armaduras y arcos incluidos.')
