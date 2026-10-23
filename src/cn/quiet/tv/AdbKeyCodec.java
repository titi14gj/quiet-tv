package cn.quiet.tv;

import java.math.BigInteger;
import java.nio.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;

/** ADB's legacy RSA token and 524-byte public-key encoding, not an APK signing key. */
final class AdbKeyCodec {
    static byte[] publicKey(RSAPublicKey key){
        if(key.getModulus().bitLength()!=2048||!key.getPublicExponent().equals(BigInteger.valueOf(65537)))throw new IllegalArgumentException("Expected RSA-2048/F4");
        BigInteger n=key.getModulus(),word=BigInteger.ONE.shiftLeft(32);
        ByteBuffer bytes=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(64).putInt(n.mod(word).modInverse(word).negate().intValue());
        bytes.put(little(n)).put(little(BigInteger.ONE.shiftLeft(4096).mod(n))).putInt(65537);return bytes.array();
    }
    private static byte[] little(BigInteger value){byte[] source=value.toByteArray(),result=new byte[256];for(int i=0;i<256&&i<source.length;i++)result[i]=source[source.length-1-i];return result;}
    static byte[] sign(PrivateKey key,byte[] token)throws GeneralSecurityException{
        if(token.length!=20)throw new GeneralSecurityException("Invalid ADB token");
        byte[] prefix={0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};
        Signature signature=Signature.getInstance("NONEwithRSA");signature.initSign(key);signature.update(prefix);signature.update(token);return signature.sign();
    }
}
