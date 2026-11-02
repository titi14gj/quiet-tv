package android.content;
public class Intent {
 public static final String ACTION_MAIN="main",CATEGORY_LEANBACK_LAUNCHER="tv",CATEGORY_LAUNCHER="phone";
 public static final int FLAG_ACTIVITY_NEW_TASK=0x10000000,FLAG_ACTIVITY_RESET_TASK_IF_NEEDED=0x200000;
 public String category,pkg;public ComponentName component;public int flags;
 public Intent(String action){}public Intent addCategory(String v){category=v;return this;}
 public Intent setPackage(String v){pkg=v;return this;}public Intent setComponent(ComponentName v){component=v;return this;}
 public Intent addFlags(int v){flags|=v;return this;}
}
