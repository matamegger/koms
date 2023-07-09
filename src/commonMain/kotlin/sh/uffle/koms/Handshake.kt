package sh.uffle.koms

import sh.uffle.koms.client.CLIENT_VERSION
import sh.uffle.koms.server.SERVER_VERSION
import sh.uffle.koms.socket.Socket
import sh.uffle.koms.socket.readOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val KOMS: ByteArray = "KOMS".toByteArray()
internal val SERVER_HANDSHAKE = KOMS.toMutableList().plus(SERVER_VERSION.asByteList()).toByteArray()
internal val CLIENT_HANDSHAKE = KOMS.toMutableList().plus(CLIENT_VERSION.asByteList()).toByteArray()

internal sealed class HandshakeResult {
    object Failure : HandshakeResult()
    data class Success(val remoteVersion: Int) : HandshakeResult()
}

internal interface Handshake {
    suspend fun handshake(socket: Socket): HandshakeResult
}

internal class ServerHandshakeWithClientFirstMove(private val timeout: Duration = 1.seconds) : Handshake {
    override suspend fun handshake(socket: Socket): HandshakeResult {
        val clientVersion = socket.receiveHandshake(timeout)
        if (runCatching { socket.write(SERVER_HANDSHAKE) }.getOrDefault(0) < 8) {
            return HandshakeResult.Failure
        }
        return if (clientVersion != null) HandshakeResult.Success(clientVersion) else HandshakeResult.Failure
    }
}

internal class ClientHandshakeWithClientFirstMove(private val timeout: Duration = 1.seconds) : Handshake {
    override suspend fun handshake(socket: Socket): HandshakeResult {
        if (runCatching { socket.write(CLIENT_HANDSHAKE) }.getOrDefault(0) < 8) {
            return HandshakeResult.Failure
        }
        val serverVersion = runCatching { socket.receiveHandshake(timeout) }.getOrNull()

        return if (serverVersion != null) HandshakeResult.Success(serverVersion) else HandshakeResult.Failure
    }
}

private suspend fun Socket.receiveHandshake(timeout: Duration) = readOrNull(8, timeout)
    ?.takeIf {
        it.take(4).toByteArray().contentEquals(KOMS)
    }
    ?.takeLast(4)?.asInt()
