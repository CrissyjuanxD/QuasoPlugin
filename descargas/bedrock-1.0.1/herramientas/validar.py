#!/usr/bin/env python3
import hashlib
import json
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

B = Path(__file__).resolve().parent.parent
P = B / 'resource_pack'
SOURCE = B.parent / 'warden-cave-biomas/assets/minecraft/optifine/ctm/warden_cave'

def unique(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, key
        result[key] = value
    return result

for path in P.rglob('*.json'):
    json.loads(path.read_text(), object_pairs_hook=unique)
for path in P.rglob('*.png'):
    with Image.open(path) as image: image.verify()
atlas = json.loads((P / 'textures/terrain_texture.json').read_text())['texture_data']
assert len(atlas) == 7
for name, entry in atlas.items():
    png = P / (entry['textures'] + '.png')
    assert png.read_bytes() == (SOURCE / png.name).read_bytes(), png
    assert Image.open(png).size == (16, 16)
for texture_set in P.rglob('*.texture_set.json'):
    data = json.loads(texture_set.read_text())['minecraft:texture_set']
    assert texture_set.with_name(data['color'] + '.png').is_file()
    mask_path = texture_set.with_name(data['metalness_emissive_roughness'] + '.png')
    with Image.open(mask_path) as mask, Image.open(SOURCE / (data['color'] + '_e.png')).convert('RGBA') as source:
        assert mask.size == source.size == (16, 16)
        pixels = list(mask.getdata())
        assert all(pixel[1] == round(max(r, g, b) * a / 255)
                   for pixel, (r, g, b, a) in zip(pixels, source.getdata()))
        assert any(g > 0 for r, g, b in pixels) and any(g == 0 for r, g, b in pixels)
for path in [B / 'packs/QuasoWardenCave-1.0.0.mcpack', B / 'Quaso-Bedrock-1.0.1-instalar.zip',
             B / 'extensions/QuasoWardenCave-1.0.0.jar']:
    with ZipFile(path) as z:
        assert z.testzip() is None
        if path.suffix == '.mcpack':
            assert 'manifest.json' in z.namelist()
            for p in P.rglob('*'):
                if p.is_file(): assert z.read(str(p.relative_to(P))) == p.read_bytes()
        elif path.suffix == '.jar':
            assert 'extension.yml' in z.namelist()
            assert not any(n.startswith('org/geysermc/') for n in z.namelist()), 'No debe redistribuir Geyser'
        else:
            for line in z.read('SHA256SUMS.txt').decode().splitlines():
                digest, name = line.split('  ', 1)
                assert hashlib.sha256(z.read(name)).hexdigest() == digest, name
            assert z.read('plugins/Geyser-Spigot/extensions/QuasoWardenCave-1.0.0.jar') == (B / 'extensions/QuasoWardenCave-1.0.0.jar').read_bytes()
            assert not any(n.endswith('config.yml') for n in z.namelist()), 'No sobreescribir el config de Geyser'
print('OK: 7 texturas CTM idénticas, 4 máscaras MER, JSON/PNG, contenido de ZIP/JAR y hashes de instalación.')
