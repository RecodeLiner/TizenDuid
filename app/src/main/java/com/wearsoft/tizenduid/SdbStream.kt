package com.wearsoft.tizenduid

import okio.*

class SdbStream internal constructor(
    private val messageQueue: SdbMessageQueue,
    private val sdbWriter: SdbWriter,
    val localId: Int,
    val remoteId: Int
) : AutoCloseable {

    private var isClosed = false

    val source = object : Source {

        private var message: SdbMessage? = null
        private var bytesRead = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            val message = message() ?: return -1

            val bytesRemaining = message.payloadLength - bytesRead
            val bytesToRead = byteCount.toInt().coerceAtMost(bytesRemaining)

            sink.write(message.payload, bytesRead, bytesToRead)

            bytesRead += bytesToRead

            check(bytesRead <= message.payloadLength)

            if (bytesRead == message.payloadLength) {
                this.message = null
                sdbWriter.writeOkay(localId, remoteId)
            }

            return bytesToRead.toLong()
        }

        private fun message(): SdbMessage? {
            message?.let { return it }
            val nextMessage = nextMessage()
            message = nextMessage
            bytesRead = 0
            return nextMessage
        }

        override fun close() {}

        override fun timeout() = Timeout.NONE
    }.buffer()

    private fun nextMessage(): SdbMessage? {
        return try {
            messageQueue.take(localId, Constants.CMD_WRTE)
        } catch (e: IOException) {
            close()
            return null
        }
    }

    override fun close() {
        if (isClosed) return
        isClosed = true

        sdbWriter.writeClose(localId, remoteId)
        messageQueue.stopListening(localId)
    }
}