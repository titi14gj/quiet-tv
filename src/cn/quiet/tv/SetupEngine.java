package cn.quiet.tv;

import java.io.IOException;

/** Verified, reversible operations for one known profile. No uninstall, arbitrary package or root. */
final class SetupEngine {
    interface Shell {LocalAdb.Result run(String command)throws Exception;}
    interface Journal {void remember(String pkg)throws Exception;void forget(String pkg)throws Exception;}
    private final Shell shell;private final Journal journal;private final boolean supported;private final String fingerprint,apk;private boolean verified;
    SetupEngine(Shell shell,Journal journal,boolean supported,String fingerprint,String apk){this.shell=shell;this.journal=journal;this.supported=supported;this.fingerprint=fingerprint;this.apk=apk;}
    void verifyIdentity()throws Exception{
        verified=false;LocalAdb.Result identity=shell.run("id"),build=shell.run("getprop ro.build.fingerprint");
        if(identity.exit!=0||!identity.output.matches("(?s).*\\buid=(2000|0)\\b.*")||build.exit!=0||fingerprint.isEmpty()||!fingerprint.equals(build.output.trim()))throw new IOException("本机身份校验失败，未允许执行配置");
        verified=true;
    }
    SetupPolicy.State state(String pkg)throws Exception{
        requirePackage(pkg);LocalAdb.Result result=shell.run("dumpsys package "+SetupPolicy.quote(pkg));
        if(result.exit!=0)throw new IOException("读取包状态失败："+pkg);return SetupPolicy.parse(result.output);
    }
    String hide(String pkg)throws Exception{
        requireProfile();SetupPolicy.State before=state(pkg);
        if(!before.installed)throw new IOException("包未安装："+pkg);
        if(before.hidden)return pkg+"：原先已不可用，本向导未改动";
        if(before.enabled>1)return pkg+"：原先已停用，本向导未改动";
        journal.remember(pkg); // Persist recovery information before the state-changing command.
        LocalAdb.Result changed=shell.run("pm hide --user 0 "+SetupPolicy.quote(pkg));
        SetupPolicy.State after=state(pkg);
        if(!after.hidden){journal.forget(pkg);throw new IOException("配置未生效："+pkg+"；"+changed.output);}
        shell.run("am force-stop "+SetupPolicy.quote(pkg));
        return pkg+"：hidden=true，当前用户不可用；旧进程需重启后核验";
    }
    String takeover()throws Exception{
        requireProfile();SetupPolicy.State before=state(SetupPolicy.HOME);
        LocalAdb.Result own=shell.run("pm path cn.quiet.tv");
        if(own.exit!=0||!own.output.startsWith("package:"))throw new IOException("未确认新桌面已安装，保留原厂桌面");
        LocalAdb.Result preferred=shell.run(helper("home-set cn.quiet.tv/.MainActivity"));
        if(preferred.exit!=0||!preferred.output.contains("preferred-after=ComponentInfo{cn.quiet.tv/"))throw new IOException("无法设置新桌面首选项，保留原厂桌面");
        try{
            String result=hide(SetupPolicy.HOME);
            LocalAdb.Result resolved=shell.run(helper("home-resolve"));
            if(resolved.exit!=0||!resolved.output.contains("resolved=cn.quiet.tv/"))throw new IOException("HOME 解析未指向静境 TV");
            return result+"\nHOME 解析：静境 TV；请按 Home 并在正常重启后复查";
        }catch(Exception error){
            if(!before.hidden){
                try{restore(SetupPolicy.HOME);}catch(Exception rollback){throw new IOException(error.getMessage()+"；恢复未确认，请保留 ADB 通道后重试恢复",rollback);}
            }
            throw error;
        }
    }
    String restore(String pkg)throws Exception{
        requireProfile();requirePackage(pkg);
        shell.run("pm unhide --user 0 "+SetupPolicy.quote(pkg));
        SetupPolicy.State restored=state(pkg);
        if(!restored.installed||restored.hidden)throw new IOException("恢复未生效："+pkg);
        journal.forget(pkg);return pkg+"：已恢复包可用状态";
    }
    private String helper(String args){return "CLASSPATH="+SetupPolicy.quote(apk)+" app_process /system/bin cn.quiet.tools.TvAdmin "+args;}
    private void requireProfile()throws IOException{if(!supported)throw new IOException("当前型号/固件没有经过验证的修改规则，仅支持检测");if(!verified)throw new IOException("请先验证本机身份");}
    private void requirePackage(String pkg){if(!SetupPolicy.allowed(pkg))throw new IllegalArgumentException("包不在已验证规则中");}
}
