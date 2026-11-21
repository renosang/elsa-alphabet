'use strict';
const app=document.getElementById('app');
let screen='',category=7,paint=null,timer=0,frameId=0,dialog=null,currentReplay=null;
const reduced=Studio.reduced();
function el(tag,cls,text){const n=document.createElement(tag);if(cls)n.className=cls;if(text!=null)n.textContent=text;return n}
function btn(text,label,action,cls){const b=el('button',cls,text);b.type='button';b.setAttribute('aria-label',label);b.onclick=action;return b}
function animate(node,cls){if(reduced)return;node.classList.remove(cls);void node.getBoundingClientRect();node.classList.add(cls)}
function say(key){try{Studio.speak(key)}catch(e){}}
let audio;
function sound(right){
  try{audio=audio||new AudioContext();audio.resume();const now=audio.currentTime;(right?[660,880]:[220]).forEach((freq,i)=>{const osc=audio.createOscillator(),gain=audio.createGain(),t=now+i*.13;osc.type='sine';osc.frequency.setValueAtTime(freq,t);if(!right)osc.frequency.exponentialRampToValueAtTime(145,t+.18);gain.gain.setValueAtTime(.0001,t);gain.gain.exponentialRampToValueAtTime(.09,t+.014);gain.gain.exponentialRampToValueAtTime(.0001,t+.22);osc.connect(gain);gain.connect(audio.destination);osc.start(t);osc.stop(t+.24)})}catch(e){}
}
function effect(key){try{if(Studio.effect)Studio.effect(key);else sound(key==='saved'||key==='stamp')}catch(e){sound(true)}}
function confetti(){
  if(reduced)return;const canvas=document.getElementById('fx'),ctx=canvas.getContext('2d'),w=canvas.width=innerWidth,h=canvas.height=innerHeight;
  const ribbons=Array.from({length:100},()=>({x:Math.random()*w,y:-Math.random()*h*.65,s:3+Math.random()*5,c:palette[Math.floor(Math.random()*9)],v:.6+Math.random()}));const start=performance.now();cancelAnimationFrame(frameId);
  function draw(t){const f=Math.min(1,(t-start)/1500);ctx.clearRect(0,0,w,h);if(f>=1)return;for(const r of ribbons){ctx.save();ctx.translate(r.x+Math.sin(f*9+r.x)*30,r.y+f*h*1.8*r.v);ctx.rotate(f*8+r.x);ctx.fillStyle=r.c;ctx.fillRect(-r.s,-r.s,r.s*2,r.s);ctx.restore()}frameId=requestAnimationFrame(draw)}frameId=requestAnimationFrame(draw);
}
function teardown(){clearTimeout(timer);cancelAnimationFrame(frameId);if(paint){paint.dispose();paint=null}dialog=null;app.replaceChildren();app.className=''}
function frame(title,replay,game=false){
  teardown();app.className=screen==='blocks'?'wood':'';const header=el('header','basicHeader');header.append(btn('×','Quay lại',goBack,'exit icon'),el('h1','',title));app.append(header);
  let stage,panel;if(game){const main=el('section','game');stage=el('div','stage');panel=el('div','panel');main.append(stage,panel);app.append(main)}
  const foot=el('footer');currentReplay=replay;foot.append(btn('🔊','Nghe hướng dẫn',replay,'speaker icon'));app.append(foot);return{header,stage,panel,foot};
}
function card(label,art,action){const b=btn('',label,action,'card');const visual=el('div','cardArt');visual.innerHTML=art;b.append(visual,el('strong','',label));return b}
function pencilsArt(){return '<svg viewBox="0 0 240 180"><rect width="240" height="180" fill="white"/>'+['#ff628e','#ffcc45','#75d773','#8b62fa'].map((c,k)=>'<path d="M'+(37+k*48)+' 180v-40l13-22 13 22v40Z" fill="'+c+'"/>').join('')+'</svg>'}
function showHub(){showGallery(category)}
function showGallery(i){
  teardown();category=i;screen='gallery';const group=groups[i];app.className='galleryScreen';app.style.setProperty('--theme',group.theme);app.dataset.theme=group.key;
  const deco=el('div','decorations');deco.innerHTML='<i class="rainbowPencil"></i><i class="paperclip">⌕</i><i class="eraserDeco"></i><i class="sparkDeco">✦</i>';app.append(deco);
  const top=el('header','galleryHeader');top.append(btn('‹','Đóng xưởng vẽ',()=>Studio.close(),'exit icon'),el('h1','',group.name),btn('🖼️','Tranh của bé',showAlbum,'albumButton icon'));app.append(top);
  const tabs=el('nav','categoryBar');groups.forEach((g,j)=>{const b=btn(g.icon,g.name,()=>showGallery(j),j===i?'selected':'');b.title=g.name;tabs.append(b)});app.append(tabs);requestAnimationFrame(()=>tabs.querySelector('.selected').scrollIntoView({behavior:reduced?'auto':'smooth',inline:'center',block:'nearest'}));
  const shelf=el('section','galleryGrid');shelf.append(card('Giấy trắng vẽ tự do',pencilsArt(),()=>showCanvas('blank')));group.items.forEach(id=>shelf.append(card(labels[id],artSVG(id),()=>showCanvas(id))));app.append(shelf);say('gallery');
}
function showAlbum(){
  screen='album';const {foot}=frame('Tranh của bé',()=>say('album'));const shelf=el('section','shelf');app.insertBefore(shelf,foot);const files=JSON.parse(Studio.pictures());
  if(!files.length)shelf.append(el('p','empty','Tranh con lưu sẽ hiện ở đây.'));files.forEach((file,i)=>{const b=btn('','Xem tranh '+(files.length-i),()=>showPicture(file),'card');const img=el('img','albumPhoto');img.loading='lazy';img.src='album/'+file;b.append(img);shelf.append(b)});say('album');
}
function showPicture(file){screen='picture';const {foot}=frame('Tranh của bé',()=>say('album'));const shelf=el('section','shelf pictureShelf');app.insertBefore(shelf,foot);const img=el('img','albumPhoto');img.src='album/'+file;shelf.append(img)}
function confirmAction(title,yes,spoken){
  if(dialog)return;dialog=el('div','overlay');const box=el('div','dialog');box.setAttribute('role','dialog');box.append(el('div','dialogArt','🗑️'),el('p','',title));const actions=el('div','dialogActions');actions.append(btn('↩','Quay lại',dismiss,'cancelChoice'),btn('✓','Đồng ý',()=>{dismiss();yes()},'yesChoice'));box.append(actions);dialog.append(box);app.append(dialog);if(spoken)say(spoken);
}
function dismiss(){if(dialog)dialog.remove();dialog=null}
function paletteItem(tool,value,index,onChoose){
  const label=tool.id==='stamp'?'Nhãn dán '+value:'Màu '+(index+1),b=btn('',label,()=>onChoose(b,value),'paletteItem '+tool.shape);b.dataset.value=value;
  if(tool.id==='stamp')b.append(el('span','stickerFace',value));
  else{const body=el('span','physicalColor');body.style.setProperty('--c',value==='rainbow'?'linear-gradient(135deg,#ff4b7d,#ffd43b,#46d68c,#3598ff,#9b5df0)':value);body.append(el('i'));b.append(body)}return b;
}
function showCanvas(id){
  teardown();screen='canvas';app.className='canvasScreen';app.style.setProperty('--theme',groups[category].theme);
  const top=el('header','canvasHeader');top.append(btn('×','Đóng tranh',goBack,'exit icon'),el('h1','',id==='blank'?'Vẽ tự do':labels[id]));app.append(top);
  const actions=el('div','canvasActions');const reset=btn('⟳','Làm mới',()=>confirmAction('Vẽ lại từ đầu?',()=>{paint.clear();delete board.dataset.dirty;effect('reset')},'reset'),'refresh actionButton');const camera=btn('📷','Lưu tranh',savePicture,'camera actionButton');actions.append(reset,camera);app.append(actions);
  const layout=el('section','studioLayout'),stage=el('div','canvasStage'),paperStack=el('div','paperStack'),board=el('div','paper');paperStack.append(board);stage.append(paperStack);layout.append(stage);
  const controls=el('aside','studioControls'),paletteDock=el('div','paletteDock'),toolDock=el('div','toolDock'),status=el('div','toolStatus');status.setAttribute('role','status');controls.append(paletteDock,toolDock,status);layout.append(controls);app.append(layout);
  paint=studioCanvas(board,id);let active=id==='blank'?'crayon':'fill',currentColor=palette[0],currentStamp=stickers[0];
  board.addEventListener('pointerdown',()=>{board.dataset.dirty='yes'});board.addEventListener('paint-ripple',e=>{animate(board,'ripple');effect('bucket')});
  function select(container,chosen){for(const item of container.children){const on=item===chosen;item.classList.toggle('selected',on);item.setAttribute('aria-pressed',String(on))}}
  function renderPalette(tool){
    paletteDock.replaceChildren();paletteDock.className='paletteDock palette-'+tool.shape;if(tool.id==='erase'){controls.classList.add('eraserMode');status.textContent='Kéo tẩy trên màu';return}controls.classList.remove('eraserMode');
    const values=tool.id==='stamp'?stickers:palette;values.forEach((value,index)=>{const item=paletteItem(tool,value,index,(chosen,val)=>{select(paletteDock,chosen);if(tool.id==='stamp'){currentStamp=val;paint.setStamp(val);status.textContent='Chọn '+val}else{currentColor=val;paint.setColor(val);status.textContent=val==='rainbow'?'Màu cầu vồng':'Đã chọn màu'};effect('tap')});paletteDock.append(item);if(value===(tool.id==='stamp'?currentStamp:currentColor))item.classList.add('selected')});
  }
  toolSpecs.forEach(spec=>{const b=btn(spec.icon,spec.name,()=>{active=spec.id;paint.setTool(active);select(toolDock,b);renderPalette(spec);status.textContent=spec.name;effect('tap')},'toolButton');b.append(el('small','',spec.name));toolDock.append(b);if(active===spec.id)b.classList.add('selected')});renderPalette(toolSpecs.find(x=>x.id===active));status.textContent=toolSpecs.find(x=>x.id===active).name;say('canvas');
  function savePicture(){
    if(!paint)return;app.classList.add('captureMode');requestAnimationFrame(()=>setTimeout(()=>{const png=paint.image();app.classList.remove('captureMode');if(!png)return;if(Studio.save(png)){delete board.dataset.dirty;animate(app,'cameraFlash');confetti();effect('shutter');say('saved');status.textContent='Đã lưu vào Tranh của bé'}else status.textContent='Chưa lưu được. Con thử lại nhé.'},200));
  }
}
function goBack(){
  if(dialog){dismiss();return}if(screen==='hub'||screen==='blocks'||screen==='gallery'){clearTimeout(timer);Studio.close()}
  else if(screen==='picture')showAlbum();else if(screen==='canvas'){const leave=()=>showGallery(category);if(app.querySelector('.paper[data-dirty]'))confirmAction('Rời tranh chưa lưu?',leave);else leave()}else showGallery(category);
}
if(Studio.mode()==='blocks')playBlocks();else showGallery(category);
