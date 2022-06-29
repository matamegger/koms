package run.threads.koms.cosocket

import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit

class Socket internal constructor(private val socket: AsynchronousSocketChannel) {

    constructor() : this(AsynchronousSocketChannel.open())

    val isConnected: Boolean get() = socket.isOpen && socket.remoteAddress != null

    suspend fun connect(port: Int, host: String): Boolean {
        suspendCancellableCoroutine<Void> { continuation ->
            socket.connect(InetSocketAddress(host, port), continuation, ContinuationHandler<Void>())
        }
        return isConnected
    }

    suspend fun read(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset) = read(
        dst,
        offset,
        length,
        0,
        TimeUnit.MILLISECONDS
    )

    suspend fun read(
        dst: ByteArray,
        offset: Int = 0,
        length: Int = dst.size - offset,
        timeout: Long,
        timeUnit: TimeUnit
    ) = suspendCancellableCoroutine<Int> { continuation ->
        val buffer = ByteBuffer.wrap(dst).apply {
            position(offset)
            limit(offset + length)
        }
        socket.read(buffer, timeout, timeUnit, continuation, ContinuationHandler<Int>())
    }

    suspend fun write(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset) =
        suspendCancellableCoroutine<Int> { continuation ->
            val buffer = ByteBuffer.wrap(dst).apply {
                position(offset)
                limit(offset + length)
            }
            socket.write(buffer, 0, TimeUnit.MILLISECONDS, continuation, ContinuationHandler<Int>())
        }

    fun close() {
        socket.close()
    }
}