package sh.uffle.koms.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.uffle.koms.*
import sh.uffle.koms.socket.DefaultSocket

internal class DefaultKom constructor(coroutineDispatcher: CoroutineDispatcher) : Kom {
    private val scopeProvider: ScopeProvider = DefaultScopeProvider(coroutineDispatcher)

    private val bundleLock = Mutex()
    private var bundle: Bundle? = null

    private val _komState = MutableStateFlow(KomState.Disconnected)
    override val komState: StateFlow<KomState> = _komState.asStateFlow()

    private val _messages = MutableSharedFlow<Message>()
    override val messages: SharedFlow<Message> = _messages.asSharedFlow()

    override suspend fun connect(port: Int, host: String) {
        bundleLock.withLock {
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
                kom.setSocket(
                    DefaultSocket().apply { connect(port, host) }
                )
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
                    is ConnectionState.Connecting -> _komState.update { KomState.Connecting }
                    is ConnectionState.Connected -> _komState.update { KomState.Connected }
                    is ConnectionState.Disconnected -> {
                        _komState.update { KomState.Disconnected }
                        disconnect()
                    }
                }
            }
        }
    }

    override suspend fun disconnect() {
        bundleLock.withLock {
            bundle?.kom?.disconnect()
            bundle?.scope?.cancel()
            bundle = null
        }
    }

    override suspend fun send(data: Data) {
        bundle?.run { scope.launch { kom.send(data) } }
    }
}

private data class Bundle(
    val kom: InternalKom,
    val scope: CoroutineScope
)