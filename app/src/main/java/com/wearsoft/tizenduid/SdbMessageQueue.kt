package com.wearsoft.tizenduid

internal class SdbMessageQueue(private val sdbReader: SdbReader) : AutoCloseable, MessageQueue<SdbMessage>() {

    override fun readMessage() = sdbReader.readMessage()

    override fun getLocalId(message: SdbMessage) = message.arg1

    override fun getCommand(message: SdbMessage) = message.command

    override fun close() = sdbReader.close()

    override fun isCloseCommand(message: SdbMessage) = message.command == Constants.CMD_CLSE
}