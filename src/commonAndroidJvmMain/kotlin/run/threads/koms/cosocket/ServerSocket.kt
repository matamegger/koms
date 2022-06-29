package run.threads.koms.cosocket

import kotlinx.coroutines.suspendCancellableCoroutine
import run.threads.koms.InetSocketAddress

import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel

class ServerSocket(val port: Int, val host: String? = null) {
    private var socket: AsynchronousServerSocketChannel? = null

    val isOpen: Boolean get() = socket?.isOpen == true && socket?.localAddress != null

    fun open() {
        socket = socket ?: AsynchronousServerSocketChannel.open()
        socket?.let {
            if (it.isOpen) {
                it.bind(InetSocketAddress(port, host))
            }
        }
    }

    fun close() {
        socket?.close()
    }

    suspend fun accept() = suspendCancellableCoroutine<AsynchronousSocketChannel> { continuation ->
        socket?.accept(continuation, ContinuationHandler<AsynchronousSocketChannel>())
    }.let { Socket(it) }
}