package com.wearsoft.tizenduid

import android.util.Log
import okio.Source
import okio.buffer

internal class SdbReader(source: Source) : AutoCloseable {

    private val bufferedSource = source.buffer()

    fun readMessage(): SdbMessage {
        synchronized(bufferedSource) {
            bufferedSource.apply {
                val command = readIntLe()
                val arg0 = readIntLe()
                val arg1 = readIntLe()
                val payloadLength = readIntLe()
                readIntLe()
                readIntLe()
                val payload = readByteArray(payloadLength.toLong())
                Log.d("RUMA2", "REP: $command, $arg0, $arg1")
                return SdbMessage(command, arg0, arg1, payloadLength, payload).also {
                    logging { "(${Thread.currentThread().name}) < $it" }
                }
            }
        }
    }

    override fun close() {
        bufferedSource.close()
    }

     private fun logging(block: () -> String) {
        if (System.getenv("DADB_LOGGING").toBoolean()) {
            println(block())
        }
    }
}