package cn.mcfun.utils;

import cn.mcfun.Main;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class AES {
    public static String encryptWithAesCBC(String plaintext) {
        try {
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine(256)), new PKCS7Padding());
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(Main.key), Main.iv);
            cipher.init(true, ivAndKey);
            return new String(Base64.getEncoder().encode(cipherData(cipher, plaintext.getBytes())));
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String,Object> decryptWithAesCBC(byte[] key,byte[] iv,byte[] encrypted) {
        try {
            byte[] cipherText = encrypted;
            PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(new RijndaelEngine(256)), new PKCS7Padding());
            CipherParameters ivAndKey = new ParametersWithIV(new KeyParameter(key), iv);
            aes.init(false, ivAndKey);
            byte[] unGzip = unGZip(cipherData(aes, cipherText));
            return unpackValue(unGzip);
        } catch (InvalidCipherTextException e) {
            throw new RuntimeException(e);
        }
    }
    public static byte[] unGZip(byte[] data) {
        byte[] b = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            GZIPInputStream gzip = new GZIPInputStream(bis);
            byte[] buf = new byte[1024];
            int num = -1;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((num = gzip.read(buf, 0, buf.length)) != -1) {
                baos.write(buf, 0, num);
            }
            b = baos.toByteArray();
            baos.flush();
            baos.close();
            gzip.close();
            bis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return b;
    }
    public static Map<String,Object> unpackValue(byte[] data){
        Map<String,Object> payload = null;
        MessageUnpacker msg = MessagePack.newDefaultUnpacker(data);
        try {
            Value v = msg.unpackValue();
            payload = (Map<String, Object>) getValue(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return payload;
    }

    public static Object getValue(Value v) {
        Object value = null;
        switch (v.getValueType()) {
            case NIL:
                v.isNilValue(); // true
                break;
            case BOOLEAN:
                boolean b = v.asBooleanValue().getBoolean();
                value = b;
                break;
            case INTEGER:
                IntegerValue iv = v.asIntegerValue();
                if (iv.isInIntRange()) {
                    int i = iv.toInt();
                    value = i;
                } else if (iv.isInLongRange()) {
                    long l = iv.toLong();
                    value = l;
                } else {
                    BigInteger i = iv.toBigInteger();
                    value = i;
                }
                break;
            case FLOAT:
                FloatValue fv = v.asFloatValue();
                float f = fv.toFloat(); // use as float
                double d = fv.toDouble(); // use as double
                value = d;
                break;
            case STRING:
                String _s = v.asStringValue().asString();
                value = _s;
                break;
            case ARRAY:
                ArrayValue a = v.asArrayValue();
                value = a;
                break;
            case MAP:
                Map<String,Object> map = new HashMap<>();
                MapValue m = v.asMapValue();
                for (Value _k : m.map().keySet()) {
                    String _ks = _k.asStringValue().asString();
                    map.put(_ks , getValue(m.map().get(_k)));
                }
                value = map;
                break;
        }
        return value;
    }
    private static byte[] cipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws InvalidCipherTextException {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int length2 = cipher.doFinal(outBuf, length1);
        int actualLength = length1 + length2;
        byte[] cipherArray = new byte[actualLength];
        for (int x = 0; x < actualLength; x++) {
            cipherArray[x] = outBuf[x];
        }
        return cipherArray;
    }

}
