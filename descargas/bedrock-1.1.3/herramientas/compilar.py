#!/usr/bin/env python3
"""Compila exclusivamente para Geyser-Spigot, cuyas librerías están reubicadas."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from zipfile import ZipFile

BASE = Path(__file__).resolve().parent.parent
GEYSER_SHA256 = {
    'c61720bbea714a704d9b76a2f1e9a7ac91271e48101ec745ccd668fda9514015',  # b1256
    '9f0efc83d1ece6f77552c573930431f08a760fa5c3eb02528c584fc291d44f92',  # b1262
}
if len(sys.argv) != 3:
    raise SystemExit('Uso: python compilar.py /ruta/Geyser-Spigot-2.12.0-b1256.jar /ruta/paper/libraries (JDK 25+)')
geyser = Path(sys.argv[1]).resolve()
if hashlib.sha256(geyser.read_bytes()).hexdigest() not in GEYSER_SHA256:
    raise SystemExit('El JAR no corresponde a Geyser-Spigot 2.12.0-b1256/b1262 verificado. Standalone y otros binarios no son compatibles.')
with ZipFile(geyser) as z:
    assert 'org/geysermc/geyser/platform/spigot/shaded/it/unimi/dsi/fastutil/objects/Object2IntMap.class' in z.namelist()
    assert 'org/geysermc/geyser/platform/spigot/shaded/net/kyori/adventure/key/Key.class' in z.namelist()
jdk = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else None
javac = str(jdk / 'javac') if jdk else 'javac'
jar = str(jdk / 'jar') if jdk else 'jar'
destination = BASE / 'extensions/QuasoWardenCave-1.1.3.jar'
classpath = os.pathsep.join([str(geyser), *map(str, Path(sys.argv[2]).rglob('*.jar'))])
destination.parent.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='quaso-geyser-') as temp:
    classes = Path(temp) / 'classes'
    classes.mkdir()
    sources = sorted((BASE / 'extension/src/main/java').rglob('*.java'))
    subprocess.run([javac, '--release', '21', '-encoding', 'UTF-8', '-cp', classpath,
                    '-d', str(classes), *map(str, sources)], check=True)
    shutil.copytree(BASE / 'extension/src/main/resources', classes, dirs_exist_ok=True)
    subprocess.run([jar, '--create', '--file', str(destination), '-C', str(classes), '.'], check=True)
print(destination)
