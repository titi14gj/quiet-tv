package cn.quiet.tv;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.math.BigInteger;
import java.util.*;
import javax.crypto.Cipher;

public final class SetupTests {
    static int checks;
    interface Action {void run()throws Exception;}
    static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
    static void fails(Action action)throws Exception{boolean failed=false;try{action.run();}catch(Exception expected){failed=true;}check(failed);}
    static LocalAdb.Result result(String s){return new LocalAdb.Result(s,0);}
    static final class Device implements SetupEngine.Shell,SetupEngine.Journal {
        Set<String> hidden=new HashSet<>(),journal=new HashSet<>();List<String> writes=new ArrayList<>();
        boolean deny,wrongHome,badIdentity,failJournal;int enabled;
        public LocalAdb.Result run(String c){
            if(c.equals("id"))return result("uid=2000(shell)");
            if(c.startsWith("getprop"))return result(badIdentity?"other":SetupPolicy.VERIFIED_FINGERPRINT);
            String pkg=null;for(String p:new String[]{SetupPolicy.HOME,SetupPolicy.AD1,SetupPolicy.AD2})if(c.contains(p))pkg=p;
            if(c.startsWith("dumpsys"))return result("User 0: installed=true hidden="+hidden.contains(pkg)+" stopped=false enabled="+enabled);
            if(c.startsWith("pm hide")){check(journal.contains(pkg));writes.add(c);if(!deny)hidden.add(pkg);}
            else if(c.startsWith("pm unhide")){writes.add(c);hidden.remove(pkg);}
            else if(c.equals("pm path cn.quiet.tv"))return result("package:/data/app/quiet.apk");
            else if(c.contains("home-set"))return result("preferred-after=ComponentInfo{cn.quiet.tv/cn.quiet.tv.MainActivity}");
            else if(c.contains("home-resolve"))return result("resolved="+(wrongHome?"other/.Home":"cn.quiet.tv/.MainActivity"));
            return result("");
        }
        public void remember(String p){if(failJournal)throw new IllegalStateException("disk full");journal.add(p);}public void forget(String p){journal.remove(p);}
        SetupEngine engine(boolean supported)throws Exception{SetupEngine e=new SetupEngine(this,this,supported,SetupPolicy.VERIFIED_FINGERPRINT,"/data/app/quiet.apk");e.verifyIdentity();return e;}
    }
    static void policy()throws Exception {
        check(SetupPolicy.supported("65D2UT",21,"HELIOSR-02.08.03 release-keys",SetupPolicy.VERIFIED_FINGERPRINT));
        check(!SetupPolicy.supported("65D2UT",21,"HELIOSR-02.08.03 release-keys","other"));
        check(!SetupPolicy.supported("other",21,"HELIOSR-02.08.03 release-keys",SetupPolicy.VERIFIED_FINGERPRINT));
        fails(()->SetupPolicy.parse("User 10: installed=true hidden=false enabled=0"));
        fails(()->SetupPolicy.parse("User 0: installed=true enabled=0"));
        Device d=new Device();SetupEngine unsupported=d.engine(false);fails(()->unsupported.hide(SetupPolicy.AD1));check(d.writes.isEmpty());
        d.badIdentity=true;fails(()->d.engine(true));check(d.writes.isEmpty());d.badIdentity=false;SetupEngine e=d.engine(true);
        final SetupEngine engine=e;fails(()->engine.hide("com.android.settings"));check(d.writes.isEmpty());
        d.hidden.add(SetupPolicy.AD1);e.hide(SetupPolicy.AD1);check(d.journal.isEmpty()&&d.writes.isEmpty());
        d.enabled=3;e.hide(SetupPolicy.AD2);check(d.journal.isEmpty()&&d.writes.isEmpty());d.enabled=0;
        d.failJournal=true;fails(()->engine.hide(SetupPolicy.AD2));check(d.writes.isEmpty());d.failJournal=false;
        d.deny=true;fails(()->engine.hide(SetupPolicy.AD2));check(!d.journal.contains(SetupPolicy.AD2));
        d.deny=false;e.hide(SetupPolicy.AD2);check(d.hidden.contains(SetupPolicy.AD2)&&d.journal.contains(SetupPolicy.AD2));
        e.restore(SetupPolicy.AD2);check(!d.hidden.contains(SetupPolicy.AD2)&&!d.journal.contains(SetupPolicy.AD2));
        d.wrongHome=true;fails(engine::takeover);check(!d.hidden.contains(SetupPolicy.HOME)&&!d.journal.contains(SetupPolicy.HOME));
        d.hidden.add(SetupPolicy.HOME);fails(engine::takeover);check(d.hidden.contains(SetupPolicy.HOME));
        d.wrongHome=false;check(e.takeover().contains("HOME 解析"));
        Device unknown=new Device();SetupEngine unverified=new SetupEngine(unknown,unknown,true,SetupPolicy.VERIFIED_FINGERPRINT,"x");fails(()->unverified.hide(SetupPolicy.AD1));check(unknown.writes.isEmpty());
    }
    static void crypto()throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair keys=generator.generateKeyPair();
        RSAPublicKey key=(RSAPublicKey)keys.getPublic();byte[] encoded=AdbKeyCodec.publicKey(key);check(encoded.length==524);
        ByteBuffer words=ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);check(words.getInt()==64);
        long inverse=words.getInt()&0xffffffffL;BigInteger word=BigInteger.ONE.shiftLeft(32);
        check(key.getModulus().multiply(BigInteger.valueOf(inverse)).add(BigInteger.ONE).mod(word).signum()==0);
        byte[] modulus=new byte[256];words.get(modulus);for(int i=0;i<128;i++){byte x=modulus[i];modulus[i]=modulus[255-i];modulus[255-i]=x;}check(new BigInteger(1,modulus).equals(key.getModulus()));
        byte[] rr=new byte[256];words.get(rr);for(int i=0;i<128;i++){byte x=rr[i];rr[i]=rr[255-i];rr[255-i]=x;}check(new BigInteger(1,rr).equals(BigInteger.ONE.shiftLeft(4096).mod(key.getModulus())));check(words.getInt()==65537);
        byte[] token=new byte[20];new SecureRandom().nextBytes(token);byte[] signature=AdbKeyCodec.sign(keys.getPrivate(),token);
        Cipher rsa=Cipher.getInstance("RSA/ECB/PKCS1Padding");rsa.init(Cipher.DECRYPT_MODE,keys.getPublic());byte[] decoded=rsa.doFinal(signature);
        check(decoded.length==35&&Arrays.equals(token,Arrays.copyOfRange(decoded,15,35)));fails(()->AdbKeyCodec.sign(keys.getPrivate(),new byte[19]));
    }
    static void transport(boolean marker)throws Exception {
        ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));final Throwable[] error={null};final boolean[] notice={false};
        Thread daemon=new Thread(()->{try(Socket s=server.accept()){
            s.setSoTimeout(4000);InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();
            check(LocalAdb.decode(in).command==LocalAdb.CNXN);byte[] token=new byte[20];
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.AUTH,1,0,token));check(LocalAdb.decode(in).a==2);
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.AUTH,1,0,token));check(LocalAdb.decode(in).a==3);
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.CNXN,0x01000000,4096,new byte[0]));
            for(int operation=0;operation<2;operation++){
            LocalAdb.Packet open=LocalAdb.decode(in);check(open.command==LocalAdb.OPEN);String command=new String(open.data,LocalAdb.UTF8);
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("__QUIET_[a-f0-9]+:").matcher(command);check(m.find());
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.OKAY,7,open.a,new byte[0]));
            byte[] payload=("Permission denied\n"+(marker?m.group()+"1\n":"")).getBytes(LocalAdb.UTF8);
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.WRTE,7,open.a,payload));check(LocalAdb.decode(in).command==LocalAdb.OKAY);
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.CLSE,7,open.a,new byte[0]));check(LocalAdb.decode(in).command==LocalAdb.CLSE);
            LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.CLSE,7,open.a,new byte[0]));if(!marker)break;
            }
        }catch(Throwable t){error[0]=t;}});daemon.start();
        try(LocalAdb adb=new LocalAdb(server.getLocalPort())){
            adb.connect(new LocalAdb.Identity(){public byte[] sign(byte[] t){return new byte[256];}public byte[] publicKey(){return new byte[]{0};}},()->notice[0]=true);
            if(marker){LocalAdb.Result result=adb.shell("id");check(result.exit==1&&result.output.equals("Permission denied"));check(adb.shell("id").exit==1);}else fails(()->adb.shell("id"));check(notice[0]);
        }finally{server.close();daemon.join(5000);}check(!daemon.isAlive());if(error[0]!=null)throw new AssertionError(error[0]);
    }
    static void community()throws Exception {
        RegisteredProfile profile=RegisteredProfile.match("Smart TV Pro","G08",31,"ignored",RegisteredProfile.TCL_FP);
        check(profile!=null&&profile.packages.length==3);
        check(RegisteredProfile.match("Smart TV Pro","G08",31,"ignored","new firmware")==null);
        check(RegisteredProfile.match("Smart TV Pro","G08",30,"ignored",RegisteredProfile.TCL_FP)==null);
        check(RegisteredProfile.match("Other","G08",31,"ignored",RegisteredProfile.TCL_FP)==null);
        final Map<String,Integer> enabled=new HashMap<>(),records=new HashMap<>();final boolean[] deny={false},identity={true},disk={true};final int[] writes={0};
        SetupEngine.Shell shell=c->{
            if(c.equals("id"))return result("uid=2000(shell)");if(c.startsWith("getprop"))return result(identity[0]?RegisteredProfile.TCL_FP:"other");
            String pkg=profile.packages[0];for(String p:profile.packages)if(c.contains(p))pkg=p;
            if(c.startsWith("dumpsys"))return result("User 0: installed=true hidden=false enabled="+enabled.getOrDefault(pkg,0));
            writes[0]++;check(records.containsKey(pkg));if(!deny[0]){if(c.startsWith("pm disable-user"))enabled.put(pkg,3);else if(c.startsWith("pm enable"))enabled.put(pkg,1);else if(c.startsWith("pm default-state"))enabled.put(pkg,0);}return result("ignored");
        };
        CommunitySetupEngine.Journal journal=new CommunitySetupEngine.Journal(){public void remember(String p,int before){if(!disk[0])throw new IllegalStateException("disk full");records.put(p,before);}public void forget(String p){records.remove(p);}};
        CommunitySetupEngine engine=new CommunitySetupEngine(shell,journal,profile,RegisteredProfile.TCL_FP);String pkg=profile.packages[0];
        fails(()->engine.disable(pkg));check(writes[0]==0);engine.verifyIdentity();fails(()->engine.disable("com.android.settings"));check(writes[0]==0);
        disk[0]=false;fails(()->engine.disable(pkg));check(writes[0]==0);disk[0]=true;
        enabled.put(pkg,3);engine.disable(pkg);check(writes[0]==0&&records.isEmpty());enabled.put(pkg,0);
        deny[0]=true;fails(()->engine.disable(pkg));check(records.isEmpty());deny[0]=false;
        engine.disable(pkg);check(enabled.get(pkg)==3&&records.get(pkg)==0);engine.restore(pkg,0);check(enabled.get(pkg)==0&&records.isEmpty());
        enabled.put(pkg,1);engine.disable(pkg);check(records.get(pkg)==1);engine.restore(pkg,1);check(enabled.get(pkg)==1&&records.isEmpty());
        identity[0]=false;fails(engine::verifyIdentity);int count=writes[0];fails(()->engine.disable(pkg));check(count==writes[0]);
        SetupEngine.Shell okay=c->result(c.startsWith("pm path")?"package:/data/app/quiet.apk":c.contains("resolve-activity")?"priority=0 preferredOrder=0 match=0x108000\ncn.quiet.tv/.MainActivity":"Success");DefaultHomeEngine.select(okay);
        fails(()->DefaultHomeEngine.select(c->result(c.startsWith("pm path")?"package:/data/app/quiet.apk":c.contains("resolve-activity")?"other/.Home":"Success")));
        fails(()->DefaultHomeEngine.select(c->result(c.startsWith("pm path")?"package:/data/app/quiet.apk":c.contains("resolve-activity")?"other.cn.quiet.tv/.MainActivity":"Success")));
    }
    public static void main(String[] args)throws Exception{
        policy();community();crypto();transport(true);transport(false);
        ByteArrayOutputStream out=new ByteArrayOutputStream();LocalAdb.encode(out,new LocalAdb.Packet(LocalAdb.AUTH,1,0,new byte[]{1,2,3}));byte[] packet=out.toByteArray();packet[24]^=1;fails(()->LocalAdb.decode(new ByteArrayInputStream(packet)));
        fails(()->LocalAdb.decode(new ByteArrayInputStream(new byte[2])));
        System.out.println("Passed "+checks+" assertions: policy, identity, rollback, preexisting state, RSA, authorization, packet integrity and shell status.");
    }
}
