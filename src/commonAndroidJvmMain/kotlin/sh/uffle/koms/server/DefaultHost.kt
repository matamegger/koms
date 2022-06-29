package sh.uffle.koms.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import sh.uffle.koms.*
import sh.uffle.koms.cosocket.ServerSocket
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

private val protocol = DefaultProtocol()

internal actual class DefaultHost actual constructor(
    private val port: Int,
    private val host: String?,
    coroutineDispatcher: CoroutineDispatcher
) : Host {
    private val scopeProvider: ScopeProvider = DefaultScopeProvider(coroutineDispatcher)

    private val bundleLock = ReentrantReadWriteLock()
    private var bundle: HostBundle? = null

    override val sessions: List<String>
        get() = bundle?.komManager?.activeIds ?: emptyList()

    private val _messages = MutableSharedFlow<Message>()
    override val messages: SharedFlow<Message> = _messages.asSharedFlow()

    private val _events = MutableSharedFlow<KomEvent>()
    override val events: SharedFlow<KomEvent> = _events.asSharedFlow()

    override val isRunning: Boolean get() = bundle?.socket?.isOpen == true

    override fun start() {
        bundleLock.write {
            val socket = ServerSocket(port, host)
            socket.open()
            if (socket.isOpen) {
                //TODO started
            }
            val komManager = KomManager(scopeProvider, protocol)

            bundle = HostBundle(
                socket,
                KomGate(socket, komManager, scopeProvider),
                komManager,
                protocol,
                scopeProvider.createScope(),
            ).apply {
                scope.launch {
                    komManager.events.collect { _events.emit(it) }
                }
                scope.launch {
                    komManager.messages.collect { _messages.emit(it) }
                }
                komGate.open()
            }
        }
    }

    override fun stop() {
        bundle?.run {
            scope.cancel()
            socket.close()
            komGate.dispose()
            komManager.dispose()
        }
        bundle = null
    }

    override fun send(ids: List<String>, data: Data) {
        bundle?.run {
            scope.launch {
                ids.forEach { id -> komManager.get(id)?.send(data) }
            }
        }
    }

    override fun disconnect(id: String) {
        bundle?.run {
            scope.launch { komManager.stop(id) }
        }
    }
}

private data class HostBundle(
    val socket: ServerSocket,
    val komGate: KomGate,
    val komManager: KomManager,
    val protocol: Protocol,
    val scope: CoroutineScope
)