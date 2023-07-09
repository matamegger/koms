package sh.uffle.koms.testunits

import sh.uffle.koms.socket.Socket
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsSequence
import strikt.assertions.hasSize
import kotlin.math.max
import kotlin.time.Duration

class TestableSocket(override var isConnected: Boolean = false) : Socket {
    private var dataForRead = ByteArray(0)
    var consumedDataBytes = 0
        private set
    private var writtenData = mutableListOf<Byte>()
    val writtenDataBytes get() = writtenData.size
    var gotClosed = false
        private set

    var onConnect: (Int, String) -> Boolean = { _, _ -> true }
    var beforeRead: (() -> Unit)? = null
    var beforeWrite: (() -> Unit)? = null

    private var ioActions: List<IoAction> = emptyList()

    override suspend fun connect(port: Int, host: String): Boolean {
        isConnected = onConnect(port, host)
        return isConnected
    }

    override suspend fun read(dst: ByteArray, offset: Int, length: Int, timeout: Duration): Int {
        beforeRead?.invoke()
        dataForRead.copyInto(dst, offset, consumedDataBytes, consumedDataBytes + length)
        ioActions = ioActions.transformLastOrAdd(
            shouldTransform = { it is IoAction.Read },
            transform = { (it as IoAction.Read).copy(toExclusive = consumedDataBytes + length) },
            add = { IoAction.Read(consumedDataBytes, consumedDataBytes + length) },
        )
        consumedDataBytes += length
        return length
    }

    override suspend fun write(dst: ByteArray, offset: Int, length: Int): Int {
        beforeWrite?.invoke()
        writtenData.addAll(dst.copyOfRange(offset, offset + length).toList())
        ioActions = ioActions.transformLastOrAdd(
            shouldTransform = { it is IoAction.Write },
            transform = { (it as IoAction.Write).copy(toExclusive = writtenData.size) },
            add = { IoAction.Write(writtenData.size - length, writtenData.size) },
        )
        return length
    }

    override fun close() {
        isConnected = false
        gotClosed = true
    }

    fun addData(data: List<Byte>) {
        dataForRead += data.toByteArray()
    }

    fun expectActions(vararg actions: IoAction, verifyOrder: Boolean = false) {
        expectThat(ioActions)
            .hasSize(actions.size).run {
                if (verifyOrder) {
                    containsSequence(actions.toList())
                } else {
                    contains(actions.toList())
                }
            }
    }

    fun expectWritten(ioAction: IoAction.Write): DescribeableBuilder<List<Byte>> {
        return expectThat(writtenData.subList(ioAction.from, ioAction.toExclusive))
    }
}

private fun <T> List<T>.transformLastOrAdd(
    shouldTransform: (T) -> Boolean,
    transform: (T) -> T,
    add: () -> T,
): List<T> {
    val last = takeIf { isNotEmpty() }?.last()
    return if (last != null && shouldTransform(last)) {
        take(max(0, size - 1)) + transform(last)
    } else {
        this + add()
    }
}

sealed interface IoAction {
    val from: Int
    val toExclusive: Int

    data class Read(override val from: Int, override val toExclusive: Int) : IoAction
    data class Write(override val from: Int, override val toExclusive: Int) : IoAction
}
