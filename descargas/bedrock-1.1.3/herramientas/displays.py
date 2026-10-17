#!/usr/bin/env python3
"""Modelos de los displays usados por Quaso; caché reproducible de recursos originales."""
import base64, copy, hashlib, io, json, math, re, urllib.request
from pathlib import Path
from PIL import Image
BASE=Path(__file__).resolve().parent.parent
PACK=BASE/'resource_pack'
CACHE=BASE/'herramientas/display-originals'
REV='247b188d4739ce437b021e9b5cffbc1569968d16'
def put(p,d):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def source(path):
 p=CACHE/path
 if not p.exists():
  p.parent.mkdir(parents=True,exist_ok=True)
  with urllib.request.urlopen('https://raw.githubusercontent.com/misode/mcmeta/'+REV+'/assets/minecraft/'+path,timeout=30) as r:p.write_bytes(r.read())
 return p

def load_model(key):
 key=key.removeprefix('minecraft:');d=json.loads(source('models/'+key+'.json').read_text())
 parent=load_model(d['parent']) if d.get('parent') and not d['parent'].endswith('block/block') else {}
 return {**parent,**d,'textures':{**parent.get('textures',{}),**d.get('textures',{})}}

def hierarchy():
 bones=[]
 for n,p in [('translation',None),('left','translation'),('scale','left'),('right','scale'),('item_origin','right')]:
  b={'name':n,'pivot':[0,0,0]}
  if p:b['parent']=p
  bones.append(b)
 return bones

catalog=[{'id':0,'key':'air'}];geometries=[];textures={'m0':'textures/entity/quaso/display/empty'}
Image.new('RGBA',(16,16)).save('/tmp/quaso-empty.png')
(PACK/'textures/entity/quaso/display').mkdir(parents=True,exist_ok=True)
(PACK/'textures/entity/quaso/display/empty.png').write_bytes(Path('/tmp/quaso-empty.png').read_bytes())
def geometry(i,bones,w=128,h=128):
 geometries.append({'description':{'identifier':'geometry.quaso.display_'+str(i),'texture_width':w,'texture_height':h,
  'visible_bounds_width':32,'visible_bounds_height':32,'visible_bounds_offset':[0,0,0]},'bones':bones})
geometry(0,hierarchy(),16,16)
# Block states are matched by their names and properties, never protocol numeric IDs.
materials=['honeycomb_block','packed_ice','spawner','sculk_catalyst','purple_glazed_terracotta','purple_stained_glass','smooth_stone_slab','pointed_dripstone','ancient_debris','nether_quartz_ore','nether_gold_ore']
materials += [prefix+m+'_ore' for prefix in ['','deepslate_'] for m in ['coal','copper','iron','gold','lapis','redstone','emerald','diamond']]
materials += ['cyan_glazed_terracotta','lime_glazed_terracotta','gray_glazed_terracotta']
seen={}
for material in materials:
 state=json.loads(source('blockstates/'+material+'.json').read_text())
 for conditions,v in state['variants'].items():
  if isinstance(v,list):v=v[0]
  model=load_model(v['model']);signature=json.dumps(v,sort_keys=True)
  identifier=seen.get(signature)
  if identifier is None:
   identifier=len(textures);seen[signature]=identifier
   atlas=Image.new('RGBA',(128,128));offsets={}
   bones=hierarchy()
   modelroot={'name':'model','parent':'item_origin','pivot':[-8,8,8],
              'rotation':[-v.get('x',0),-v.get('y',0),0]}
   bones.append(modelroot)
   def resolve(t,rotation=0):
    while isinstance(t,str) and t.startswith('#'):t=model['textures'][t[1:]]
    if isinstance(t,dict):t=t['sprite']
    t=t.removeprefix('minecraft:')
    key=(t,rotation)
    if key not in offsets:
     im=Image.open(source('textures/'+t+'.png')).convert('RGBA')
     im=im.crop((0,0,im.width,im.width)).resize((16,16),Image.Resampling.NEAREST)
     if rotation:im=im.rotate(-rotation)
     k=len(offsets);offsets[key]=((k%8)*16,(k//8)*16);atlas.paste(im,offsets[key])
    return offsets[key]
   for idx,e in enumerate(model.get('elements',[])):
    a=e['from'];b=e['to'];cube={'origin':[-b[0],a[1],a[2]],'size':[b[j]-a[j] for j in range(3)],'uv':{}}
    for face,f in e['faces'].items():
     u,vv=resolve(f['texture'],f.get('rotation',0))
     uv=f.get('uv')
     if uv is None:
      uv={'north':[16-b[0],16-b[1],16-a[0],16-a[1]],'south':[a[0],16-b[1],b[0],16-a[1]],
          'west':[a[2],16-b[1],b[2],16-a[1]],'east':[16-b[2],16-b[1],16-a[2],16-a[1]],
          'up':[a[0],a[2],b[0],b[2]],'down':[a[0],16-b[2],b[0],16-a[2]]}[face]
     # Same per-face UV convention as the Blockbench Bedrock exporter.
     target=face
     start=[u+uv[0],vv+uv[1]];size=[uv[2]-uv[0],uv[3]-uv[1]]
     if face in ('up','down'):
      start=[u+uv[2],vv+uv[3]];size=[-size[0],-size[1]]
     entry={'uv':start,'uv_size':size}
     cube['uv'][target]=entry
    bone={'name':'element_'+str(idx),'parent':'model','pivot':[0,0,0],'cubes':[cube]}
    if 'rotation' in e:
     rot=e['rotation'];p=rot['origin'];bone['pivot']=[-p[0],p[1],p[2]]
     bone['rotation']=[0,0,0];axis='xyz'.index(rot['axis']);bone['rotation'][axis]=rot['angle']*(-1 if axis<2 else 1)
     if rot.get('rescale'):
      factor=1/math.cos(math.radians(rot['angle']))
      # Scale the element in its local plane before its rotation.
      for j in range(3):
       if j==axis:continue
       origin=bone['pivot'][j];cube['origin'][j]=origin+(cube['origin'][j]-origin)*factor;cube['size'][j]*=factor
    bones.append(bone)
   geometry(identifier,bones)
   path='textures/entity/quaso/display/model_'+str(identifier);textures['m'+str(identifier)]=path;atlas.save(PACK/(path+'.png'))
  catalog.append({'id':identifier,'key':material,'block':'minecraft:'+material,
                  'states':dict(pair.split('=') for pair in conditions.split(',') if pair)})
# Seven textured heads compose the existing grave. Reuse their public skin textures.
src=(BASE.parents[1]/'src/main/java/SistemaTumbas/GravesManager.java').read_text()
profiles=[]
for encoded in re.findall(r'value:\\\"([A-Za-z0-9+/=]+)\\\"',src):
 try: url=json.loads(base64.b64decode(encoded))['textures']['SKIN']['url']
 except Exception:continue
 digest=url.rsplit('/',1)[1]
 if digest in profiles:continue
 profiles.append(digest);p=CACHE/'heads'/f'{digest}.png';p.parent.mkdir(parents=True,exist_ok=True)
 if not p.exists():
  with urllib.request.urlopen('https://textures.minecraft.net/texture/'+digest,timeout=30) as r:p.write_bytes(r.read())
 i=len(textures);bones=hierarchy()
 bones.append({'name':'head','parent':'item_origin','pivot':[-8,8,8],'cubes':[
  {'origin':[-12,4,4],'size':[8,8,8],'uv':[0,0]},
  {'origin':[-12,4,4],'size':[8,8,8],'uv':[32,0],'inflate':.25}]})
 im=Image.open(p).convert('RGBA');geometry(i,bones,im.width,im.height)
 path='textures/entity/quaso/display/head_'+str(i);textures['m'+str(i)]=path;im.save(PACK/(path+'.png'))
 catalog.append({'id':i,'key':'grave_head','profile':digest})
assert len(profiles)==7,profiles
put(BASE/'extension/src/main/resources/displays.json',catalog)
put(PACK/'models/entity/quaso_displays.json',{'format_version':'1.16.0','minecraft:geometry':geometries})
# T * left rotation * nonuniform scale * right rotation, with an item-only origin offset.
channels={p+'_'+a:(1 if p=='s' else 0) for p in ['t','l','s','r'] for a in 'xyz'}
init=['v.seq = q.property(\'quaso:revision\');','v.elapsed = 1000;']
pre=['(v.seq != q.property(\'quaso:revision\')) ? { '+''.join(f'v.old_{n}=v.{n};' for n in channels)+"v.seq=q.property('quaso:revision');v.elapsed=0;};", "v.elapsed=v.elapsed+q.delta_time;", "v.blend=math.min(1,v.elapsed/math.max(.001,q.property('quaso:duration')));"]
for n in channels:
 init.append(f"v.{n}=q.property('quaso:{n}');v.old_{n}=v.{n};")
 fn='lerprotate' if n[0] in 'lr' else 'lerp'
 pre.append(f"v.{n}=math.{fn}(v.old_{n},q.property('quaso:{n}'),v.blend);")
animation={'loop':True,'bones':{
 'translation':{'position':['-v.t_x*16','v.t_y*16','v.t_z*16']},
 'left':{'rotation':['-v.l_x','-v.l_y','v.l_z']},
 'scale':{'scale':['v.s_x','v.s_y','v.s_z']},
 'right':{'rotation':['-v.r_x','-v.r_y','v.r_z']},
 'item_origin':{'position':["q.property('quaso:item')*8","q.property('quaso:item')*-8","q.property('quaso:item')*-8"]}}}
put(PACK/'animations/quaso_displays.json',{'format_version':'1.8.0','animations':{'animation.quaso.displays':animation}})
put(PACK/'entity/quaso_display.entity.json',{'format_version':'1.10.0','minecraft:client_entity':{'description':{
 'identifier':'quaso:display','materials':{'default':'entity_alphatest','bright':'entity_emissive_alpha','glass':'entity_alphablend'},
 'textures':textures,'geometry':{'m'+str(i):'geometry.quaso.display_'+str(i) for i in range(len(textures))},
 'animations':{'transform':'animation.quaso.displays'},'scripts':{'initialize':init,'pre_animation':pre,'animate':['transform']},
 'render_controllers':['controller.render.quaso.displays']}}})
put(PACK/'render_controllers/quaso_displays.json',{'format_version':'1.8.0','render_controllers':{'controller.render.quaso.displays':{
 'arrays':{'geometries':{'Array.models':['Geometry.m'+str(i) for i in range(len(textures))]},'textures':{'Array.skins':['Texture.m'+str(i) for i in range(len(textures))]}},
 'geometry':"Array.models[q.property('quaso:display_model')]",'textures':["Array.skins[q.property('quaso:display_model')]"],
 'materials':[{'*':"q.property('quaso:translucent') ? Material.glass : (q.property('quaso:bright') ? Material.bright : Material.default)"}]}}})
put(CACHE/'sources.json',{'minecraft_assets':'https://github.com/misode/mcmeta/tree/'+REV+'/assets/minecraft',
 'heads':'Textures referenced by SistemaTumbas/GravesManager.java; https://textures.minecraft.net/texture/<hash>',
 'sha256':{p.relative_to(CACHE).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(CACHE.rglob('*')) if p.is_file() and p.name!='sources.json'}})
print(f'Displays: {len(textures)} modelos, {len(catalog)} estados y {len(profiles)} cabezas de tumba.')
