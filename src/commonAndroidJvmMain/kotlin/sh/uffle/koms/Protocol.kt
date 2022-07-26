package sh.uffle.koms

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface Protocol {
    val serverHandshake: Handshake
    val clientHandshake: Handshake
}

internal class DefaultProtocol(
    timeout: Duration = 1.seconds
) : Protocol {
    override val serverHandshake: Handshake = ServerHandshake(timeout)
    override val clientHandshake: Handshake = ClientHandshake(timeout)
}