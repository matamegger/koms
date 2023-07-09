package sh.uffle.koms.socket

import kotlinx.coroutines.suspendCancellableCoroutine
import sh.uffle.koms.internal.socket.ContinuationHandler
import sh.uffle.koms.internal.socket.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

internal actual class DefaultServerSocket actual constructor(
    override val port: Int,
    override val host: String?,
) : ServerSocket {
    private val socket: AsynchronousServerSocketChannel by lazy { AsynchronousServerSocketChannel.open() }

    override val isOpen: Boolean get() = socket.isOpen && socket.localAddress != null

    override fun open() {
        if (!socket.isOpen) return
        socket.bind(InetSocketAddress(port, host))
    }

    override fun close() {
        socket.close()
    }

    override suspend fun accept() = suspendCancellableCoroutine { continuation ->
        socket.accept(continuation, ContinuationHandler<AsynchronousSocketChannel>())
    }.let { DefaultSocket(it) }
}
