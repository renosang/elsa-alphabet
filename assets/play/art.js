'use strict';
// Original closed-shape SVG drawings. Closed regions power kid-safe masking.
const groupSpecs=[
  ['Thú cưng','🐱','cats','#8B5CF6',['Mèo đi ngủ','Mèo chơi bóng','Mèo đầu bếp','Mèo công chúa','Mèo phi hành gia','Mèo và cá','Gia đình mèo']],
  ['Sói con','🐺','wolves','#6366F1',['Sói con vui vẻ','Sói trong rừng','Sói đội mũ','Sói và trăng','Sói trượt tuyết','Sói đọc sách','Gia đình sói']],
  ['Kỳ lân','🦄','unicorns','#EC4899',['Kỳ lân cầu vồng','Kỳ lân ngôi sao','Kỳ lân trên mây','Kỳ lân công chúa','Kỳ lân ngủ ngon','Kỳ lân và hoa','Kỳ lân phép thuật']],
  ['Xe cộ','🚚','vehicles','#0EA5E9',['Xe ben chở đá','Máy xúc','Xe cứu hỏa','Xe tải quái vật','Xe cần cẩu','Xe cứu thương','Xe cảnh sát']],
  ['Đại dương','🐟','fish','#06B6D4',['Cá cầu vồng','Cá nóc','Cá voi con','Cá ngựa','Rùa biển','Bạch tuộc','Cá heo']],
  ['Yêu thương','💖','hearts','#F43F5E',['Trái tim cười','Đôi bạn thân','Trái tim có cánh','Gấu ôm tim','Thư yêu thương','Cầu vồng trái tim','Vườn yêu thương']],
  ['Quà tặng','🎁','gifts','#F59E0B',['Hộp quà nơ lớn','Bánh sinh nhật','Bóng bay','Kẹo ngọt','Mũ tiệc','Gấu quà tặng','Bữa tiệc sao']],
  ['Khủng long','🦕','dinos','#10B981',['Khủng long bạo chúa','Khủng long cổ dài','Khủng long ba sừng','Khủng long gai','Khủng long cánh','Khủng long bọc giáp','Khủng long con']],
  ['Trái đất','🌍','worlds','#22C55E',['Trái đất vui vẻ','Tên lửa','Hành tinh sao','Phi hành gia','Mặt trăng ngủ','Hệ mặt trời','Trạm không gian']],
  ['Trái cây','🍐','fruits','#84CC16',['Quả lê vui vẻ','Dâu tây','Cam mọng nước','Dưa hấu','Dứa đội kính','Chùm nho','Giỏ trái cây']],
  ['Sáng tạo','🎨','creative','#A855F7',['Bảng màu','Cọ vẽ','Lâu đài sắc màu','Cầu vồng','Bông hoa','Ngôi nhà mơ ước','Khu vườn sắc màu']]
];
const groups=groupSpecs.map(g=>({name:g[0],icon:g[1],key:g[2],theme:g[3],items:g[4].map((_,i)=>g[2]+'-'+(i+1))}));
const labels={};groupSpecs.forEach(g=>g[4].forEach((name,i)=>labels[g[2]+'-'+(i+1)]=name));
const ink='#25273b';
function artSVG(id,colored=false){
  const parts=id.split('-'),kind=parts[0],v=Number(parts[1]||1),white='#fff';
  const colors=['#ff8eb5','#ffd45e','#71d9bd','#7fc4ff','#b99af7','#ff9b63'];
  const c=n=>colored?colors[n%colors.length]:white;
  const path=(d,n=0)=>'<path d="'+d+'" fill="'+c(n)+'"/>';
  const ellipse=(x,y,rx,ry,n=0)=>'<ellipse cx="'+x+'" cy="'+y+'" rx="'+rx+'" ry="'+ry+'" fill="'+c(n)+'"/>';
  const circle=(x,y,r,n=0)=>'<circle cx="'+x+'" cy="'+y+'" r="'+r+'" fill="'+c(n)+'"/>';
  const line=d=>'<path d="'+d+'" fill="none"/>';
  const eye=(x,y)=>'<circle cx="'+x+'" cy="'+y+'" r="4" fill="'+ink+'" stroke="none"/><circle cx="'+(x-1)+'" cy="'+(y-1)+'" r="1" fill="white" stroke="none"/>';
  const face=(x,y)=>eye(x-16,y)+eye(x+16,y)+line('M'+(x-8)+' '+(y+13)+'q8 9 16 0');
  const star=(x,y,s)=>'<path d="M'+x+' '+(y-9*s)+'l'+(3*s)+' '+(7*s)+' '+(8*s)+' '+s+'-6 '+(5*s)+' '+(2*s)+' '+(8*s)+'-7-4-7 4 2-8-6-5 8-1Z" fill="'+c(1)+'"/>';
  const cloud=(x,y)=>path('M'+x+' '+(y+13)+'q-5-12 8-13 5-17 20-7 13-6 18 8 14-3 15 10 0 9-11 9H'+(x+7)+'q-7 0-7-7Z');
  let a=cloud(15,31)+star(207,31,.8)+path('M-5 146Q52 116 111 143t134-12v55H-5Z',2);
  function animal(wolf){
    a+=path(wolf?'M75 72L61 30l39 27Z':'M75 70L67 35l32 22Z',3)+path('M139 57l34-27-9 45Z',3)+ellipse(119,91,50,43,3)+ellipse(119,139,46,28,3)+ellipse(91,158,17,7)+ellipse(147,158,17,7)+face(119,87);
    if(wolf)a+=path('M78 108q-29 13-37 41 29 5 49-17Z',4);else a+=line('M82 101L60 97M82 108L60 112M157 101l22-4M157 108l22 5');
  }
  if(kind==='cats'||kind==='wolves')animal(kind==='wolves');
  else if(kind==='unicorns'){
    a+=ellipse(116,127,50,31)+path('M79 118Q65 92 78 67q17-30 53-18 23 8 32 34 8 17 27 26-4 25-30 17l-17-13-8 42h-19l-3-31-18 31H77l14-39Z')+path('M113 51l8-35 15 34Z',1)+path('M90 65q-31-15-32 11-21 11-5 27-15 18 10 27 14-19 15-41 1-15 12-24Z',4)+face(154,91);
  }else if(kind==='vehicles'){
    if(v===2||v===5)a+=path('M37 116h112V73h25l19 20v38H37Z',1)+path('M99 72l56-35 9 10-45 42Z')+path('M150 37h19v13h-19Z',3);
    else a+=path('M29 62h111v62H29Z',1)+path('M140 79h28q9 0 15 13l14 21v17h-57Z',v===3?0:3)+path('M150 87h18l14 21h-32Z',4)+(v===1?path('M42 54l13-20h22l12 20M91 54l13-20h22l12 20'):'');
    a+=path('M28 121h171v16H28Z')+circle(68,139,20)+circle(68,139,9,4)+circle(164,139,20)+circle(164,139,9,4);
  }else if(kind==='fish'){
    a+=ellipse(116,101,62,42,3)+path('M59 101L20 70v62Z',1)+path('M106 62q14-28 31-7l-4 17Z',4)+circle(145,91,5,0)+line('M155 111q12 9 23 0');
  }else if(kind==='hearts'){
    a+=path('M120 151C45 104 54 48 87 47q22 0 33 22 11-22 33-22c34 1 42 57-33 104Z')+face(120,93);if(v===3)a+=path('M72 83L28 59q3 35 51 50M168 83l44-24q-3 35-51 50Z',3);
  }else if(kind==='gifts'){
    a+=path('M55 78h130v79H55Z',v%3)+path('M45 60h150v27H45Z',1)+path('M111 61h18v96h-18Z',4)+path('M119 60Q77 53 84 30q8-18 35 30Zm2 0q42-7 35-30-8-18-35 30Z')+star(47,38,.7)+star(197,42,.65);
  }else if(kind==='dinos'){
    if(v===2||v===4)a+=path('M25 128q34 5 52-31 15-28 54-15 19 6 23-27 3-30 22-35 23-6 30 13 5 16-14 24l-18 7q-3 58-22 69l-3 24h-20l-3-21q-25 9-43-1l-9 22H55l7-29q-20 12-37 0Z',2)+face(180,38);
    else a+=path('M24 126q35 4 57-27 29-38 72-12 18 11 37 4 23-8 29 12 7 24-23 30l-25 1-7 23h-20l-2-19H91l-8 19H63l5-29q-25 13-44-2Z',2)+face(190,107)+path('M68 103L76 70l17 20 17-34 17 29 18-22 15 28Z',v%2?1:4);
  }else if(kind==='worlds'){
    a+=circle(120,96,59,3)+path('M80 62q23-17 33-2l-8 17 19 14-8 17-25-4-15-20Zm58-18 18 15-5 18 20 9-3 25-25 8-11-24 11-18-12-16Z',2)+face(120,118);if(v>1)a+=path('M36 64l22-11 13 8-21 12Z')+path('M48 69l6 22 9 2 2-27Z');
  }else if(kind==='fruits'){
    const shapes=['M84 145q-29-25-3-69 17-29 27-43 18 9 23 30 38 16 29 53-10 42-76 29Z','M65 133q-13-38 22-65 39-29 78 0 35 27 21 65-63 33-121 0Z'];
    a+=path(shapes[v%2],v%3)+path('M111 46q-2-25 18-31',2)+path('M123 29q19-21 39-4-12 20-39 4Z',2)+face(119,99);
  }else{
    a+=path('M49 119q-13-51 39-70 55-20 90 24 25 31 2 55-18 18-42 4-15-9-28 6-27 31-61-19Z',4)+circle(83,83,9)+circle(111,66,9,1)+circle(141,67,9,2)+circle(164,91,9,3)+circle(148,116,9,5)+path('M40 154l82-91 14 12-82 91Z')+path('M121 62l17-16 12 12-14 17Z',1);
  }
  for(let i=0;i<v;i++)a+=star(28+i*24,170-(i%2)*8,.38);
  return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 180"><rect width="240" height="180" fill="white"/><g stroke="'+ink+'" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">'+a+'</g></svg>';
}
