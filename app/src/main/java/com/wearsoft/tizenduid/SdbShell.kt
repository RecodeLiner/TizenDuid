package com.wearsoft.tizenduid

import java.io.IOException

class SdbShell(
    private val stream: SdbStream
) : AutoCloseable {

    @Throws(IOException::class)
    fun readAll(): String {
        return stream.source.readByteArray().decodeToString()
    }

    override fun close() {
        stream.close()
    }

}

