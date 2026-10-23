#!/usr/bin/env python3
"""Check compiled method calls and resource fallbacks against SDK API history.
This is a static check, not an Android 4.4 runtime test.
"""
from pathlib import Path
import os,re,subprocess,sys,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT',str(Path.home()/'Library/Android/sdk'))))
api=ET.parse(sdk/'platforms/android-33/data/api-versions.xml').getroot()
classes={c.get('name'):c for c in api.findall('class')}
# DEX strings use modified UTF-8 (including NUL); method descriptors are ASCII.
dump=subprocess.check_output([str(sdk/'build-tools/33.0.0/dexdump'),'-d',str(root/'build/dex/classes.dex')],text=True,encoding='utf-8',errors='replace')
parents={};caller=None
for line in dump.splitlines():
 m=re.search(r"Class descriptor\s+: 'L([^;]+);'",line)
 if m:caller=m[1]
 m=re.search(r"Superclass\s+: 'L([^;]+);'",line)
 if m:parents[caller]=m[1]
def introduced(owner,method,seen=None):
 seen=set() if seen is None else seen
 if owner in seen:return None
 seen.add(owner)
 c=classes.get(owner)
 if c is None:
  return introduced(parents[owner],method,seen) if owner in parents else None
 since=int(c.get('since','1'))
 if since>19:return since
 for m in c.findall('method'):
  if m.get('name')==method:return max(since,int(m.get('since',str(since))))
 for parent in list(c.findall('extends'))+list(c.findall('implements')):
  result=introduced(parent.get('name'),method,seen)
  if result is not None:return max(since,result)
 return None
issues=[];guarded=set();unresolved=set();caller=None;calls=0
for line in dump.splitlines():
 m=re.search(r"Class descriptor\s+: 'L([^;]+);'",line)
 if m:caller=m[1]
 m=re.search(r'invoke-\S+\s+\{[^}]*\}, L([^;]+);\.([^:]+):(\([^ ]+)',line)
 if not m:continue
 owner,name,desc=m.groups();calls+=1
 level=introduced(owner,name+desc)
 if level is None:
  if owner.startswith(('android/','java/')):unresolved.add(owner+'.'+name+desc)
 elif level>19:
  ref=f'{owner}.{name}{desc} (API {level})'
  if caller=='cn/quiet/tv/AndroidCompat$Api21':guarded.add(ref)
  else:issues.append(f'{caller}: {ref}')
# The only dispatcher to newer APIs must retain explicit SDK checks.
compat=(root/'src/cn/quiet/tv/AndroidCompat.java').read_text()
if compat.count('Build.VERSION.SDK_INT>=21')!=5:issues.append('Review AndroidCompat SDK guards')
if any('<vector' in p.read_text() for p in (root/'res/drawable').glob('*.xml')):issues.append('VectorDrawable in unqualified resources')
for p in (root/'res/drawable-anydpi-v21').glob('*.xml'):
 if not (root/'res/drawable-xxxhdpi'/p.with_suffix('.png').name).exists():issues.append('Missing PNG fallback: '+p.name)
if 'Theme.Holo.NoActionBar.Fullscreen' not in (root/'res/values/styles.xml').read_text():issues.append('Missing KitKat theme')
if 'minSdkVersion="19"' not in (root/'AndroidManifest.xml').read_text():issues.append('minSdkVersion must be 19')
dex35="DEX version '035'" in dump
if not dex35:issues.append('Expected DEX 035')
print(f'Inspected {calls} compiled method calls; DEX 035: {dex35}')
print('Isolated, version-guarded framework references:')
for item in sorted(guarded):print('  '+item)
if unresolved:
 print('Unresolved public framework references (manual review required):')
 for item in sorted(unresolved):print('  '+item)
for issue in issues:print('ERROR: '+issue)
if issues or unresolved:sys.exit(1)
print('API 19 static checks passed. Device runtime validation is still required.')
