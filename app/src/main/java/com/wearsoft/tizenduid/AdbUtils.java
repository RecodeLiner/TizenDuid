package com.wearsoft.tizenduid;

import com.cgutman.adblib.AdbCrypto;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class AdbUtils {

    public static final String PUBLIC_KEY_NAME = "public.key";
    public static final String PRIVATE_KEY_NAME = "private.key";

    public static AdbCrypto readCryptoConfig(File dataDir) {
        File pubKey = new File(dataDir, PUBLIC_KEY_NAME);
        File privKey = new File(dataDir, PRIVATE_KEY_NAME);

        AdbCrypto crypto = null;
        if (pubKey.exists() && privKey.exists())
        {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(new AndroidBase64(), privKey, pubKey);
            } catch (Exception e) {
                crypto = null;
            }
        }

        return crypto;
    }

    public static AdbCrypto writeNewCryptoConfig(File dataDir) {
        File pubKey = new File(dataDir, PUBLIC_KEY_NAME);
        File privKey = new File(dataDir, PRIVATE_KEY_NAME);

        KeyPairGenerator rsaKeyPg = null;
        try {
            rsaKeyPg = KeyPairGenerator.getInstance("RSA");
            rsaKeyPg.initialize(2048);
            KeyPair keyPair = rsaKeyPg.genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        AdbCrypto crypto = null;

        try {
            crypto = AdbCrypto.generateAdbKeyPair(new AndroidBase64());
            crypto.saveAdbKeyPair(privKey, pubKey);

        } catch (Exception e) {
            crypto = null;
        }

        return crypto;
    }

    public static boolean safeClose(Closeable c) {
        if (c == null)
            return false;

        try {
            c.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}

