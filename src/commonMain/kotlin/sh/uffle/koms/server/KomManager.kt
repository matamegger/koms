package sh.uffle.koms.server

import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.uffle.koms.BaseKom
import sh.uffle.koms.ConnectionState
import sh.uffle.koms.Message
import sh.uffle.koms.ScopeProvider
import sh.uffle.koms.socket.KomSocket

internal class KomManager(
    private val scopeProvider: ScopeProvider,
    private val createBaseKom: (socket: KomSocket) -> BaseKom,
) {
    private val komsLock = Mutex()
    private var koms: Map<String, KomHolder> = mapOf()
    private var disposed = false

    val activeIds
        get() = koms
            .filter { it.value.kom.status.value is ConnectionState.Connected }
            .map { it.key }

    val registeredIds get() = koms.map { it.key }

    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<KomManagerEvent>()
    val events = _events.asSharedFlow()

    suspend fun create(socket: KomSocket) {
        if (disposed) {
            socket.close()
            return
        }
        val komHolder = KomHolder(
            createBaseKom(socket),
            scopeProvider.createScope(),
        )
        komsLock.withLock {
            val id = getId()
            komHolder.apply {
                addStatusWatcher(id)
                addMessageRelay(id)
            }
            koms += id to komHolder
        }
    }

    fun stop(id: String) {
        koms[id]?.kom?.disconnect()
    }

    private suspend fun remove(id: String) {
        koms[id]?.run {
            koms = koms - id
            if (disposed && koms.isEmpty()) {
                _events.emit(KomManagerEvent.Disposed)
            }
            scope.cancel()
        }
    }

    fun get(id: String): BaseKom? = koms[id]?.kom

    fun dispose() {
        disposed = true
        koms.values.toList().forEach { it.kom.disconnect() }
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
                    _events.emit(KomManagerEvent.KomDisconnected(id))
                    remove(id)
                }

                is ConnectionState.Connected -> _events.emit(KomManagerEvent.KomConnected(id))
                else -> Unit
            }
        }
    }

    private fun KomHolder.addMessageRelay(id: String) = scope.launch {
        while (coroutineContext.isActive) {
            val message = kom.read() ?: continue
            _messages.emit(Message(id, message))
        }
    }
}

private data class KomHolder(
    val kom: BaseKom,
    val scope: CoroutineScope,
)

internal sealed interface KomManagerEvent {
    data class KomConnected(val id: String) : KomManagerEvent
    data class KomDisconnected(val id: String) : KomManagerEvent
    object Disposed : KomManagerEvent
}
