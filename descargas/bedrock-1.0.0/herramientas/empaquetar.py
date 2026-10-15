#!/usr/bin/env python3
"""Empaqueta sin meter fuentes Java, credenciales o el config completo del servidor."""
import hashlib,json,zipfile
from pathlib import Path
B=Path(__file__).resolve().parent.parent

def archive(root,dest):
 dest.parent.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
  for p in sorted(root.rglob('*')):
   if p.is_file():z.write(p,p.relative_to(root))
 with zipfile.ZipFile(dest) as z:assert z.testzip() is None and 'manifest.json'in z.namelist()
main=B/'packs/QuasoTextures-1.0.0.mcpack';mobs=B/'opcional_mobs/QuasoMobs-1.0.0.mcpack'
archive(B/'resource_pack',main);archive(B/'opcional_mobs/resource_pack',mobs)
checks={str(p.relative_to(B)):hashlib.sha256(p.read_bytes()).hexdigest() for p in [main,mobs,B/'custom_mappings/quaso_items.json']}
(B/'SHA256SUMS.txt').write_text('\n'.join(v+'  '+k for k,v in checks.items())+'\n')
with zipfile.ZipFile(B/'Quaso-Bedrock-1.0.0-instalar.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 z.write(main,'plugins/Geyser-Spigot/packs/'+main.name);z.write(B/'custom_mappings/quaso_items.json','plugins/Geyser-Spigot/custom_mappings/quaso_items.json');z.write(mobs,'opcional_mobs/'+mobs.name)
 for name in ['README.md','UNICODES.md','VALIDACION.md','config-geyser-fragmento.yml','rangos_bedrock.yml','SHA256SUMS.txt']:
  if name=='README.md':
   text=(B/name).read_text().replace('(packs/QuasoTextures-1.0.0.mcpack)','(plugins/Geyser-Spigot/packs/QuasoTextures-1.0.0.mcpack)').replace('(custom_mappings/quaso_items.json)','(plugins/Geyser-Spigot/custom_mappings/quaso_items.json)')
   z.writestr(name,text)
  else:z.write(B/name,name)
print('Packs e instalador generados:',main.stat().st_size,'bytes principal;',mobs.stat().st_size,'bytes mobs.')
