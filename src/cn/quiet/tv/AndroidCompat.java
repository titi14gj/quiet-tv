package cn.quiet.tv;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

/** Newer framework calls are isolated so KitKat does not verify API 21 code on startup. */
final class AndroidCompat {
    private AndroidCompat() {}
    static void letterSpacing(TextView view,float value){if(Build.VERSION.SDK_INT>=21)Api21.letterSpacing(view,value);}
    static void elevation(View view,float value){if(Build.VERSION.SDK_INT>=21)Api21.elevation(view,value);}
    static void popupElevation(PopupWindow popup,float value){if(Build.VERSION.SDK_INT>=21)Api21.popupElevation(popup,value);}
    static void clipToOutline(View view){if(Build.VERSION.SDK_INT>=21)Api21.clipToOutline(view);}
    static Drawable loadBanner(ResolveInfo info,PackageManager pm){return Build.VERSION.SDK_INT>=21?Api21.loadBanner(info,pm):null;}

    @TargetApi(21)
    private static final class Api21 {
        static void letterSpacing(TextView view,float value){view.setLetterSpacing(value);}
        static void elevation(View view,float value){view.setElevation(value);}
        static void popupElevation(PopupWindow popup,float value){popup.setElevation(value);}
        static void clipToOutline(View view){view.setClipToOutline(true);}
        static Drawable loadBanner(ResolveInfo info,PackageManager pm){
            Drawable banner=info.activityInfo.loadBanner(pm);
            return banner!=null?banner:info.activityInfo.applicationInfo.loadBanner(pm);
        }
    }
}
