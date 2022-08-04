package com.wearsoft.tizenduid

import com.cgutman.adblib.AdbCrypto
import java.io.File
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException

object AdbUtils {
    private const val PUBLIC_KEY_NAME = "public.key"
    private const val PRIVATE_KEY_NAME = "private.key"
    fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, PUBLIC_KEY_NAME)
        val privKey = File(dataDir, PRIVATE_KEY_NAME)
        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            try {
                crypto = AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (ignored: Exception) {
            }
        }
        return crypto
    }

    fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, PUBLIC_KEY_NAME)
        val privKey = File(dataDir, PRIVATE_KEY_NAME)
        val rsaKeyPg: KeyPairGenerator
        try {
            rsaKeyPg = KeyPairGenerator.getInstance("RSA")
            rsaKeyPg.initialize(2048)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        var crypto: AdbCrypto?
        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }
        return crypto
    }
}