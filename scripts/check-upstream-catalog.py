#!/usr/bin/env python3
"""A community reference is not an executable profile. Check provenance and a reviewed subset."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parent.parent
data=json.loads((root/'assets/setup/upstream-catalog.json').read_text())
assert data['schema']==1 and re.fullmatch('[0-9a-f]{40}',data['commit'])
ids=set()
for row in data['references']:
    assert re.fullmatch('[a-z0-9-]+',row['id']) and row['id'] not in ids
    ids.add(row['id'])
    assert row['url']=='https://github.com/Gathin23/cleanyourtv/blob/'+data['commit']+'/examples/'+row['id']+'/debloat.txt'
assert len(ids)>150
source=(root/'assets/setup/tcl-g08-eu-source.txt').read_text()
active={line.split('#',1)[0].strip() for line in source.splitlines() if line.split('#',1)[0].strip()}
for pkg in ['com.tcl.dashboard','com.tcl.messagebox','com.tcl.logkit']:
    assert pkg in active
print(f'Catalog verified: {len(ids)} community references; reviewed TCL subset present in pinned source.')
