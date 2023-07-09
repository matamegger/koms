package sh.uffle.koms.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import sh.uffle.koms.BaseKom
import sh.uffle.koms.ConnectionState
import sh.uffle.koms.Data
import sh.uffle.koms.Message
import kotlin.coroutines.coroutineContext

internal class DefaultKom(private val createBaseKom: () -> BaseKom) : Kom {
    private val komLock = Mutex()
    private var baseKom: BaseKom? = null
    private var scope: CoroutineScope? = null

    private val _messages = MutableSharedFlow<Message>()
    override val messages: SharedFlow<Message> = _messages.asSharedFlow()

    private val _komState = MutableStateFlow(KomState.Disconnected)
    override val komState: StateFlow<KomState> = _komState.asStateFlow()

    override suspend fun connect(port: Int, host: String) {
        komLock.withLock {
            if (baseKom != null) return

            val baseKom = createBaseKom()

            scope = CoroutineScope(coroutineContext + SupervisorJob()).apply {
                setupStateObserver(baseKom)
            }
            baseKom.connect(port, host)
            this.baseKom = baseKom
        }
    }

    override suspend fun disconnect() {
        baseKom?.disconnect()
    }

    override suspend fun send(data: Data) {
        baseKom?.send(data)
    }

    private fun CoroutineScope.setupStateObserver(kom: BaseKom) {
        launch {
            kom.status.collect {
                when (it) {
                    is ConnectionState.Connecting -> _komState.update { KomState.Connecting }
                    is ConnectionState.Connected -> {
                        setupMessageRelay(kom)
                        _komState.update { KomState.Connected }
                    }
                    is ConnectionState.Disconnected -> {
                        _komState.update { KomState.Disconnected }
                        disconnect()
                    }
                }
            }
        }
    }

    private fun CoroutineScope.setupMessageRelay(kom: BaseKom) {
        launch {
            while (coroutineContext.isActive) {
                val message = kom.read() ?: continue
                _messages.emit(Message("server", message))
            }
        }
    }
}
