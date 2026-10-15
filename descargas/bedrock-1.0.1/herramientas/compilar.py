#!/usr/bin/env python3
"""Compila la extensión contra el Geyser oficial usado en la validación."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

BASE = Path(__file__).resolve().parent.parent
GEYSER_SHA256 = 'e31619e6c0fdd0e4a06d547ff476fe6bed23d0e8e5cc56479846ec80bc2a5e77'
if len(sys.argv) != 2:
    raise SystemExit('Uso: python compilar.py /ruta/Geyser-Standalone-2.12.0-b1256.jar (JDK 21+)')
geyser = Path(sys.argv[1]).resolve()
if hashlib.sha256(geyser.read_bytes()).hexdigest() != GEYSER_SHA256:
    raise SystemExit('El JAR no corresponde al Geyser 2.12.0-b1256 oficial verificado. No se compila contra otro binario.')
jdk = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else None
javac = str(jdk / 'javac') if jdk else 'javac'
jar = str(jdk / 'jar') if jdk else 'jar'
destination = BASE / 'extensions/QuasoWardenCave-1.0.0.jar'
destination.parent.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='quaso-geyser-') as temp:
    classes = Path(temp) / 'classes'
    classes.mkdir()
    sources = sorted((BASE / 'extension/src/main/java').rglob('*.java'))
    subprocess.run([javac, '--release', '21', '-encoding', 'UTF-8', '-cp', str(geyser),
                    '-d', str(classes), *map(str, sources)], check=True)
    shutil.copytree(BASE / 'extension/src/main/resources', classes, dirs_exist_ok=True)
    subprocess.run([jar, '--create', '--file', str(destination), '-C', str(classes), '.'], check=True)
print(destination)
