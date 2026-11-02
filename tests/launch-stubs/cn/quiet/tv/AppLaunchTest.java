package cn.quiet.tv;
import android.content.*;import android.content.pm.*;import java.util.*;
public class AppLaunchTest {
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError(checks);}
 static ResolveInfo app(String name){ResolveInfo r=new ResolveInfo();r.activityInfo=new ActivityInfo();r.activityInfo.packageName="example.app";r.activityInfo.name=name;r.activityInfo.exported=true;r.activityInfo.enabled=true;r.activityInfo.applicationInfo=new ApplicationInfo();r.activityInfo.applicationInfo.enabled=true;return r;}
 public static void main(String[] args){
  final List<ResolveInfo> tv=new ArrayList<>(),phone=new ArrayList<>();final List<String> queried=new ArrayList<>();
  PackageManager pm=new PackageManager(){public List<ResolveInfo> queryIntentActivities(Intent i,int f){check(i.pkg.equals("example.app"));queried.add(i.category);return i.category.equals(Intent.CATEGORY_LEANBACK_LAUNCHER)?tv:phone;}};
  ResolveInfo privateEntry=app("Private");privateEntry.activityInfo.exported=false;
  ResolveInfo disabled=app("Disabled");disabled.activityInfo.applicationInfo.enabled=false;
  tv.add(null);tv.add(new ResolveInfo());tv.add(privateEntry);tv.add(disabled);tv.add(app("TV"));phone.add(app("Phone"));
  Intent first=AppLaunch.resolve(pm,"example.app");check(first.component.name.equals("TV"));check(first.category.equals(Intent.CATEGORY_LEANBACK_LAUNCHER));check(queried.size()==1);check((first.flags&Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)!=0);check((first.flags&Intent.FLAG_ACTIVITY_NEW_TASK)!=0);
  tv.clear();tv.add(app("UpdatedTV"));check(AppLaunch.resolve(pm,"example.app").component.name.equals("UpdatedTV"));
  tv.get(0).activityInfo.enabled=false;Intent fallback=AppLaunch.resolve(pm,"example.app");check(fallback.component.name.equals("Phone"));check(fallback.category.equals(Intent.CATEGORY_LAUNCHER));
  phone.clear();check(AppLaunch.resolve(pm,"example.app")==null);check(!AppLaunch.available(null));check(!AppLaunch.available(privateEntry));
  System.out.println("Passed "+checks+" launch assertions (JVM Android boundary doubles; not device task-lifecycle validation).");
 }
}
