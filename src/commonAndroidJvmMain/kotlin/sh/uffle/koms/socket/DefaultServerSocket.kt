package sh.uffle.koms.socket

import kotlinx.coroutines.suspendCancellableCoroutine

import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

internal actual class DefaultServerSocket(
    override val port: Int,
    override val host: String? = null
) : ServerSocket {
    private var socket: AsynchronousServerSocketChannel? = null

    override val isOpen: Boolean get() = socket?.isOpen == true && socket?.localAddress != null

    override fun open() {
        socket = socket ?: AsynchronousServerSocketChannel.open()
        socket?.let {
            if (it.isOpen) {
                it.bind(InetSocketAddress(port, host))
            }
        }
    }

    override fun close() {
        socket?.close()
    }

    override suspend fun accept() = suspendCancellableCoroutine { continuation ->
        socket?.accept(continuation, ContinuationHandler<AsynchronousSocketChannel>())
    }.let { DefaultSocket(it) }
}