package cn.quiet.tv;

import android.content.Context;
import android.graphics.*;
import org.json.*;
import java.util.*;

/** Native foreground/background layers exported from the approved editable SVG studies. */
final class NativeArtwork {
    static class Art {String key,name;int top,bottom;Bitmap bitmap;}
    static final HashMap<String,Art> catalog=new HashMap<>();
    static Bitmap brands;
    static boolean loaded;
    final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
    final RectF dest=new RectF();
    final Context context;final Art art;final int brand;
    NativeArtwork(Context c,String pkg){context=c;load(c);art=catalog.get(pkg);
        brand=pkg.contains("xiaodianshi")?0:pkg.contains("ktcp")?1:pkg.contains("mgtv")?2:pkg.contains("qiyi")?3:-1;}
    static synchronized void load(Context c){if(loaded)return;loaded=true;
        try{java.io.InputStream in=c.getAssets().open("icons/catalog.json");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();
            JSONArray list=new JSONArray(out.toString("UTF-8"));for(int i=0;i<list.length();i++){JSONObject o=list.getJSONObject(i);String pkg=o.getString("pkg");if(pkg.isEmpty())continue;Art a=new Art();a.key=o.getString("key");a.name=o.getString("name");a.top=Color.parseColor(o.getString("top"));a.bottom=Color.parseColor(o.getString("bottom"));catalog.put(pkg,a);}
        }catch(Exception e){android.util.Log.e("QuietArtwork","catalog",e);}
    }
    boolean draw(Canvas c,int w,int h,float focus){
        if(brand<0&&art==null)return false;
        dest.set(0,0,w,h);float rx=w*.10f,ry=h*.16f;
        if(brand>=0){
            if(brands==null)try{brands=BitmapFactory.decodeStream(context.getAssets().open("icons/brand-study.png"));}catch(Exception e){return false;}
            float[][] bounds={{146,581,336,205},{527,593,290,181},{873,593,294,181},{1224,593,299,181}};
            float[] b=bounds[brand];BitmapShader shader=new BitmapShader(brands,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);Matrix m=new Matrix();m.setScale(w/b[2],h/b[3]);m.postTranslate(-b[0]*w/b[2],-b[1]*h/b[3]);shader.setLocalMatrix(m);paint.setShader(shader);c.drawRoundRect(dest,rx,ry,paint);paint.setShader(null);
        }else{
            paint.setShader(new LinearGradient(0,0,w,h,art.top,art.bottom,Shader.TileMode.CLAMP));c.drawRoundRect(dest,rx,ry,paint);paint.setShader(null);
            paint.setShader(new RadialGradient(w*.23f,0,w*.7f,new int[]{0x26ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(dest,rx,ry,paint);paint.setShader(null);
            if(art.bitmap==null)try{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inPreferredConfig=Bitmap.Config.ARGB_8888;opts.inSampleSize=w<500?2:1;art.bitmap=BitmapFactory.decodeStream(context.getAssets().open("icons/"+art.key+".png"),null,opts);}catch(Exception e){android.util.Log.e("QuietArtwork",art.key,e);}
            if(art.bitmap!=null){c.save();c.translate(0,-h*.015f*focus);c.scale(1+.018f*focus,1+.018f*focus,w/2f,h/2f);c.drawBitmap(art.bitmap,null,dest,paint);c.restore();}
        }
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{(int)(14+focus*18)<<24|0xffffff,0x00ffffff,0x03ffffff},new float[]{0,.48f,1},Shader.TileMode.CLAMP));c.drawRoundRect(dest,rx,ry,paint);paint.setShader(null);
        return true;
    }
}
