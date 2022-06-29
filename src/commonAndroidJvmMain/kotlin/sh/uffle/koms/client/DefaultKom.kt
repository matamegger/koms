package sh.uffle.koms.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sh.uffle.koms.*
import sh.uffle.koms.cosocket.Socket
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

internal actual class DefaultKom actual constructor(coroutineDispatcher: CoroutineDispatcher) : Kom {
    private val scopeProvider: ScopeProvider = DefaultScopeProvider(coroutineDispatcher)

    private val bundleLock = ReentrantReadWriteLock(true)
    private var bundle: Bundle? = null

    private val _komState = MutableStateFlow(KomState.Disconnected)
    override val komState: StateFlow<KomState> = _komState.asStateFlow()

    private val _messages = MutableSharedFlow<Message>()
    override val messages: SharedFlow<Message> = _messages.asSharedFlow()

    override fun connect(port: Int, host: String) {
        bundleLock.write {
            if (bundle != null) return
            _komState.update { KomState.Connecting }
            bundle = Bundle(
                InternalKom(
                    DefaultProtocol().clientHandshake,
                    scopeProvider,
                ),
                scopeProvider.createScope()
            ).apply {
                setupRelays()
                scope.launch {
                    val socket = Socket().apply {
                        connect(port, host)
                    }
                    kom.setSocket(socket)
                }
            }
        }
    }

    private fun Bundle.setupRelays() {
        scope.launch {
            kom.messages.collect { _messages.emit(Message("server", it)) }
        }
        scope.launch {
            kom.status.collect {
                when (it) {
                    ConnectionState.Connecting -> _komState.update { KomState.Connecting }
                    is ConnectionState.Connected -> _komState.update { KomState.Connected }
                    ConnectionState.Disconnected -> {
                        _komState.update { KomState.Disconnected }
                        disconnect()
                    }
                }
            }
        }
    }

    override fun disconnect() {
        bundleLock.write {
            bundle?.kom?.disconnect()
            bundle?.scope?.cancel()
            bundle = null
        }
    }

    override fun send(data: Data) {
        bundleLock.read {
            bundle?.run { scope.launch { kom.send(data) } }
        }
    }
}

private data class Bundle(
    val kom: InternalKom,
    val scope: CoroutineScope
)