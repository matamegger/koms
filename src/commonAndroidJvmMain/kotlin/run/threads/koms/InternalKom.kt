package run.threads.koms

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import run.threads.koms.cosocket.Socket

sealed class ConnectionState {
    object Connecting : ConnectionState()
    class Connected(val remoteVersion: Int) : ConnectionState()
    object Disconnected : ConnectionState()
}

internal class InternalKom(
    private val handshake: Handshake,
    scopeProvider: ScopeProvider
) {
    private lateinit var socket: Socket
    private val scope = scopeProvider.createScope("InternalKom")
    private val _status = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    private val _outputQueue = Channel<Data>()
    private val _receivedData = MutableSharedFlow<Data>()

    val status: StateFlow<ConnectionState> = _status.asStateFlow()
    val messages: SharedFlow<Data> = _receivedData.asSharedFlow()

    fun setSocket(socket: Socket) {
        this.socket = socket
        scope.launch {
            try {
                when (val result = handshake.handshake(socket)) {
                    is HandshakeResult.Success -> {
                        connected(result.remoteVersion)
                    }
                    is HandshakeResult.Failed -> {
                        disconnect()
                    }
                }
            } catch (e: Throwable) {
                disconnect()
                if (e is CancellationException) throw e
            }
        }
    }

    private fun connected(remoteVersion: Int) {
        _status.update { ConnectionState.Connected(remoteVersion) }
        startMessageProcessing()
    }

    private fun startMessageProcessing() {
        scope.launch {
            try {
                while (isActive) {
                    val data = socket.readDataOrNull() ?: break
                    _receivedData.emit(data)
                }
            } finally {
                disconnect()
            }
        }
        _outputQueue.consumeAsFlow().onEach {
            try {
                with(socket) {
                    var wrote = write(it.bytes.size.asByteArray())
                    if (wrote < 0) disconnect()
                    wrote = write(it.bytes)
                    if (wrote < 0) disconnect()
                }
            } catch (e: Throwable) {
                disconnect()
                if (e is CancellationException) throw e
            }
        }.launchIn(scope)
    }

    fun disconnect() {
        scope.cancel()
        socket.close()
        _status.update { ConnectionState.Disconnected }
    }

    fun send(data: Data) {
        scope.launch { _outputQueue.send(data) }
    }
}

