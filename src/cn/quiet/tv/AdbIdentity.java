package cn.quiet.tv;

import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;

/** Per-install ADB identity stored in the app's private, backup-excluded directory. */
final class AdbIdentity implements LocalAdb.Identity {
    private final KeyPair pair;
    AdbIdentity(Context context)throws Exception{
        File directory=new File(context.getFilesDir(),"local-adb");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("无法保存本机授权密钥");
        File privateFile=new File(directory,"private.der"),publicFile=new File(directory,"public.der");
        if(privateFile.exists()&&publicFile.exists()){
            KeyFactory factory=KeyFactory.getInstance("RSA");pair=new KeyPair(factory.generatePublic(new X509EncodedKeySpec(read(publicFile))),factory.generatePrivate(new PKCS8EncodedKeySpec(read(privateFile))));
        }else{
            KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);pair=generator.generateKeyPair();
            write(privateFile,pair.getPrivate().getEncoded());write(publicFile,pair.getPublic().getEncoded());
        }
    }
    public byte[] sign(byte[] token)throws Exception{return AdbKeyCodec.sign(pair.getPrivate(),token);}
    public byte[] publicKey(){return (Base64.encodeToString(AdbKeyCodec.publicKey((RSAPublicKey)pair.getPublic()),Base64.NO_WRAP)+" quiet-tv@android\0").getBytes(LocalAdb.UTF8);}
    private static byte[] read(File file)throws IOException{if(file.length()>8192)throw new IOException("授权密钥文件损坏");FileInputStream in=new FileInputStream(file);try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}finally{in.close();}}
    private static void write(File file,byte[] data)throws IOException{FileOutputStream out=new FileOutputStream(file);try{out.write(data);}finally{out.close();}}
}
