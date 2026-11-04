"""Download public HYCOM November 2025 surface currents in cached weekly chunks."""
import requests,re,json,datetime,hashlib,time
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'backend/data/currents';OUT.mkdir(parents=True,exist_ok=True)
# Include one day before November for backward trajectories on November 1.
start=datetime.datetime(2025,10,31);end=datetime.datetime(2025,12,1)
origin=datetime.datetime(2025,1,1,12)
a=int((start-origin).total_seconds()/10800);b=int((end-origin).total_seconds()/10800)
result={'provider':'HYCOM ESPC-D-V02','source_url':'https://tds.hycom.org/thredds/catalogs/ESPC-D-V02_u3z.html?dataset=ESPC-D-V02-u3z-2025','units':'m/s','depth_m':0,'scale_factor':0.001,'missing_value':-30000,'note':'Global operational analysis; no local tidal or windage calibration.','retrieved_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'download_urls':[]}
for axis in ['u','v']:
 name='water_'+axis;url=f'https://tds.hycom.org/thredds/dodsC/ESPC-D-V02/{axis}3z/2025.ascii';allrows=[];alltimes=[];hashes=[]
 for first in range(a,b+1,56):
  last=min(first+55,b);query=f'{name}[{first}:1:{last}][0:1:0][2755:1:2800][1510:1:1538]';cache=OUT/f'hycom_202511_{axis}_{first}_{last}.ascii'
  print('Reading',axis,first,last,flush=True)
  if cache.exists():text=cache.read_text(encoding='utf-8')
  else:
   for attempt in range(4):
    try:
     r=requests.get(url+'?'+query,timeout=90);r.raise_for_status();text=r.text
     if '---------------------------------------------' not in text:raise ValueError('Invalid OPeNDAP response')
     cache.write_text(text,encoding='utf-8');break
    except Exception:
     if attempt==3:raise
     time.sleep(2)
  hashes.append(hashlib.sha256(text.encode()).hexdigest());result['download_urls'].append(url+'?'+query)
  chunk=text.split('---------------------------------------------',1)[1];rows=[]
  for line in chunk.splitlines():
   if re.match(r'^\[\d+\]\[0\]\[\d+\],',line):rows.append([None if int(v.strip())==-30000 else int(v.strip())*.001 for v in line.split(',')[1:]])
  def coord(key):
   m=re.search(re.escape(name+'.'+key)+r'\[\d+\]\s*\n([^\n]+)',chunk)
   if not m:raise ValueError('Missing coordinate '+key)
   return [float(x.strip()) for x in m.group(1).split(',')]
  times=coord('time');lat=coord('lat');lon=coord('lon')
  if len(rows)!=len(times)*len(lat) or any(len(row)!=len(lon) for row in rows):raise ValueError('Incomplete grid')
  allrows.extend(rows);alltimes.extend(times)
 if axis=='u':
  result.update({'times':[int((datetime.datetime(2000,1,1)+datetime.timedelta(hours=t)-datetime.datetime(1970,1,1)).total_seconds()) for t in alltimes],'latitudes':lat,'longitudes':lon})
 else:
  if lat!=result['latitudes'] or lon!=result['longitudes'] or len(alltimes)!=len(result['times']):raise ValueError('Grid mismatch')
 result[axis]=[allrows[i*len(lat):(i+1)*len(lat)] for i in range(len(alltimes))];result[axis+'_sha256']=hashes
 print(axis,'valid cells',sum(v is not None for row in allrows for v in row),flush=True)
f=OUT/'hycom_202511.json';temp=f.with_suffix('.tmp');temp.write_text(json.dumps(result,separators=(',',':')),encoding='utf-8');temp.replace(f);print(f,flush=True)
