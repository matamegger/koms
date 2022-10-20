package sh.uffle.koms.socket

internal interface ServerSocket {
    val port: Int
    val host: String?
    val isOpen: Boolean
    fun open()
    fun close()

    suspend fun accept(): DefaultSocket
}