package sh.uffle.koms.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import sh.uffle.koms.Data
import sh.uffle.koms.Handshake
import sh.uffle.koms.Message
import sh.uffle.koms.ServerHandshakeWithClientFirstMove

const val SERVER_VERSION: Int = 0

fun Host(
    port: Int,
    host: String? = null,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Host = DefaultHost(port, host, ServerHandshakeWithClientFirstMove(), coroutineDispatcher)

interface Host {
    val sessions: List<String>
    val messages: SharedFlow<Message>
    val events: SharedFlow<KomEvent>
    val isRunning: Boolean

    suspend fun start()
    suspend fun stop()
    suspend fun send(ids: List<String>, data: Data)

    suspend fun disconnect(id: String)
}

suspend fun Host.disconnect(ids: List<String>) = ids.forEach { disconnect(it) }

suspend fun <R> Host.sequentialMessaging(
    receiver: String,
    block: suspend (SequentialMessagingBlock.() -> R),
): R = sequentialMessaging(listOf(receiver), block)

suspend fun <R> Host.sequentialMessaging(receiver: List<String>, block: suspend (SequentialMessagingBlock.() -> R)): R {
    return SequentialMessagingBlock(this, receiver).block()
}

suspend fun Host.send(data: Data) = send(sessions, data)

suspend fun Host.send(id: String, data: Data) = send(listOf(id), data)

class SequentialMessagingBlock(internal val host: Host, internal val ids: List<String>)

suspend fun SequentialMessagingBlock.receive() = host.messages.filter { it.sender in ids }.first()

suspend fun SequentialMessagingBlock.send(data: Data) = host.send(ids, data)

internal expect class DefaultHost(
    port: Int,
    host: String?,
    handshake: Handshake,
    coroutineDispatcher: CoroutineDispatcher,
) : Host
