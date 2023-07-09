package sh.uffle.koms.testunits

import sh.uffle.koms.Data
import sh.uffle.koms.socket.KomSocket
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import kotlin.time.Duration

class TestableKomSocket(
    override var remoteVersion: Int? = null,
) : KomSocket {
    override val isOpen: Boolean get() = remoteVersion != null

    private val dataForReading = mutableListOf<Data>()
    private var dataForReadingIndex = 0
    private val writtenData = mutableListOf<Data>()
    var closed = false
        private set
    var onConnect: ((port: Int, host: String) -> Int?)? = null
    var onWrite: ((data: Data) -> Boolean)? = { isOpen }
    var onRead: (() -> Boolean)? = { isOpen }

    override suspend fun connect(port: Int, host: String): Boolean {
        remoteVersion = onConnect?.invoke(port, host)
        return isOpen
    }

    override suspend fun read(timeout: Duration): Data? {
        return dataForReading
            .takeIf { onRead?.invoke() == true }
            ?.getOrNull(dataForReadingIndex++)
    }

    override suspend fun write(data: Data): Boolean {
        if (onWrite?.invoke(data) == true) {
            writtenData.add(data)
            return true
        }
        return false
    }

    override fun close() {
        closed = true
        remoteVersion = null
    }

    fun addReadableData(data: Data) {
        dataForReading.add(data)
    }

    fun expectWritten(expectedData: Data, exactly: Boolean = true) {
        expectThat(writtenData).apply {
            if (exactly) contains(expectedData) else containsExactly(expectedData)
        }
    }
}
