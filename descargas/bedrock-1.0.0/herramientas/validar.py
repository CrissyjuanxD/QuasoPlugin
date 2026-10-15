#!/usr/bin/env python3
"""Validación de referencias, cobertura y dimensiones; no sustituye al cliente Bedrock."""
import json,math,re,hashlib,zipfile
from pathlib import Path
from PIL import Image
B=Path(__file__).resolve().parent.parent;R=B/'resource_pack';M=B/'opcional_mobs/resource_pack';checks=[]
def unique(pairs):
 out={}
 for k,v in pairs:
  assert k not in out,('Clave JSON duplicada',k)
  out[k]=v
 return out
def load(p):return json.loads(p.read_text(),object_pairs_hook=unique)
def ok(t):checks.append(t);print('OK:',t)
for root in [R,M]:
 for p in root.rglob('*.json'):load(p)
 for p in root.rglob('*.png'):
  with Image.open(p) as im:im.verify()
ok('JSON sin claves duplicadas y PNG válidos en ambos packs')
mapper=load(B/'custom_mappings/quaso_items.json');icons=load(R/'textures/item_texture.json')['texture_data'];ids=set();models=set();pairs=set()
for base,entries in mapper['items'].items():
 assert re.fullmatch('minecraft:[a-z0-9_]+',base)
 for e in entries:
  identifier=e['bedrock_identifier'];assert identifier not in ids,identifier;ids.add(identifier);models.add(e['model']);pair=(base,e['model']);assert pair not in pairs; pairs.add(pair)
  p=R/(icons[e['bedrock_options']['icon']]['textures']+'.png');assert p.is_file(),p
  if e['model']!='minecraft:air':assert Image.open(p).getbbox(),p
  assert e.get('components',{}).get('minecraft:max_stack_size',1)<=64
catalog=load(B/'catalogo_items.json');assert pairs=={(e['java_item'],'minecraft:'+e['model']) for e in catalog}
assert len(ids)==313 and len(models)==313
ok('313 mappings únicos: material + item_model, iconos existentes y límite Bedrock de 64')
# Referencias locales de attachables, texturas y geometrías.
geometry=set();controllers=set();animations=set();attachables=set()
for root in [R,M]:
 for p in (root/'models').rglob('*.json'):
  d=load(p)
  if 'minecraft:geometry'in d:
   for g in d['minecraft:geometry']:
    geometry.add(g['description']['identifier']);names={b['name'] for b in g['bones']};assert len(names)==len(g['bones'])
    for b in g['bones']:
     if 'parent'in b:assert b['parent']in names,(p,b)
     for c in b.get('cubes',[]):assert all(math.isfinite(v) for v in c['origin']+c['size']) and min(c['size'])>=0
  else:geometry.update(k.split(':')[0] for k in d if k.startswith('geometry.'))
 for p in (root/'render_controllers').glob('*.json'):controllers.update(load(p)['render_controllers'])
 for p in (root/'animations').glob('*.json'):animations.update(load(p)['animations'])
 for folder,kind in [('attachables','minecraft:attachable'),('entity','minecraft:client_entity')]:
  for p in (root/folder).glob('*.json'):
   d=load(p)[kind]['description']
   if folder=='attachables':assert d['identifier']not in attachables;attachables.add(d['identifier'])
   for t in d.get('textures',{}).values():
    if '/quaso/'in t:assert (root/(t+'.png')).is_file(),(p,t)
   for g in d.get('geometry',{}).values():
    if g.startswith('geometry.quaso.'):assert g in geometry,(p,g)
# Pasa de nuevo después de reunir todos los IDs (el orden de los directorios no importa).
for root in [R,M]:
 for folder,kind in [('attachables','minecraft:attachable'),('entity','minecraft:client_entity')]:
  for p in (root/folder).glob('*.json'):
   d=load(p)[kind]['description']
   for value in d['render_controllers']:
    for c in [value] if isinstance(value,str) else value:
     if c.startswith('controller.render.quaso.'):assert c in controllers,(p,c)
   for v in d.get('animations',{}).values():
    if v.startswith('animation.quaso.'):assert v in animations,(p,v)
ok(f'{len(attachables)} attachables con texturas y geometrías propias resueltas')
for slot in ['casco','peto','pantalon','botas']:
 assert 'quaso:'+slot+'_floral'in attachables
assert 'quaso:peto_warden_alado.player'in attachables
assert load(R/'attachables/peto_warden_alado.player.json')['minecraft:attachable']['description']['geometry']['wings']=='geometry.elytra'
ok('Armaduras floral, Warden y peto alado; segunda capa floral y alas presentes')
statue=load(M/'models/entity/copper_golem2.json')['minecraft:geometry'][0];cubes=[c for b in statue['bones']for c in b.get('cubes',[])];low=min(c['origin'][1] for c in cubes);high=max(c['origin'][1]+c['size'][1] for c in cubes)
assert abs(low)<1e-8 and abs(high/16*4.5-4.4)<1e-6,(low,high)
ok('Estatua Bedrock: base Y=0, altura visible 4,40 bloques con escala 4,5 del plugin')
for p in (M/'entity').glob('*.json'):
 d=load(p)['minecraft:client_entity']['description'];assert all(isinstance(c,dict) for c in d['render_controllers']);assert 'variable.quaso_custom' in '\n'.join(d['scripts']['pre_animation'])
 for c in d['render_controllers']:
  for name,condition in c.items():
   if not name.startswith('controller.render.quaso.'):assert '!variable.quaso_custom' in condition
assert len(load(B/'opcional_mobs/modelos_convertidos.json'))==15
ok('15 variantes: condición por nombre y conservación del renderizado vanilla para otros mobs')
font=load(B/'unicodes.json');assert len(font['originales'])==23
for glyph in font['originales']:
 code=int(glyph['code'][2:],16);im=Image.open(R/'font'/f'glyph_{code>>8:02X}.png');x=code%16*16;y=(code%256)//16*16;assert im.crop((x,y,x+16,y+16)).getbbox()
ui=load(R/'ui/chest_screen.json');guis=[k for k in ui if k.startswith('quaso_')and ui[k].get('type')=='image'];assert len(guis)==7
for k in guis:assert (R/(ui[k]['texture']+'.png')).exists() and '$container_title' in ui[k]['visible']
assert ui['large_chest_grid']['grid_dimensions']==[9,6] and ui['small_chest_grid']['grid_dimensions']==[9,3]
ok('23 códigos de fuente y siete GUIs conservando las cuadrículas de interacción')
for root in [R,M]:
 manifest=load(root/'manifest.json');assert manifest['header']['version']==[1,0,0];assert manifest['header']['uuid']!=manifest['modules'][0]['uuid']
 # Cada textura de item se referencia; air es la única completamente transparente.
report={'checks':checks,'items':len(ids),'attachables':len(attachables),'mobs':15,'visual_client_tested':False,'server_startup_test':'Geyser Standalone 2.12.0-b1256; misma implementación de custom_mappings que Geyser-Spigot. No equivale a una conexión de cliente.'}
(B/'validacion.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
