package sh.uffle.koms.client

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import sh.uffle.koms.Data
import sh.uffle.koms.Message

const val CLIENT_VERSION: Int = 0

fun Kom(coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO): Kom = DefaultKom(coroutineDispatcher)

interface Kom {
    val komState: StateFlow<KomState>
    val messages: SharedFlow<Message>
    suspend fun connect(port: Int, host: String)
    suspend fun disconnect()
    suspend fun send(data: Data)
}

enum class KomState {
    Disconnected,
    Connecting,
    Connected,
}


