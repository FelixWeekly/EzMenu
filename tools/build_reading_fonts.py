"""Build reproducible bilingual reading fonts from the cached OFL sources."""
from pathlib import Path
import importlib.util,json,re
from fontTools.ttLib import TTFont
from fontTools.merge import Merger
from fontTools.subset import Options,Subsetter

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('font_base',ROOT/'tools/build_cjk_serif.py')
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
CACHE=ROOT/'tools/.cache'
OUT=ROOT/'app/src/main/res/font'

def repertoire():
    result=set()
    for lead in range(0xa1,0xf8):
        for tail in range(0xa1,0xff):
            try: result.update(bytes([lead,tail]).decode('gb2312'))
            except UnicodeDecodeError: pass
    for folder in ['app/src/main/java','app/src/main/assets/seed','app/src/main/res/values']:
        for path in (ROOT/folder).rglob('*'):
            if path.is_file():
                result.update(c for c in path.read_text(encoding='utf-8',errors='ignore') if 0x2e80<=ord(c)<=0xffef)
    return result

def subset(source,weight,path,chars,latin):
    font=base.pin_all_axes(TTFont(source),weight)
    options=Options();options.layout_features=[];options.hinting=False;options.glyph_names=False
    sub=Subsetter(options=options)
    codes={ord(c) for c in chars}
    if latin: codes=set(range(0x20,0x250))|set(range(0x2000,0x2070))
    else: codes-={c for c in codes if 0x20<=c<0x250 or 0x2000<=c<0x2070}
    sub.populate(unicodes=codes);sub.subset(font)
    base.scale_upem(font,1000);base.strip_layout_tables(font)
    font.save(path);font.close()

def main():
    chars=repertoire();report={}
    for family,cjk,latin in [('serif','NotoSerifSC-VF.ttf','Newsreader[opsz,wght].ttf'),('reading_sans','NotoSansSC-VF.ttf','Manrope[wght].ttf')]:
        for weight in (400,500,600):
            a=CACHE/(family+'_latin_'+str(weight)+'.ttf');b=CACHE/(family+'_cjk_'+str(weight)+'.ttf')
            subset(CACHE/latin,weight,a,chars,True);subset(CACHE/cjk,weight,b,chars,False)
            out=OUT/(('serif_cjk' if family=='serif' else family)+'_'+str(weight)+'.ttf')
            merged=Merger().merge([str(a),str(b)]);merged.save(out);merged.close();base.unify_metrics(str(out))
            f=TTFont(out);cmap=f.getBestCmap();missing=[c for c in chars if ord(c) not in cmap and 0x4e00<=ord(c)<=0x9fff]
            assert not missing,missing
            report[out.name]={'glyphs':len(cmap),'bytes':out.stat().st_size,'weight':f['OS/2'].usWeightClass}
            f.close();print(out.name,report[out.name],flush=True)
    (CACHE/'reading-font-audit.json').write_text(json.dumps(report,indent=2),encoding='utf-8')

if __name__=='__main__':main()
