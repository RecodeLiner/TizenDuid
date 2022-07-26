package com.wearsoft.tizenduid

import java.io.IOException

class SdbShellStream(
    private val stream: SdbStream
) : AutoCloseable {

    @Throws(IOException::class)
    fun readAll(): String {
        return stream.source.readByteArray().decodeToString()
    }

    @Throws(IOException::class)
    fun read(): SdbShellPacket {
        stream.source.apply {
            val id = checkId(readByte().toInt())
            val length = checkLength(id, readIntLe())
            val payload = readByteArray(length.toLong())
            return SdbShellPacket(id, payload)
        }
    }

    @Throws(IOException::class)
    fun write(string: String) {
        write(ID_STDIN, string.toByteArray())
    }

    @Throws(IOException::class)
    fun write(id: Int, payload: ByteArray? = null) {
        stream.sink.apply {
            writeByte(id)
            writeIntLe(payload?.size ?: 0)
            if (payload != null) write(payload)
            flush()
        }
    }

    override fun close() {
        stream.close()
    }

    private fun checkId(id: Int): Int {
        check(id == ID_STDOUT || id == ID_STDERR || id == ID_EXIT) {
            "Invalid shell packet id: $id"
        }
        return id
    }

    private fun checkLength(id: Int, length: Int): Int {
        check(length >= 0) { "Shell packet length must be >= 0: $length" }
        check(id != ID_EXIT || length == 1) { "Shell exit packet does not have payload length == 1: $length" }
        return length
    }
}

class SdbShellPacket(
    val id: Int,
    val payload: ByteArray
) {

    override fun toString() = "${idStr(id)}: ${payloadStr(id, payload)}"

    companion object {

        private fun idStr(id: Int) = when(id) {
            ID_STDOUT -> "STDOUT"
            ID_STDERR -> "STDERR"
            ID_EXIT -> "EXIT"
            else -> throw IllegalArgumentException("Invalid shell packet id: $id")
        }

        private fun payloadStr(id: Int, payload: ByteArray) = if (id == ID_EXIT) {
            "${payload[0]}"
        } else {
            String(payload)
        }
    }
}

class SdbShellResponse(
    val output: String,
    val errorOutput: String,
    val exitCode: Int
) {

    val allOutput: String by lazy { "$output$errorOutput" }

    override fun toString() = "Shell response ($exitCode):\n$allOutput"
}
