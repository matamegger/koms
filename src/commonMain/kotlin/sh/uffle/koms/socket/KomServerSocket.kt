package sh.uffle.koms.socket

internal interface KomServerSocket {
    /**
     * The port this server socket is listening on.
     */
    val port: Int

    /**
     * The host name this server socket is listening on.
     */
    val host: String?

    /**
     * If the server socket is open.
     */
    val isOpen: Boolean

    /**
     * Accepts the next [KomSocket] connection.
     * suspends until a new connections is available.
     */
    suspend fun accept(): KomSocket

    /**
     * Closes the server socket.
     * Potential errors are ignored.
     */
    fun close()
}
