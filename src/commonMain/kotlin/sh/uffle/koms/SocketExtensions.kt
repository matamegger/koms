package sh.uffle.koms

import sh.uffle.koms.socket.Socket
import sh.uffle.koms.socket.read

internal suspend fun Socket.readDataOrNull(): Data? {
    val sizeBuffer = readOrNull(4) ?: return null

    val size = sizeBuffer.asInt()
    val data = readOrNull(size) ?: return null

    return Data(data)
}

internal suspend fun Socket.readOrNull(amount: Int): ByteArray? {
    val data = ByteArray(amount)
    var position = 0
    do {
        val read = read(data, position, amount - position)
        if (read < 0) {
            return null
        }
        position += read
    } while (position != amount)

    return data
}