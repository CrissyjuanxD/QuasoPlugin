#!/usr/bin/env python3
"""Conversión CEM a geometría Bedrock. Selección por nombre; complemento independiente.
Las animaciones CEM no son ejecutables en Bedrock: se adaptan locomoción y mirada a Molang.
"""
import argparse,copy,json,math,re,shutil,uuid
from pathlib import Path
P=argparse.ArgumentParser();P.add_argument('--java',type=Path,required=True);P.add_argument('--vanilla',type=Path,required=True);a=P.parse_args()
B=Path(__file__).resolve().parent.parent;S=a.java/'assets/minecraft';V=a.vanilla;R=B/'opcional_mobs/resource_pack'
def load(p):return json.loads(p.read_text())
def put(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def cp(p,q):q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
variants={'zombie':{2:['Zombie Floral'],3:['Warden Zombie','Infested Zombie'],4:['Zombie BloodMoon']},'skeleton':{3:['Infested Skeleton'],4:['Esqueleto BloodMoon']},'creeper':{2:['Creeper Floral'],3:['Infested Creeper'],4:['Creeper BloodMoon']},'spider':{2:['Spider Floral','Araña Floral'],4:['Araña BloodMoon']},'cave_spider':{3:['Infested Cave Spider']},'ghast':{3:['Infested Ghast']},'bee':{2:['Abeja Floral'],3:['Mini Abeja Floral','Corrupted Bee']},'copper_golem':{2:['Estatua de Recompensas']}}
rcs={};anims={};audit=[]
def names_condition(names):
 strings=[]
 prefixes=['','§l']+['§'+c+b for c in '0123456789abcdefghijmnpqstu' for b in ['', '§l']]
 for name in names:
  for prefix in prefixes:
   strings.extend([prefix+name,'§r'+prefix+name,prefix+name+'§r'])
 return 'query.is_name_any('+', '.join("'"+n.replace("'","\\'")+"'" for n in dict.fromkeys(strings))+')'
def texpath(key):return S/(key.split(':')[-1]+('' if key.endswith('.png') else '.png'))
def cube(box,off,scale=1,java_local=False):
 co=box['coordinates'];origin=co[:3];size=co[3:]
 if java_local:origin=[origin[0],-origin[1]-size[1],origin[2]]
 c={'origin':[round((origin[i]+off[i])*scale,6) for i in range(3)],'size':[round(v*scale,6) for v in size]}
 if 'textureOffset' in box:
  u,v=box['textureOffset'];w,h,d=size
  # Explicita el UV antes de escalar: el ghast crece 4,5x, su atlas no.
  coords={'north':[u+d,v+d,w,h],'east':[u,v+d,d,h],'south':[u+d+w+d,v+d,w,h],'west':[u+d+w,v+d,d,h],'up':[u+d,v,w,d],'down':[u+d+w,v+d,w,-d]}
  c['uv']={face:{'uv':value[:2],'uv_size':value[2:]} for face,value in coords.items()}
 else:
  c['uv']={}
  for k,f in [('North','north'),('South','south'),('West','west'),('East','east'),('Up','up'),('Down','down')]:
   if 'uv'+k in box:
    u,v,U,W=box['uv'+k];c['uv'][f]={'uv':[u,v],'uv_size':[U-u,W-v]}
 if box.get('sizeAdd'):c['inflate']=box['sizeAdd']*scale
 return c
for mob,entries in variants.items():
 entity=load(V/'entity'/f'{mob}.entity.json');desc=entity['minecraft:client_entity']['description'];desc.setdefault('materials',{}).update({'quaso':'entity_alphatest','quaso_glow':'entity_emissive_alpha'});scripts=desc.setdefault('scripts',{});pre=scripts.setdefault('pre_animation',[]);animate=scripts.setdefault('animate',[])
 conditions=[]
 for index,names in entries.items():
  ident=mob+str(index);cem=load(S/'optifine/cem'/f'{ident}.jem');bones=[];bone_anims={};scale=4/9 if mob=='copper_golem' else 4.5 if mob=='ghast' else 1
  native={}
  if mob in ['ghast','bee']:
   n=load(V/'models/entity'/f'{mob}.geo.json')['minecraft:geometry'][0];native={b['name']:b for b in n['bones']}
  def add(part,offset=(0,0,0),parent=None,top=False):
   name='quaso_'+part.get('id',part.get('part','part'))
   if any(b['name']==name for b in bones):name+='_'+str(len(bones))
   absolute='y' in part.get('invertAxis','');t=part.get('translate',[0,0,0]);off=list(offset)
   if top and absolute:
    pivot=[t[0],-t[1],-t[2]]
    if mob=='copper_golem':pivot=[0,0,0]
   elif top:
    p=part['part']
    if mob=='ghast':
     pivot=[0,32/4.5,0] if p=='body' else [v/4.5 for v in native['tentacles_'+str(int(p[8:])-1)]['pivot']]
    elif mob=='bee':
     aliases={'torso':'body','left_wing':'leftwing_bone','right_wing':'rightwing_bone','front_legs':'leg_front','middle_legs':'leg_mid','back_legs':'leg_back'};pivot=native.get(aliases.get(p,p),{}).get('pivot',[0,5,0])
    else:pivot=[0,0,0]
    off=list(pivot)
   else:
    off=[offset[i]+t[i]*(1 if absolute or i!=1 else -1) for i in range(3)];pivot=off
   b={'name':name,'pivot':[round(v*scale,6) for v in pivot],'cubes':[cube(box,off,scale,not absolute) for box in part.get('boxes',[])]}
   if parent:b['parent']=parent
   if part.get('rotate'):b['rotation']=part['rotate']
   if part.get('mirrorTexture')=='u':b['mirror']=True
   bones.append(b)
   for sub in part.get('submodels',[]):add(sub,off,name)
   if top:
    p=part['part'];walk='math.cos(query.modified_distance_moved * 38.17) * math.min(query.modified_move_speed * 2, 1) * 35'
    if p=='head' and mob not in ['bee','copper_golem']:bone_anims[name]={'rotation':['query.target_x_rotation','query.target_y_rotation',0]}
    if p in ['left_leg','right_leg'] and mob!='copper_golem':bone_anims[name]={'rotation':[('' if p=='left_leg' else '-')+'('+walk+')',0,0]}
    if p in ['left_arm','right_arm'] and mob in ['zombie','skeleton']:bone_anims[name]={'rotation':[-75 if mob=='zombie' else 0,0,0]}
    if p.startswith('leg') and p[3:].isdigit():
     i=int(p[3:]);sign=1 if i%2 else -1
     if mob in ['spider','cave_spider']:bone_anims[name]={'rotation':[0,f'{sign*(45-(i-1)//2*30)} + ({walk}) * 0.3',sign*35]}
     else:bone_anims[name]={'rotation':[f'{sign} * ({walk})',0,0]}
    if p.endswith('_wing'):
     sign=1 if p=='left_wing' else -1;bone_anims[name]={'rotation':[0,0,f'{sign} * math.sin(query.life_time * 1600) * 35']}
    if p.startswith('tentacle'):bone_anims[name]={'rotation':[f'10 + math.sin(query.life_time * 170 + {len(bones)*25}) * 8',0,0]}
  for part in cem['models']:add(part,top=True)
  # Mantiene puntos de sujeción para los objetos de los esqueletos.
  if mob in ['skeleton','zombie']:
   for side in ['left','right']:
    parent='quaso_'+side+'_arm'
    if any(b['name']==parent for b in bones):bones.append({'name':side+'Item','parent':parent,'pivot':[6 if side=='left' else -6,15,1]})
  geo='geometry.quaso.'+ident
  put(R/'models/entity'/f'{ident}.json',{'format_version':'1.16.0','minecraft:geometry':[{'description':{'identifier':geo,'texture_width':cem['textureSize'][0],'texture_height':cem['textureSize'][1],'visible_bounds_width':12 if mob=='ghast' else 6,'visible_bounds_height':12 if mob=='ghast' else 6,'visible_bounds_offset':[0,1,0]},'bones':bones}]})
  texture=texpath(cem['texture']) if cem.get('texture') else S/'optifine/random/entity'/mob/f'{ident}.png'
  target=f'textures/entity/quaso/{ident}';cp(texture,R/(target+'.png'))
  desc['textures'][ident]=target;desc['geometry'][ident]=geo
  var='variable.quaso_'+ident;conditions.append(var);pre.append(var+' = '+names_condition(names)+';')
  controller='controller.render.quaso.'+ident
  rcs[controller]={'geometry':'Geometry.'+ident,'materials':[{'*':'Material.quaso'}],'textures':['Texture.'+ident]}
  if ident=='bee2':
   skins=['Texture.bee2']
   for state in ['nectar','angry','angry_nectar']:
    source=S/'optifine/random/entity/bee'/f'bee_{state}2.png';dest=f'textures/entity/quaso/bee2_{state}'
    cp(source,R/(dest+'.png'));desc['textures']['bee2_'+state]=dest;skins.append('Texture.bee2_'+state)
   rcs[controller]['arrays']={'textures':{'Array.skins':skins}}
   rcs[controller]['textures']=["Array.skins[query.property('minecraft:has_nectar') + query.is_angry * 2]"]
  desc.setdefault('render_controllers',[]).append({controller:var})
  emission=texture.with_stem(texture.stem+'_e')
  if emission.exists():
   cp(emission,R/(target+'_e.png'));desc['textures'][ident+'_e']=target+'_e';rcs[controller+'_e']={'geometry':'Geometry.'+ident,'materials':[{'*':'Material.quaso_glow'}],'textures':['Texture.'+ident+'_e']};desc['render_controllers'].append({controller+'_e':var})
  if bone_anims:
   animation='animation.quaso.'+ident;anims[animation]={'loop':True,'bones':bone_anims};desc.setdefault('animations',{})[ident]=animation;animate.append({ident:var})
  audit.append({'model':ident,'names':names,'source':str(texture.relative_to(S)),'cubes':sum(len(b['cubes']) for b in bones if 'cubes' in b),'emissive':emission.exists()})
 pre.append('variable.quaso_custom = '+' || '.join(conditions)+';')
 # Sólo oculta los renderizadores vanilla cuando coincide uno de los nombres del plugin.
 rendered=[]
 for value in desc['render_controllers']:
  if isinstance(value,str):rendered.append({value:'!variable.quaso_custom'})
  elif any(k.startswith('controller.render.quaso.') for k in value):rendered.append(value)
  else:rendered.append({k:'('+str(v)+') && !variable.quaso_custom' for k,v in value.items()})
 desc['render_controllers']=rendered;put(R/'entity'/f'{mob}.entity.json',entity)
put(R/'render_controllers/quaso_mobs.json',{'format_version':'1.8.0','render_controllers':rcs});put(R/'animations/quaso_mobs.json',{'format_version':'1.8.0','animations':anims})
main=load(B/'resource_pack/manifest.json');manifest={'format_version':2,'header':{'name':'Quaso · Mobs por nombre 1.0.0 (opcional)','description':'Modelos florales, Warden Cave, BloodMoon y estatua. Requiere comprobar renderizado en el cliente.','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'quaso-bedrock-mobs-header')),'version':[1,0,0],'min_engine_version':[1,26,50]},'modules':[{'type':'resources','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'quaso-bedrock-mobs-module')),'version':[1,0,0]}],'dependencies':[{'uuid':main['header']['uuid'],'version':[1,0,0]}]};put(R/'manifest.json',manifest);cp(B/'resource_pack/pack_icon.png',R/'pack_icon.png');put(B/'opcional_mobs/modelos_convertidos.json',audit)
print('Convertidos',len(audit),'modelos opcionales con selección por nombre y fallback vanilla.')
