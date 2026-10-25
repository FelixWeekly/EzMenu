"""Record real-time UI motion and trim instrumentation startup / teardown frames."""
import argparse,pathlib,subprocess,time,shutil
p=argparse.ArgumentParser();p.add_argument('--adb',default=shutil.which('adb') or 'adb');p.add_argument('--serial',default='emulator-5554');p.add_argument('--output',default='portfolio/ezmenu-demo.mp4');a=p.parse_args()
base=[a.adb,'-s',a.serial];out=pathlib.Path(a.output);out.parent.mkdir(parents=True,exist_ok=True)
def text(*args):return subprocess.check_output(base+list(args)).decode().strip()
command='screenrecord --size 720x1600 --bit-rate 4000000 --time-limit 90 /sdcard/ezmenu-v2-demo.mp4 & echo $!; wait'
record=subprocess.Popen(base+['shell',command],stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
pid=record.stdout.readline().decode().strip();assert pid.isdigit() and int(pid)>1,pid
capture_uptime=float(text('shell','cat','/proc/uptime').split()[0])
try:
 r=subprocess.run(base+['shell','am','instrument','-w','-e','class','com.ezmenu.app.StudioVisualRegressionTest#visualChaptersAndSharePreviewRemainUsable','-e','theme','light','-e','visualName','demo','-e','cinematic','true','com.ezmenu.app.test/androidx.test.runner.AndroidJUnitRunner'],capture_output=True,timeout=180)
finally:
 subprocess.run(base+['shell','kill','-2',pid],capture_output=True)
 record.communicate(timeout=15)
(out.parent/'demo-test.txt').write_text(r.stdout.decode('utf-8',errors='replace'),encoding='utf-8')
assert b'OK (1 test)' in r.stdout,r.stdout.decode(errors='replace')
raw=pathlib.Path('build/validation/identity/demo-raw.mp4');raw.parent.mkdir(parents=True,exist_ok=True)
subprocess.run(base+['pull','/sdcard/ezmenu-v2-demo.mp4',str(raw)],check=True)
start=max(0,float(text('exec-out','run-as','com.ezmenu.app','cat','cache/preview-start-ms'))/1000-capture_uptime-.35)
end=float(text('exec-out','run-as','com.ezmenu.app','cat','cache/preview-end-ms'))/1000-capture_uptime
assert end>start
subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-ss',str(start),'-i',str(raw),'-t',str(end-start),'-c:v','libx264','-preset','fast','-crf','19','-an','-movflags','+faststart',str(out)],check=True)
assert out.stat().st_size>1000
print(out,out.stat().st_size,'trimmedSeconds',round(end-start,2))
