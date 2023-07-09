package sh.uffle.koms.socket

import kotlinx.coroutines.ensureActive
import sh.uffle.koms.Handshake
import kotlin.coroutines.coroutineContext

internal class DefaultKomServerSocket(
    private val serverSocket: ServerSocket,
    private val handshake: Handshake,
) : KomServerSocket {
    override val port = serverSocket.port
    override val host = serverSocket.host
    override val isOpen = serverSocket.isOpen

    override suspend fun accept(): KomSocket {
        while (true) {
            coroutineContext.ensureActive()
            val baseKom = runCatching { serverSocket.accept() }
                .getOrNull()
                ?.let { socket ->
                    DefaultKomSocket(socket, handshake)
                } ?: continue
            if (!baseKom.connect()) continue
            return baseKom
        }
    }

    override fun close() {
        runCatching { serverSocket.close() }
    }
}
