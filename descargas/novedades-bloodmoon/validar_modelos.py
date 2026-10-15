"""Revisa los selectores CEM/random del lote; no sustituye una prueba en Minecraft.

Uso: python validar_modelos.py [ruta/a/assets]
Solo requiere Python 3. bee2.jem es un recurso externo del pack del usuario.
"""
from pathlib import Path
import json
import re
import struct
import sys


def rules(path, kind):
    assert path.is_file(), f"Falta selector: {path}"
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, value = line.split("=", 1)
        value = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m[1], 16)), value)
        values[key.strip()] = value.strip()
    found = []
    selector = "models" if kind == "cem" else "skins"
    for key, value in values.items():
        if key.startswith(selector + "."):
            index = int(key.split(".")[1])
            found.append((index, int(value), values.get(f"name.{index}")))
    found.sort()
    assert found, f"Sin reglas {selector}: {path}"
    assert [r[0] for r in found] == list(range(1, len(found) + 1)), path
    assert found[-1][1:] == (1, None), f"Falta fallback vanilla explícito: {path}"
    assert all(r[2] for r in found[:-1]), f"Variante sin condición por nombre: {path}"
    return found


def select(parsed, name):
    for _, variant, condition in parsed:
        if condition is None or re.fullmatch(condition.removeprefix("iregex:"), name, re.I):
            return variant
    raise AssertionError(name)


def validate(assets):
    minecraft = Path(assets) / "minecraft"
    optifine = minecraft / "optifine"
    external = optifine / "cem/bee2.jem"
    paths = list((optifine / "cem").glob("*.jem"))
    paths += list((optifine / "random").rglob("*.png"))
    # Detecta el fallo original: un PNG numerado sin .properties activa variación aleatoria.
    for path in paths:
        match = re.fullmatch(r"(.+?)([2-9][0-9]*)(?:_e)?", path.stem)
        if match:
            kind = "cem" if path.suffix == ".jem" else "random"
            rules(path.with_name(match[1] + ".properties"), kind)

    checked = 0
    for path in optifine.rglob("*.properties"):
        if path.name == "emissive.properties":
            continue
        kind = "cem" if path.parent.name == "cem" else "random"
        parsed = rules(path, kind)
        for _, variant, _ in parsed:
            if variant == 1 and kind == "random":
                continue  # Minecraft proporciona la textura vanilla.
            suffix = "" if variant == 1 else str(variant)
            resource = path.with_name(path.stem + suffix + (".jem" if kind == "cem" else ".png"))
            assert resource.is_file() or resource == external, f"Variante ausente: {resource}"
        assert select(parsed, "Mob normal") == 1, path
        assert select(parsed, "") == 1, path
        checked += 1

    cases = {
        "cem/zombie": [("Zombie BloodMoon", 4), ("Zombie Floral", 2), ("Warden Zombie", 3)],
        "cem/skeleton": [("Esqueleto BloodMoon", 4), ("Infested Skeleton", 3)],
        "cem/creeper": [("Creeper BloodMoon", 4), ("Creeper Floral", 2), ("Infested Creeper", 3)],
        "cem/spider": [("Araña BloodMoon", 4), ("Araña Floral", 2)],
        "cem/bee": [("Mini Abeja Floral", 3), ("Corrupted Bee", 3), ("Abeja Floral", 2)],
        "cem/copper_golem": [("Estatua de Recompensas", 2)],
        "random/entity/zombie/zombie": [("Zombie BloodMoon", 1), ("Zombie Floral", 2), ("Warden Zombie", 3)],
        "random/entity/skeleton/skeleton": [("Esqueleto BloodMoon", 1), ("Infested Skeleton", 3)],
        "random/entity/creeper/creeper": [("Creeper BloodMoon", 1), ("Creeper Floral", 2), ("Infested Creeper", 3)],
        "random/entity/spider/spider": [("Araña BloodMoon", 1), ("Araña Floral", 2)],
        "random/entity/spider/spider_eyes": [("Araña BloodMoon", 4), ("Araña Floral", 2)],
    }
    for relative, examples in cases.items():
        parsed = rules(optifine / (relative + ".properties"), "cem" if relative.startswith("cem/") else "random")
        for name, expected in examples:
            for decorated in [name, "§c§l" + name]:
                assert select(parsed, decorated) == expected, (relative, decorated, expected)

    for path in (optifine / "cem").glob("*.jem"):
        model = json.loads(path.read_text())
        texture = model.get("texture")
        if texture:
            namespace, relative = texture.split(":", 1)
            png = Path(assets) / namespace / relative
            assert png.is_file(), f"Textura ausente: {path} -> {png}"
            data = png.read_bytes()
            assert data[:8] == b"\x89PNG\r\n\x1a\n", png
            assert list(struct.unpack(">II", data[16:24])) == model["textureSize"], path
    return checked, not external.is_file()


if __name__ == "__main__":
    assets = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent / "assets"
    count, needs_bee = validate(assets)
    print(f"OK: {count} selectores; variantes, nombres, fallback vanilla y atlas revisados.")
    if needs_bee:
        print("Integración pendiente: conservar el bee2.jem original del usuario y sus texturas. No se incluye en este lote.")
