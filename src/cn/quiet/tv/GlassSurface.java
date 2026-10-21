package cn.quiet.tv;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Wallpaper lens, optical rim and specular light, rendered with API 21 Canvas. */
final class GlassSurface extends Drawable {
    final MainActivity owner;final View target;final boolean focused;final float radius;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
    GlassSurface(MainActivity o,View v,boolean f,int r){owner=o;target=v;focused=f;radius=o.dp(r);}
    public void draw(Canvas c){
        Rect b=getBounds();float inset=owner.dp(1);RectF r=new RectF(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);
        float cr=Math.min(radius,r.height()/2);
        Bitmap sample=owner.glassSample();
        if(sample!=null){
            int[] root=new int[2],here=new int[2];owner.root.getLocationOnScreen(root);target.getLocationOnScreen(here);
            BitmapShader shader=new BitmapShader(sample,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);Matrix m=new Matrix();
            m.setScale(owner.root.getWidth()/(float)sample.getWidth(),owner.root.getHeight()/(float)sample.getHeight());m.postTranslate(root[0]-here[0],root[1]-here[1]);
            m.postScale(focused?1.10f:1.045f,focused?1.10f:1.045f,r.centerX(),r.centerY());shader.setLocalMatrix(m);
            ColorMatrix saturation=new ColorMatrix();saturation.setSaturation(1.35f);p.setColorFilter(new ColorMatrixColorFilter(saturation));p.setShader(shader);
            c.drawRoundRect(r,cr,cr,p);p.setShader(null);p.setColorFilter(null);
        }
        p.setColor(focused?0x16121a2a:0x30121a2a);c.drawRoundRect(r,cr,cr,p);
        p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,focused?new int[]{0x52ffffff,0x12ffffff,0x08ffffff,0x30d7efff}:new int[]{0x29ffffff,0x09ffffff,0x04ffffff,0x19d7efff},new float[]{0,.34f,.64f,1},Shader.TileMode.CLAMP));
        c.drawRoundRect(r,cr,cr,p);p.setShader(null);
        // A broad highlight and asymmetric rim give volume without a solid white focus fill.
        p.setShader(new RadialGradient(r.left+r.width()*.2f,r.top-r.height()*.15f,Math.max(r.height(),r.width()*.8f),new int[]{focused?0x36ffffff:0x16ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(r,cr,cr,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(owner.dp(1.2f));
        p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,focused?new int[]{0xe6ffffff,0x40ffffff,0x90d5f0ff,0x27ffffff}:new int[]{0x8affffff,0x20ffffff,0x55d5f0ff,0x15ffffff},new float[]{0,.42f,.76f,1},Shader.TileMode.CLAMP));c.drawRoundRect(r,cr,cr,p);p.setShader(null);
        RectF inner=new RectF(r);inner.inset(owner.dp(1.3f),owner.dp(1.3f));
        p.setStrokeWidth(owner.dp(.6f));p.setColor(focused?0x42ffffff:0x16ffffff);c.drawRoundRect(inner,Math.max(0,cr-owner.dp(1.3f)),Math.max(0,cr-owner.dp(1.3f)),p);p.setStyle(Paint.Style.FILL);
    }
    public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    static Bitmap capture(View backdrop){if(backdrop.getWidth()==0)return null;int w=360,h=Math.max(1,(int)(360f*backdrop.getHeight()/backdrop.getWidth()));Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.scale(w/(float)backdrop.getWidth(),h/(float)backdrop.getHeight());backdrop.draw(c);
        int[] a=new int[w*h],temp=new int[w*h];b.getPixels(a,0,w,0,0,w,h);
        for(int pass=0;pass<2;pass++){for(int y=0;y<h;y++)for(int x=0;x<w;x++){int rr=0,gg=0,bb=0,n=0;for(int k=-3;k<=3;k++){int v=a[y*w+Math.max(0,Math.min(w-1,x+k))];rr+=Color.red(v);gg+=Color.green(v);bb+=Color.blue(v);n++;}temp[y*w+x]=Color.rgb(rr/n,gg/n,bb/n);}
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){int rr=0,gg=0,bb=0,n=0;for(int k=-3;k<=3;k++){int v=temp[Math.max(0,Math.min(h-1,y+k))*w+x];rr+=Color.red(v);gg+=Color.green(v);bb+=Color.blue(v);n++;}a[y*w+x]=Color.rgb(rr/n,gg/n,bb/n);}}
        b.setPixels(a,0,w,0,0,w,h);return b;}
}
