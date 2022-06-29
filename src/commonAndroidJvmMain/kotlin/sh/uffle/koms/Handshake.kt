package sh.uffle.koms

import CLIENT_HANDSHAKE
import KOMS
import SERVER_HANDSHAKE
import sh.uffle.koms.cosocket.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal sealed class HandshakeResult {
    object Failed : HandshakeResult()
    data class Success(val remoteVersion: Int) : HandshakeResult()
}

internal interface Handshake {
    suspend fun handshake(socket: Socket): HandshakeResult
}

internal class ServerHandshake(private val timeout: Duration = 1.seconds) : Handshake {
    override suspend fun handshake(socket: Socket): HandshakeResult {
        if (socket.write(SERVER_HANDSHAKE) < 0) {
            return HandshakeResult.Failed
        }
        val clientVersion = socket.receiveHandshake()

        return if (clientVersion != null) HandshakeResult.Success(clientVersion) else HandshakeResult.Failed
    }
}

internal class ClientHandshake(private val timeout: Duration = 1.seconds) : Handshake {
    override suspend fun handshake(socket: Socket): HandshakeResult {
        if (socket.write(CLIENT_HANDSHAKE) < 0) {
            return HandshakeResult.Failed
        }
        val serverVersion = socket.receiveHandshake()

        return if (serverVersion != null) HandshakeResult.Success(serverVersion) else HandshakeResult.Failed
    }
}

private suspend fun Socket.receiveHandshake() = readOrNull(8)
    ?.takeIf {
        it.take(4).toByteArray().contentEquals(KOMS)
    }
    ?.takeLast(4)?.asInt()