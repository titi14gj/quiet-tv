#!/usr/bin/env python3
"""Import references from an explicit pinned revision. Does not enable any execution rule."""
import argparse,json,re,urllib.request
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--commit',required=True);args=parser.parse_args()
if not re.fullmatch('[0-9a-f]{40}',args.commit):parser.error('a full immutable commit SHA is required')
base='https://raw.githubusercontent.com/Gathin23/cleanyourtv/'+args.commit+'/'
readme=urllib.request.urlopen(base+'examples/README.md',timeout=30).read().decode('utf-8')
rows=[]
for line in readme.splitlines():
    parts=[p.strip() for p in line.split('|')]
    if len(parts)<6:continue
    match=re.fullmatch(r'`([a-z0-9-]+)/`',parts[1])
    if not match:continue
    slug=match[1];rows.append(dict(id=slug,title=parts[2],os=parts[3],notes=parts[4],url='https://github.com/Gathin23/cleanyourtv/blob/'+args.commit+'/examples/'+slug+'/debloat.txt'))
if len(rows)<150:raise ValueError('Unexpected README format; refusing import')
root=Path(__file__).resolve().parent.parent
(root/'assets/setup/upstream-catalog.json').write_text(json.dumps(dict(schema=1,source='Gathin23/cleanyourtv',commit=args.commit,references=rows),ensure_ascii=False,indent=2)+'\n')
print(f'Imported {len(rows)} reference records; executable rules are unchanged.')
