package sh.uffle.koms.socket

import kotlinx.coroutines.suspendCancellableCoroutine
import sh.uffle.koms.internal.socket.ContinuationHandler
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

internal actual class DefaultSocket internal constructor(
    private val socket: AsynchronousSocketChannel,
) : Socket {
    actual constructor() : this(AsynchronousSocketChannel.open())

    override val isConnected: Boolean get() = socket.isOpen && socket.remoteAddress != null

    override suspend fun connect(port: Int, host: String): Boolean {
        suspendCancellableCoroutine { continuation ->
            socket.connect(InetSocketAddress(host, port), continuation, ContinuationHandler<Void>())
        }
        return isConnected
    }

    override suspend fun read(
        dst: ByteArray,
        offset: Int,
        length: Int,
        timeout: Duration,
    ) = suspendCancellableCoroutine { continuation ->
        val buffer = ByteBuffer.wrap(dst).apply {
            position(offset)
            limit(offset + length)
        }
        socket.read(
            buffer,
            timeout.inWholeMilliseconds,
            TimeUnit.MILLISECONDS,
            continuation,
            ContinuationHandler<Int>(),
        )
    }

    override suspend fun write(
        dst: ByteArray,
        offset: Int,
        length: Int,
    ) = suspendCancellableCoroutine { continuation ->
        val buffer = ByteBuffer.wrap(dst).apply {
            position(offset)
            limit(offset + length)
        }
        socket.write(buffer, 0, TimeUnit.MILLISECONDS, continuation, ContinuationHandler<Int>())
    }

    override fun close() {
        socket.close()
    }
}
