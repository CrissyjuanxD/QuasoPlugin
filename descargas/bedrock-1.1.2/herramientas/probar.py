#!/usr/bin/env python3
"""Uso: probar.py Geyser-Spigot.jar paper/libraries carpeta-con-mockito-y-bytebuddy"""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
if len(sys.argv)!=4:raise SystemExit(__doc__)
base=Path(__file__).resolve().parent.parent
jdk=Path(os.environ['JAVA_HOME'])/'bin' if 'JAVA_HOME' in os.environ else None
java=str(jdk/'java') if jdk else 'java'
javac=str(jdk/'javac') if jdk else 'javac'
dependencies=[*Path(sys.argv[2]).resolve().rglob('*.jar'),*Path(sys.argv[3]).resolve().glob('*.jar')]
agents=[p for p in dependencies if p.name.startswith('byte-buddy-agent-')]
if len(agents)!=1:raise SystemExit('Debe haber un byte-buddy-agent en la carpeta de dependencias de pruebas.')
cp=os.pathsep.join(map(str,[Path(sys.argv[1]).resolve(),base/'extensions/QuasoWardenCave-1.1.2.jar',*dependencies]))
with tempfile.TemporaryDirectory(prefix='quaso-visual-test-') as temp:
    subprocess.run([javac,'--release','21','-encoding','UTF-8','-cp',cp,'-d',temp,
                    *map(str,(base/'extension/src/test/java').rglob('*.java'))],check=True)
    for test in ['PresentationTest','WorldTranslationTest','AtmosphereTest','CustomBiomesTest','MobModelsTest']:
        subprocess.run([java,'-ea','-javaagent:'+str(agents[0]),'-cp',temp+os.pathsep+cp,
                        'studios.viciont.quasogeyser.'+test],check=True,cwd=temp)
