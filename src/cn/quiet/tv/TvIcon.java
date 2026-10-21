package cn.quiet.tv;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Locale;

/** Resolution-independent, consistently centered TV app marks. No low-res APK artwork. */
final class TvIcon extends View {
    final String pkg, label;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF box=new RectF(0,0,100,100);
    final Path path=new Path();
    final NativeArtwork artwork;
    float focus;
    android.animation.ValueAnimator focusAnimator;
    TvIcon(Context c,String pkg,String label){super(c);this.pkg=pkg;this.label=label;artwork=new NativeArtwork(c,pkg);setContentDescription(label);}
    void setFocusedArtwork(boolean focused){if(focusAnimator!=null)focusAnimator.cancel();focusAnimator=android.animation.ValueAnimator.ofFloat(focus,focused?1:0);focusAnimator.setDuration(220);focusAnimator.addUpdateListener(a->{focus=(Float)a.getAnimatedValue();invalidate();});focusAnimator.start();}
    static int color(String pkg){
        NativeArtwork.Art a=NativeArtwork.catalog.get(pkg);if(a!=null)return a.top;
        if(pkg.contains("xiaodianshi"))return 0xfff45b98;
        if(pkg.contains("ktcp"))return 0xff2196ed;
        if(pkg.contains("mgtv"))return 0xffff8617;
        if(pkg.contains("qiyi"))return 0xff26c964;
        if(pkg.contains("keep"))return 0xff655d78;
        if(pkg.contains("gitv"))return 0xff197de1;
        if(pkg.contains("screen"))return 0xff8b5be7;
        if(pkg.contains("mediaplayer"))return 0xff3984b9;
        if(pkg.contains("appstore"))return 0xff2a86ea;
        if(pkg.contains("cleantools"))return 0xff20c6ad;
        if(pkg.contains("smarthome"))return 0xff28b6a0;
        return Color.HSVToColor(new float[]{(pkg.hashCode()&0x7fffffff)%360,.58f,.72f});
    }
    protected void onDraw(Canvas canvas){
        if(artwork.draw(canvas,getWidth(),getHeight(),focus))return;
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/100f,getHeight()/100f);
        float radiusX=100f*getResources().getDisplayMetrics().density*16/getWidth(),radiusY=100f*getResources().getDisplayMetrics().density*16/getHeight();
        path.reset();path.addRoundRect(box,radiusX,radiusY,Path.Direction.CW);canvas.clipPath(path);
        int base=color(pkg);float[] hsv=new float[3];Color.colorToHSV(base,hsv);hsv[2]*=.72f;int dark=Color.HSVToColor(hsv);
        if(pkg.contains("ktcp")){base=0xfffdfefe;dark=0xffe8eff6;}
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,0,100,100,base,dark,Shader.TileMode.CLAMP));canvas.drawRect(box,p);p.setShader(null);
        p.setShader(new LinearGradient(0,0,0,100,new int[]{0x28ffffff,0x00ffffff,0x10000000},new float[]{0,.55f,1},Shader.TileMode.CLAMP));canvas.drawRect(box,p);p.setShader(null);
        p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setColor(Color.WHITE);
        canvas.save();canvas.translate(50,50);canvas.scale(getHeight()*.88f/getWidth(),.88f);canvas.translate(-50,-50);
        String name=label.toLowerCase(Locale.ROOT);
        if(pkg.contains("xiaodianshi"))bili(canvas);
        else if(pkg.contains("ktcp"))tencent(canvas);
        else if(pkg.contains("mgtv"))mango(canvas);
        else if(pkg.contains("qiyi"))word(canvas,"iQIYI",34,0xffffffff);
        else if(pkg.contains("keep")){stroke(6);path.reset();path.moveTo(34,25);path.lineTo(34,76);path.lineTo(70,27);canvas.drawPath(path,p);canvas.drawLine(45,60,70,76,p);}
        else if(pkg.contains("gitv"))broadcast(canvas);
        else if(pkg.contains("screen")){stroke(4);canvas.drawRoundRect(new RectF(23,27,77,67),6,6,p);canvas.drawLine(38,76,62,76,p);canvas.drawLine(50,68,50,76,p);spark(canvas,50,47,13);}
        else if(pkg.contains("mediaplayer")){play(canvas,38,28,72,50,38,73,Color.WHITE);}
        else if(pkg.contains("appstore")||name.contains("应用安装"))bag(canvas);
        else if(pkg.contains("cleantools")){p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(55,19);path.lineTo(30,55);path.lineTo(46,55);path.lineTo(42,82);path.lineTo(73,43);path.lineTo(55,43);path.close();canvas.drawPath(path,p);}
        else if(pkg.contains("smarthome")){stroke(5);path.reset();path.moveTo(22,47);path.lineTo(50,23);path.lineTo(78,47);canvas.drawPath(path,p);path.reset();path.moveTo(30,43);path.lineTo(30,77);path.lineTo(70,77);path.lineTo(70,43);canvas.drawPath(path,p);canvas.drawRoundRect(new RectF(44,57,57,77),2,2,p);}
        else if(name.contains("投屏")||pkg.contains("whaleylink")){stroke(4);canvas.drawRoundRect(new RectF(22,24,80,67),5,5,p);canvas.drawLine(42,77,64,77,p);canvas.drawLine(53,68,53,77,p);canvas.drawArc(new RectF(16,48,46,78),-90,90,false,p);canvas.drawArc(new RectF(16,58,36,78),-90,90,false,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(19,75,3,p);}
        else if(pkg.contains("setting"))gear(canvas);
        else if(pkg.contains("facerecognition")){stroke(4);canvas.drawCircle(50,39,14,p);canvas.drawArc(new RectF(27,52,73,91),180,180,false,p);spark(canvas,76,27,9);}
        else if(pkg.contains("message")){stroke(4);path.reset();path.moveTo(25,25);path.lineTo(76,25);path.quadTo(80,25,80,30);path.lineTo(80,64);path.quadTo(80,69,75,69);path.lineTo(43,69);path.lineTo(27,80);path.lineTo(27,69);path.quadTo(20,69,20,62);path.lineTo(20,32);path.quadTo(20,25,25,25);canvas.drawPath(path,p);canvas.drawLine(33,41,66,41,p);canvas.drawLine(33,52,58,52,p);}
        else if(pkg.contains("music")){stroke(5);path.reset();path.moveTo(42,67);path.lineTo(42,29);path.lineTo(71,22);path.lineTo(71,60);canvas.drawPath(path,p);p.setStyle(Paint.Style.FILL);canvas.drawOval(new RectF(25,60,43,74),p);canvas.drawOval(new RectF(54,53,72,67),p);}
        else if(pkg.contains("sports")){stroke(4);canvas.drawCircle(50,50,28,p);canvas.drawLine(50,22,50,78,p);canvas.drawLine(22,50,78,50,p);canvas.drawArc(new RectF(35,22,65,78),90,180,false,p);canvas.drawArc(new RectF(35,22,65,78),-90,180,false,p);}
        else if(pkg.contains("kids")){p.setStyle(Paint.Style.FILL);canvas.drawCircle(30,32,11,p);canvas.drawCircle(70,32,11,p);canvas.drawCircle(50,51,28,p);p.setColor(base);canvas.drawCircle(40,47,3,p);canvas.drawCircle(60,47,3,p);canvas.drawOval(new RectF(44,56,56,63),p);}
        else {String mark=label.trim();if(mark.isEmpty())mark="TV";else mark=mark.substring(0,Character.charCount(mark.codePointAt(0)));word(canvas,mark,44,Color.WHITE);}
        canvas.restore();p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.7f);p.setColor(0x35ffffff);canvas.drawRoundRect(new RectF(.5f,.5f,99.5f,99.5f),radiusX,radiusY,p);canvas.restore();
    }
    void stroke(float n){p.setShader(null);p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(n);}
    void word(Canvas c,String text,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);if(p.measureText(text)>82)p.setTextSize(size*82/p.measureText(text));Paint.FontMetrics fm=p.getFontMetrics();c.drawText(text,50,50-(fm.ascent+fm.descent)/2,p);}
    void bili(Canvas c){stroke(4.8f);c.drawRoundRect(new RectF(22,31,78,73),9,9,p);c.drawLine(41,29,31,18,p);c.drawLine(59,29,69,18,p);c.drawLine(33,47,42,45,p);c.drawLine(58,45,67,47,p);path.reset();path.moveTo(42,57);path.quadTo(46,68,50,59);path.quadTo(54,68,58,57);c.drawPath(path,p);c.drawLine(34,74,31,80,p);c.drawLine(66,74,69,80,p);}
    void tencent(Canvas c){p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(20,20,80,80,0xff4cd68a,0xff00a5ed,Shader.TileMode.CLAMP));path.reset();path.moveTo(35,19);path.cubicTo(28,15,24,20,24,26);path.lineTo(24,77);path.cubicTo(24,83,29,85,34,82);path.lineTo(82,56);path.cubicTo(88,52,88,47,82,44);path.close();c.drawPath(path,p);p.setShader(null);play(c,20,31,61,50,20,70,0xffffcf31);play(c,38,34,68,50,38,67,0xff248cf4);}
    void play(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);path.reset();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.close();c.drawPath(path,p);}
    void mango(Canvas c){stroke(5);path.reset();path.moveTo(31,19);path.cubicTo(18,14,20,33,20,50);path.cubicTo(20,72,19,85,33,82);path.lineTo(78,58);path.cubicTo(91,50,82,43,75,39);path.close();c.drawPath(path,p);stroke(7);path.reset();path.moveTo(34,66);path.lineTo(34,34);path.lineTo(48,56);path.lineTo(61,43);path.lineTo(61,63);c.drawPath(path,p);}
    void broadcast(Canvas c){stroke(4.5f);c.save();c.rotate(35,50,50);c.drawOval(new RectF(21,34,79,66),p);c.rotate(90,50,50);c.drawOval(new RectF(21,34,79,66),p);c.restore();p.setStyle(Paint.Style.FILL);c.drawCircle(50,50,5,p);}
    void bag(Canvas c){stroke(4);c.drawRoundRect(new RectF(25,34,75,79),6,6,p);c.drawArc(new RectF(37,20,63,48),180,180,false,p);spark(c,50,56,12);}
    void spark(Canvas c,float x,float y,float radius){p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(x,y-radius);path.lineTo(x+radius*.3f,y-radius*.3f);path.lineTo(x+radius,y);path.lineTo(x+radius*.3f,y+radius*.3f);path.lineTo(x,y+radius);path.lineTo(x-radius*.3f,y+radius*.3f);path.lineTo(x-radius,y);path.lineTo(x-radius*.3f,y-radius*.3f);path.close();c.drawPath(path,p);}
    void gear(Canvas c){stroke(5);c.drawCircle(50,50,23,p);c.drawCircle(50,50,8,p);for(int n=0;n<8;n++){c.save();c.rotate(n*45,50,50);c.drawLine(50,17,50,26,p);c.restore();}}
}
