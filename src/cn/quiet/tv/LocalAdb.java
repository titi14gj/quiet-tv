package cn.quiet.tv;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.util.UUID;

/** Opt-in legacy ADB transport. Only the device's loopback interface is reachable. */
final class LocalAdb implements Closeable {
    static final int CNXN=0x4e584e43,AUTH=0x48545541,OPEN=0x4e45504f,OKAY=0x59414b4f,WRTE=0x45545257,CLSE=0x45534c43;
    static final Charset UTF8=Charset.forName("UTF-8");
    interface Identity {byte[] sign(byte[] token)throws Exception;byte[] publicKey()throws Exception;}
    interface Notice {void authorizationRequired();}
    static final class Packet {
        final int command,a,b;final byte[] data;
        Packet(int command,int a,int b,byte[] data){this.command=command;this.a=a;this.b=b;this.data=data;}
    }
    static final class Result {
        final String output;final int exit;
        Result(String output,int exit){this.output=output;this.exit=exit;}
    }
    private final Socket socket;private final InputStream input;private final OutputStream output;
    private int stream=0,peerMax=4096;private boolean ready;
    LocalAdb(int port)throws IOException{
        if(port<1||port>65535)throw new IOException("端口无效");
        Socket candidate=new Socket();
        try{candidate.connect(new InetSocketAddress("127.0.0.1",port),2500);candidate.setTcpNoDelay(true);
            input=candidate.getInputStream();output=candidate.getOutputStream();socket=candidate;
        }catch(IOException e){try{candidate.close();}catch(IOException ignored){}throw e;}
    }
    void connect(Identity identity,Notice notice)throws Exception{
        write(new Packet(CNXN,0x01000000,4096,"host::quiet-tv\0".getBytes(UTF8)));
        boolean signed=false,published=false;long deadline=System.nanoTime()+60000000000L;
        for(int count=0;count<8;count++){
            Packet packet=read(deadline);
            if(packet.command==CNXN){
                if(packet.a<0x01000000||packet.b<1024)throw new IOException("ADB 版本或数据包大小不支持");
                peerMax=Math.min(4096,packet.b);ready=true;return;
            }
            if(packet.command!=AUTH||packet.a!=1||packet.data.length!=20)throw new IOException("不支持的 ADB 握手；本版不支持配对码/TLS");
            if(!signed){write(new Packet(AUTH,2,0,identity.sign(packet.data)));signed=true;}
            else if(!published){write(new Packet(AUTH,3,0,identity.publicKey()));published=true;notice.authorizationRequired();}
            else throw new IOException("系统未接受授权，请确认电视上的 ADB 授权提示后重试");
        }
        throw new IOException("ADB 授权未完成");
    }
    synchronized Result shell(String command)throws IOException{
        if(!ready)throw new IOException("尚未连接本机 ADB");
        String marker="__QUIET_"+UUID.randomUUID().toString().replace("-","")+":";
        String script=command+"\nprintf '\\n"+marker+"%s\\n' \"$?\"\n";
        byte[] destination=("shell:"+script+"\0").getBytes(UTF8);
        if(destination.length>peerMax)throw new IOException("命令超过旧版 ADB 长度限制");
        int local=++stream,remote=0;long deadline=System.nanoTime()+20000000000L;
        ByteArrayOutputStream collected=new ByteArrayOutputStream();
        write(new Packet(OPEN,local,0,destination));
        try{
            while(true){
                Packet packet=read(deadline);
                // Some daemons send a final CLSE after our close acknowledgement.
                // It belongs to an already completed stream; do not acknowledge it again.
                if(packet.command==CLSE&&packet.b>0&&packet.b<local)continue;
                if(packet.b!=local)throw new IOException("ADB 数据流编号不匹配（"+Integer.toHexString(packet.command)+" / "+packet.a+" / "+packet.b+"，预期 "+local+"）");
                if(packet.command==OKAY){if(remote!=0&&remote!=packet.a)throw new IOException("ADB 对端编号变化");remote=packet.a;}
                else if(packet.command==WRTE){
                    if(remote==0)remote=packet.a;
                    if(packet.a!=remote||collected.size()+packet.data.length>262144)throw new IOException("ADB 输出异常或超出限制");
                    collected.write(packet.data);write(new Packet(OKAY,local,remote,new byte[0]));
                }else if(packet.command==CLSE){
                    if(remote!=0&&packet.a!=remote)throw new IOException("ADB 关闭编号不匹配");
                    write(new Packet(CLSE,local,packet.a,new byte[0]));break;
                }else throw new IOException("非预期 ADB 消息");
            }
            String text=new String(collected.toByteArray(),UTF8).replace("\r","");int pos=text.lastIndexOf(marker);
            if(pos<0)throw new IOException("未收到命令退出状态，不能确认执行结果");
            String status=text.substring(pos+marker.length()).trim();int code;
            try{code=Integer.parseInt(status);}catch(NumberFormatException e){throw new IOException("命令退出状态损坏");}
            return new Result(text.substring(0,pos).trim(),code);
        }catch(IOException e){close();throw e;}
    }
    private Packet read(long deadline)throws IOException{
        long remaining=(deadline-System.nanoTime())/1000000L;if(remaining<=0)throw new SocketTimeoutException("等待 ADB 超时");
        socket.setSoTimeout((int)Math.min(remaining,60000));return decode(input);
    }
    private void write(Packet packet)throws IOException{encode(output,packet);}
    static void encode(OutputStream out,Packet packet)throws IOException{
        int sum=0;for(byte value:packet.data)sum+=value&255;
        ByteBuffer header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(packet.command).putInt(packet.a).putInt(packet.b).putInt(packet.data.length).putInt(sum).putInt(~packet.command);
        out.write(header.array());out.write(packet.data);out.flush();
    }
    static Packet decode(InputStream in)throws IOException{
        byte[] bytes=new byte[24];readFully(in,bytes);ByteBuffer h=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int command=h.getInt(),a=h.getInt(),b=h.getInt(),length=h.getInt(),checksum=h.getInt(),magic=h.getInt();
        if(magic!=~command||length<0||length>1048576)throw new IOException("ADB 包头不合法");
        byte[] data=new byte[length];readFully(in,data);int sum=0;for(byte value:data)sum+=value&255;
        if(sum!=checksum)throw new IOException("ADB 校验和不匹配");return new Packet(command,a,b,data);
    }
    static void readFully(InputStream in,byte[] data)throws IOException{int pos=0,n;while(pos<data.length){n=in.read(data,pos,data.length-pos);if(n<0)throw new EOFException("ADB 连接已关闭");pos+=n;}}
    public void close(){ready=false;try{socket.close();}catch(IOException ignored){}}
}
