package sh.uffle.koms.socket

internal expect class DefaultServerSocket(
    port: Int,
    host: String? = null,
) : ServerSocket
