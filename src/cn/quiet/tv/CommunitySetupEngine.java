package cn.quiet.tv;
import java.io.IOException;

/** A small reviewed disable-user subset, never a remote script or a whole community list. */
final class CommunitySetupEngine {
    interface Journal {void remember(String pkg,int previous)throws Exception;void forget(String pkg)throws Exception;}
    private final SetupEngine.Shell shell;private final Journal journal;private final RegisteredProfile profile;
    private final String fingerprint;private boolean verified;
    CommunitySetupEngine(SetupEngine.Shell shell,Journal journal,RegisteredProfile profile,String fingerprint){this.shell=shell;this.journal=journal;this.profile=profile;this.fingerprint=fingerprint;}
    void verifyIdentity()throws Exception{
        verified=false;LocalAdb.Result id=shell.run("id"),fp=shell.run("getprop ro.build.fingerprint");
        if(id.exit!=0||!id.output.matches("(?s).*\\buid=(2000|0)\\b.*")||fp.exit!=0||!fingerprint.equals(fp.output.trim()))throw new IOException("ADB 本机身份未匹配");verified=true;
    }
    private void allowed(String pkg)throws IOException{if(!verified||profile==null||!profile.allows(pkg)||!RegisteredProfile.TCL_FP.equals(fingerprint))throw new IOException("项目不在当前固件可执行规则中");}
    private SetupPolicy.State state(String pkg)throws Exception{LocalAdb.Result result=shell.run("dumpsys package "+SetupPolicy.quote(pkg));if(result.exit!=0)throw new IOException("无法读取包状态");return SetupPolicy.parse(result.output);}
    String describe()throws Exception{StringBuilder out=new StringBuilder();for(String pkg:profile.packages){allowed(pkg);try{SetupPolicy.State s=state(pkg);out.append(pkg).append("：installed=").append(s.installed).append(" / hidden=").append(s.hidden).append(" / enabled=").append(s.enabled).append('\n');}catch(Exception e){out.append(pkg).append("：").append(e.getMessage()).append('\n');}}return out.toString();}
    String disable(String pkg)throws Exception{
        allowed(pkg);SetupPolicy.State before=state(pkg);
        if(!before.installed||before.hidden||before.enabled>1)return pkg+"：原先不可用/未安装，未改动";
        journal.remember(pkg,before.enabled);
        LocalAdb.Result changed=shell.run("pm disable-user --user 0 "+SetupPolicy.quote(pkg));SetupPolicy.State after=state(pkg);
        if(!after.installed||after.hidden||after.enabled!=3){if(after.installed&&!after.hidden&&after.enabled==before.enabled)journal.forget(pkg);throw new IOException(pkg+"：未确认 disabled-user；"+changed.output);}
        return pkg+"：enabled=3，当前用户已停用；重启持久性待复查";
    }
    String restore(String pkg,int previous)throws Exception{
        allowed(pkg);if(previous!=0&&previous!=1)throw new IOException("恢复记录无效");
        shell.run("pm "+(previous==0?"default-state":"enable")+" --user 0 "+SetupPolicy.quote(pkg));SetupPolicy.State after=state(pkg);
        if(!after.installed||after.hidden||after.enabled!=previous)throw new IOException(pkg+"：恢复未确认");journal.forget(pkg);return pkg+"：已恢复原 enabled="+previous;
    }
}
