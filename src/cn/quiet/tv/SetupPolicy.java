package cn.quiet.tv;

import java.util.regex.*;

/** Explicit device profile; an installed application name alone never authorizes a mutation. */
final class SetupPolicy {
    static final String HOME="com.helios.launcher",AD1="com.helios.ad.service",AD2="cn.whaley.adsys";
    static final String VERIFIED_FINGERPRINT="Helios/helios/helios:5.0.1/HELIOSR-02.08.03/1849508:user/release-keys";
    static boolean supported(String model,int sdk,String display,String fingerprint){return "65D2UT".equals(model)&&sdk==21&&display!=null&&display.startsWith("HELIOSR-02.08.03 ")&&VERIFIED_FINGERPRINT.equals(fingerprint);}
    static boolean allowed(String pkg){return HOME.equals(pkg)||AD1.equals(pkg)||AD2.equals(pkg);}
    static String quote(String value){return "'"+value.replace("'","'\\''")+"'";}
    static final class State {
        final boolean installed,hidden;final int enabled;
        State(boolean installed,boolean hidden,int enabled){this.installed=installed;this.hidden=hidden;this.enabled=enabled;}
    }
    static State parse(String dump){
        Matcher user=Pattern.compile("(?m)^\\s*User 0:\\s*([^\\n]+)").matcher(dump);
        if(!user.find())throw new IllegalArgumentException("未读到用户 0 的包状态");
        String row=user.group(1);Matcher installed=Pattern.compile("(?:^|\\s)installed=(true|false)(?:\\s|$)").matcher(row);
        Matcher hidden=Pattern.compile("(?:^|\\s)hidden=(true|false)(?:\\s|$)").matcher(row);
        Matcher enabled=Pattern.compile("(?:^|\\s)enabled=(\\d+)(?:\\s|$)").matcher(row);
        if(!installed.find()||!hidden.find()||!enabled.find())throw new IllegalArgumentException("固件包状态格式未适配");
        return new State(Boolean.parseBoolean(installed.group(1)),Boolean.parseBoolean(hidden.group(1)),Integer.parseInt(enabled.group(1)));
    }
}
