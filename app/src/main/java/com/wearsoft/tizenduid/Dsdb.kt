package com.wearsoft.tizenduid

import java.io.File
import java.io.IOException

interface Dsdb : AutoCloseable {

    @Throws(IOException::class)
    fun open(destination: String): SdbStream

    fun supportsFeature(feature: String): Boolean

    fun connectionString(): Set<String>

    @Throws(IOException::class)
    fun shell(command: String): String {
        openShell(command).use { stream ->
            return stream.readAll()
        }
    }

    @Throws(IOException::class)
    fun openShell(command: String = ""): SdbShellStream {
        val stream = open("shell:$command")
        return SdbShellStream(stream)
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun create(dataDir: File, host: String, port: Int, keyPair: AdbKeyPair? = AdbKeyPair.readDefault(dataDir)): Dsdb = DsdbImpl(host, port, keyPair)

    }
}