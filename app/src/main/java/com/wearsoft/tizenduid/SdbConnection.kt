package com.wearsoft.tizenduid

import android.util.Log
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.io.IOException
import java.net.Socket
import java.util.*

internal class SdbConnection internal constructor(
    sdbReader: SdbReader,
    private val sdbWriter: SdbWriter,
    private val closeable: Closeable?,
    private val supportedFeatures: Set<String>,
    private val version: Int,
    private val maxPayloadSize: Int
) : AutoCloseable {

    private val random = Random()
    private val messageQueue = SdbMessageQueue(sdbReader)

    @Throws(IOException::class)
    fun open(destination: String): SdbStream {
        val localId = newId()
        messageQueue.startListening(localId)
        try {
            sdbWriter.writeOpen(localId, destination)
            val message = messageQueue.take(localId, Constants.CMD_OKAY)
            val remoteId = message.arg0
            return SdbStream(messageQueue, sdbWriter, maxPayloadSize, localId, remoteId)
        } catch (e: Throwable) {
            messageQueue.stopListening(localId)
            throw e
        }
    }

    fun supportsFeature(feature: String): Boolean {
        return supportedFeatures.contains(feature)
    }

    fun connectionString(): Set<String> {
        return supportedFeatures
    }

    private fun newId(): Int {
        return random.nextInt()
    }

    @TestOnly
    internal fun ensureEmpty() {
        messageQueue.ensureEmpty()
    }

    override fun close() {
        try {
            messageQueue.close()
            sdbWriter.close()
            closeable?.close()
        } catch (ignore: Throwable) {}
    }

    companion object {

        fun connect(socket: Socket, keyPair: AdbKeyPair? = null): SdbConnection {
            val source = socket.source()
            val sink = socket.sink()
            return connect(source, sink, keyPair, socket)
        }

        private fun connect(source: Source, sink: Sink, keyPair: AdbKeyPair? = null, closeable: Closeable? = null): SdbConnection {
            val sdbReader = SdbReader(source)
            val sdbWriter = SdbWriter(sink)

            try {
                return connect(sdbReader, sdbWriter, keyPair, closeable)
            } catch (t: Throwable) {
                sdbReader.close()
                sdbWriter.close()
                throw t
            }
        }

        private fun connect(sdbReader: SdbReader, sdbWriter: SdbWriter, keyPair: AdbKeyPair?, closeable: Closeable?): SdbConnection {
            sdbWriter.writeConnect()

            var message = sdbReader.readMessage()

            if (message.command == Constants.CMD_AUTH) {
                checkNotNull(keyPair) { "Authentication required but no KeyPair provided" }
                check(message.arg0 == Constants.AUTH_TYPE_TOKEN) { "Unsupported auth type: $message" }

                val signature = keyPair.signPayload(message)
                sdbWriter.writeAuth(Constants.AUTH_TYPE_SIGNATURE, signature)
                message = sdbReader.readMessage()
                if (message.command == Constants.CMD_AUTH) {
                    keyPair.getAdbPublicKeyPayload()
                        ?.let { sdbWriter.writeAuth(Constants.AUTH_TYPE_RSA_PUBLIC, it) }
                    message = sdbReader.readMessage()
                }
            }

            if (message.command != Constants.CMD_CNXN) throw IOException("Connection failed: $message")

            val connectionString = parseConnectionString(String(message.payload))
//            Log.d("TIZEN", "STRING: ${message.payload.decodeToString()}")
            val version = message.arg0
            val maxPayloadSize = message.arg1

            return SdbConnection(sdbReader, sdbWriter, closeable, connectionString.features, version, maxPayloadSize)
        }

        // ie: "device::ro.product.name=sdk_gphone_x86;ro.product.model=Android SDK built for x86;ro.product.device=generic_x86;features=fixed_push_symlink_timestamp,apex,fixed_push_mkdir,stat_v2,abb_exec,cmd,abb,shell_v2"
        private fun parseConnectionString(connectionString: String): ConnectionStringTizen {
            val keyValues = connectionString.substringAfter("device::")
                .split(";")
                .map { it.split("=") }
                .mapNotNull { if (it.size != 2) null else it[0] to it[1] }
                .toMap()
            val features = setOf(connectionString.substring(connectionString.indexOf("device::")+8, 15))
            return ConnectionStringTizen(features)
        }
    }
}

private data class ConnectionStringTizen(val features: Set<String>)