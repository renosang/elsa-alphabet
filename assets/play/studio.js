'use strict';
const W=1200,H=900;
const palette=['#ff3d83','#ff7051','#ffbf32','#78ca49','#00b9a5','#2799eb','#7555db','#9b5f35','#242638','#ffffff','rainbow'];
const toolSpecs=[
  {id:'paint',icon:'🖌️',name:'Cọ nước',shape:'brush'},
  {id:'crayon',icon:'✏️',name:'Bút chì',shape:'pencil'},
  {id:'spray',icon:'🧴',name:'Bình xịt',shape:'can'},
  {id:'fill',icon:'🪣',name:'Xô màu',shape:'drop'},
  {id:'glitter',icon:'🌟',name:'Kim tuyến',shape:'crayon'},
  {id:'stamp',icon:'🔵',name:'Con dấu',shape:'sticker'},
  {id:'erase',icon:'🧽',name:'Cục tẩy',shape:'none'}
];
const stickers=['⛄','⚽','🔮','🌱','🌟','🍓','🌻','😎','☀️'];
function layer(){const c=document.createElement('canvas');c.width=W;c.height=H;return c}
function studioCanvas(host,id){
  const display=layer(),paint=layer(),sticker=layer(),outline=layer(),stroke=layer(),mask=layer(),fillLayer=layer();
  const ctx=display.getContext('2d'),p=paint.getContext('2d'),k=sticker.getContext('2d'),o=outline.getContext('2d'),s=stroke.getContext('2d');
  host.append(display);
  let ready=false,alive=true,regions=null,locked=-1,last=null,zoom=1,panX=0,panY=0,scale=1,left=0,top=0,pinch=null;
  let tool=id==='blank'?'crayon':'fill',color=palette[0],stamp=stickers[0],hue=0,sprayTimer=0;
  const fingers=new Map();
  function compose(preview=true){
    ctx.clearRect(0,0,W,H);ctx.drawImage(paint,0,0);ctx.drawImage(sticker,0,0);
    if(preview&&last&&tool!=='erase'){
      ctx.globalAlpha=tool==='paint'?.58:1;ctx.drawImage(stroke,0,0);ctx.globalAlpha=1;
    }
    ctx.drawImage(outline,0,0);ctx.globalCompositeOperation='destination-over';ctx.fillStyle='white';ctx.fillRect(0,0,W,H);ctx.globalCompositeOperation='source-over';
  }
  function layout(){
    const width=host.clientWidth,height=host.clientHeight;scale=Math.min(width/W,height/H)*zoom;
    const dx=Math.max(0,(W*scale-width)/2),dy=Math.max(0,(H*scale-height)/2);
    panX=Math.max(-dx,Math.min(dx,panX));panY=Math.max(-dy,Math.min(dy,panY));
    left=(width-W*scale)/2+panX;top=(height-H*scale)/2+panY;
    display.style.width=W*scale+'px';display.style.height=H*scale+'px';display.style.left=left+'px';display.style.top=top+'px';
  }
  const observer=new ResizeObserver(()=>layout());observer.observe(host);
  function partition(data){
    const ids=new Uint16Array(W*H),queue=new Int32Array(W*H);for(let i=0;i<ids.length;i++)if(data[i*4]<175)ids[i]=65535;
    let region=0;for(let seed=0;seed<ids.length;seed++){
      if(ids[seed])continue;region++;let head=0,tail=1;queue[0]=seed;ids[seed]=region;
      while(head<tail){const j=queue[head++],x=j%W;const add=n=>{if(ids[n]===0){ids[n]=region;queue[tail++]=n}};
        if(x)add(j-1);if(x<W-1)add(j+1);if(j>=W)add(j-W);if(j<W*(H-1))add(j+W)}
    }return ids;
  }
  function clear(){p.clearRect(0,0,W,H);k.clearRect(0,0,W,H);s.clearRect(0,0,W,H);last=null;compose()}
  if(id==='blank'){ready=true;clear()}else{
    const img=new Image();img.onload=()=>{if(!alive)return;o.drawImage(img,0,0,W,H);const data=o.getImageData(0,0,W,H);regions=partition(data.data);
      for(let i=0;i<data.data.length;i+=4){const strength=Math.min(1,(255-data.data[i])/210);data.data[i]=35;data.data[i+1]=37;data.data[i+2]=55;data.data[i+3]=Math.round(255*strength)}
      o.putImageData(data,0,0);ready=true;compose()};img.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(artSVG(id));
  }
  function coords(e){const r=host.getBoundingClientRect();return{x:e.clientX-r.left,y:e.clientY-r.top}}
  function paper(pt){return{x:(pt.x-left)/scale,y:(pt.y-top)/scale}}
  function index(pt){return pt.x<0||pt.y<0||pt.x>=W||pt.y>=H?-1:Math.floor(pt.y)*W+Math.floor(pt.x)}
  function setMask(region){
    if(!regions||locked===region)return;locked=region;const m=mask.getContext('2d'),d=m.createImageData(W,H);
    for(let j=0;j<regions.length;j++)if(regions[j]===region)d.data[j*4+3]=255;m.putImageData(d,0,0);
  }
  function colorStyle(c,context){
    if(c!=='rainbow')return c;const g=context.createLinearGradient(0,0,W,H);['#ff3d83','#ffbf32','#4ade80','#2799eb','#8b5cf6'].forEach((x,i)=>g.addColorStop(i/4,x));return g;
  }
  function bucket(start){
    const region=regions?regions[start]:0;if(regions)setMask(region);const f=fillLayer.getContext('2d');f.clearRect(0,0,W,H);f.fillStyle=colorStyle(color,f);f.fillRect(0,0,W,H);
    if(regions){f.globalCompositeOperation='destination-in';f.drawImage(mask,0,0);f.globalCompositeOperation='source-over'}
    p.drawImage(fillLayer,0,0);compose();host.dispatchEvent(new CustomEvent('paint-ripple',{detail:{index:start}}));
  }
  function clipStroke(){if(regions){s.globalCompositeOperation='destination-in';s.drawImage(mask,0,0);s.globalCompositeOperation='source-over'}}
  function dots(to,spread,count,alpha,size){
    for(let j=0;j<count;j++){const angle=Math.random()*Math.PI*2,r=Math.sqrt(Math.random())*spread;s.globalAlpha=alpha;s.fillRect(to.x+Math.cos(angle)*r,to.y+Math.sin(angle)*r,size,size)}s.globalAlpha=1;
  }
  function eraseSegment(from,to){
    for(const target of [p,k]){target.save();target.globalCompositeOperation='destination-out';target.lineCap='round';target.lineJoin='round';target.lineWidth=48;target.beginPath();target.moveTo(from.x,from.y);target.lineTo(to.x+.01,to.y+.01);target.stroke();target.restore()}compose(false);
  }
  function segment(to){
    if(!last)return;if(tool==='erase'){eraseSegment(last,to);last=to;return}
    const dx=to.x-last.x,dy=to.y-last.y,length=Math.hypot(dx,dy);s.lineCap='round';s.lineJoin='round';s.fillStyle=colorStyle(color,s);s.strokeStyle=colorStyle(color,s);
    if(tool==='crayon'||tool==='spray'){
      const crayon=tool==='crayon',steps=Math.max(1,Math.ceil(length/5));for(let n=1;n<=steps;n++){const pt={x:last.x+dx*n/steps,y:last.y+dy*n/steps};dots(pt,crayon?13:38,crayon?45:30,crayon?.52:.27,crayon?3:2)}
    }else{
      s.lineWidth=tool==='glitter'?24:32;if(tool==='glitter')s.strokeStyle='hsl('+(hue=(hue+length/2+4)%360)+' 90% 55%)';
      s.beginPath();s.moveTo(last.x,last.y);s.lineTo(to.x+.01,to.y+.01);s.stroke();
      if(tool==='glitter'&&Math.random()>.35){s.fillStyle='white';s.font='22px sans-serif';s.fillText('✦',to.x-8,to.y+7)}
    }clipStroke();last=to;compose();
  }
  function placeStamp(pos){
    k.save();k.font='86px "Noto Color Emoji",sans-serif';k.textAlign='center';k.textBaseline='middle';k.shadowColor='#4b2c8060';k.shadowBlur=10;k.fillText(stamp,pos.x,pos.y);k.restore();compose();host.dispatchEvent(new CustomEvent('stamp-bounce',{detail:{x:pos.x,y:pos.y}}));
  }
  function gesture(){const [a,b]=Array.from(fingers.values());return{mid:{x:(a.x+b.x)/2,y:(a.y+b.y)/2},dist:Math.max(1,Math.hypot(a.x-b.x,a.y-b.y))}}
  let pendingFill=-1;
  host.onpointerdown=e=>{
    if(!ready||fingers.size>=2)return;host.setPointerCapture(e.pointerId);const pt=coords(e);fingers.set(e.pointerId,pt);
    if(fingers.size===2){last=null;pendingFill=-1;s.clearRect(0,0,W,H);compose();const g=gesture();pinch={anchor:paper(g.mid),distance:g.dist,zoom};return}
    const pos=paper(pt),i=index(pos);if(i<0||regions&&regions[i]===65535)return;
    if(tool==='fill'){pendingFill=i;return}if(tool==='stamp'){placeStamp(pos);return}if(regions)setMask(regions[i]);s.clearRect(0,0,W,H);last=pos;segment(pos);
    if(tool==='spray')sprayTimer=setInterval(()=>{if(last)segment({x:last.x+.01,y:last.y+.01})},55);
  };
  host.onpointermove=e=>{
    if(!fingers.has(e.pointerId))return;const pt=coords(e);fingers.set(e.pointerId,pt);
    if(pinch&&fingers.size===2){const g=gesture();zoom=Math.max(1,Math.min(5,pinch.zoom*g.dist/pinch.distance));layout();panX+=g.mid.x-(left+pinch.anchor.x*scale);panY+=g.mid.y-(top+pinch.anchor.y*scale);layout();return}
    if(last)segment(paper(pt));
  };
  function end(e,cancel){
    if(!fingers.has(e.pointerId))return;fingers.delete(e.pointerId);clearInterval(sprayTimer);sprayTimer=0;
    if(!cancel&&pendingFill>=0)bucket(pendingFill);pendingFill=-1;
    if(last&&!cancel&&tool!=='erase'){p.save();p.globalAlpha=tool==='paint'?.58:1;p.drawImage(stroke,0,0);p.restore()}
    s.clearRect(0,0,W,H);last=null;if(!fingers.size)pinch=null;compose();
  }
  host.onpointerup=e=>end(e,false);host.onpointercancel=e=>end(e,true);
  return{
    setTool(t){tool=t;s.clearRect(0,0,W,H)},setColor(c){color=c},setStamp(x){stamp=x},clear,
    image(){compose(false);return ready?display.toDataURL('image/png').split(',')[1]:null},
    dispose(){alive=false;clearInterval(sprayTimer);observer.disconnect()}
  };
}
