"""Download public HYCOM surface currents; retain source metadata and null land cells."""
import requests,re,json,datetime,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'java-backend/data/currents';OUT.mkdir(parents=True,exist_ok=True)
# 2025 Q2, UTC. Include a halo outside the observation rectangle for backtracking.
start=datetime.datetime(2025,4,1,12);end=datetime.datetime(2025,7,1,12)
origin=datetime.datetime(2025,1,1,12)
a=int((start-origin).total_seconds()/10800);b=int((end-origin).total_seconds()/10800)
result={'provider':'HYCOM ESPC-D-V02','source_url':'https://tds.hycom.org/thredds/catalog.html','units':'m/s','depth_m':0,'scale_factor':0.001,'missing_value':-30000,'note':'Global operational analysis, not FVCOM; no windage or coastal tide calibration.','retrieved_at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
for axis in ['u','v']:
 name='water_'+axis;url=f'https://tds.hycom.org/thredds/dodsC/ESPC-D-V02/{axis}3z/2025.ascii'
 query=f'{name}[{a}:1:{b}][0:1:0][2755:1:2800][1510:1:1538]'
 print('Downloading',axis,flush=True)
 r=requests.get(url+'?'+query,timeout=180);r.raise_for_status();text=r.text
 (OUT/f'hycom_2025q2_{axis}.ascii').write_text(text,encoding='utf-8')
 chunks=text.split('---------------------------------------------',1)[1]
 rows=[]
 for line in chunks.splitlines():
  if re.match(r'^\[\d+\]\[0\]\[\d+\],',line):
   vals=line.split(',')[1:];rows.append([None if int(v.strip())==-30000 else int(v.strip())*.001 for v in vals])
 def coord(key):
  m=re.search(re.escape(name+'.'+key)+r'\[\d+\]\s*\n([^\n]+)',chunks)
  if not m:raise ValueError('Missing coordinate '+key)
  return [float(x.strip()) for x in m.group(1).split(',')]
 times=coord('time');lat=coord('lat');lon=coord('lon')
 if len(rows)!=len(times)*len(lat) or any(len(row)!=len(lon) for row in rows):raise ValueError('Incomplete grid')
 if axis=='u':
  result.update({'times':[int((datetime.datetime(2000,1,1)+datetime.timedelta(hours=t)-datetime.datetime(1970,1,1)).total_seconds()) for t in times],'latitudes':lat,'longitudes':lon})
 else:
  if lat!=result['latitudes'] or lon!=result['longitudes']:raise ValueError('Grid mismatch')
 result[axis]=[rows[i*len(lat):(i+1)*len(lat)] for i in range(len(times))]
 result[axis+'_sha256']=hashlib.sha256(r.content).hexdigest()
 print(axis,'valid cells',sum(v is not None for row in rows for v in row),flush=True)
f=OUT/'hycom_2025q2.json';f.write_text(json.dumps(result,separators=(',',':')),encoding='utf-8');print(f,flush=True)
