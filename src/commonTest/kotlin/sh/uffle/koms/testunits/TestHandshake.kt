package sh.uffle.koms.testunits

import sh.uffle.koms.Handshake
import sh.uffle.koms.HandshakeResult
import sh.uffle.koms.socket.Socket

internal class TestHandshake : Handshake {
    var onHandshake: (Socket) -> Int? = { _ -> 1 }

    override suspend fun handshake(socket: Socket): HandshakeResult {
        return onHandshake(socket)?.let {
            HandshakeResult.Success(it)
        } ?: HandshakeResult.Failure
    }
}
