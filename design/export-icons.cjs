// Render the editable SVG foregrounds for Android 5, which has no SVG runtime.
const fs=require('fs'),vm=require('vm'),path=require('path');
const sharp=require(process.env.QUIET_SHARP||'sharp');
(async()=>{
 const catalog=vm.runInNewContext(fs.readFileSync(path.join(__dirname,'catalog.js'),'utf8')+'\nTV_CATALOG');
 const out=path.join(__dirname,'../assets/icons');fs.mkdirSync(out,{recursive:true});
 const meta=[];
 for(const a of catalog){
  const mark=a.shape.replaceAll('url(#F)','url(#front)').replaceAll('url(#A)','url(#accent)');
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="960" height="600" viewBox="0 0 960 600"><defs><linearGradient id="front" x2=".65" y2="1"><stop stop-color="#fff"/><stop offset=".45" stop-color="#fff"/><stop offset="1" stop-color="#edf4ff"/></linearGradient><linearGradient id="accent" x2="1" y2="1"><stop stop-color="${a.base[0]}"/><stop offset="1" stop-color="${a.base[1]}"/></linearGradient><filter id="shadow" x="-30%" y="-30%" width="160%" height="170%"><feGaussianBlur in="SourceAlpha" stdDeviation="1.3"/><feOffset dy="2"/><feComponentTransfer><feFuncA type="linear" slope=".2"/></feComponentTransfer><feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><svg x="226" y="48" width="508" height="504" viewBox="0 0 120 120"><g filter="url(#shadow)">${mark}</g></svg></svg>`;
  await sharp(Buffer.from(svg)).png().toFile(path.join(out,a.key+'.png'));
  meta.push({key:a.key,pkg:a.pkg||'',name:a.name,top:a.base[0],bottom:a.base[1]});
 }
 fs.writeFileSync(path.join(out,'catalog.json'),JSON.stringify(meta,null,2));
 console.log('Exported '+meta.length+' separate 960×600 transparent foregrounds.');
})().catch(e=>{console.error(e);process.exit(1)});
