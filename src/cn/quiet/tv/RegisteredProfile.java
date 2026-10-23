package cn.quiet.tv;

/** Executable rules are reviewed separately from community model references. */
final class RegisteredProfile {
    static final String TCL_FP="TCL/G08_4K_GB/G08:12/STT2.221228.001/AS34:user/release-keys";
    final String id,title,source;final String[] packages,labels;
    private RegisteredProfile(String id,String title,String source,String[] packages,String[] labels){this.id=id;this.title=title;this.source=source;this.packages=packages;this.labels=labels;}
    static RegisteredProfile match(String model,String device,int sdk,String display,String fingerprint){
        if("G08".equals(device)&&"Smart TV Pro".equals(model)&&sdk==31&&TCL_FP.equals(fingerprint))
            return new RegisteredProfile("tcl-g08-eu-android12","TCL G08 欧洲 Android 12 · 社区参考规则（本项目未实机验证）",
                "https://github.com/Gathin23/cleanyourtv/blob/5e9e4f119e405eb8bcc18296f2567c9d6a7c4217/examples/tcl-g08-eu-android12/debloat.txt",
                new String[]{"com.tcl.dashboard","com.tcl.messagebox","com.tcl.logkit"},
                new String[]{"TCL 首页推送","TCL 消息组件","TCL 日志采集"});
        return null;
    }
    boolean allows(String pkg){for(String p:packages)if(p.equals(pkg))return true;return false;}
}
