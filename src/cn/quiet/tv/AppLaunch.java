package cn.quiet.tv;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** Refresh launch targets at click time; prefer externally usable TV entries. */
final class AppLaunch {
    static boolean available(ResolveInfo entry){
        return entry!=null&&entry.activityInfo!=null&&entry.activityInfo.exported
            &&entry.activityInfo.enabled&&entry.activityInfo.applicationInfo!=null
            &&entry.activityInfo.applicationInfo.enabled;
    }
    static Intent resolve(PackageManager pm,String pkg){
        for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(pkg);
            for(ResolveInfo entry:pm.queryIntentActivities(query,0)){
                if(!available(entry)||!pkg.equals(entry.activityInfo.packageName))continue;
                return query.setComponent(new ComponentName(pkg,entry.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            }
        }
        return null;
    }
}
