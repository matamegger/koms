package sh.uffle.koms

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import sh.uffle.koms.socket.KomSocket
import kotlin.time.Duration.Companion.seconds

internal interface BaseKom {
    val status: StateFlow<ConnectionState>

    suspend fun connect(port: Int, host: String)
    suspend fun send(data: Data)
    suspend fun read(): Data?
    fun disconnect()
}

internal class DefaultBaseKom(
    private val socket: KomSocket,
) : BaseKom {
    private val _status = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val status = _status.asStateFlow()

    init {
        _status.update { if (socket.isOpen) ConnectionState.Connected(socket.remoteVersion!!) else ConnectionState.Disconnected }
    }

    override suspend fun connect(port: Int, host: String) {
        val previousState =
            _status.getAndUpdate { if (it != ConnectionState.Disconnected) it else ConnectionState.Connecting }
        if (previousState != ConnectionState.Disconnected) return
        if (!socket.connect(port, host)) {
            disconnect()
            return
        }
        _status.update { ConnectionState.Connected(socket.remoteVersion!!) }
    }

    override suspend fun send(data: Data) {
        if (!socket.write(data)) disconnect()
    }

    override suspend fun read(): Data? = socket.read(1.0.seconds).also { it ?: disconnect() }

    override fun disconnect() {
        socket.close()
        _status.update { ConnectionState.Disconnected }
    }
}
