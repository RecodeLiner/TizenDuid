package com.wearsoft.tizenduid

import org.jetbrains.annotations.TestOnly
import java.net.Socket

internal class DsdbImpl(
    private val host: String,
    private val port: Int,
    private val keyPair: AdbKeyPair? = null
) : Dsdb {

    var model = ""

    private var connection: Pair<SdbConnection, Socket>? = null

    override fun open(destination: String) = connection().open(destination)

    override fun supportsFeature(feature: String): Boolean {
        return connection().supportsFeature(feature)
    }

    override fun connectionString(): Set<String> {
        return connection().connectionString()
    }

    override fun close() {
        connection?.first?.close()
    }
    override fun toString() = "$host:$port"

    @TestOnly
    fun closeConnection() {
        connection?.second?.close()
    }

    @Synchronized
    private fun connection(): SdbConnection {
        var connection = connection
        if (connection == null || connection.second.isClosed) {
            connection = newConnection()
            this.connection = connection
        }
        return connection.first
    }

    private fun newConnection(): Pair<SdbConnection, Socket> {
        val socket = Socket(host, port)
        val sdbConnection = SdbConnection.connect(socket, keyPair)
        return sdbConnection to socket
    }
}
