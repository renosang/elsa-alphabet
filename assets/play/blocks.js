const brickColors=['#3B82F6','#EF4444','#FACC15','#22C55E'];
const brickNames=['xanh dương','đỏ','vàng','xanh lá'];
function shuffle(values){for(let i=values.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[values[i],values[j]]=[values[j],values[i]]}return values}
function level(){
  const total=4+Math.floor(Math.random()*9),colors=shuffle([0,1,2,3]).slice(0,3+Math.floor(Math.random()*2));
  const blocks=colors.slice();
  while(blocks.length<total)blocks.push(colors[Math.floor(Math.random()*colors.length)]);
  shuffle(blocks);
  const target=colors[Math.floor(Math.random()*colors.length)],count=blocks.filter(x=>x===target).length;
  const near=[];
  for(let d=1;near.length<3;d++){if(count-d>0)near.push(count-d);near.push(count+d)}
  return {blocks,target,count,choices:shuffle([count,...near.slice(0,3)])};
}
function blockIcon(x,y,c){
  const face=(points,fill)=>'<polygon points="'+points+'" fill="'+fill+'" stroke="#334155" stroke-opacity=".30" stroke-width=".85" stroke-linejoin="round"/>';
  const top='0,-23 40,0 0,23 -40,0';
  let g=face('-40,0 0,23 0,44 -40,21',c)+face('0,23 40,0 40,21 0,44',c)+face('0,23 40,0 40,21 0,44','#0003')+face(top,c)+face(top,'#fff3');
  for(const [sx,sy] of [[0,-12],[-18,-2],[18,-2],[0,8]]){
    g+='<path d="M'+(sx-6)+' '+sy+'v-3h12v3a6 3 0 1 1-12 0" fill="'+c+'"/><ellipse cx="'+sx+'" cy="'+(sy-3)+'" rx="6" ry="3.5" fill="'+c+'" stroke="#fff6" stroke-width="1"/>';
  }
  return '<g transform="translate('+x+' '+y+')">'+g+'</g>';
}
function playBlocks(){
  const q=level();screen='blocks';const replay=()=>say(String(q.target));
  const {stage,panel,foot,header}=frame('Có bao nhiêu khối màu '+brickNames[q.target]+'?',replay,true);
  const chip=document.createElementNS('http://www.w3.org/2000/svg','svg');chip.setAttribute('viewBox','-44 -27 88 74');chip.setAttribute('width','48');chip.setAttribute('height','48');chip.innerHTML=blockIcon(0,0,brickColors[q.target]);header.insertBefore(chip,header.querySelector('h1'));
  const board=document.createElementNS(chip.namespaceURI,'svg');board.classList.add('board');stage.append(board);
  const cols=Math.ceil(Math.sqrt(q.blocks.length)),positions=q.blocks.map((c,i)=>({c,i,x:i%cols,y:Math.floor(i/cols)}));
  const counts=[0,0,0,0];let locked=false;
  const projected=positions.map(b=>({...b,sx:(b.x-b.y)*40,sy:(b.x+b.y)*23})).sort((a,b)=>a.sy-b.sy);
  const left=Math.min(...projected.map(b=>b.sx))-50,right=Math.max(...projected.map(b=>b.sx))+50;
  board.setAttribute('viewBox',[left,-38,right-left,Math.max(...projected.map(b=>b.sy))+94].join(' '));
  for(const b of projected){
    const brick=document.createElementNS(chip.namespaceURI,'g');brick.classList.add('brick');brick.dataset.color=b.c;
    brick.setAttribute('role','button');brick.setAttribute('tabindex','0');brick.setAttribute('aria-label','Khối màu '+brickNames[b.c]+' thứ '+(b.i+1));
    brick.innerHTML=blockIcon(b.sx,b.sy,brickColors[b.c]);board.append(brick);
    function tap(){
      if(locked)return;
      if(!brick.dataset.count){brick.dataset.count=++counts[b.c];const tag=document.createElementNS(chip.namespaceURI,'g');
        tag.innerHTML='<circle cx="'+b.sx+'" cy="'+(b.sy-4)+'" r="10" fill="white"/><text x="'+b.sx+'" y="'+(b.sy+1)+'" text-anchor="middle" font-size="14" font-weight="900" fill="#253147">'+brick.dataset.count+'</text>';brick.append(tag)}
      animate(brick,'bounce');
    }
    brick.onclick=tap;brick.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();tap()}};
  }
  foot.append(btn('A=','Gợi ý màu cần đếm',()=>board.querySelectorAll('[data-color="'+q.target+'"]').forEach(el=>animate(el,'hint')),'icon'));
  const grid=el('div','answers');panel.append(grid);
  for(const value of q.choices){
    const answer=btn(String(value),'Đáp án '+value,()=>{
      if(locked)return;locked=true;const correct=value===q.count;
      answer.classList.add(correct?'good':'bad');sound(correct);
      if(correct){Studio.completed();confetti()}
      timer=setTimeout(()=>{if(correct)playBlocks();else{locked=false;answer.classList.remove('bad')}},correct?1500:800);
    },'answer');grid.append(answer);
  }
  replay();
}
