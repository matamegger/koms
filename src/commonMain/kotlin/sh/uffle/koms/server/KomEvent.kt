package sh.uffle.koms.server

sealed class KomEvent {
    data class Connected(val id: String) : KomEvent()
    data class Disconnected(val id: String) : KomEvent()
    object HostStopping : KomEvent()
    object HostStopped : KomEvent()
}
