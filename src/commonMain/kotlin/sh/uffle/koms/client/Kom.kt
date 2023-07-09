package sh.uffle.koms.client

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import sh.uffle.koms.ClientHandshakeWithClientFirstMove
import sh.uffle.koms.Data
import sh.uffle.koms.DefaultBaseKom
import sh.uffle.koms.Message
import sh.uffle.koms.socket.DefaultKomSocket
import sh.uffle.koms.socket.DefaultSocket

const val CLIENT_VERSION: Int = 0

fun Kom(): Kom = DefaultKom {
    DefaultBaseKom(
        DefaultKomSocket(
            DefaultSocket(),
            ClientHandshakeWithClientFirstMove(),
        ),
    )
}

interface Kom {
    val komState: StateFlow<KomState>
    val messages: SharedFlow<Message>
    suspend fun connect(port: Int, host: String)
    suspend fun disconnect()
    suspend fun send(data: Data)
}

suspend fun <R> Kom.sequentialMessaging(block: suspend (SequentialMessagingBlock.() -> R)): R {
    return SequentialMessagingBlock(this).block()
}

class SequentialMessagingBlock(internal val kom: Kom)

suspend fun SequentialMessagingBlock.receive() = kom.messages.first()

suspend fun SequentialMessagingBlock.send(data: Data) = kom.send(data)

enum class KomState {
    Disconnected,
    Connecting,
    Connected,
}
