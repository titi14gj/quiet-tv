package android.content.pm;
import android.content.Intent;import java.util.List;
public abstract class PackageManager {public abstract List<ResolveInfo> queryIntentActivities(Intent intent,int flags);}
