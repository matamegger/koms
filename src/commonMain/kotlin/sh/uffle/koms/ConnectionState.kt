package sh.uffle.koms

sealed class ConnectionState {
    object Connecting : ConnectionState()
    class Connected(val remoteVersion: Int) : ConnectionState()
    object Disconnected : ConnectionState()
}
