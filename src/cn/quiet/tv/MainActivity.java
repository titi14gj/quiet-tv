// Portions adapted from CleanYourTV, Copyright (c) 2026 Gathin, MIT. See licenses/.
package cn.quiet.tv;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.text.*;

public class MainActivity extends Activity {
    FrameLayout root;
    LinearLayout page,content,dockBand,gridGroup;
    View gridBottom;
    ScrollView scroller;
    ScrollView allScroller;
    LinearLayout allContent;
    FrameLayout allPage;
    GlassView glass;
    View focusedCard;
    Typeface light,medium;
    TextView greeting,inputBadge;
    Bitmap aurora,silk,coast,lake,glassBitmap;
    Bitmap glassSample(){if(glassBitmap==null)glassBitmap=GlassSurface.capture(backdrop);return glassBitmap;}
    PopupWindow openPopup;
    android.animation.ValueAnimator themeAnimator;
    Backdrop backdrop;
    TextView clock, hero, sub;
    int theme = 0xff658cff;
    String mode, selected, zone;
    boolean expanded, showSystem, allAppsOpen;
    final Handler handler = new Handler();
    final ArrayList<ResolveInfo> apps = new ArrayList<>();
    final ArrayList<ResolveInfo> dock = new ArrayList<>();
    final ArrayList<ResolveInfo> grid = new ArrayList<>();
    final ArrayList<ResolveInfo> allApps = new ArrayList<>();
    final ArrayList<View> allViews = new ArrayList<>();
    final ArrayList<View> topViews = new ArrayList<>(), dockViews = new ArrayList<>(), gridViews = new ArrayList<>();
    final HashMap<String,Integer> colors = new HashMap<>();
    final String[] defaults = {"com.xiaodianshi.tv.yst", "com.ktcp.video", "com.mgtv.tv", "com.qiyivideo.maoning"};
    int dockIndex, previewIndex, allIndex;

    int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    public void onCreate(Bundle b) {
        super.onCreate(b);
        try{light=Typeface.createFromAsset(getAssets(),"fonts/outfit-light.ttf");medium=Typeface.createFromAsset(getAssets(),"fonts/outfit-medium.ttf");}catch(Exception e){light=Typeface.create("sans-serif-light",0);medium=Typeface.create("sans-serif-medium",0);}
        try{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inPreferredConfig=Bitmap.Config.ARGB_8888;aurora=BitmapFactory.decodeStream(getAssets().open("wallpapers/aurora.jpg"),null,opts);}catch(Exception e){}

        try{BitmapFactory.Options opts=new BitmapFactory.Options();opts.inPreferredConfig=Bitmap.Config.ARGB_8888;silk=BitmapFactory.decodeStream(getAssets().open("wallpapers/mac-silk.jpg"),null,opts);coast=BitmapFactory.decodeStream(getAssets().open("wallpapers/mac-coast.jpg"),null,opts);}catch(Exception e){}
        try{lake=BitmapFactory.decodeStream(getAssets().open("wallpapers/mountain-lake.png"));}catch(Exception e){}
        mode = getPreferences(0).getString("wallpaper", "glow");
        if(!getPreferences(0).getBoolean("v5wallpaper",false)){mode="lake";getPreferences(0).edit().putString("wallpaper",mode).putBoolean("v5wallpaper",true).apply();}
        showSystem = getPreferences(0).getBoolean("showSystem", false);
        selected = getPreferences(0).getString("selected", defaults[0]);
        zone = getPreferences(0).getString("zone", "dock");
        expanded = getPreferences(0).getBoolean("expanded", false);
        allAppsOpen=getPreferences(0).getBoolean("allAppsOpen",false);
        previewIndex=getPreferences(0).getInt("previewIndex",0);
        allIndex=getPreferences(0).getInt("allIndex",0);
        getWindow().setFlags(1024,1024);
        getWindow().getDecorView().setSystemUiVisibility(4096|2|4);
        handler.post(tick);
    }
    protected void onResume() { super.onResume(); discover(); build(); }
    protected void onPause() { saveFocus();if(openPopup!=null)openPopup.dismiss(); super.onPause(); }
    protected void onDestroy() { handler.removeCallbacks(tick); super.onDestroy(); }
    final Runnable tick = new Runnable() {
        public void run() {
            if(clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date()));
            handler.postDelayed(this,30000);
        }
    };
    void saveFocus() {
        getPreferences(0).edit().putString("selected",selected).putString("zone",zone)
            .putBoolean("expanded",expanded).putBoolean("showSystem",showSystem)
            .putBoolean("allAppsOpen",allAppsOpen).putInt("previewIndex",previewIndex).putInt("allIndex",allIndex).apply();
    }
    String pkg(ResolveInfo r) { return r.activityInfo.packageName; }
    void discover() {
        apps.clear(); dock.clear(); grid.clear();allApps.clear();
        HashMap<String,ResolveInfo> found = new HashMap<>();
        // TV entry points take priority over phone-style activities in the same package.
        for(String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for(ResolveInfo r : getPackageManager().queryIntentActivities(i,0)) {
                String p = pkg(r);
                if(!p.equals(getPackageName()) && !p.equals("com.helios.launcher")
                    && !p.equals("com.android.packageinstaller") && !p.equals("com.helios.task")
                    && !found.containsKey(p)) found.put(p,r);
            }
        }
        for(int n=0;n<4;n++) {
            String p = getPreferences(0).getString("dock"+n,defaults[n]);
            if(found.containsKey(p) && !dock.contains(found.get(p))) dock.add(found.get(p));
        }
        ArrayList<ResolveInfo> sorted = new ArrayList<>(found.values());
        Collections.sort(sorted,(a,b)->a.loadLabel(getPackageManager()).toString().compareTo(b.loadLabel(getPackageManager()).toString()));
        allApps.addAll(sorted);
        for(ResolveInfo r:sorted) {
            String p=pkg(r);
            if(showSystem || (r.activityInfo.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)==0
                || p.equals("com.helios.mediaplayer") || p.equals("com.helios.screen")
                || p.equals("com.whaley.tv.tvplayer.ui") || dock.contains(r)) apps.add(r);
        }
        for(ResolveInfo r:apps) if(dock.size()<4&&!dock.contains(r)) dock.add(r);
        for(ResolveInfo r:apps) if(!dock.contains(r)&&grid.size()<4) grid.add(r);
    }
    TextView text(String s,int size,int color) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(s.matches(".*[一-鿿].*")?Typeface.create("sans-serif-medium",0):medium);t.setIncludeFontPadding(false);return t;
    }
    GradientDrawable bg(int color,int stroke) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    // Layout and visual language adapted from CleanYourTV (Gathin, MIT).
    // Native views and property animations keep the port compatible with API 21.
    void build() {
        final String wantedPackage=selected, wantedZone=zone;
        final boolean wantedExpanded=expanded;
        final boolean wantedAll=allAppsOpen;
        topViews.clear();dockViews.clear();gridViews.clear();allViews.clear();
        focusedCard=null;
        root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);root.setClipChildren(false);root.setClipToPadding(false);
        backdrop=new Backdrop(null);root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams headerLp=new FrameLayout.LayoutParams(-1,dp(94));
        headerLp.setMargins(dp(60),dp(26),dp(70),0);root.addView(header,headerLp);
        LinearLayout left=new LinearLayout(this);left.setOrientation(1);
        header.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        greeting=text(greetingText(),12,0x99ffffff);greeting.setLetterSpacing(0);
        left.addView(greeting);
        LinearLayout timeRow=new LinearLayout(this);timeRow.setGravity(Gravity.CENTER_VERTICAL);left.addView(timeRow);
        clock=text(new SimpleDateFormat("HH:mm",Locale.CHINA).format(new Date()),52,Color.WHITE);
        clock.setTypeface(light);clock.setLetterSpacing(-.04f);clock.setIncludeFontPadding(false);timeRow.addView(clock);
        View divider=new View(this);divider.setBackgroundColor(0x40ffffff);
        LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(1),dp(29));dl.setMargins(dp(20),dp(8),dp(18),0);timeRow.addView(divider,dl);
        TextView date=text(new SimpleDateFormat("M月d日  EEEE",Locale.CHINA).format(new Date()),14,0xb3ffffff);
        date.setSingleLine(true);timeRow.addView(date);
        LinearLayout pills=new LinearLayout(this);pills.setGravity(Gravity.CENTER_VERTICAL);pills.setClipChildren(false);pills.setClipToPadding(false);
        // Keep focus growth and the optical rim inside the measured header bounds.
        pills.setPadding(dp(10),dp(10),dp(10),dp(10));pills.setTranslationY(-dp(10));header.setClipChildren(false);header.setClipToPadding(false);header.addView(pills);
        addTop(pills,getPreferences(0).getString("inputBadge","信号源"),0,()->showInputs());
        addTop(pills,networkName(),networkIcon(),()->showNetwork());
        addTop(pills,"设置",R.drawable.ic_settings,()->showSettings());

        hero=text("",48,Color.WHITE);hero.setVisibility(View.GONE);

        scroller=new ScrollView(this);scroller.setFillViewport(false);scroller.setVerticalScrollBarEnabled(false);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);scroller.setClipToPadding(false);
        FrameLayout.LayoutParams sl=new FrameLayout.LayoutParams(-1,-1);sl.setMargins(dp(56),dp(144),dp(56),0);root.addView(scroller,sl);
        content=new LinearLayout(this);content.setOrientation(1);content.setClipChildren(false);content.setClipToPadding(false);
        scroller.addView(content,new ScrollView.LayoutParams(-1,-2));
        dockBand=new LinearLayout(this);dockBand.setGravity(Gravity.CENTER_VERTICAL);dockBand.setClipChildren(false);dockBand.setClipToPadding(false);
        dockBand.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams bandLp=new LinearLayout.LayoutParams(-1,dp(200));bandLp.setMargins(dp(10),dp(10),dp(10),dp(24));content.addView(dockBand,bandLp);
        for(int n=0;n<dock.size();n++) {
            View wrap=tile(dock.get(n),"dock",n);dockViews.add((View)wrap.getTag());
            dockBand.addView(wrap,new LinearLayout.LayoutParams(0,dp(200),1));
        }
        gridGroup=new LinearLayout(this);gridGroup.setOrientation(1);gridGroup.setClipChildren(false);
        content.addView(gridGroup,new LinearLayout.LayoutParams(-1,-2));gridGroup.setVisibility(wantedExpanded?View.VISIBLE:View.GONE);
        LinearLayout row=null;
        for(int n=0;n<grid.size();n++) {
            if(n%4==0){row=new LinearLayout(this);row.setClipChildren(false);gridGroup.addView(row,new LinearLayout.LayoutParams(-1,dp(200)));}
            View wrap=tile(grid.get(n),"grid",n);gridViews.add((View)wrap.getTag());row.addView(wrap,new LinearLayout.LayoutParams(0,dp(200),1));
        }
        if(row!=null&&grid.size()%4!=0)for(int n=grid.size()%4;n<4;n++)row.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
        TextView allHint=text("向下打开所有应用",14,0xbfffffff);allHint.setGravity(Gravity.CENTER);allHint.setPadding(0,dp(14),0,0);
        gridGroup.addView(allHint,new LinearLayout.LayoutParams(-1,dp(62)));
        allHint.setOnClickListener(v->openAllApps());
        gridBottom=new Space(this);content.addView(gridBottom,new LinearLayout.LayoutParams(-1,dp(230)));gridBottom.setVisibility(wantedExpanded?View.VISIBLE:View.GONE);
        buildAllAppsPage();
        glass=new GlassView(this,null);glass.setAlpha(0);glass.setFocusable(false);glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(glass,new FrameLayout.LayoutParams(dp(10),dp(10)));
        scroller.getViewTreeObserver().addOnScrollChangedListener(()->moveGlass(false));
        scroller.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,orr,ob)->{
            // Anchor the home row near the bottom; reserve space for focus growth and labels.
            int padding=Math.max(dp(20),b-t-dp(200+24+56));
            if(content.getPaddingTop()!=padding){content.setPadding(0,padding,0,0);}
            hero.setY(dp(144)+padding-dp(84));
        });
        if(glassBitmap!=null){glassBitmap.recycle();glassBitmap=null;}
        setContentView(root);
        selected=wantedPackage;zone=wantedZone;
        root.post(()->{selected=wantedPackage;zone=wantedZone;expanded=wantedExpanded;allAppsOpen=wantedAll;gridGroup.setVisibility(expanded?View.VISIBLE:View.GONE);gridBottom.setVisibility(expanded?View.VISIBLE:View.GONE);allPage.setVisibility(wantedAll?View.VISIBLE:View.GONE);scroller.setVisibility(wantedAll?View.GONE:View.VISIBLE);restoreFocus();refreshInputBadge();root.post(()->{moveGlass(false);for(View control:topViews)control.invalidate();});});
    }
    void buildAllAppsPage(){
        allPage=new FrameLayout(this);allPage.setVisibility(View.GONE);allPage.setClipChildren(true);
        FrameLayout.LayoutParams pageLp=new FrameLayout.LayoutParams(-1,-1);pageLp.topMargin=dp(140);root.addView(allPage,pageLp);
        allPage.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x10080d1d,0xb5080d1d}));
        TextView title=text("所有应用",29,Color.WHITE);
        FrameLayout.LayoutParams titleLp=new FrameLayout.LayoutParams(-2,dp(48));titleLp.leftMargin=dp(70);allPage.addView(title,titleLp);
        TextView detail=text(allApps.size()+" 个应用  ·  返回回到快捷应用",14,0xbfffffff);detail.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        FrameLayout.LayoutParams detailLp=new FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP|Gravity.RIGHT);detailLp.rightMargin=dp(70);allPage.addView(detail,detailLp);
        allScroller=new ScrollView(this);allScroller.setVerticalScrollBarEnabled(false);allScroller.setOverScrollMode(View.OVER_SCROLL_NEVER);allScroller.setClipToPadding(true);allScroller.setClipChildren(true);
        FrameLayout.LayoutParams listLp=new FrameLayout.LayoutParams(-1,-1);listLp.setMargins(dp(56),dp(58),dp(56),dp(24));allPage.addView(allScroller,listLp);
        allContent=new LinearLayout(this);allContent.setOrientation(1);allContent.setClipChildren(false);allContent.setClipToPadding(false);allContent.setPadding(dp(10),dp(20),dp(10),dp(60));allScroller.addView(allContent,new ScrollView.LayoutParams(-1,-2));
        LinearLayout row=null;
        for(int n=0;n<allApps.size();n++){
            if(n%4==0){row=new LinearLayout(this);row.setClipChildren(false);row.setClipToPadding(false);allContent.addView(row,new LinearLayout.LayoutParams(-1,dp(220)));}
            View wrap=tile(allApps.get(n),"all",n);allViews.add((View)wrap.getTag());row.addView(wrap,new LinearLayout.LayoutParams(0,dp(220),1));
        }
        if(row!=null&&allApps.size()%4!=0)for(int n=allApps.size()%4;n<4;n++)row.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));
    }
    void openAllApps(){
        if(allAppsOpen||allViews.isEmpty())return;
        allAppsOpen=true;scroller.setVisibility(View.GONE);allPage.setVisibility(View.VISIBLE);
        allPage.setAlpha(0);allPage.setTranslationY(dp(18));allPage.animate().alpha(1).translationY(0).setDuration(230).start();
        allPage.post(()->{focusAt(allViews,allIndex);saveFocus();});
    }
    void closeAllApps(){
        allAppsOpen=false;allPage.animate().cancel();allPage.setVisibility(View.GONE);scroller.setVisibility(View.VISIBLE);
        expanded=true;gridGroup.setVisibility(View.VISIBLE);gridBottom.setVisibility(View.VISIBLE);
        if(!gridViews.isEmpty())focusAt(gridViews,previewIndex);else focusAt(dockViews,dockIndex);saveFocus();
    }
    String greetingText(){int h=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);return h<5?"夜深了，慢慢看。":h<12?"早上好，开启新的一天。":h<18?"下午好，片刻属于自己。":"晚上好，享受你的时间。";}
    GradientDrawable rounded(int start,int end,int stroke,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{start,end});d.setCornerRadius(dp(radius));
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    void addTop(LinearLayout row,String label,int iconId,Runnable action) {
        final int index=topViews.size();final FrameLayout control=new FrameLayout(this);
        control.setFocusable(true);control.setClickable(true);control.setId(View.generateViewId());control.setContentDescription(index==0?"信号源："+label:label);
        if(index==0){
            TextView caption=text(label,14,Color.WHITE);caption.setGravity(Gravity.CENTER);caption.setSingleLine(true);caption.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.setMaxWidth(dp(260));caption.setPadding(dp(20),0,dp(20),0);
            control.addView(caption,new FrameLayout.LayoutParams(-2,-1));inputBadge=caption;
        }else{
            ImageView image=new ImageView(this);image.setImageResource(iconId);image.setColorFilter(Color.WHITE);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            control.addView(image,new FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
        }
        control.setBackground(new GlassSurface(this,control,false,23));
        control.setOnFocusChangeListener((v,f)->{v.setBackground(new GlassSurface(this,v,f,23));v.animate().scaleX(f?1.055f:1).scaleY(f?1.055f:1).setDuration(180).start();if(f){zone="top";glass.setAlpha(0);saveFocus();}});
        control.setOnClickListener(v->action.run());control.setOnKeyListener((v,key,e)->{
            if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==21){topViews.get(Math.max(0,index-1)).requestFocus();return true;}
            if(key==22){topViews.get(Math.min(topViews.size()-1,index+1)).requestFocus();return true;}
            if(key==19)return true;if(key==20){if(allAppsOpen)focusAt(allViews,allIndex);else focusAt(dockViews,dockIndex);return true;}return false;
        });
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(index==0?-2:dp(46),dp(46));lp.leftMargin=dp(14);row.addView(control,lp);topViews.add(control);
    }
    void refreshInputBadge(){
        final TextView caption=inputBadge;final View anchor=topViews.get(0);
        new Thread(()->{WhaleyInputs.Snapshot state=WhaleyInputs.read();int port=state.previous-4;
            String value=port>=0&&port<3?state.hdmiLabel(port).replace(" — "," · "):"信号源";
            handler.post(()->{if(caption!=inputBadge||isFinishing())return;caption.setText(value);anchor.setContentDescription("信号源："+value);getPreferences(0).edit().putString("inputBadge",value).apply();});
        },"HDMI-badge").start();
    }
    View tile(final ResolveInfo r,final String area,final int index) {
        FrameLayout wrap=new FrameLayout(this);wrap.setClipChildren(false);wrap.setClipToPadding(false);
        FrameLayout card=new FrameLayout(this);card.setFocusable(true);card.setClickable(true);card.setId(View.generateViewId());
        card.setBackground(rounded(0xff25252b,0xff1a1a20,0,24));card.setClipToOutline(true);
        FrameLayout.LayoutParams cl=new FrameLayout.LayoutParams(dp(232),dp(145),Gravity.TOP|Gravity.CENTER_HORIZONTAL);cl.topMargin=dp(14);wrap.addView(card,cl);
        card.setBackground(rounded(0xff25252b,0xff1a1a20,0,24));
        final TvIcon artwork=new TvIcon(this,pkg(r),r.loadLabel(getPackageManager()).toString());card.addView(artwork,new FrameLayout.LayoutParams(-1,-1));
        NativeArtwork.Art known=NativeArtwork.catalog.get(pkg(r));String title=known!=null?known.name:pkg(r).contains("xiaodianshi")?"哔哩哔哩":pkg(r).contains("ktcp")?"腾讯视频":pkg(r).contains("qiyi")?"爱奇艺":r.loadLabel(getPackageManager()).toString();
        TextView name=text(title,18,0xfff2f2f5);name.setSingleLine(true);name.setGravity(Gravity.CENTER);name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams nl=new FrameLayout.LayoutParams(dp(260),dp(34),Gravity.TOP|Gravity.CENTER_HORIZONTAL);nl.topMargin=dp(178);wrap.addView(name,nl);
        card.setOnFocusChangeListener((v,f)->{
            artwork.setFocusedArtwork(f);v.setElevation(f?dp(14):dp(3));v.setRotationY(f?-1.2f:0);
            v.animate().scaleX(f?1.09f:1).scaleY(f?1.09f:1).rotationY(0).setDuration(f?260:180)
                .setInterpolator(new android.view.animation.OvershootInterpolator(f?1.2f:0)).start();
            name.animate().alpha(f?1:.78f).setDuration(180).start();
            if(f){selected=pkg(r);zone=area;focusedCard=v;
                if(area.equals("dock")){dockIndex=index;collapseGrid();}
                else if(area.equals("grid")){previewIndex=index;expanded=true;gridGroup.setVisibility(View.VISIBLE);gridBottom.setVisibility(View.VISIBLE);}
                else allIndex=index;
                if(!colors.containsKey(selected))colors.put(selected,TvIcon.color(pkg(r)));animateTheme(colors.get(selected));
                root.post(()->{if(!area.equals("dock"))ensureVisible(v);moveGlass(true);});saveFocus();
            }
        });
        card.setOnClickListener(v->launch(r));card.setOnLongClickListener(v->{editDock(r);return true;});
        final boolean[] keyHeld={false},longHandled={false};
        final Runnable holdAction=()->{if(keyHeld[0]&&card.hasFocus()){longHandled[0]=true;editDock(r);}};
        card.setOnKeyListener((v,key,e)->{
            if(key==23||key==66||key==160){
                if(e.getAction()==KeyEvent.ACTION_DOWN){if(e.getRepeatCount()==0){keyHeld[0]=true;longHandled[0]=false;handler.postDelayed(holdAction,550);}
                    else if(!longHandled[0]&&keyHeld[0]){handler.removeCallbacks(holdAction);longHandled[0]=true;editDock(r);}}
                else if(e.getAction()==KeyEvent.ACTION_UP){handler.removeCallbacks(holdAction);boolean open=keyHeld[0]&&!longHandled[0]&&!e.isCanceled();keyHeld[0]=false;if(open)launch(r);}return true;
            }
            if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
            ArrayList<View> list=area.equals("dock")?dockViews:area.equals("all")?allViews:gridViews;
            if(key==21||key==22){int col=area.equals("dock")?index:index%4;int step=key==21?-1:1;
                if((step<0&&col>0)||(step>0&&col<(area.equals("dock")?list.size()-1:3)&&index+1<list.size()))focusAt(list,index+step);return true;}
            if(key==19){if(area.equals("dock"))topViews.get(2).requestFocus();else if(area.equals("all")){if(index<4)closeAllApps();else focusAt(allViews,index-4);}else focusAt(dockViews,Math.min(index,dock.size()-1));return true;}
            if(key==20){if(area.equals("dock")){dockIndex=index;if(!gridViews.isEmpty()){gridGroup.setVisibility(View.VISIBLE);gridBottom.setVisibility(View.VISIBLE);expanded=true;root.post(()->focusAt(gridViews,Math.min(index,gridViews.size()-1)));}}
                else if(area.equals("grid"))openAllApps();
                else if(index+4<allViews.size())focusAt(allViews,index+4);else if(index/4<(allViews.size()-1)/4)focusAt(allViews,allViews.size()-1);return true;}return false;
        });wrap.setTag(card);return wrap;
    }
    void ensureVisible(View card){
        ScrollView viewport=allAppsOpen?allScroller:scroller;LinearLayout body=allAppsOpen?allContent:content;
        Rect rect=new Rect(0,0,card.getWidth(),card.getHeight());body.offsetDescendantRectToMyCoords(card,rect);
        int scroll=viewport.getScrollY(),top=scroll+dp(24),bottom=scroll+viewport.getHeight()-dp(48);
        int below=allAppsOpen?54:110;
        if(rect.top<top)viewport.smoothScrollTo(0,Math.max(0,rect.top-dp(24)));
        else if(rect.bottom+dp(below)>bottom)viewport.smoothScrollTo(0,Math.max(0,rect.bottom+dp(below)-viewport.getHeight()+dp(48)));
    }
    void moveGlass(boolean animate){
        if(glass!=null)glass.setAlpha(0);
    }

    Drawable loadBanner(ResolveInfo r){
        try{Drawable d=r.activityInfo.loadBanner(getPackageManager());if(d==null)d=r.activityInfo.applicationInfo.loadBanner(getPackageManager());
            if(d!=null&&d.getIntrinsicWidth()<d.getIntrinsicHeight()*1.35f)return null;return d;}catch(Exception e){return null;}
    }
    void animateTheme(int color){
        if(themeAnimator!=null)themeAnimator.cancel();themeAnimator=android.animation.ValueAnimator.ofObject(new android.animation.ArgbEvaluator(),theme,color);
        themeAnimator.setDuration(450);themeAnimator.addUpdateListener(a->{theme=(Integer)a.getAnimatedValue();backdrop.invalidate();});themeAnimator.start();
    }
    void focusAt(ArrayList<View> list,int index) {if(!list.isEmpty())list.get(Math.max(0,Math.min(index,list.size()-1))).requestFocus();}
    void restoreFocus() {
        ArrayList<ResolveInfo> data=allAppsOpen?allApps:zone.equals("grid")&&expanded?grid:dock;
        ArrayList<View> views=allAppsOpen?allViews:zone.equals("grid")&&expanded?gridViews:dockViews;
        for(int n=0;n<data.size();n++)if(pkg(data.get(n)).equals(selected)){focusAt(views,n);return;}
        if(allAppsOpen&&!allViews.isEmpty())focusAt(allViews,allIndex);else {focusAt(dockViews,0);if(dockViews.isEmpty())topViews.get(0).requestFocus();}
    }
    void launch(ResolveInfo r) {
        saveFocus();try{startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(pkg(r),r.activityInfo.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(Exception e){Toast.makeText(this,"暂时无法打开该应用",0).show();}
    }
    void editDock(final ResolveInfo r) {
        final Dialog dialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(32),dp(28),dp(32),dp(28));panel.setClipChildren(false);
        panel.setBackground(rounded(0xff30323d,0xff14151d,0x55ffffff,26));
        panel.addView(text("调整常用应用",27,Color.WHITE));
        TextView subtitle=text("将「"+r.loadLabel(getPackageManager())+"」放到以下位置；已有应用会交换位置。",15,0xffb2b8ca);subtitle.setPadding(0,dp(10),0,dp(28));panel.addView(subtitle);
        LinearLayout row=new LinearLayout(this);row.setClipChildren(false);panel.addView(row);
        for(int n=0;n<4;n++){
            final int slot=n;LinearLayout option=new LinearLayout(this);option.setOrientation(1);option.setPadding(dp(7),dp(7),dp(7),dp(12));option.setFocusable(true);
            option.setBackground(rounded(0x102d303e,0x102d303e,0,16));
            FrameLayout preview=new FrameLayout(this);preview.setBackground(bg(0xff242630,0));preview.setClipToOutline(true);option.addView(preview,new LinearLayout.LayoutParams(-1,dp(112)));
            if(n<dock.size()){
                ResolveInfo app=dock.get(n);FrameLayout.LayoutParams artLp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);
                preview.addView(new TvIcon(this,pkg(app),app.loadLabel(getPackageManager()).toString()),artLp);
            }
            TextView name=text("第 "+(n+1)+" 位",15,Color.WHITE);name.setGravity(17);name.setPadding(0,dp(14),0,0);option.addView(name);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(5),0,dp(5),0);row.addView(option,lp);
            option.setOnFocusChangeListener((v,f)->{v.setBackground(rounded(f?0x38ffffff:0x102d303e,f?0x14ffffff:0x102d303e,f?0xb3ffffff:0,16));v.animate().scaleX(f?1.025f:1).scaleY(f?1.025f:1).setDuration(180).start();});
            option.setOnClickListener(v->{
                String[] order=new String[4];int old=-1;
                for(int k=0;k<4;k++){order[k]=k<dock.size()?pkg(dock.get(k)):"";if(order[k].equals(pkg(r)))old=k;}
                if(old>=0)order[old]=order[slot];order[slot]=pkg(r);
                android.content.SharedPreferences.Editor edit=getPreferences(0).edit();for(int k=0;k<4;k++)edit.putString("dock"+k,order[k]);edit.apply();
                dialog.dismiss();selected=pkg(r);zone="dock";expanded=false;allAppsOpen=false;discover();build();
            });
        }
        final boolean[] awaitingRelease={true};dialog.setOnKeyListener((d,key,event)->{
            if(key!=23&&key!=66&&key!=160)return false;
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)awaitingRelease[0]=false;
            if(awaitingRelease[0]){if(event.getAction()==KeyEvent.ACTION_UP)awaitingRelease[0]=false;return true;}return false;
        });
        dialog.setContentView(panel);Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.65f;window.setAttributes(attrs);dialog.show();window.setLayout(dp(1100),-2);row.getChildAt(0).requestFocus();
    }
    public void onBackPressed(){if(allAppsOpen){closeAllApps();return;}collapseGrid();zone="dock";focusAt(dockViews,dockIndex);saveFocus();}
    void collapseGrid(){expanded=false;if(gridGroup!=null)gridGroup.setVisibility(View.GONE);if(gridBottom!=null)gridBottom.setVisibility(View.GONE);if(scroller!=null)scroller.smoothScrollTo(0,0);}
    public boolean onKeyDown(int key,KeyEvent e){if(key==82){wallpapers();return true;}return super.onKeyDown(key,e);}
 int iconColor(ResolveInfo r){try{Drawable source=loadBanner(r);if(source==null)source=r.loadIcon(getPackageManager());Drawable d=source.getConstantState()!=null?source.getConstantState().newDrawable().mutate():source;Rect old=new Rect(d.getBounds());Bitmap b=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);d.setBounds(0,0,24,24);d.draw(c);d.setBounds(old);float[] hsv=new float[3];int rr=0,gg=0,bb=0,n=0;for(int y=0;y<24;y++)for(int x=0;x<24;x++){int v=b.getPixel(x,y);Color.colorToHSV(v,hsv);if(Color.alpha(v)>180&&hsv[1]>.35f&&hsv[2]>.3f){rr+=Color.red(v);gg+=Color.green(v);bb+=Color.blue(v);n++;}}b.recycle();if(n>0)return Color.rgb(rr/n,gg/n,bb/n);}catch(Exception e){}return colorFor(r.activityInfo.packageName);}
 int colorFor(String p){if(p.contains("ktcp"))return 0xff33a2ff;if(p.contains("qiyi"))return 0xff32d386;if(p.contains("xiaodian"))return 0xfff476a9;if(p.contains("mgtv"))return 0xffff922e;return Color.HSVToColor(new float[]{(p.hashCode()&0x7fffffff)%360,.65f,.9f});}
 void settings(){try{startActivity(new Intent().setComponent(new ComponentName("com.helios.setting","com.helios.activity.StartActivity")));}catch(Exception e){try{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));}catch(Exception ex){Toast.makeText(this,"系统设置不可用",0).show();}}}
    String networkName(){
        try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            android.net.NetworkInfo n=cm.getActiveNetworkInfo();if(n==null||!n.isConnected())return "未连接";
            if(n.getType()==android.net.ConnectivityManager.TYPE_ETHERNET)return "以太网";
            if(n.getType()==android.net.ConnectivityManager.TYPE_WIFI){android.net.wifi.WifiManager wm=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                String s=wm.getConnectionInfo().getSSID();if(s!=null&&!s.contains("unknown"))return s.replace("\"","");return "Wi-Fi";}return "已连接";
        }catch(Exception e){return "网络";}
    }
    int networkIcon(){try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        android.net.NetworkInfo n=cm.getActiveNetworkInfo();if(n!=null&&n.getType()==android.net.ConnectivityManager.TYPE_ETHERNET)return R.drawable.ic_ethernet;
    }catch(Exception e){}return R.drawable.ic_wifi;}
    void showNetwork(){
        popup(topViews.get(1),new String[]{networkName(),"网络设置"},new Runnable[]{()->openNetwork(),()->openNetwork()});
    }
    void openNetwork(){
        try{startActivity(new Intent().setComponent(new ComponentName("com.helios.setting","com.helios.activity.networkSetting.NetworkSettingActivity")));}
        catch(Exception e){settings();}
    }
    void showSettings(){
        popup(topViews.get(2),new String[]{"壁纸与外观",showSystem?"应用范围：全部":"应用范围：精简","电视系统设置","关于与开源许可"},new Runnable[]{
            ()->wallpapers(),()->{showSystem=!showSystem;saveFocus();discover();build();},()->settings(),()->about()});
    }
    void popup(View anchor,String[] labels,Runnable[] actions){popup(anchor,labels,actions,245);}
    void popup(View anchor,String[] labels,Runnable[] actions,int widthDp){
        if(openPopup!=null)openPopup.dismiss();
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(9),dp(9),dp(9),dp(9));
        panel.setBackground(new GlassSurface(this,panel,false,22));
        PopupWindow window=new PopupWindow(panel,dp(widthDp),-2,true);openPopup=window;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setOutsideTouchable(true);window.setElevation(dp(20));
        for(int n=0;n<labels.length;n++){
            final int index=n;TextView item=text(labels[n],16,0xfff4f4f8);item.setSingleLine(true);item.setEllipsize(android.text.TextUtils.TruncateAt.END);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(16),0,dp(12),0);item.setFocusable(true);
            item.setBackground(rounded(0x00303038,0x00303038,0,12));panel.addView(item,new LinearLayout.LayoutParams(-1,dp(48)));
            item.setOnFocusChangeListener((v,f)->{item.setTextColor(Color.WHITE);item.setBackground(f?new GlassSurface(this,item,true,12):new ColorDrawable(Color.TRANSPARENT));});
            item.setOnClickListener(v->{window.dismiss();actions[index].run();});
            item.setOnKeyListener((v,key,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(key==19&&index==0)return true;if(key==20&&index==labels.length-1)return true;return key==21||key==22;});
        }
        // API 21 dropdown clipping varies by firmware. Position explicitly inside the
        // root's visible rectangle, including a TV safe margin and measured popup size.
        Rect visible=new Rect();root.getWindowVisibleDisplayFrame(visible);
        int[] rootPos=new int[2],anchorPos=new int[2];root.getLocationOnScreen(rootPos);anchor.getLocationOnScreen(anchorPos);
        visible.intersect(rootPos[0],rootPos[1],rootPos[0]+root.getWidth(),rootPos[1]+root.getHeight());
        int safe=dp(48);visible.inset(safe,safe);
        int popupWidth=Math.min(dp(widthDp),visible.width());
        panel.measure(View.MeasureSpec.makeMeasureSpec(popupWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        int popupHeight=Math.min(panel.getMeasuredHeight(),visible.height());
        window.setWidth(popupWidth);window.setHeight(popupHeight);
        int x=Math.max(visible.left,Math.min(anchorPos[0]+anchor.getWidth()-popupWidth,visible.right-popupWidth));
        int y=Math.max(visible.top,Math.min(anchorPos[1]+anchor.getHeight()+dp(12),visible.bottom-popupHeight));
        glass.animate().cancel();glass.setAlpha(0);
        window.setOnDismissListener(()->{openPopup=null;anchor.requestFocus();});
        window.showAtLocation(root,Gravity.TOP|Gravity.LEFT,x-rootPos[0],y-rootPos[1]);
        panel.getChildAt(0).requestFocus();
    }
    void showInputs(){
        final View anchor=topViews.get(0);anchor.setEnabled(false);
        new Thread(()->{
            WhaleyInputs.Snapshot inputs=WhaleyInputs.read();
            android.util.Log.i("QuietInputs",inputs.hdmiLabel(0)+" | "+inputs.hdmiLabel(1)+" | "+inputs.hdmiLabel(2)+" error="+inputs.error);
            handler.post(()->{
                anchor.setEnabled(true);if(isFinishing()||!anchor.isAttachedToWindow()||!hasWindowFocus())return;
                String av=inputs.connected!=null&&inputs.connected.length>7&&!inputs.connected[7]?"AV — 未连接":"AV";
                popup(anchor,new String[]{inputs.hdmiLabel(0),inputs.hdmiLabel(1),inputs.hdmiLabel(2),av,"数字电视","模拟电视"},new Runnable[]{
                    ()->openInput(4),()->openInput(5),()->openInput(6),()->openInput(7),()->openInput(2),()->openInput(1)},350);
            });
        },"HDMI-CEC-query").start();
    }
    void openInput(int source){
        try{startActivity(new Intent("com.whaley.tv.tvplayer.ui.START_TV_PLAYER")
            .setComponent(new ComponentName("com.whaley.tv.tvplayer.ui","com.whaley.tv.tvplayer.ui.RootActivity"))
            .putExtra("inputSrc",source).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(Exception e){Toast.makeText(this,"信号源切换失败",0).show();}
    }
    void about(){
        final Dialog dialog=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(34),dp(28),dp(34),dp(28));
        panel.setBackground(rounded(0xff30313b,0xff171820,0x55ffffff,24));panel.addView(text("静境 TV 5.1",26,Color.WHITE));
        TextView body=text("视觉移植自 CleanYourTV · Gathin\n保留 MIT 许可；Outfit 字体使用 SIL OFL 1.1。\n本版适配 Android 5.0，保留壁纸与 Dock 设置。\n\n应用精简仅隐藏入口，不会停用后台服务。",16,0xffc6c8d3);body.setPadding(0,dp(18),0,dp(22));panel.addView(body);
        TextView close=text("返回",16,Color.WHITE);close.setGravity(17);close.setBackground(getResources().getDrawable(R.drawable.pill_bg));close.setFocusable(true);close.setOnClickListener(v->dialog.dismiss());panel.addView(close,new LinearLayout.LayoutParams(-1,dp(44)));
        dialog.setContentView(panel);Window w=dialog.getWindow();w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.setLayout(dp(650),-2);dialog.show();w.setLayout(dp(650),-2);close.requestFocus();
    }
    void wallpapers(){
        final Dialog dialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(32),dp(28),dp(32),dp(28));
        panel.setBackground(rounded(0xff30323d,0xff14151d,0x55ffffff,26));
        panel.addView(text("选择你的氛围",28,Color.WHITE));TextView subtitle=text("壁纸与外观  /  按确定应用，返回保留当前设置",14,0xffaeb3c5);subtitle.setPadding(0,dp(8),0,dp(28));panel.addView(subtitle);
        LinearLayout row=new LinearLayout(this);row.setClipChildren(false);panel.addView(row,new LinearLayout.LayoutParams(-1,dp(160)));
        String[] names={"山湖暮色","黑曜光晕","极光 · Aurora","海岸暮色","流光丝绸","纯黑"};String[] keys={"lake","glow","aurora","coast","dusk","black"};
        View active=null;
        for(int n=0;n<keys.length;n++){
            final String key=keys[n];LinearLayout option=new LinearLayout(this);option.setOrientation(1);option.setPadding(dp(6),dp(6),dp(6),dp(8));option.setFocusable(true);option.setBackground(rounded(0x102d303e,0x102d303e,0,16));
            Backdrop preview=new Backdrop(key);preview.setBackground(bg(Color.BLACK,0));preview.setClipToOutline(true);option.addView(preview,new LinearLayout.LayoutParams(-1,dp(92)));
            TextView name=text(names[n],14,Color.WHITE);name.setGravity(17);name.setPadding(0,dp(10),0,0);option.addView(name);
            TextView check=text(mode.equals(key)?"正在使用":"",11,0xffaeb8cc);check.setGravity(17);option.addView(check);
            LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,-1,1);op.setMargins(dp(4),0,dp(4),0);row.addView(option,op);
            option.setOnFocusChangeListener((v,f)->{v.setBackground(rounded(f?0x38ffffff:0x102d303e,f?0x14ffffff:0x102d303e,f?0xb3ffffff:0,16));v.animate().scaleX(f?1.035f:1).scaleY(f?1.035f:1).setDuration(180).start();});
            option.setOnClickListener(v->{mode=key;if(glassBitmap!=null){glassBitmap.recycle();glassBitmap=null;}getPreferences(0).edit().putString("wallpaper",mode).apply();backdrop.invalidate();for(View control:topViews)control.invalidate();dialog.dismiss();});
            if(mode.equals(key))active=option;
        }
        dialog.setContentView(panel);Window window=dialog.getWindow();window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams attrs=window.getAttributes();attrs.dimAmount=.65f;window.setAttributes(attrs);dialog.show();window.setLayout(dp(1090),-2);
        if(active!=null)active.requestFocus();
    }
    class Backdrop extends View {
        final Paint p=new Paint(3);final String forced;
        Backdrop(String value){super(MainActivity.this);forced=value;}
        void radial(Canvas c,float x,float y,float radius,int color){p.setShader(new RadialGradient(x,y,radius,new int[]{color,color&0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),p);p.setShader(null);}
        protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();String value=forced==null?mode:forced;c.drawColor(Color.BLACK);
            if(value.equals("glow")){
                p.setShader(new RadialGradient(w*.32f,h*.42f,w*.62f,new int[]{(theme&0xffffff)|0x8c000000,(theme&0xffffff)|0x35000000,theme&0xffffff},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
                c.drawRect(0,0,w,h,p);p.setShader(null);
            }else if(value.equals("lake")&&lake!=null){c.drawBitmap(lake,null,new RectF(0,0,w,h),p);p.setShader(new LinearGradient(0,0,0,h,new int[]{0x20000000,0,0x60000000},new float[]{0,.5f,1},Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);}
            else if(value.equals("aurora")&&aurora!=null){c.drawBitmap(aurora,null,new RectF(0,0,w,h),p);}
            else if((value.equals("coast")&&coast!=null)||(value.equals("dusk")&&silk!=null)){
                c.drawBitmap(value.equals("coast")?coast:silk,null,new RectF(0,0,w,h),p);
                p.setShader(new LinearGradient(0,0,0,h,new int[]{0x10000000,0,0x40000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            }
        }
    }

}
