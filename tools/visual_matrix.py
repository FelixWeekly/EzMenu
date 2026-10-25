"""Run real installed-app visual checks; always restore device display preferences."""
import argparse, json, pathlib, subprocess, time, shutil, os
p=argparse.ArgumentParser();p.add_argument('--adb',default=shutil.which('adb') or 'adb');p.add_argument('--serial',default='emulator-5554');p.add_argument('--output',default='shots/v2/matrix');a=p.parse_args()
out=pathlib.Path(a.output);out.mkdir(parents=True,exist_ok=True)
base=[a.adb,'-s',a.serial]
def adb(*args,**kw):return subprocess.run(base+list(args),capture_output=True,**kw)
def text(*args):
 r=adb(*args);r.check_returncode();return r.stdout.decode('utf-8',errors='replace').strip()
size=text('shell','wm','size');density=text('shell','wm','density');font=text('shell','settings','get','system','font_scale')
results=[]
try:
 for width,height,scale in [(360,720,1),(412,824,1),(840,600,1),(360,720,2)]:
  for theme in ['light','dark']:
   name=str(width)+'-'+theme+'-font'+str(scale)
   text('shell','wm','size',str(width*3)+'x'+str(height*3));text('shell','wm','density','480');text('shell','settings','put','system','font_scale',str(scale))
   time.sleep(1)
   r=adb('shell','am','instrument','-w','-e','class','com.ezmenu.app.StudioVisualRegressionTest#visualChaptersAndSharePreviewRemainUsable','-e','theme',theme,'-e','visualName',name,'com.ezmenu.app.test/androidx.test.runner.AndroidJUnitRunner',timeout=180)
   log=r.stdout.decode('utf-8',errors='replace');(out/(name+'.txt')).write_text(log,encoding='utf-8')
   ok=r.returncode==0 and 'OK (1 test)' in log
   results.append({'configuration':name,'passed':ok});print(name,ok,flush=True)
   for chapter in ['library','detail','editor','cooking','inspiration','journal','entry','share','profile','settings']:
    shot=adb('exec-out','run-as','com.ezmenu.app','cat','cache/studio-'+name+'-'+chapter+'.png')
    if shot.stdout.startswith(b'\x89PNG'):(out/(name+'-'+chapter+'.png')).write_bytes(shot.stdout)
   if not ok:print(log,flush=True)
finally:
 oldsize=size.split('Override size: ')[-1] if 'Override size:' in size else 'reset'
 olddensity=density.split('Override density: ')[-1] if 'Override density:' in density else 'reset'
 text('shell','wm','size',oldsize);text('shell','wm','density',olddensity)
 if font in ['null','']:text('shell','settings','delete','system','font_scale')
 else:text('shell','settings','put','system','font_scale',font)
 (out/'results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
if not all(x['passed'] for x in results):raise SystemExit(1)
