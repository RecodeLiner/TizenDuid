package com.wearsoft.tizenduid

import okio.Buffer
import okio.BufferedSource

internal class SdbMessage(
    val command: Int,
    val arg0: Int,
    val arg1: Int,
    val payloadLength: Int,
    val payload: ByteArray
) {

    override fun toString() = "${commandStr()}[${argStr(arg0)}, ${argStr(arg1)}] ${payloadStr()}"

    private fun payloadStr(): String {
        if (payloadLength == 0) return ""
        return when (command) {
            Constants.CMD_AUTH -> if (arg0 == Constants.AUTH_TYPE_RSA_PUBLIC) String(payload) else "auth[${payloadLength}]"
            Constants.CMD_WRTE -> writePayloadStr()
            Constants.CMD_OPEN -> String(payload, 0, payloadLength - 1)
            else -> "payload[$payloadLength]"
        }
    }

    private fun writePayloadStr(): String {
        shellPayloadStr()?.let { return it }
        return  "payload[$payloadLength]"
    }

    @Suppress("UsePropertyAccessSyntax")
    private fun shellPayloadStr(): String? {
        val source: BufferedSource = getSource()
        if (source.buffer.size < 5) return null
        val id = source.readByte().toInt()
        if (id < 0 || id > 3) return null
        val length = source.readIntLe()
        if (length != source.buffer.size.toInt()) return null
        if (id == 3) return "[shell] exit(${source.readByte()})"
        val payload = String(payload, 5, payloadLength - 5)
        return "[shell] $payload"
    }

    private fun getSource(): BufferedSource {
        return Buffer().apply { write(payload, 0, payloadLength) }
    }

    private fun argStr(arg: Int) = String.format("%X", arg)

    private fun commandStr() = when (command) {
        Constants.CMD_AUTH -> "AUTH"
        Constants.CMD_CNXN -> "CNXN"
        Constants.CMD_OPEN -> "OPEN"
        Constants.CMD_OKAY -> "OKAY"
        Constants.CMD_CLSE -> "CLSE"
        Constants.CMD_WRTE -> "WRTE"
        else -> "????"
    }
}