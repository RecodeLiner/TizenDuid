package com.wearsoft.tizenduid

import com.cgutman.adblib.AdbBase64
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

class AdbKeyPair(
    private var keyPair: KeyPair
//    private val privateKey: PrivateKey,
//    internal val publicKeyBytes: ByteArray
) {

    private val base64: AdbBase64 = AndroidBase64()

    @Throws(IOException::class)
    fun getAdbPublicKeyPayload(): ByteArray? {
        val convertedKey = convertRsaPublicKeyToAdbFormat(keyPair.public as RSAPublicKey)
        val keyString = StringBuilder(720)
        keyString.append(base64.encodeToString(convertedKey))
        keyString.append(" unknown@unknown")
        keyString.append('\u0000')
        return keyString.toString().toByteArray(charset("UTF-8"))
    }

    internal fun signPayload(message: SdbMessage): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.private)
        cipher.update(SIGNATURE_PADDING)
        return cipher.doFinal(message.payload)
    }

    private fun convertRsaPublicKeyToAdbFormat(pubkey: RSAPublicKey): ByteArray? {
        val r32 = BigInteger.ZERO.setBit(32)
        var n = pubkey.modulus
        val r = BigInteger.ZERO.setBit(2048)
        var rr = r.modPow(BigInteger.valueOf(2L), n)
        var rem = n.remainder(r32)
        val n0inv = rem.modInverse(r32)
        val myN = IntArray(64)
        val myRr = IntArray(64)
        for (i in 0..63) {
            var res = rr.divideAndRemainder(r32)
            rr = res[0]
            rem = res[1]
            myRr[i] = rem.toInt()
            res = n.divideAndRemainder(r32)
            n = res[0]
            rem = res[1]
            myN[i] = rem.toInt()
        }
        val bbuf = ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN)
        bbuf.putInt(64)
        bbuf.putInt(n0inv.negate().toInt())
        var var14 = myN
        var var13 = myN.size
        var i: Int
        var var12: Int = 0
        while (var12 < var13) {
            i = var14[var12]
            bbuf.putInt(i)
            ++var12
        }
        var14 = myRr
        var13 = myRr.size
        var12 = 0
        while (var12 < var13) {
            i = var14[var12]
            bbuf.putInt(i)
            ++var12
        }
        bbuf.putInt(pubkey.publicExponent.toInt())
        return bbuf.array()
    }

    companion object {

        private val SIGNATURE_PADDING = ubyteArrayOf(
            0x00u, 0x01u, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu,
            0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0x00u,
            0x30u, 0x21u, 0x30u, 0x09u, 0x06u, 0x05u, 0x2bu, 0x0eu, 0x03u, 0x02u, 0x1au, 0x05u, 0x00u,
            0x04u, 0x14u
        ).toByteArray()

        @JvmStatic
        fun readDefault(dataDir: File): AdbKeyPair? {

            val privateKeyFile = File(dataDir, "private.key")
            val publicKeyFile = File(dataDir, "public.key")
            return read(privateKeyFile, publicKeyFile)
        }

        @JvmStatic
        fun read(privateKeyFile: File, publicKeyFile: File): AdbKeyPair? {
            if (!privateKeyFile.exists() || !publicKeyFile.exists()) return null

            val privKeyLength: Int = privateKeyFile.length().toInt()
            val pubKeyLength: Int = publicKeyFile.length().toInt()
            val privKeyBytes = ByteArray(privKeyLength)
            val pubKeyBytes = ByteArray(pubKeyLength)
            val privIn = FileInputStream(privateKeyFile)
            val pubIn = FileInputStream(publicKeyFile)
            privIn.read(privKeyBytes)
            pubIn.read(pubKeyBytes)
            privIn.close()
            pubIn.close()
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKeySpec: EncodedKeySpec = PKCS8EncodedKeySpec(privKeyBytes)
            val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(pubKeyBytes)
            return AdbKeyPair(keyPair = KeyPair(
                keyFactory.generatePublic(publicKeySpec),
                keyFactory.generatePrivate(privateKeySpec)
            ))
        }
    }
}