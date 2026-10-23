package cn.quiet.tv;
import java.io.IOException;

/** Standard HOME registration only; no factory package is disabled. Caller verifies ADB identity. */
final class DefaultHomeEngine {
    static void select(SetupEngine.Shell shell)throws Exception{
        LocalAdb.Result own=shell.run("pm path cn.quiet.tv");
        if(own.exit!=0||!own.output.startsWith("package:"))throw new IOException("未确认静境 TV 安装状态");
        LocalAdb.Result set=shell.run("cmd package set-home-activity --user 0 cn.quiet.tv/.MainActivity");
        LocalAdb.Result resolve=shell.run("cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME");
        String[] lines=resolve.output.trim().split("[\\r\\n]+");String component=lines[lines.length-1].trim();
        if(set.exit!=0||resolve.exit!=0||!(component.equals("cn.quiet.tv/.MainActivity")||component.equals("cn.quiet.tv/cn.quiet.tv.MainActivity")))throw new IOException("未确认默认桌面切换成功；"+set.output+"；"+resolve.output);
    }
}
