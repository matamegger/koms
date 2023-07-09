package sh.uffle.koms.socket

import sh.uffle.koms.Data
import sh.uffle.koms.Handshake
import sh.uffle.koms.HandshakeResult
import sh.uffle.koms.asByteArray
import sh.uffle.koms.asInt
import kotlin.time.Duration

internal class DefaultKomSocket(
    private val socket: Socket,
    private val handshake: Handshake,
) : KomSocket {
    override val isOpen
        get() = socket.isConnected && remoteVersion != null
    override var remoteVersion: Int? = null

    /**
     * Establishes the Kom session i.e. performs the handshake on an already
     * open [Socket]
     */
    suspend fun connect(): Boolean {
        remoteVersion = runCatching { handleHandshake() }.getOrNull()
        return if (remoteVersion == null) {
            close()
            false
        } else {
            true
        }
    }

    override suspend fun connect(port: Int, host: String): Boolean {
        runCatching { socket.connect(port, host) }
        if (!socket.isConnected) {
            close()
            return false
        }
        return connect()
    }

    private suspend fun handleHandshake(): Int? = when (val result = handshake.handshake(socket)) {
        is HandshakeResult.Success -> result.remoteVersion
        is HandshakeResult.Failure -> null
    }

    override suspend fun read(
        timeout: Duration,
    ): Data? = socket.readDataOrNull(timeout)

    override suspend fun write(data: Data): Boolean {
        return runCatching {
            socket.write(data.bytes.size.asByteArray()) == 4 &&
                socket.write(data.bytes) == data.bytes.size
        }.getOrDefault(false)
    }

    override fun close() {
        remoteVersion = null
        runCatching(socket::close)
    }
}

private suspend fun Socket.readDataOrNull(timeout: Duration): Data? {
    val sizeBuffer = readOrNull(4, timeout) ?: return null

    val size = sizeBuffer.asInt()
    val data = readOrNull(size, timeout) ?: return null

    return Data(data)
}
