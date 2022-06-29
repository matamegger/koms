package run.threads.koms.server

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import run.threads.koms.Data
import run.threads.koms.Message

const val SERVER_VERSION: Int = 0

fun Host(
    port: Int,
    host: String? = null,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
): Host = DefaultHost(port, host, coroutineDispatcher)

interface Host {
    val sessions: List<String>
    val messages: SharedFlow<Message>
    val events: SharedFlow<KomEvent>
    val isRunning: Boolean

    fun start()
    fun stop()
    fun send(ids: List<String>, data: Data)

    fun disconnect(id: String)
}

fun Host.send(data: Data) = send(sessions, data)

fun Host.send(id: String, data: Data) = send(listOf(id), data)

internal expect class DefaultHost(
    port: Int,
    host: String?,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Host