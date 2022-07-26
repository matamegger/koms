package sh.uffle.koms.server

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import sh.uffle.koms.*
import sh.uffle.koms.cosocket.Socket
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


internal class KomManager(
    private val scopeProvider: ScopeProvider,
    private val protocol: Protocol
) {
    private val komsLock = ReentrantReadWriteLock()
    private val koms: MutableMap<String, KomHolder> = mutableMapOf()

    val activeIds
        get() = komsLock.read {
            koms
                .filter { it.value.kom.status.value is ConnectionState.Connected }
                .map { it.key }
        }

    val registeredIds get() = komsLock.read { koms.map { it.key } }

    private val _messages = MutableSharedFlow<Message>()
    val messages = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<KomEvent>()
    val events = _events.asSharedFlow()

    fun create(socket: Socket) {
        val komHolder = KomHolder(
            InternalKom(
                protocol.serverHandshake,
                scopeProvider
            ),
            scopeProvider.createScope()
        )
        komsLock.write {
            val id = getId()
            komHolder.apply {
                addStatusWatcher(id)
                addMessageRelay(id)
            }
            komHolder.kom.setSocket(socket)
            koms[id] = komHolder
        }
    }

    fun stop(id: String) {
        komsLock.read { koms[id]?.kom?.disconnect() }
    }

    private fun remove(id: String) {
        komsLock.write {
            koms.remove(id)?.run {
                scope.cancel()
                kom.disconnect()
            }
        }
    }

    fun get(id: String): InternalKom? = komsLock.read { koms[id]?.kom }

    fun dispose() {
        komsLock.read { koms.keys.toList() }.forEach(::remove)
    }

    private fun getId(): String {
        var id: String
        do {
            id = UUID.randomUUID().toString()
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