package sh.uffle.koms.server

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.uffle.koms.ScopeProvider
import sh.uffle.koms.socket.DefaultServerSocket

internal class KomGate(
    val server: DefaultServerSocket,
    val komManager: KomManager,
    val scopeProvider: ScopeProvider
) {
    private val scope = scopeProvider.createScope("KomGate")

    private var komAcceptingJob: Job? = null

    fun open() {
        if (komAcceptingJob != null) return
        komAcceptingJob = scope.launch {
            while (isActive) {
                val komSocket = server.accept()
                komManager.create(komSocket)
            }
        }
    }

    fun close() {
        komAcceptingJob?.cancel()
        komAcceptingJob = null
    }

    fun dispose() {
        close()
        scope.cancel()
    }
}