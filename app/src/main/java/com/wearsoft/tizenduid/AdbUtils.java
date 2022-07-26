package com.wearsoft.tizenduid;

import com.cgutman.adblib.AdbCrypto;

import java.io.File;
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
            } catch (Exception ignored) {
            }
        }

        return crypto;
    }

    public static AdbCrypto writeNewCryptoConfig(File dataDir) {
        File pubKey = new File(dataDir, PUBLIC_KEY_NAME);
        File privKey = new File(dataDir, PRIVATE_KEY_NAME);

        KeyPairGenerator rsaKeyPg;
        try {
            rsaKeyPg = KeyPairGenerator.getInstance("RSA");
            rsaKeyPg.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        AdbCrypto crypto;

        try {
            crypto = AdbCrypto.generateAdbKeyPair(new AndroidBase64());
            crypto.saveAdbKeyPair(privKey, pubKey);

        } catch (Exception e) {
            crypto = null;
        }

        return crypto;
    }

}

