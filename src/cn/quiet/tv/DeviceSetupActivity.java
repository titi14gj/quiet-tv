package cn.quiet.tv;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** User-operated setup: reading information never implicitly runs an ADB command. */
public final class DeviceSetupActivity extends Activity {
    private final Handler ui=new Handler();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile LocalAdb client;
    private volatile boolean connected,destroyed;
    private boolean busy;
    private boolean supported;
    private TextView report,inventory,summary;
    private EditText port;
    private Button connectButton,applyButton,restoreButton;
    private CheckBox ads1,ads2,home;
    private SharedPreferences prefs;
    private final SetupEngine.Journal journal=new SetupEngine.Journal(){
        public void remember(String pkg)throws Exception{Set<String> entries=changed();entries.add(pkg);persist(entries);}
        public void forget(String pkg)throws Exception{Set<String> entries=changed();entries.remove(pkg);persist(entries);}
        private void persist(Set<String> entries)throws IOException{if(!prefs.edit().putStringSet("changed_packages",entries).commit())throw new IOException("未能保存恢复记录，停止本次操作");}
    };
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("device-setup",0);
        supported=SetupPolicy.supported(Build.MODEL,Build.VERSION.SDK_INT,Build.DISPLAY,Build.FINGERPRINT);
        getWindow().setFlags(1024,1024);getWindow().getDecorView().setSystemUiVisibility(4096|2|4);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff101522);scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(58),dp(30),dp(58),dp(40));scroll.addView(body);setContentView(scroll);
        body.addView(text("电视初始化与精简",28,Color.WHITE));
        body.addView(text("先检测 → 开启网络 ADB → 在系统弹窗授权 → 遥控选择并执行",16,0xffbdc9e0));
        summary=text("型号："+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\n固件："+Build.DISPLAY+"\n"+(supported?"匹配已验证规则：微鲸 65D2UT / HELIOSR-02.08.03":"此固件仅提供检测；没有自动精简规则"),16,0xffd7e4f6);summary.setPadding(0,dp(14),0,dp(14));body.addView(summary);
        Button list=button("查看预装应用清单",v->inventory.setVisibility(inventory.getVisibility()==View.GONE?View.VISIBLE:View.GONE));body.addView(list);
        inventory=text("正在读取预装应用…",13,0xffafbed5);inventory.setVisibility(View.GONE);body.addView(inventory);
        TextView help=text("本应用的确认框不能授予系统权限。请先在电视开发者选项开启网络 ADB；连接时若出现系统 RSA 授权框，用遥控器确认。未开放本机 TCP 端口的电视，需要先通过电脑/USB 开启。此版不支持无线调试配对码或 TLS。",15,0xffb8c8e4);help.setPadding(0,dp(16),0,dp(8));body.addView(help);
        LinearLayout address=new LinearLayout(this);address.setGravity(Gravity.CENTER_VERTICAL);body.addView(address);
        address.addView(text("本机 127.0.0.1  端口 ",16,Color.WHITE));
        port=new EditText(this);port.setText("5555");port.setTextColor(Color.WHITE);port.setTextSize(18);port.setInputType(InputType.TYPE_CLASS_NUMBER);port.setSingleLine(true);address.addView(port,new LinearLayout.LayoutParams(dp(130),dp(52)));
        connectButton=button("连接本机 ADB（系统确认）",v->connect());body.addView(connectButton);
        ads1=choice(body,"限制广告服务：com.helios.ad.service");ads2=choice(body,"限制广告组件：cn.whaley.adsys");home=choice(body,"接管 Home：停用原厂桌面对当前用户的可用性");
        body.addView(text("已适配的微鲸固件使用 pm hide，不删除 APK 或数据。已有不可用状态不会计入本向导的修改。后台进程是否退出、重启后是否生效仍需复查；不能据此保证所有广告消失。",14,0xffb8c8e4));
        applyButton=button("执行本次选择",v->confirmApply());body.addView(applyButton);
        restoreButton=button("恢复本向导的修改",v->confirmRestore());body.addView(restoreButton);
        Button refresh=button("重新检测包状态",v->job(()->{SetupEngine engine=engine();engine.verifyIdentity();return states(engine);}));body.addView(refresh);
        Button launcher=button("返回静境 TV",v->{startActivity(new Intent(this,MainActivity.class));finish();});body.addView(launcher);
        report=text(prefs.getString("last_report","等待检测。未连接、未修改系统。"),14,0xffdce8f8);report.setPadding(0,dp(18),0,0);body.addView(report);
        updateButtons();
        job(()->{
            ArrayList<String> rows=new ArrayList<>();int total=0;
            for(ApplicationInfo app:getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES)){
                total++;if((app.flags&ApplicationInfo.FLAG_SYSTEM)!=0)rows.add(getPackageManager().getApplicationLabel(app)+"  ·  "+app.packageName);
            }
            Collections.sort(rows);final int count=total;final String listText=join(rows);
            ui.post(()->{if(!destroyed){list.setText("查看预装应用清单（"+rows.size()+" 个系统应用 / 共 "+count+" 个包）");inventory.setText(listText);}});
            return "设备信息已读取。点击连接时才会访问本机 ADB；所有修改需要另行选择与确认。";
        });
    }
    private CheckBox choice(LinearLayout body,String title){CheckBox c=new CheckBox(this);c.setText(title);c.setTextColor(Color.WHITE);c.setTextSize(16);c.setPadding(dp(4),dp(10),dp(4),dp(10));c.setFocusable(true);c.setEnabled(supported);body.addView(c);return c;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String title,View.OnClickListener listener){
        Button b=new Button(this);b.setText(title);b.setTextSize(16);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);b.setPadding(dp(18),0,dp(18),0);b.setFocusable(true);
        StateListDrawable state=new StateListDrawable();state.addState(new int[]{android.R.attr.state_focused},shape(0xff4766a0));state.addState(new int[]{-android.R.attr.state_enabled},shape(0xff232b3a));state.addState(new int[]{},shape(0xff29364e));b.setBackground(state);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));lp.setMargins(0,dp(7),0,dp(7));b.setLayoutParams(lp);b.setOnClickListener(listener);return b;
    }
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));return d;}
    private void connect(){
        final int number;try{number=Integer.parseInt(port.getText().toString());if(number<1||number>65535)throw new NumberFormatException();}catch(Exception e){report.setText("请输入 1–65535 的本机端口");return;}
        job(()->{
            connected=false;LocalAdb previous=client;if(previous!=null)previous.close();
            AdbIdentity identity=new AdbIdentity(this);if(destroyed)throw new IOException("页面已关闭");LocalAdb connection=new LocalAdb(number);client=connection;if(destroyed){connection.close();throw new IOException("页面已关闭");}
            connection.connect(identity,()->ui.post(()->{if(!destroyed)report.setText("请在电视系统弹出的 ADB 授权框中，用遥控器确认；正在等待授权（最多约 60 秒）…");}));
            SetupEngine engine=engine();engine.verifyIdentity();connected=true;
            return "本机 ADB 已连接，设备身份匹配。"+(supported?"\n"+states(engine):"\n当前固件没有已验证的修改规则，因此保持只读检测。");
        });
    }
    private SetupEngine engine()throws IOException{LocalAdb connection=client;if(connection==null)throw new IOException("请先连接本机 ADB");return new SetupEngine(connection::shell,journal,supported,Build.FINGERPRINT,getApplicationInfo().sourceDir);}
    private String states(SetupEngine engine)throws Exception{
        StringBuilder result=new StringBuilder();
        for(String pkg:new String[]{SetupPolicy.HOME,SetupPolicy.AD1,SetupPolicy.AD2}){
            try{SetupPolicy.State state=engine.state(pkg);result.append(pkg).append("\ninstalled=").append(state.installed).append(" / hidden=").append(state.hidden).append(" / enabled=").append(state.enabled).append('\n');}
            catch(Exception e){result.append(pkg).append("：").append(message(e)).append('\n');}
        }
        return result.toString();
    }
    private void confirmApply(){
        final boolean first=ads1.isChecked(),second=ads2.isChecked(),takeover=home.isChecked();
        if(!first&&!second&&!takeover){report.setText("请先用遥控器选择要处理的项目");return;}
        String items=(first?SetupPolicy.AD1+"\n":"")+(second?SetupPolicy.AD2+"\n":"")+(takeover?"原厂桌面 → 静境 TV\n":"");
        new AlertDialog.Builder(this).setTitle("确认本次修改").setMessage(items+"\n只处理上述选择，保留原 APK 和数据；逐项读回，不自动重启。").setNegativeButton("取消",null).setPositiveButton("确认执行",(d,w)->job(()->{
            if(!supported)throw new IOException("未匹配已验证规则");SetupEngine engine=engine();engine.verifyIdentity();StringBuilder result=new StringBuilder();
            if(first)attempt(result,()->engine.hide(SetupPolicy.AD1));if(second)attempt(result,()->engine.hide(SetupPolicy.AD2));if(takeover)attempt(result,engine::takeover);
            return result+"\n请按 Home 测试，并在自行正常重启后重新检测。未验证的操作会明确保留失败/未知状态。";
        })).show();
    }
    private void confirmRestore(){
        final Set<String> entries=changed();if(entries.isEmpty()){report.setText("没有本向导的修改记录。通过电脑或其他工具设置的状态不会自动恢复。");return;}
        new AlertDialog.Builder(this).setTitle("恢复向导修改").setMessage(join(new ArrayList<>(entries))+"\n\n仅恢复向导执行前可用、由向导设为 hidden 的包。若上次结果未知，也会重新读回。").setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->job(()->{
            SetupEngine engine=engine();engine.verifyIdentity();StringBuilder result=new StringBuilder();
            for(String pkg:new String[]{SetupPolicy.HOME,SetupPolicy.AD1,SetupPolicy.AD2})if(entries.contains(pkg))attempt(result,()->engine.restore(pkg));return result.toString();
        })).show();
    }
    private Set<String> changed(){Set<String> result=new HashSet<>();for(String pkg:prefs.getStringSet("changed_packages",Collections.<String>emptySet()))if(SetupPolicy.allowed(pkg))result.add(pkg);return result;}
    private interface Job {String run()throws Exception;}
    private static void attempt(StringBuilder result,Job action){try{result.append(action.run()).append('\n');}catch(Exception e){result.append("失败或未确认：").append(message(e)).append('\n');}}
    private void job(Job action){
        if(busy)return;busy=true;report.setText("正在处理…");updateButtons();
        worker.execute(()->{
            String output;
            try{output=action.run();}catch(Exception e){connected=false;LocalAdb connection=client;if(connection!=null)connection.close();output="未完成："+message(e)+"\n若未开放网络 ADB，请先在电视开发者选项或通过电脑开启；仅在本应用点击确认不能授予系统权限。";}
            final String value=output;
            ui.post(()->{if(destroyed)return;busy=false;report.setText(value);prefs.edit().putString("last_report",value).apply();updateButtons();});
        });
    }
    private void updateButtons(){if(connectButton==null)return;connectButton.setEnabled(!busy);port.setEnabled(!busy);applyButton.setEnabled(!busy&&connected&&supported);restoreButton.setEnabled(!busy&&connected&&supported);ads1.setEnabled(!busy&&supported);ads2.setEnabled(!busy&&supported);home.setEnabled(!busy&&supported);}
    private static String join(List<String> values){StringBuilder out=new StringBuilder();for(String value:values)out.append(value).append('\n');return out.toString();}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    @Override protected void onDestroy(){destroyed=true;LocalAdb connection=client;if(connection!=null)connection.close();worker.shutdownNow();super.onDestroy();}
}
