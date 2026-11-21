'use strict';
// Browser fallback for visual tests and graceful preview outside Android.
if(typeof window.Studio==='undefined')window.Studio={
  reduced:()=>matchMedia('(prefers-reduced-motion:reduce)').matches,
  speak:()=>{},effect:()=>{},mode:()=> 'art',close:()=>history.back(),
  pictures:()=> '[]',save:()=>true,completed:()=>{}
};
