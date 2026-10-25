"""Run two instrumentation phases across a real Android app process restart."""
import argparse,pathlib,subprocess,shutil
p=argparse.ArgumentParser();p.add_argument('--adb',default=shutil.which('adb') or 'adb');p.add_argument('--serial',default='emulator-5554');p.add_argument('--output',default='build/validation/restart-probe.txt');a=p.parse_args()
base=[a.adb,'-s',a.serial];component='com.ezmenu.app.test/androidx.test.runner.AndroidJUnitRunner'
def phase(method,extra=[]):
 r=subprocess.run(base+['shell','am','instrument','-w','-e','class','com.ezmenu.app.RecipeRestartRegressionTest#'+method]+extra+[component],capture_output=True,timeout=120)
 assert r.returncode==0 and b'OK (1 test)' in r.stdout,r.stdout.decode(errors='replace')
 return r.stdout
first=phase('a_preparePersistentRecipeAndPreference')
subprocess.run(base+['shell','am','force-stop','com.ezmenu.app'],check=True)
second=phase('b_verifyPersistentRecipeAndPreference',['-e','verifyProcessRestart','true'])
out=pathlib.Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(first+second);print('Restart probe passed',a.serial)
