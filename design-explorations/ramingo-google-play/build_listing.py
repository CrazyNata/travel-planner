"""Rebuild the Ramingo listing kit. Pillow; no network or application writes.

Run with --prepare-crops only on the authorized local capture workstation.
Subsequent rebuilds use privacy-reviewed crops inside screenshots/sources/ui.
"""
from pathlib import Path
import argparse, json, hashlib, textwrap
from PIL import Image, ImageDraw, ImageFont, ImageCms, ImageOps

ROOT=Path(__file__).resolve().parent
REPO=ROOT.parents[1]
RAW=REPO/'tmp/ramingo-listing-capture'
FONTS=REPO/'mobile/app/src/main/res/font'
ICC=ImageCms.ImageCmsProfile(ImageCms.createProfile('sRGB')).tobytes()
INK='#251D3B'; VIOLET='#6C5CE7'; PAPER='#FBF6ED'; MUTED='#71677F'
def font(n,bold=True): return ImageFont.truetype(str(FONTS/('manrope_extrabold.ttf' if bold else 'manrope_medium.ttf')),n)
def write(p,s):
 p=ROOT/p; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s.strip()+'\n',encoding='utf-8')
def save(im,p):
 p=ROOT/p; p.parent.mkdir(parents=True,exist_ok=True); im.convert('RGB').save(p,icc_profile=ICC,optimize=True)

# Whole rectangles only: no rewritten UI text, replacement thumbnails or synthetic controls.
# The invitation crop starts below the last member. No names/emails enter the kit.
CROPS={
 'en-map':('en-overview-rome',(32,666,688,1110)),
 'ru-map':('ru-overview',(32,666,688,1150)),
 'en-walk':('en-sight-days',(32,179,688,893)),
 'ru-walk':('ru-walk',(32,179,688,893)),
 'en-route':('en-route-italy',(35,335,685,1519)),
 'ru-route':('ru-route-top',(35,335,685,1519)),
 'en-leg':('en-route-italy',(35,335,685,613)),
 'ru-leg':('ru-route-top',(35,335,685,613)),
 'en-rest-map':('en-restaurants',(36,505,684,799)),
 'ru-rest-map':('ru-restaurants',(36,505,684,799)),
 'en-restaurant':('en-restaurants',(230,856,665,1026)),
 'ru-restaurant':('ru-restaurants',(230,856,665,1026)),
 'en-restaurant2':('en-restaurants',(230,1267,665,1437)),
 'ru-restaurant2':('ru-restaurants',(230,1267,665,1437)),
 'en-stay':('en-lodging',(65,641,582,694)),
 'ru-stay':('en-lodging',(65,641,582,694)), # proper business name only, no language-dependent copy
 'en-pet-tabs':('en-vets',(36,314,684,418)),
 'ru-pet-tabs':('ru-vets-tab',(36,314,684,418)),
 'en-vet':('en-vets',(250,850,684,1055)),
 'ru-vet':('ru-vets-tab',(250,850,684,1055)),
 'en-shop':('en-pets',(250,850,684,1055)),
 'ru-shop':('ru-vets',(250,850,684,1055)),
 'en-sections':('en-drawer-final',(32,106,492,1040)),
 'ru-sections':('ru-organize',(32,106,492,1040)),
 'en-invite':('en-viewer-private',(32,730,688,1149)),
 'ru-invite':('ru-viewer-private',(32,730,688,1149)),
}

HEADS={
 'en': [('Plan your next','road trip'),('Drive there.','Explore on foot.'),('Every stop.','One clear plan.'),('Make room for','great places.'),('Travel better','prepared with pets.'),('A place for','every detail.'),('Let them see','the whole plan.')],
 'ru': [('Спланируйте','своё автопутешествие'),('Между городами —','на авто. По городу — пешком.'),('Все остановки.','Один понятный план.'),('Сохраните места,','куда хочется попасть.'),('Продумайте поездку','с питомцем.'),('Каждая деталь','на своём месте.'),('Покажите близким','весь план поездки.')]
}
SUBS={
 'en':['Routes, stops and city walks, together.','Plan the journey and the days in between.','Keep the next destination in sight.','See restaurants on the map and save your picks.','Browse vet clinics and pet shops before you travel.','Your trip, organized into useful sections.','Invite travel companions with viewer access.'],
 'ru':['Маршруты, остановки и прогулки — в одном плане.','Продумайте переезды и прогулки на каждый день.','Заранее решите, куда отправиться дальше.','Смотрите рестораны на карте и сохраняйте их.','В каталоге — ветеринарные клиники и зоомагазины.','Все разделы путешествия — в одном приложении.','Приглашайте попутчиков на просмотр поездки.']}
NAMES=['01-road-trip','02-routes','03-stops','04-places','05-pets','06-organize','07-share']
def fittext(d,txt,xy,maxwidth,size,color=INK,bold=True):
 while d.textlength(txt,font=font(size,bold))>maxwidth: size-=1
 d.text(xy,txt,font=font(size,bold),fill=color,stroke_width=0)
 return size
def label(d,txt,x,y,color=MUTED,size=30): d.text((x,y),txt,font=font(size,False),fill=color)
def ui(key): return Image.open(ROOT/f'screenshots/sources/ui/{key}.png').convert('RGB')
def panel(im,key,x,y,w,r=30):
 src=ui(key); h=round(src.height*w/src.width); src=src.resize((w,h),Image.Resampling.LANCZOS)
 mask=Image.new('L',(w,h)); ImageDraw.Draw(mask).rounded_rectangle((0,0,w,h),r,fill=255)
 ImageDraw.Draw(im).rounded_rectangle((x-2,y-2,x+w+2,y+h+2),r+2,fill='#E7E1EF')
 im.paste(src,(x,y),mask); return h
def logo(im,x,y,size=58,color=INK):
 icon=Image.open(REPO/'design-reference/ramingo-google-play-icon-512.png').convert('RGB').resize((size,size),Image.Resampling.LANCZOS)
 mask=Image.new('L',(size,size)); ImageDraw.Draw(mask).rounded_rectangle((0,0,size,size),int(size*.22),fill=255)
 im.paste(icon,(x,y),mask); ImageDraw.Draw(im).text((x+size+17,y-2),'Ramingo',font=font(int(size*.72)),fill=color)
def base(lang,i,style='ivory'):
 bg={'ivory':PAPER,'violet':VIOLET,'peach':'#FFD4B2'}[style]
 im=Image.new('RGB',(1240,2208),bg); d=ImageDraw.Draw(im)
 if style=='violet': d.rectangle((0,550,1240,2208),fill=PAPER)
 fg='#FFFFFF' if style=='violet' else INK
 logo(im,76,68,58,fg)
 label(d,f'{i+1:02d} / 07',1050,80,'#DDD6FF' if style=='violet' else MUTED,28)
 y=205
 for line in HEADS[lang][i]: fittext(d,line,(76,y),1088,91 if lang=='en' else 76,fg); y+=111 if lang=='en' else 102
 fittext(d,SUBS[lang][i],(78,465),1080,37,'#E5DEFF' if style=='violet' else MUTED,False)
 d.line((76,2112,1164,2112),fill='#CFC2F4' if style=='violet' else '#DDD4E5',width=2)
 label(d,'ROAD TRIPS  /  CITY WALKS' if lang=='en' else 'АВТОПУТЕШЕСТВИЯ  /  ПРОГУЛКИ',76,2140,'#E5DEFF' if style=='violet' else MUTED,26)
 return im
def shot(lang,i,style='ivory'):
 im=base(lang,i,style); d=ImageDraw.Draw(im); p=lang+'-'
 if i==0:
  label(d,'THE WHOLE JOURNEY' if lang=='en' else 'ПУТЕШЕСТВИЕ ЦЕЛИКОМ',78,580,VIOLET)
  h=panel(im,p+'map',76,642,1088,48)
  label(d,'YOUR NEXT STOP' if lang=='en' else 'СЛЕДУЮЩАЯ ОСТАНОВКА',78,642+h+70,VIOLET)
  panel(im,p+'leg',76,642+h+133,1088,46)
 elif i==1:
  label(d,'BY CAR' if lang=='en' else 'НА АВТОМОБИЛЕ',78,575,VIOLET)
  h=panel(im,p+'leg',76,635,1088,40)
  label(d,'ON FOOT' if lang=='en' else 'ПЕШКОМ ПО ГОРОДУ',78,635+h+52,VIOLET)
  panel(im,p+'walk',210,635+h+115,820,42)
 elif i==2:
  # Four complete route legs, no account data or pictures.
  panel(im,p+'route',200,570,840,46)
 elif i==3:
  label(d,'PLACES ALONG YOUR ROUTE' if lang=='en' else 'МЕСТА ПО МАРШРУТУ',78,580,VIOLET)
  h=panel(im,p+'rest-map',76,644,1088,40)
  label(d,'RESTAURANTS' if lang=='en' else 'РЕСТОРАНЫ',78,644+h+55,VIOLET)
  panel(im,p+'restaurant',130,644+h+112,980,28)
  panel(im,p+'restaurant2',130,1660,980,28)
 elif i==4:
  panel(im,p+'pet-tabs',76,582,1088,36)
  label(d,'CATALOG PREVIEW' if lang=='en' else 'МЕСТА ИЗ КАТАЛОГА',78,810,VIOLET)
  panel(im,p+'vet',112,876,1016,36)
  panel(im,p+'shop',112,1450,1016,36)
  # second crop deliberately ends before footer; no photo or photo contributor names
 elif i==5:
  panel(im,p+'sections',240,565,760,48)
 elif i==6:
  label(d,'VIEWER ACCESS' if lang=='en' else 'ДОСТУП НА ПРОСМОТР',78,638,VIOLET)
  h=panel(im,p+'invite',76,718,1088,48)
  yy=718+h+100
  lines=['One trip plan.','Everyone knows what’s next.'] if lang=='en' else ['Один план поездки.','Все знают, что дальше.']
  for line in lines: fittext(d,line,(78,yy),1080,58,INK); yy+=80
  label(d,'Invite by email' if lang=='en' else 'Приглашение по электронной почте',78,yy+48,MUTED,34)
 return im

def feature_graphic(lang):
 # Keep all text inside the central 16:9 crop used by Google Play discovery cards.
 scene=Image.open(ROOT/'feature-graphic/source/ramingo-itinerary-artwork-v2.png').convert('RGB')
 scene=ImageOps.fit(scene,(570,500),method=Image.Resampling.LANCZOS,centering=(0.5,0.5))
 im=Image.new('RGB',(1024,500),'#FFF3DA'); im.paste(scene,(454,0))
 d=ImageDraw.Draw(im)
 d.text((96,42),'Ramingo',font=font(34),fill=INK)
 if lang=='en':
  heads=['Every stop.','One clear plan.']; sub='Routes, places and city walks together.'; head_size=44; sub_size=20
 else:
  heads=['Все остановки —','в одном плане.']; sub='Маршруты, места и прогулки — вместе.'; head_size=42; sub_size=18
 for n,line in enumerate(heads): fittext(d,line,(96,144+n*64),340,head_size,INK)
 fittext(d,sub,(96,300),340,sub_size,MUTED,False)
 return im

def graphic(lang,kind):
 if kind=='plan': return feature_graphic(lang)
 im=Image.new('RGB',(1024,500), '#E9E3FF' if kind=='plan' else '#FFCCA5' if kind=='explore' else VIOLET)
 d=ImageDraw.Draw(im); dark=kind!='minimal'; fg=INK if dark else '#FFF3DA'
 logo(im,72,70,46,fg)
 if kind=='plan':
  lines=['Good trips','start with a plan.'] if lang=='en' else ['Путешествия','начинаются с плана.']
  for n,line in enumerate(lines): fittext(d,line,(72,194+n*68),485,47,fg)
  label(d,'Road trips. City walks. One plan.' if lang=='en' else 'Автопоездки. Прогулки. Один план.',74,365,MUTED,22)
  d.rounded_rectangle((558,78,982,424),radius=42,fill='#FFD3B3')
  panel(im,lang+'-map',584,104,372,40)
 elif kind=='explore':
  # Brand illustration extended from the existing approved coastal road, not a map/UI.
  d.polygon([(600,0),(1024,0),(1024,500),(840,500)],fill='#F58E76')
  d.polygon([(600,0),(650,90),(770,210),(705,300),(785,390),(1010,500),(700,500),(590,350),(625,250),(670,210),(580,80)],fill=VIOLET)
  d.line([(640,70),(730,200),(650,290),(740,385),(920,500)],fill='#FFF3DA',width=35,joint='curve')
  lines=['Make room','for discovery.'] if lang=='en' else ['Навстречу','открытиям.']
  for n,line in enumerate(lines): fittext(d,line,(72,192+n*66),500,51,fg)
 elif kind=='minimal':
  lines=['Your journey.','Thoughtfully planned.'] if lang=='en' else ['Ваше путешествие.','Всё продумано.']
  for n,line in enumerate(lines): fittext(d,line,(72,190+n*66),860,52,fg)
  d.line((74,382,940,382),fill='#B4A8FF',width=3)
  for x in [90,360,640,923]: d.ellipse((x-8,374,x+8,390),fill='#FFF3DA')
 return im

def main():
 args=argparse.ArgumentParser(); args.add_argument('--prepare-crops',action='store_true'); args.add_argument('--feature-only',action='store_true'); a=args.parse_args()
 if a.feature_only:
  for lang in ['en','ru']:
   save(feature_graphic(lang),f'feature-graphic/final/{"ru/" if lang=="ru" else ""}feature-graphic.png')
  print('Exported RU and EN crop-safe feature graphics only')
  return
 if a.prepare_crops:
  manifest=[]
  for key,(source,box) in CROPS.items():
   im=Image.open(RAW/(source+'.png')); crop=im.crop(box)
   save(crop,f'screenshots/sources/ui/{key}.png')
   manifest.append({'asset':key+'.png','capture':source+'.png','crop':box,'source_sha256':hashlib.sha256((RAW/(source+'.png')).read_bytes()).hexdigest(),'trip':'Italy with family','method':'adb screencap; native Android 0.2.77; light theme','date':'2026-10-19'})
  write('screenshots/sources/provenance.json',json.dumps(manifest,indent=2,ensure_ascii=False))
 for lang in ['en','ru']:
  for i,name in enumerate(NAMES): save(shot(lang,i),f'screenshots/final/{"ru/" if lang=="ru" else ""}{name}.png')
  save(feature_graphic(lang),f'feature-graphic/final/{"ru/" if lang=="ru" else ""}feature-graphic.png')
  for kind in ['explore','plan','minimal']: save(graphic(lang,kind),f'feature-graphic/concepts/{lang}-{kind}.png')
  for style in ['ivory','violet','peach']: save(shot(lang,0,style),f'screenshots/drafts/{lang}-direction-{style}.png')
  # Exact 9:16 supplemental export. Source composition is not stretched.
  for i,name in enumerate(NAMES):
   src=shot(lang,i).resize((1078,1920),Image.Resampling.LANCZOS)
   out=Image.new('RGB',(1080,1920),PAPER); out.paste(src,(1,0))
   save(out,f'screenshots/optional-1080x1920/{lang}/{name}.png')
  board=Image.new('RGB',(1960,690),PAPER); bd=ImageDraw.Draw(board)
  bd.text((26,20),'Ramingo / Google Play / '+lang.upper(),font=font(28),fill=INK)
  for i,name in enumerate(NAMES):
   src=Image.open(ROOT/f'screenshots/final/{"ru/" if lang=="ru" else ""}{name}.png').resize((260,463),Image.Resampling.LANCZOS)
   board.paste(src,(26+i*276,90)); bd.text((26+i*276,576),name,font=font(18,False),fill=MUTED)
  save(board,f'preview-{lang}.png')
 # Feature and screenshot direction comparison boards.
 board=Image.new('RGB',(1536,880),PAPER); d=ImageDraw.Draw(board)
 d.text((32,24),'Feature graphic / Trip plan · Explore · Minimal',font=font(32),fill=INK)
 concepts=[('TRIP PLAN / SELECTED',Image.open(ROOT/'feature-graphic/final/feature-graphic.png').convert('RGB')),('EXPLORE',graphic('en','explore')),('MINIMAL',graphic('en','minimal'))]
 for i,(name,concept) in enumerate(concepts):
  im=concept.resize((480,234)); board.paste(im,(24+i*504,110)); d.text((24+i*504,370),name,font=font(24),fill=INK)
 for i,style in enumerate(['ivory','violet','peach']):
  im=shot('en',0,style).resize((220,392)); board.paste(im,(150+i*504,450))
 save(board,'concepts-overview.png')
 report=[]
 for p in sorted(ROOT.rglob('*.png')):
  with Image.open(p) as im:
   if '/final/' in p.as_posix():
    expected=(1024,500) if 'feature-graphic' in str(p) else (1240,2208)
    assert im.size==expected,(p,im.size)
    assert im.mode=='RGB' and im.info.get('icc_profile'),p
   report.append({'file':str(p.relative_to(ROOT)),'size':im.size,'mode':im.mode,'sRGB':bool(im.info.get('icc_profile')),'bytes':p.stat().st_size})
 write('validation-images.json',json.dumps(report,indent=2))
 print(f'Exported and checked {len(report)} PNG assets')
if __name__=='__main__': main()
