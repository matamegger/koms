package sh.uffle.koms.socket

import io.kotest.core.spec.style.DescribeSpec
import sh.uffle.koms.testunits.TestHandshake
import sh.uffle.koms.testunits.TestServerSocket
import sh.uffle.koms.testunits.TestableSocket
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class DefaultKomServerSocketSpec : DescribeSpec({
    lateinit var handshake: TestHandshake
    lateinit var serverSocket: TestServerSocket

    lateinit var komServer: DefaultKomServerSocket

    beforeEach {
        handshake = TestHandshake()
        serverSocket = TestServerSocket(isOpen = true)

        komServer = DefaultKomServerSocket(serverSocket, handshake)
    }

    describe("accepting a connection") {
        describe("when the server socket throws") {
            lateinit var expectedSocket: TestableSocket

            beforeEach {
                expectedSocket = TestableSocket(isConnected = true)
                var firstAccept = true
                serverSocket.onAccept = {
                    if (firstAccept) {
                        firstAccept = false
                        throw RuntimeException()
                    }
                    expectedSocket
                }
            }

            it("takes the next connection") {
                handshake.onHandshake = {
                    expectThat(it).isEqualTo(expectedSocket)
                    1
                }
                komServer.accept()
            }
        }

        describe("with a successful socket connection") {
            describe("and a successful handshake") {
                val expectedRemoteVersion = 10

                lateinit var socket: TestableSocket

                beforeEach {
                    socket = TestableSocket(isConnected = true)
                    handshake.onHandshake = { expectedRemoteVersion }
                    serverSocket.onAccept = { socket }
                }

                it("returns a KomSocket") {
                    val komSocket = komServer.accept()
                    expectThat(komSocket).apply {
                        get { isOpen }.isTrue()
                        get { remoteVersion } isEqualTo expectedRemoteVersion
                    }
                }
            }

            describe("and a failing handshake") {
                val expectedRemoteVersion = 11

                lateinit var sockets: List<TestableSocket>

                beforeEach {
                    sockets = listOf(
                        TestableSocket(isConnected = true),
                        TestableSocket(isConnected = true),
                    )
                    var acceptCount = 0
                    serverSocket.onAccept = {
                        sockets[acceptCount++]
                    }
                    handshake.onHandshake = { socket ->
                        expectedRemoteVersion
                            .takeIf { socket == sockets.last() }
                    }
                }

                it("takes accepts the next connection") {
                    val komSocket = komServer.accept()
                    expectThat(komSocket).apply {
                        get { isOpen }.isTrue()
                        get { remoteVersion } isEqualTo expectedRemoteVersion
                    }
                }
            }
        }
    }

    describe("closing the kom server") {
        it("closes the server socket") {
            komServer.close()

            expectThat(serverSocket.gotClosed).isTrue()
        }
    }
})
