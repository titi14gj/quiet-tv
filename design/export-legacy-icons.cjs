// Rasterize the project's simple, solid-fill VectorDrawable paths for API 19.
// Android 21+ keeps the original anydpi vectors. No new third-party icon artwork.
const fs=require('fs'),path=require('path');
const sharp=require(process.env.QUIET_SHARP||'sharp');
const root=path.resolve(__dirname,'..');
const source=path.join(root,'res/drawable-anydpi-v21');
const output=path.join(root,'res/drawable-xxxhdpi');
const attr=(xml,name)=>{const m=xml.match(new RegExp('android:'+name+'="([^"]+)"'));if(!m)throw Error('Missing '+name);return m[1];};
(async()=>{
 fs.mkdirSync(output,{recursive:true});
 for(const name of fs.readdirSync(source).filter(n=>n.endsWith('.xml'))){
  const xml=fs.readFileSync(path.join(source,name),'utf8');
  if(/<group|<clip-path|strokeColor|fillType|fillAlpha/.test(xml))throw Error('Unsupported vector feature: '+name);
  const w=parseFloat(attr(xml,'width')),h=parseFloat(attr(xml,'height'));
  const shapes=[...xml.matchAll(/<path\b[^>]*\/\s*>/g)].map(m=>{
   const color=attr(m[0],'fillColor');if(!/^#[0-9a-f]{8}$/i.test(color))throw Error('Expected ARGB fill: '+name);
   const alpha=parseInt(color.slice(1,3),16)/255;
   return `<path fill="#${color.slice(3)}" fill-opacity="${alpha}" d="${attr(m[0],'pathData')}"/>`;
  }).join('');
  if(!shapes)throw Error('No paths: '+name);
  const svg=`<svg xmlns="http://www.w3.org/2000/svg" width="${w*4}" height="${h*4}" viewBox="0 0 ${attr(xml,'viewportWidth')} ${attr(xml,'viewportHeight')}">${shapes}</svg>`;
  await sharp(Buffer.from(svg)).png().toFile(path.join(output,name.replace('.xml','.png')));
 }
 console.log('Exported API 19 fallback icons.');
})().catch(e=>{console.error(e);process.exit(1)});
