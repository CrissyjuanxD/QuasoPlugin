#!/usr/bin/env python3
"""Porta los PNG CTM existentes; genera geometría de enredadera y datos PBR, sin redibujar."""
import json
from pathlib import Path
import shutil
import uuid
from PIL import Image

B = Path(__file__).resolve().parent.parent
P = B / 'resource_pack'
SOURCE = B.parent / 'warden-cave-biomas/assets/minecraft/optifine/ctm/warden_cave'

def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n')

save(P / 'manifest.json', {
    'format_version': 2,
    'header': {'name': 'Quaso · Warden Cave 1.0.0',
               'description': 'Frutas y minerales de Warden Cave · requiere QuasoWardenCave en Geyser',
               'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://github.com/CrissyjuanxD/QuasoPlugin/bedrock/warden-cave/header')),
               'version': [1, 0, 0], 'min_engine_version': [1, 26, 30]},
    'modules': [{'type': 'resources',
                 'uuid': str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://github.com/CrissyjuanxD/QuasoPlugin/bedrock/warden-cave/module')),
                 'version': [1, 0, 0]}],
    'capabilities': ['pbr']
})
shutil.copyfile(B.parent / 'bedrock-1.0.0/resource_pack/pack_icon.png', P / 'pack_icon.png')
textures = {}
sounds = {'format_version': [1, 1, 0]}
names = ['ore_cyan', 'ore_green', 'ore_purple', 'ore_gray', 'baya_sculk', 'fruta_abisal', 'baya_luminosa']
for name in names:
    target = P / 'textures/blocks/quaso' / (name + '.png')
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE / (name + '.png'), target)
    textures['quaso_wc_' + name] = {'textures': 'textures/blocks/quaso/' + name}
    sounds['geyser_custom:quaso_wc_' + name] = {'sound': 'stone' if name.startswith('ore_') else 'cave_vines' if name == 'baya_luminosa' else 'froglight'}
    if name.startswith('ore_'):
        # Convert the supplied emissive alpha/luminance mask to Bedrock MER's green channel.
        with Image.open(SOURCE / (name + '_e.png')).convert('RGBA') as mask:
            mer = Image.new('RGB', mask.size)
            mer.putdata([(0, round(max(r, g, b) * a / 255), 230) for r, g, b, a in mask.getdata()])
            mer.save(target.with_name(name + '_mer.png'))
        save(target.with_name(name + '.texture_set.json'), {
            'format_version': '1.16.100', 'minecraft:texture_set': {
                'color': name, 'metalness_emissive_roughness': name + '_mer'
            }
        })
save(P / 'textures/terrain_texture.json', {
    'resource_pack_name': 'Quaso Warden Cave', 'texture_name': 'atlas.terrain',
    'padding': 8, 'num_mip_levels': 4, 'texture_data': textures
})
save(P / 'blocks.json', sounds)
faces = {face: {'uv': [0, 0], 'uv_size': [16, 16], 'material_instance': '*'} for face in ['north', 'south']}
save(P / 'models/blocks/warden_vine.geo.json', {
    'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.quaso.warden_vine', 'texture_width': 16,
                        'texture_height': 16, 'visible_bounds_width': 2, 'visible_bounds_height': 2,
                        'visible_bounds_offset': [0, 0.5, 0]},
        'bones': [{'name': 'vine', 'pivot': [0, 0, 0], 'cubes': [
            {'origin': [-8, 0, 0], 'size': [16, 16, 0], 'pivot': [0, 8, 0],
             'rotation': [0, angle, 0], 'uv': faces} for angle in [-45, 45]
        ]}]
    }]
})
print('Siete texturas originales, cuatro máscaras MER y geometría de enredadera listas.')
