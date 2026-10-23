package cn.quiet.tv;

import android.content.*;
import android.content.pm.*;
import android.os.Build;
import java.io.*;
import java.util.*;
import org.json.*;

/** Explicit local report; no serial, account, SSID, Wi-Fi credentials or upload. */
final class SetupReport {
    static final String CATALOG="setup/upstream-catalog.json";
    static JSONObject catalog(Context context)throws Exception{InputStream in=context.getAssets().open(CATALOG);try{return new JSONObject(read(in));}finally{in.close();}}
    static JSONArray related(Context context)throws Exception{
        JSONArray result=new JSONArray(),rows=catalog(context).getJSONArray("references");
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String haystack=(row.getString("title")+" "+row.getString("id")).toLowerCase(java.util.Locale.US);
            for(String alias:new String[]{Build.MODEL,Build.DEVICE}){String needle=alias.toLowerCase(java.util.Locale.US).trim();
                if(needle.length()>=3&&java.util.regex.Pattern.compile("(?<![a-z0-9])"+java.util.regex.Pattern.quote(needle)+"(?![a-z0-9])").matcher(haystack).find()){result.put(new JSONObject().put("id",row.getString("id")).put("title",row.getString("title")).put("url",row.getString("url")));break;}
            }
        }return result;
    }
    static String references(Context context)throws Exception{
        JSONObject data=catalog(context);JSONArray refs=data.getJSONArray("references");StringBuilder out=new StringBuilder("社区参考记录："+refs.length()+" 条\n来源提交："+data.getString("commit")+"\n\n这些是社区记录，不代表本 APK 已具备每款的可执行规则。\n\n");
        for(int i=0;i<refs.length();i++){JSONObject row=refs.getJSONObject(i);out.append(row.getString("title")).append(" · ").append(row.getString("os")).append('\n').append(row.getString("notes")).append("\n\n");}return out.toString();
    }
    static String create(Context context,SetupEngine.Shell shell)throws Exception{
        JSONObject root=new JSONObject(),device=new JSONObject();PackageManager pm=context.getPackageManager();
        root.put("schema",1).put("app_version",pm.getPackageInfo(context.getPackageName(),0).versionName).put("created_at_ms",System.currentTimeMillis());
        device.put("manufacturer",Build.MANUFACTURER).put("brand",Build.BRAND).put("model",Build.MODEL).put("device",Build.DEVICE).put("product",Build.PRODUCT).put("android",Build.VERSION.RELEASE).put("sdk",Build.VERSION.SDK_INT).put("display",Build.DISPLAY).put("fingerprint",Build.FINGERPRINT);root.put("device",device);
        boolean whaley=SetupPolicy.supported(Build.MODEL,Build.VERSION.SDK_INT,Build.DISPLAY,Build.FINGERPRINT);RegisteredProfile profile=RegisteredProfile.match(Build.MODEL,Build.DEVICE,Build.VERSION.SDK_INT,Build.DISPLAY,Build.FINGERPRINT);
        root.put("executable_rule",whaley?"whaley-65d2ut-heliosr-020803":profile==null?"none":profile.id);
        JSONObject catalog=catalog(context);root.put("upstream_catalog_commit",catalog.getString("commit"));root.put("upstream_reference_count",catalog.getJSONArray("references").length());root.put("community_reference_candidates",related(context));
        JSONArray apps=new JSONArray();List<ApplicationInfo> list=pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);Collections.sort(list,(a,b)->a.packageName.compareTo(b.packageName));
        for(ApplicationInfo app:list){JSONObject item=new JSONObject();item.put("package",app.packageName).put("label",pm.getApplicationLabel(app).toString()).put("system",(app.flags&ApplicationInfo.FLAG_SYSTEM)!=0).put("installed",(app.flags&ApplicationInfo.FLAG_INSTALLED)!=0).put("enabled",app.enabled);
            try{item.put("enabled_setting",pm.getApplicationEnabledSetting(app.packageName));PackageInfo info=pm.getPackageInfo(app.packageName,PackageManager.GET_UNINSTALLED_PACKAGES);item.put("version",info.versionName);}catch(Exception e){item.put("detail_unavailable",true);}apps.put(item);
        }root.put("packages",apps);
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);JSONArray homes=new JSONArray();
        for(ResolveInfo ri:pm.queryIntentActivities(home,0))homes.put(ri.activityInfo.packageName+"/"+ri.activityInfo.name);root.put("home_candidates",homes);
        ResolveInfo current=pm.resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);root.put("app_resolved_home",current==null?"none":current.activityInfo.packageName+"/"+current.activityInfo.name);
        JSONObject adb=new JSONObject();adb.put("connected_for_report",shell!=null);
        if(shell!=null){
            String[] commands={"id","getprop ro.build.fingerprint","getprop ro.adb.secure","pm list packages -d --user 0","cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME"};
            for(String command:commands){JSONObject result=new JSONObject();try{LocalAdb.Result r=shell.run(command);result.put("exit",r.exit).put("output",r.output);}catch(Exception e){result.put("error",e.getMessage());}adb.put(command,result);}
            Set<String> packages=new TreeSet<>();for(int i=0;i<homes.length();i++)packages.add(homes.getString(i).split("/")[0]);
            if(whaley)Collections.addAll(packages,SetupPolicy.HOME,SetupPolicy.AD1,SetupPolicy.AD2);if(profile!=null)Collections.addAll(packages,profile.packages);
            JSONObject states=new JSONObject();int count=0;for(String pkg:packages){if(++count>16)break;if(!pkg.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))continue;try{LocalAdb.Result r=shell.run("dumpsys package "+SetupPolicy.quote(pkg));SetupPolicy.State s=SetupPolicy.parse(r.output);states.put(pkg,new JSONObject().put("installed",s.installed).put("hidden",s.hidden).put("enabled",s.enabled).put("exit",r.exit));}catch(Exception e){states.put(pkg,new JSONObject().put("error",e.getMessage()));}}adb.put("package_states",states);
        }root.put("adb",adb);root.put("note","只读诊断报告；未据此执行停用。没有执行规则的设备需分析并验证，不能保证接管。未包含硬件序列号、账户或 Wi-Fi 凭据。");return root.toString(2);
    }
    static String read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>2097152)throw new IOException("报告/目录超过大小限制");out.write(bytes,0,n);}return new String(out.toByteArray(),"UTF-8");}
}
