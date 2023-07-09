package sh.uffle.koms

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface Protocol {
    val serverHandshake: Handshake
    val clientHandshake: Handshake
}

internal class ClientFirstProtocol(
    timeout: Duration = 1.seconds,
) : Protocol {
    override val serverHandshake: Handshake = ServerHandshakeWithClientFirstMove(timeout)
    override val clientHandshake: Handshake = ClientHandshakeWithClientFirstMove(timeout)
}
