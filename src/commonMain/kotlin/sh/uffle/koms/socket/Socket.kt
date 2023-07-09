package sh.uffle.koms.socket

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal interface Socket {
    val isConnected: Boolean

    suspend fun connect(port: Int, host: String): Boolean

    suspend fun read(
        dst: ByteArray,
        offset: Int = 0,
        length: Int = dst.size - offset,
        timeout: Duration,
    ): Int

    suspend fun write(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset): Int

    fun close()
}

internal suspend fun Socket.read(
    dst: ByteArray,
    offset: Int = 0,
    length: Int = dst.size - offset,
) = read(dst, offset, length, 0.toDuration(DurationUnit.MILLISECONDS))

internal suspend fun Socket.readOrNull(amount: Int, timeout: Duration): ByteArray? = runCatching {
    val data = ByteArray(amount)
    var position = 0
    do {
        val read = read(data, position, amount - position, timeout)
        if (read < 0) {
            return null
        }
        position += read
    } while (position != amount)

    data
}.getOrNull()
