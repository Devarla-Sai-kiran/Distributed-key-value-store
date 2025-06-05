package core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

public class HashUtil {
    public static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = md.digest(input.getBytes());
            BigInteger hashInt = new BigInteger(1, hashBytes);
            String hashStr = hashInt.toString(16);
            while (hashStr.length() < 40) {
                hashStr = "0" + hashStr;
            }
            return hashStr;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
