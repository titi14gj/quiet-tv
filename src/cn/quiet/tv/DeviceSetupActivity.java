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
import java.io.*;
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
    private RegisteredProfile profile;
    private final List<CheckBox> communityChoices=new ArrayList<>();
    private Button exportButton,defaultButton;
    private static final int EXPORT_REPORT=17;
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
    private final CommunitySetupEngine.Journal communityJournal=new CommunitySetupEngine.Journal(){
        public void remember(String pkg,int previous)throws Exception{if(!prefs.edit().putInt("community."+profile.id+"."+pkg,previous).commit())throw new IOException("无法保存恢复记录");}
        public void forget(String pkg)throws Exception{if(!prefs.edit().remove("community."+profile.id+"."+pkg).commit())throw new IOException("无法更新恢复记录");}
    };
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);prefs=getSharedPreferences("device-setup",0);
        supported=SetupPolicy.supported(Build.MODEL,Build.VERSION.SDK_INT,Build.DISPLAY,Build.FINGERPRINT);
        profile=RegisteredProfile.match(Build.MODEL,Build.DEVICE,Build.VERSION.SDK_INT,Build.DISPLAY,Build.FINGERPRINT);
        getWindow().setFlags(1024,1024);getWindow().getDecorView().setSystemUiVisibility(4096|2|4);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xff101522);scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(58),dp(30),dp(58),dp(40));scroll.addView(body);setContentView(scroll);
        body.addView(text("电视初始化与精简",28,Color.WHITE));
        body.addView(text("先检测 → 开启网络 ADB → 在系统弹窗授权 → 遥控选择并执行",16,0xffbdc9e0));
        summary=text("型号："+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\n固件："+Build.DISPLAY+"\n"+(supported?"匹配已验证规则：微鲸 65D2UT / HELIOSR-02.08.03":profile!=null?"已匹配可执行规则："+profile.title:"未匹配可执行规则：可选择默认桌面，并导出诊断报告申请适配"),16,0xffd7e4f6);summary.setPadding(0,dp(14),0,dp(14));body.addView(summary);
        body.addView(button("查看上游社区机型参考（不等于全部可执行）",v->job(()->{
            final String content=SetupReport.references(this);ui.post(()->{if(!destroyed){TextView text=text(content,14,Color.WHITE);text.setPadding(dp(20),dp(10),dp(20),dp(10));ScrollView view=new ScrollView(this);view.addView(text);new AlertDialog.Builder(this).setTitle("CleanYourTV 社区记录").setView(view).setPositiveButton("关闭",null).show();}});return "参考列表已打开。只有匹配本 APK 内可执行规则的固件，才会开放对应精简选项。";
        })));
        exportButton=button("导出适配报告（未连接也可导出基础信息）",v->exportReport());body.addView(exportButton);
        body.addView(button("打开系统默认桌面设置",v->openDefaultSettings()));
        Button list=button("查看预装应用清单",v->inventory.setVisibility(inventory.getVisibility()==View.GONE?View.VISIBLE:View.GONE));body.addView(list);
        inventory=text("正在读取预装应用…",13,0xffafbed5);inventory.setVisibility(View.GONE);body.addView(inventory);
        TextView help=text("本应用的确认框不能授予系统权限。请先在电视开发者选项开启网络 ADB；连接时若出现系统 RSA 授权框，用遥控器确认。未开放本机 TCP 端口的电视，需要先通过电脑/USB 开启。此版不支持无线调试配对码或 TLS。",15,0xffb8c8e4);help.setPadding(0,dp(16),0,dp(8));body.addView(help);
        LinearLayout address=new LinearLayout(this);address.setGravity(Gravity.CENTER_VERTICAL);body.addView(address);
        address.addView(text("本机 127.0.0.1  端口 ",16,Color.WHITE));
        port=new EditText(this);port.setText("5555");port.setTextColor(Color.WHITE);port.setTextSize(18);port.setInputType(InputType.TYPE_CLASS_NUMBER);port.setSingleLine(true);address.addView(port,new LinearLayout.LayoutParams(dp(130),dp(52)));
        connectButton=button("连接本机 ADB（系统确认）",v->connect());body.addView(connectButton);
        ads1=choice(body,"限制广告服务：com.helios.ad.service");ads2=choice(body,"限制广告组件：cn.whaley.adsys");home=choice(body,"接管 Home：停用原厂桌面对当前用户的可用性");
        if(!supported){ads1.setVisibility(View.GONE);ads2.setVisibility(View.GONE);home.setVisibility(View.GONE);}
        if(supported)body.addView(text("已适配的微鲸固件使用 pm hide，不删除 APK 或数据。已有不可用状态不会计入本向导的修改。后台进程是否退出、重启后是否生效仍需复查；不能据此保证所有广告消失。",14,0xffb8c8e4));
        if(profile!=null){ads1.setVisibility(View.GONE);ads2.setVisibility(View.GONE);home.setVisibility(View.GONE);
            body.addView(text("社区参考精简子集（本项目未实机验证）：保留原厂桌面、设置、输入、遥控、Wi-Fi、投屏和更新服务；逐项选择。",14,0xffb8c8e4));
            for(int i=0;i<profile.packages.length;i++){CheckBox choice=choice(body,profile.labels[i]+"："+profile.packages[i]);communityChoices.add(choice);}
        }
        defaultButton=button("设置静境 TV 为默认桌面（保留原厂应用）",v->confirmDefault());body.addView(defaultButton);
        applyButton=button("执行本次选择",v->confirmApply());body.addView(applyButton);
        restoreButton=button("恢复本向导的修改",v->confirmRestore());body.addView(restoreButton);
        if(!supported&&profile==null){applyButton.setVisibility(View.GONE);restoreButton.setVisibility(View.GONE);}
        if(Build.VERSION.SDK_INT<24)defaultButton.setVisibility(View.GONE);
        Button refresh=button("重新检测包状态",v->job(()->{SetupEngine engine=engine();engine.verifyIdentity();if(profile!=null){CommunitySetupEngine community=communityEngine();community.verifyIdentity();return community.describe();}if(!supported)return "当前固件没有精简规则。可导出授权诊断报告，查看本机 HOME 和包状态。";return states(engine);}));body.addView(refresh);
        Button launcher=button("返回静境 TV",v->{startActivity(new Intent(this,MainActivity.class));finish();});body.addView(launcher);
        report=text(prefs.getString("last_report","等待检测。未连接、未修改系统。"),14,0xffdce8f8);report.setPadding(0,dp(18),0,0);body.addView(report);
        updateButtons();
        job(()->{
            ArrayList<String> rows=new ArrayList<>();int total=0;
            for(ApplicationInfo app:getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES)){
                total++;if((app.flags&ApplicationInfo.FLAG_SYSTEM)!=0)rows.add(getPackageManager().getApplicationLabel(app)+"  ·  "+app.packageName);
            }
            Collections.sort(rows);final int count=total;final String listText=join(rows);
            final org.json.JSONArray related=SetupReport.related(this);
            ui.post(()->{if(!destroyed){list.setText("查看预装应用清单（"+rows.size()+" 个系统应用 / 共 "+count+" 个包）");inventory.setText(listText);if(!supported&&profile==null&&related.length()>0)summary.append("\n社区有 "+related.length()+" 条可能相关的记录，但本固件未有可执行规则；导出报告会附上候选来源。");}});
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
            if(profile!=null){CommunitySetupEngine community=communityEngine();community.verifyIdentity();return "本机 ADB 已连接。匹配社区参考精简子集，尚待本项目实机验证。\n"+community.describe();}
            return "本机 ADB 已连接，设备身份匹配。"+(supported?"\n"+states(engine):"\n当前固件没有精简规则，保留系统包；可尝试默认桌面登记或导出报告。");
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
        if(profile!=null){confirmCommunity(false);return;}
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
        if(profile!=null){confirmCommunity(true);return;}
        final Set<String> entries=changed();if(entries.isEmpty()){report.setText("没有本向导的修改记录。通过电脑或其他工具设置的状态不会自动恢复。");return;}
        new AlertDialog.Builder(this).setTitle("恢复向导修改").setMessage(join(new ArrayList<>(entries))+"\n\n仅恢复向导执行前可用、由向导设为 hidden 的包。若上次结果未知，也会重新读回。").setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->job(()->{
            SetupEngine engine=engine();engine.verifyIdentity();StringBuilder result=new StringBuilder();
            for(String pkg:new String[]{SetupPolicy.HOME,SetupPolicy.AD1,SetupPolicy.AD2})if(entries.contains(pkg))attempt(result,()->engine.restore(pkg));return result.toString();
        })).show();
    }

    private CommunitySetupEngine communityEngine()throws IOException{LocalAdb connection=client;if(connection==null)throw new IOException("请先连接本机 ADB");return new CommunitySetupEngine(connection::shell,communityJournal,profile,Build.FINGERPRINT);}
    private void confirmCommunity(final boolean restore){
        final List<String> packages=new ArrayList<>();
        for(int i=0;i<profile.packages.length;i++){String pkg=profile.packages[i];if(restore?prefs.contains("community."+profile.id+"."+pkg):communityChoices.get(i).isChecked())packages.add(pkg);}
        if(packages.isEmpty()){report.setText(restore?"没有本向导的社区规则修改记录":"请先选择项目");return;}
        new AlertDialog.Builder(this).setTitle(restore?"恢复原状态":"执行社区参考规则").setMessage(join(packages)+"\n"+profile.title+"\n不卸载、不处理桌面；执行后逐项读回。来源：\n"+profile.source).setNegativeButton("取消",null).setPositiveButton("确认",(d,w)->job(()->{
            CommunitySetupEngine engine=communityEngine();engine.verifyIdentity();StringBuilder result=new StringBuilder();
            for(String pkg:packages)attempt(result,()->restore?engine.restore(pkg,prefs.getInt("community."+profile.id+"."+pkg,-1)):engine.disable(pkg));return result.toString();
        })).show();
    }
    private void openDefaultSettings(){
        try{startActivity(new Intent("android.settings.HOME_SETTINGS"));}
        catch(ActivityNotFoundException e){try{startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));}catch(ActivityNotFoundException fallback){report.setText("系统未提供默认桌面设置入口。可按 Home 查看是否出现桌面选择框；如被厂商锁定，请导出报告。");}}
    }
    private void confirmDefault(){
        new AlertDialog.Builder(this).setTitle("更改默认桌面").setMessage("登记静境 TV 为默认 HOME，保留原厂应用。厂商可能拒绝或在重启后重置；结果必须读回确认。可在系统默认桌面设置中改回。").setNegativeButton("取消",null).setPositiveButton("设置",(d,w)->job(()->{
            SetupEngine engine=engine();engine.verifyIdentity();LocalAdb connection=client;
            DefaultHomeEngine.select(connection::shell);
            return "HOME 已读回指向静境 TV。原厂应用保持可用。请按 Home 测试，正常重启后复查；失败时导出报告。";
        })).show();
    }
    private File reportFile(){return new File(getFilesDir(),"QuietTV-diagnostic.json");}
    private void exportReport(){
        final boolean includeAdb=connected;final String[] content={null};
        job(()->{
            SetupEngine.Shell shell=null;if(includeAdb){SetupEngine engine=engine();engine.verifyIdentity();LocalAdb connection=client;shell=connection::shell;}
            content[0]=SetupReport.create(this,shell);FileOutputStream output=new FileOutputStream(reportFile());try{output.write(content[0].getBytes("UTF-8"));}finally{output.close();}
            return "报告已生成（"+(includeAdb?"含授权诊断":"基础信息，未连接 ADB")+"），请选择保存位置。没有上传服务器。";
        },()->{
            Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"QuietTV-diagnostic.json");
            try{startActivityForResult(save,EXPORT_REPORT);}catch(ActivityNotFoundException e){shareReport(content[0]);}
        });
    }
    private void shareReport(String content){
        Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"QuietTV 适配报告").putExtra(Intent.EXTRA_TEXT,content);
        if(send.resolveActivity(getPackageManager())!=null){try{startActivity(Intent.createChooser(send,"发送适配报告"));return;}catch(ActivityNotFoundException ignored){}}
        File directory=getExternalFilesDir(null);if(directory==null){report.setText("系统没有保存或分享入口。报告已保存在应用私有目录；当前设备无法直接导出，请安装文件选择器后重试。");return;}
        final File file=new File(directory,"QuietTV-diagnostic.json");job(()->{FileOutputStream out=new FileOutputStream(file);try{out.write(content.getBytes("UTF-8"));}finally{out.close();}return "系统没有文件选择器或分享应用，报告已保存：\n"+file.getAbsolutePath()+"\n可尝试使用电视文件管理器复制到 U 盘；Android 新版本可能限制访问此目录。";});
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=EXPORT_REPORT)return;
        if(result!=RESULT_OK||data==null||data.getData()==null){report.setText("已取消导出，未发送报告。可重新点击导出。");return;}
        final android.net.Uri destination=data.getData();job(()->{InputStream in=new FileInputStream(reportFile());try{OutputStream out=getContentResolver().openOutputStream(destination);if(out==null)throw new IOException("无法打开保存位置");try{byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);}finally{out.close();}}finally{in.close();}return "诊断报告已保存到所选位置。请将 JSON 文件发给我们分析；导出没有改变系统包。";});
    }

    private Set<String> changed(){Set<String> result=new HashSet<>();for(String pkg:prefs.getStringSet("changed_packages",Collections.<String>emptySet()))if(SetupPolicy.allowed(pkg))result.add(pkg);return result;}
    private interface Job {String run()throws Exception;}
    private static void attempt(StringBuilder result,Job action){try{result.append(action.run()).append('\n');}catch(Exception e){result.append("失败或未确认：").append(message(e)).append('\n');}}
    private void job(Job action){job(action,null);}
    private void job(Job action,Runnable success){
        if(busy)return;busy=true;report.setText("正在处理…");updateButtons();
        worker.execute(()->{
            String output;boolean completed=false;
            try{output=action.run();completed=true;}catch(Exception e){connected=false;LocalAdb connection=client;if(connection!=null)connection.close();output="未完成："+message(e)+"\n若未开放网络 ADB，请先在电视开发者选项或通过电脑开启；仅在本应用点击确认不能授予系统权限。";}
            final String value=output;final boolean done=completed;
            ui.post(()->{if(destroyed)return;busy=false;report.setText(value);prefs.edit().putString("last_report",value).apply();updateButtons();if(done&&success!=null)success.run();});
        });
    }
    private void updateButtons(){if(connectButton==null)return;connectButton.setEnabled(!busy);port.setEnabled(!busy);applyButton.setEnabled(!busy&&connected&&(supported||profile!=null));restoreButton.setEnabled(!busy&&connected&&(supported||profile!=null));ads1.setEnabled(!busy&&supported);ads2.setEnabled(!busy&&supported);home.setEnabled(!busy&&supported);for(CheckBox c:communityChoices)c.setEnabled(!busy);exportButton.setEnabled(!busy);defaultButton.setEnabled(!busy&&connected&&Build.VERSION.SDK_INT>=24);}
    private static String join(List<String> values){StringBuilder out=new StringBuilder();for(String value:values)out.append(value).append('\n');return out.toString();}
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    @Override protected void onDestroy(){destroyed=true;LocalAdb connection=client;if(connection!=null)connection.close();worker.shutdownNow();super.onDestroy();}
}
