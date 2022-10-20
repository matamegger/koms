package sh.uffle.koms.server

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.uffle.koms.*
import sh.uffle.koms.socket.Socket


internal class KomManager(
    private val scopeProvider: ScopeProvider,
    private val protocol: Protocol
) {
    private val komsLock = Mutex()
    private var koms: Map<String, KomHolder> = mapOf()

    val activeIds
        get() = koms
            .filter { it.value.kom.status.value is ConnectionState.Connected }
            .map { it.key }


    val registeredIds get() = koms.map { it.key }

    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<KomEvent>()
    val events = _events.asSharedFlow()

    suspend fun create(socket: Socket) {
        val komHolder = KomHolder(
            InternalKom(
                protocol.serverHandshake,
                scopeProvider
            ),
            scopeProvider.createScope()
        )
        komsLock.withLock {
            val id = getId()
            komHolder.apply {
                addStatusWatcher(id)
                addMessageRelay(id)
            }
            komHolder.kom.setSocket(socket)
            koms += id to komHolder
        }
    }

    fun stop(id: String) {
        koms[id]?.kom?.disconnect()
    }

    private fun remove(id: String) {
        koms[id]?.run {
            koms = koms - id
            scope.cancel()
            kom.disconnect()
        }
    }

    fun get(id: String): InternalKom? = koms[id]?.kom

    fun dispose() {
        koms.keys.toList().forEach(::remove)
    }

    private fun getId(): String {
        var id: String
        do {
            id = uuid4().toString()
        } while (koms[id] != null)
        return id
    }

    private fun KomHolder.addStatusWatcher(id: String): Job = scope.launch {
        kom.status.collect {
            when (it) {
                is ConnectionState.Disconnected -> {
                    _events.emit(KomEvent.Disconnected(id))
                    remove(id)
                }

                is ConnectionState.Connected -> _events.emit(KomEvent.Connected(id))
                else -> Unit
            }
        }
    }

    private fun KomHolder.addMessageRelay(id: String) = scope.launch {
        kom.messages.collect { _messages.emit(Message(id, it)) }
    }
}

private data class KomHolder(
    val kom: InternalKom,
    val scope: CoroutineScope
)