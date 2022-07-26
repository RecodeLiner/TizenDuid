package com.wearsoft.tizenduid

import okio.*
import java.lang.Integer.min
import java.nio.ByteBuffer

class SdbStream internal constructor(
    private val messageQueue: SdbMessageQueue,
    private val sdbWriter: SdbWriter,
    private val maxPayloadSize: Int,
    val localId: Int,
    val remoteId: Int
) : AutoCloseable {

    var isClosed = false

    val source = object : Source {

        private var message: SdbMessage? = null
        private var bytesRead = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            val message = message() ?: return -1

            val bytesRemaining = message.payloadLength - bytesRead
            val bytesToRead = Math.min(byteCount.toInt(), bytesRemaining)

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
            val nextMessage = nextMessage(Constants.CMD_WRTE)
            message = nextMessage
            bytesRead = 0
            return nextMessage
        }

        override fun close() {}

        override fun timeout() = Timeout.NONE
    }.buffer()

    val sink = object : Sink {

        private val buffer = ByteBuffer.allocate(maxPayloadSize)

        override fun write(source: Buffer, byteCount: Long) {
            var remainingBytes = byteCount
            while (true) {
                remainingBytes -= writeToBuffer(source, byteCount)
                if (remainingBytes == 0L) return
                check(remainingBytes > 0L)
            }
        }

        private fun writeToBuffer(source: BufferedSource, byteCount: Long): Int {
            val bytesToWrite = min(buffer.remaining(), byteCount.toInt())
            val bytesWritten = source.read(buffer.array(), buffer.position(), bytesToWrite)

            buffer.position(buffer.position() + bytesWritten)
            if (buffer.remaining() == 0) {
                flush()
            }

            return bytesWritten
        }

        override fun flush() {
            sdbWriter.writeWrite(localId, remoteId, buffer.array(), 0, buffer.position())
            buffer.clear()
        }

        override fun close() {}

        override fun timeout() = Timeout.NONE
    }.buffer()

    private fun nextMessage(command: Int): SdbMessage? {
        return try {
            messageQueue.take(localId, command)
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