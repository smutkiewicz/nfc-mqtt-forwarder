package studios.aestheticapps.nfcmqttforwarder;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;

public class DesEncrypter {

    private static final String charsetName = "UTF-8";
    private static final String algorithm = "DES";

    private SecretKey secretKey;

    public DesEncrypter(String key) {
        secretKey = rawStringToKey(key);
    }

    private SecretKey rawStringToKey(String rawString) {
        byte[] raw = rawString.getBytes(Charset.forName("UTF-8"));
        if (raw.length != 8) {
            throw new IllegalArgumentException("Invalid key size.");
        }

        return new SecretKeySpec(raw, algorithm);
    }

    public String encrypt(String str) throws Exception {
        Cipher ecipher = Cipher.getInstance(algorithm);
        ecipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encode the string into bytes using utf-8
        byte[] utf8 = str.getBytes(charsetName);

        // Encrypt
        byte[] enc = ecipher.doFinal(utf8);

        // Encode bytes to base64 to get a string
        return Base64.encodeToString(enc, Base64.DEFAULT);
    }

    public String decrypt(String str) throws Exception {
        Cipher dcipher = Cipher.getInstance(algorithm);
        dcipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decode base64 to get bytes
        byte[] dec = Base64.decode(str, Base64.DEFAULT);

        byte[] utf8 = dcipher.doFinal(dec);

        // Decode using utf-8
        return new String(utf8, charsetName);
    }

}