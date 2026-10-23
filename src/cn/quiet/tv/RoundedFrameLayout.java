package cn.quiet.tv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.widget.FrameLayout;

/** API 19 rounded child clipping; API 21+ keeps the existing outline path. */
final class RoundedFrameLayout extends FrameLayout {
    private final Path clip=new Path();
    private final RectF bounds=new RectF();
    private final float radius;
    RoundedFrameLayout(Context context,float radius){super(context);this.radius=radius;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);bounds.set(0,0,w,h);clip.reset();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas){
        if(Build.VERSION.SDK_INT>=21){super.draw(canvas);return;}
        int save=canvas.save();canvas.clipPath(clip);super.draw(canvas);canvas.restoreToCount(save);
    }
}
