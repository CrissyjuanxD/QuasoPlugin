#!/usr/bin/env python3
"""Ajustes de geometría, iconos 3D y UI después de convertir.py."""
import json, math, argparse, re, shutil
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
P=argparse.ArgumentParser();P.add_argument('--java',type=Path,required=True);P.add_argument('--vanilla',type=Path,required=True);a=P.parse_args()
B=Path(__file__).resolve().parent.parent;R=B/'resource_pack';S=a.java;V=a.vanilla

def load(p):
 s=Path(p).read_text();s=re.sub(r'"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\r\n]*',lambda m:m[0] if m[0].startswith('"') else '',s);return json.loads(re.sub(r',\s*([}\]])',r'\1',s))
def put(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
# Dos iconos animados Java: primer fotograma, nunca la tira vertical completa.
for p in (R/'textures/items/quaso').glob('*.png'):
 source=S/'assets/minecraft/textures/item/custom'/p.name
 if source.with_suffix('.png.mcmeta').exists():
  im=Image.open(p);im.crop((0,0,im.width,im.width)).save(p)
# Bedrock no puede registrar pilas superiores a 64.
p=B/'custom_mappings/quaso_items.json';d=load(p)
for entries in d['items'].values():
 for e in entries:
  if e.get('components',{}).get('minecraft:max_stack_size',1)>64:e['components']['minecraft:max_stack_size']=64
put(p,d)
# Animaciones nativas de la lanza, tanto en primera como en tercera persona.
for p in (R/'attachables').glob('lanza_*.json'):
 d=load(p);q=d['minecraft:attachable']['description'];q['animations']={'held':'controller.animation.spear.held','held_first_person':'animation.spear.held_first_person','held_third_person':'animation.spear.held_third_person'};q['scripts']={'animate':['held']};put(p,d)
# Escudo: coloca su malla en el centro de la geometría original para reutilizar el bloqueo.
p=R/'models/entity/quaso_shield.json';d=load(p);bone=d['minecraft:geometry'][0]['bones'][0];bone['pivot']=[1,15.5,3];bone['texture_meshes'][0].update({'position':[1,28,-.5],'local_pivot':[0,0,0],'rotation':[0,0,0]});put(p,d)
# Estatua protectora: conserva los seis cubos y el atlas original, también como item sostenido.
m=load(S/'assets/minecraft/models/item/custom/statue_pr.json');tex=Image.open(S/'assets/minecraft/textures/item/custom/statue_pr.png').convert('RGBA');cubes=[]
for e in m['elements']:
 origin=[e['from'][0]-8,e['from'][1],e['from'][2]-8];size=[b-c for b,c in zip(e['to'],e['from'])];uv={}
 for face,f in e['faces'].items():
  u,v,u2,v2=[x*tex.width/16 for x in f['uv']];uv[face]={'uv':[u,v],'uv_size':[u2-u,v2-v]}
 cubes.append({'origin':origin,'size':size,'uv':uv})
geo={'format_version':'1.16.0','minecraft:geometry':[{'description':{'identifier':'geometry.quaso.statue_pr','texture_width':tex.width,'texture_height':tex.height,'visible_bounds_width':3,'visible_bounds_height':3,'visible_bounds_offset':[0,1,0]},'bones':[{'name':'statue','binding':'q.item_slot_to_bone_name(c.item_slot)','pivot':[0,0,0],'cubes':cubes}]}]};put(R/'models/entity/statue_pr.json',geo)
shutil.copy2(S/'assets/minecraft/textures/item/custom/statue_pr.png',R/'textures/items/quaso/statue_pr_atlas.png')
put(R/'animations/statue_pr.json',{'format_version':'1.8.0','animations':{'animation.quaso.statue_pr.held':{'loop':True,'bones':{'statue':{'scale':.35,'position':[0,12,0],'rotation':[0,0,90]}}}}})
put(R/'attachables/statue_pr.json',{'format_version':'1.10.0','minecraft:attachable':{'description':{'identifier':'quaso:statue_pr','materials':{'default':'entity_alphatest','enchanted':'entity_alphatest_glint'},'textures':{'default':'textures/items/quaso/statue_pr_atlas','enchanted':'textures/misc/enchanted_item_glint'},'geometry':{'default':'geometry.quaso.statue_pr'},'animations':{'held':'animation.quaso.statue_pr.held'},'scripts':{'animate':['held']},'render_controllers':['controller.render.item_default']}}})
# Render ortográfico del modelo real para el icono; no se muestra su atlas como si fuera un sprite.
faces=[]
for c in cubes:
 x,y,z=c['origin'];w,h,l=c['size'];X,Y,Z=x+w,y+h,z+l
 vertices={'north':[(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)],'south':[(X,Y,Z),(x,Y,Z),(x,y,Z),(X,y,Z)],'west':[(x,Y,Z),(x,Y,z),(x,y,z),(x,y,Z)],'east':[(X,Y,z),(X,Y,Z),(X,y,Z),(X,y,z)],'up':[(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],'down':[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]}
 for face,vs in vertices.items():faces.append((np.array(vs,float),c['uv'][face],{'north':.86,'south':.86,'east':.7,'west':.7,'up':1.,'down':.5}[face]))
yaw=math.radians(35);pitch=math.radians(25)
rot=np.array([[math.cos(yaw),0,-math.sin(yaw)],[math.sin(pitch)*math.sin(yaw),math.cos(pitch),math.sin(pitch)*math.cos(yaw)],[math.cos(pitch)*math.sin(yaw),-math.sin(pitch),math.cos(pitch)*math.cos(yaw)]])
allpts=np.concatenate([v@rot.T for v,_,_ in faces]);minxy=allpts[:,:2].min(axis=0);maxxy=allpts[:,:2].max(axis=0);scale=58/max(maxxy-minxy);center=(maxxy+minxy)/2
size=64;rgba=np.zeros((size,size,4),dtype=np.uint8);depth=np.full((size,size),-np.inf);pixels=np.array(tex)
for vs,uv,shade in faces:
 v=vs@rot.T;xy=(v[:,:2]-center)*[scale,-scale]+[32,32];matrix=np.column_stack((xy[1]-xy[0],xy[3]-xy[0]))
 if abs(np.linalg.det(matrix))<1e-6:continue
 inv=np.linalg.inv(matrix)
 for yy in range(max(0,int(xy[:,1].min())),min(size,int(xy[:,1].max())+1)):
  for xx in range(max(0,int(xy[:,0].min())),min(size,int(xy[:,0].max())+1)):
   u,t=inv@([xx+.5,yy+.5]-xy[0])
   if not (0<=u<=1 and 0<=t<=1):continue
   z=v[0,2]+u*(v[1,2]-v[0,2])+t*(v[3,2]-v[0,2])
   px=int(uv['uv'][0]+u*uv['uv_size'][0]);py=int(uv['uv'][1]+t*uv['uv_size'][1]);color=pixels[min(tex.height-1,max(0,py)),min(tex.width-1,max(0,px))].copy()
   if color[3] and z>depth[yy,xx]:color[:3]=(color[:3]*shade).astype(np.uint8);rgba[yy,xx]=color;depth[yy,xx]=z
Image.fromarray(rgba).save(R/'textures/items/quaso/statue_pr.png')
# Logo ancho en el scoreboard sin comprimirlo dentro de una celda de fuente.
p=V/'ui/scoreboards.json';d=load(p);controls=d['scoreboard_sidebar']['controls'][0]['main']['controls'];title=controls[0]['displayed_objective'];condition="(not ((#objective_sidebar_name - '\ue900') = #objective_sidebar_name))";title['bindings'].append({'binding_type':'view','source_property':'(not '+condition+')','target_property':'#visible'})
logo=Image.open(R/'textures/ui/quaso/croissants.png');height=round(110*logo.height/logo.width)
controls.append({'quaso_logo':{'type':'image','texture':'textures/ui/quaso/croissants','size':[110,height],'anchor_from':'top_middle','anchor_to':'bottom_middle','offset':[0,9],'layer':3,'bindings':[{'binding_name':'#objective_sidebar_name'},{'binding_type':'view','source_property':condition,'target_property':'#visible'}]}});put(R/'ui/scoreboards.json',d)
print('Acabados: iconos animados estáticos, lanzas, escudo, estatua 3D, scoreboard y límite de pilas Bedrock.')
# Evita que el fondo de las 54 casillas tape el dibujo del menú, conservando los botones.
p=R/'ui/chest_screen.json';d=load(p);normal=d['chest_label']['visible'];d['quaso_cell_image_panel@common.cell_image_panel']={'visible':normal};d['chest_grid_item@common.container_item']['$background_images']='chest.quaso_cell_image_panel'
for key,value in d.items():
 if key.startswith('quaso_') and value.get('type')=='image':value['offset']=[0,4]
put(p,d)
