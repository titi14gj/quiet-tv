package cn.quiet.tools;
import java.lang.reflect.*;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.List;

/** Local, shell-only diagnostic helper. No daemon or elevated APK. */
public class TvAdmin {
    static Object call(Object target,String name,Class<?>[] types,Object...args)throws Exception{
        Method m=target.getClass().getMethod(name,types);m.setAccessible(true);return m.invoke(target,args);
    }
    static Object service(String name,String stub)throws Exception{
        IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,name);
        return Class.forName(stub).getMethod("asInterface",IBinder.class).invoke(null,binder);
    }
    static int uid(String pkg)throws Exception{
        Object pm=Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);
        return (Integer)call(pm,"getPackageUid",new Class[]{String.class,int.class},pkg,0);
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&(args[0].equals("homes")||args[0].equals("home-set"))){
            Object pm=Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);
            List<ResolveInfo> homes=new ArrayList<>();
            System.out.println("preferred="+call(pm,"getHomeActivities",new Class[]{List.class},homes));
            ComponentName[] candidates=new ComponentName[homes.size()];
            int match=0;
            for(int i=0;i<homes.size();i++){
                ResolveInfo ri=homes.get(i);
                candidates[i]=new ComponentName(ri.activityInfo.packageName,ri.activityInfo.name);
                match=Math.max(match,ri.match);
                System.out.println("candidate="+candidates[i].flattenToString()+" match="+ri.match);
            }
            if(args[0].equals("home-set")){
                ComponentName target=ComponentName.unflattenFromString(args[1]);
                boolean found=false;for(ComponentName c:candidates)if(c.equals(target))found=true;
                if(!found)throw new IllegalArgumentException("Target is not an enabled HOME candidate");
                IntentFilter filter=new IntentFilter(Intent.ACTION_MAIN);
                filter.addCategory(Intent.CATEGORY_HOME);filter.addCategory(Intent.CATEGORY_DEFAULT);
                call(pm,"replacePreferredActivity",new Class[]{IntentFilter.class,int.class,ComponentName[].class,ComponentName.class,int.class},filter,match,candidates,target,0);
                System.out.println("preferred-after="+call(pm,"getHomeActivities",new Class[]{List.class},new ArrayList<ResolveInfo>()));
            }
            return;
        }
        if(args.length==0||args[0].equals("ops")){
            Class<?> c=Class.forName("android.app.AppOpsManager");
            for(Field f:c.getDeclaredFields())if(f.getName().startsWith("OP_")&&f.getType()==int.class){f.setAccessible(true);System.out.println(f.getName()+"="+f.getInt(null));}
            return;
        }
        if(args[0].equals("inputs")){
            Object w=Class.forName("tv.whaley.api.WtvManager").getMethod("getInstance").invoke(null);
            Object cec=call(w,"getCecManager",new Class[]{});
            Object source=call(w,"getInputSourceManger",new Class[]{});
            boolean[] status=(boolean[])call(source,"getAllInputSourceStatus",new Class[]{});
            System.out.println("current="+call(source,"getCurrentTvInputSource",new Class[]{}));
            for(int n=0;n<3;n++)System.out.println("HDMI "+(n+1)+" name="+call(cec,"getCecName",new Class[]{int.class,int.class},n,0)+" connected="+(status!=null&&status.length>n+4&&status[n+4]));
            return;
        }
        Object ops=service("appops","com.android.internal.app.IAppOpsService$Stub");
        String pkg=args[1];int code=Integer.parseInt(args[2]);int uid=uid(pkg);
        if(args[0].equals("get"))System.out.println(pkg+" uid="+uid+" op="+code+" mode="+call(ops,"checkOperation",new Class[]{int.class,int.class,String.class},code,uid,pkg));
        else if(args[0].equals("set")){
            int mode=Integer.parseInt(args[3]);call(ops,"setMode",new Class[]{int.class,int.class,String.class,int.class},code,uid,pkg,mode);
            System.out.println(pkg+" uid="+uid+" op="+code+" mode="+call(ops,"checkOperation",new Class[]{int.class,int.class,String.class},code,uid,pkg));
        }
    }
}
