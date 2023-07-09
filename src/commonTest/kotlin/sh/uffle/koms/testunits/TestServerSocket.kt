package sh.uffle.koms.testunits

import sh.uffle.koms.socket.ServerSocket
import sh.uffle.koms.socket.Socket

internal class TestServerSocket(override var isOpen: Boolean = false) : ServerSocket {
    var onAccept: suspend () -> Socket = { TestableSocket() }
    var gotClosed: Boolean = false
        private set
    override val port: Int
        get() = 1234
    override val host: String
        get() = "host"

    override fun open() {
        TODO("Not yet implemented")
    }

    override fun close() {
        gotClosed = true
    }

    override suspend fun accept(): Socket {
        return onAccept()
    }
}
