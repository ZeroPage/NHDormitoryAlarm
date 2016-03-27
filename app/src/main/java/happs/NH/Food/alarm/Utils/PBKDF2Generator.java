package happs.NH.Food.alarm.Utils;

import android.util.Base64;
import android.util.Log;

import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.Random;

public class PBKDF2Generator {

    private static final int KEY_LENGTH = 128;
    private static final int ITERATIONS = 1000;

    public static String generatePassword(String plainText) {

        String encodedSalt = Base64.encodeToString(Constant.SALT.getBytes(), Base64.NO_WRAP);

        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(plainText.toCharArray()), encodedSalt.getBytes(), ITERATIONS);
        KeyParameter key = (KeyParameter)generator.generateDerivedMacParameters(KEY_LENGTH);

        String encodedKey = Base64.encodeToString(key.getKey(), Base64.NO_WRAP);
        String hashedKey = "PBKDF2$sha256$" + ITERATIONS + "$" + encodedSalt + "$" + encodedKey;

        return hashedKey;
    }

}