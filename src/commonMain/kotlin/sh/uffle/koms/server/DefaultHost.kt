package sh.uffle.koms.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.uffle.koms.ClientFirstProtocol
import sh.uffle.koms.Data
import sh.uffle.koms.DefaultBaseKom
import sh.uffle.koms.DefaultScopeProvider
import sh.uffle.koms.Handshake
import sh.uffle.koms.Message
import sh.uffle.koms.Protocol
import sh.uffle.koms.ScopeProvider
import sh.uffle.koms.socket.DefaultKomServerSocket
import sh.uffle.koms.socket.DefaultServerSocket

private val protocol = ClientFirstProtocol()

internal actual class DefaultHost actual constructor(
    private val port: Int,
    private val host: String?,
    private val handshake: Handshake,
    coroutineDispatcher: CoroutineDispatcher,
) : Host {
    private val scopeProvider: ScopeProvider = DefaultScopeProvider(coroutineDispatcher)

    private val bundleLock = Mutex()
    private var bundle: HostBundle? = null

    override val sessions: List<String>
        get() = bundle?.komManager?.activeIds ?: emptyList()

    private val _messages = MutableSharedFlow<Message>()
    override val messages: SharedFlow<Message> = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<KomEvent>()
    override val events: SharedFlow<KomEvent> = _events.asSharedFlow()

    override val isRunning: Boolean get() = bundle?.socket?.isOpen == true

    override suspend fun start() {
        bundleLock.withLock {
            val socket = DefaultServerSocket(port, host)
            socket.open()
            if (!socket.isOpen) {
                throw IllegalStateException()
            }
            val komSocket = DefaultKomServerSocket(socket, handshake)

            val komManager = KomManager(scopeProvider) { DefaultBaseKom(it) }

            bundle = HostBundle(
                socket,
                KomGate(komSocket, komManager, scopeProvider),
                komManager,
                protocol,
                scopeProvider.createScope(),
            ).apply {
                scope.launch {
                    komManager.events
                        .collect {
                            when (it) {
                                is KomManagerEvent.KomDisconnected -> _events.emit(KomEvent.Disconnected(it.id))
                                is KomManagerEvent.KomConnected -> _events.emit(KomEvent.Connected(it.id))
                                is KomManagerEvent.Disposed -> {
                                    bundle = null
                                    _events.emit(KomEvent.HostStopped)
                                    scope.cancel()
                                }
                            }
                        }
                }
                scope.launch {
                    komManager.messages.collect { _messages.emit(it) }
                }
                komGate.open()
            }
        }
    }

    override suspend fun stop() {
        bundle?.run {
            _events.emit(KomEvent.HostStopping)
            socket.close()
            komGate.dispose()
            komManager.dispose()
        }
    }

    override suspend fun send(ids: List<String>, data: Data) {
        bundle?.komManager?.run {
            ids.forEach { id -> get(id)?.send(data) }
        }
    }

    override suspend fun disconnect(id: String) {
        bundle?.komManager?.stop(id)
    }
}

private data class HostBundle(
    val socket: DefaultServerSocket,
    val komGate: KomGate,
    val komManager: KomManager,
    val protocol: Protocol,
    val scope: CoroutineScope,
)
