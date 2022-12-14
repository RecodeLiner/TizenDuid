package com.wearsoft.tizenduid

import android.util.Log
import okio.Sink
import okio.buffer
import java.nio.ByteBuffer

internal class SdbWriter(sink: Sink) : AutoCloseable {

    private val bufferedSink = sink.buffer()

    fun writeConnect() = write(
        Constants.CMD_CNXN,
        Constants.CONNECT_VERSION,
        Constants.CONNECT_MAXDATA,
        Constants.CONNECT_PAYLOAD,
        0,
        Constants.CONNECT_PAYLOAD.size
    )

    fun writeAuth(authType: Int, authPayload: ByteArray) = write(
        Constants.CMD_AUTH,
        authType,
        0,
        authPayload,
        0,
        authPayload.size
    )

    fun writeOpen(localId: Int, destination: String) {
        val buffer = ByteBuffer.allocate(destination.length + 1)
        buffer.put(destination.toByteArray())
        buffer.put(0)
        val payload = buffer.array()
        write(Constants.CMD_OPEN, localId, 0, payload, 0, payload.size)
    }

    fun writeClose(localId: Int, remoteId: Int) {
        write(Constants.CMD_CLSE, localId, remoteId, null, 0, 0)
    }

    fun writeOkay(localId: Int, remoteId: Int) {
        write(Constants.CMD_OKAY, localId, remoteId, null, 0, 0)
    }

    private fun write(
        command: Int,
        arg0: Int,
        arg1: Int,
        payload: ByteArray?,
        offset: Int,
        length: Int
    ) {
        Log.d("RUMA2", "MSG: $command, $arg0, $arg1, $length")
        logging { "(${Thread.currentThread().name}) > ${SdbMessage(
            command,
            arg0,
            arg1,
            length,
            payload ?: ByteArray(0)
        )}" }
        synchronized(bufferedSink) {
            bufferedSink.apply {
                writeIntLe(command)
                writeIntLe(arg0)
                writeIntLe(arg1)
                if (payload == null) {
                    writeIntLe(0)
                    writeIntLe(0)
                } else {
                    writeIntLe(length)
                    writeIntLe(payloadChecksum(payload))
                }
                writeIntLe(command xor -0x1)
                if (payload != null) {
                    write(payload, offset, length)
                }
                flush()
            }
        }
    }

    override fun close() {
        bufferedSink.close()
    }

    private fun logging(block: () -> String) {
        if (System.getenv("DADB_LOGGING").toBoolean()) {
            println(block())
        }
    }

    companion object {

        private fun payloadChecksum(payload: ByteArray): Int {
            var checksum = 0
            for (byte in payload) {
                checksum += byte.toUByte().toInt()
            }
            return checksum
        }
    }
}