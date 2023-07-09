package sh.uffle.koms

import io.kotest.core.spec.style.DescribeSpec
import sh.uffle.koms.server.SERVER_VERSION
import sh.uffle.koms.testunits.IoAction
import sh.uffle.koms.testunits.TestableSocket
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class ClientHandshakeWithClientFirstMoveSpec : DescribeSpec({
    lateinit var handshake: ClientHandshakeWithClientFirstMove
    lateinit var socket: TestableSocket

    beforeEach {
        socket = TestableSocket(isConnected = true)
        handshake = ClientHandshakeWithClientFirstMove()
    }

    describe("doing the handshake") {
        it("first writes to the socket") {
            val expectedHandshakeData = "KOMS".encodeToByteArray().toList() + SERVER_VERSION.asByteList()

            socket.addData("KOMS".encodeToByteArray().toList() + listOf(0, 0, 0, 2))
            handshake.handshake(socket)
            socket.expectActions(
                IoAction.Write(0, 8),
                IoAction.Read(0, 8),
                verifyOrder = true,
            )
            socket.expectWritten(IoAction.Write(0, 8))
                .isEqualTo(expectedHandshakeData)
        }

        describe("when the server sends the correct handshake") {
            beforeEach {
                socket.addData("KOMS".encodeToByteArray().toList() + listOf(0, 0, 0, 2))
            }

            it("returns a success") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Success>()
                    .get { remoteVersion } isEqualTo 2
            }
        }

        describe("when the server sends incorrect data") {
            beforeEach {
                socket.addData("SOMEUNEXPECTEDDATA".encodeToByteArray().toList())
            }

            it("returns a failure") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Failure>()
            }
        }

        describe("when the server does not send the version") {
            beforeEach {
                socket.addData("KOMS".encodeToByteArray().toList())
            }

            it("returns a failure") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Failure>()
            }
        }

        describe("when there is an exception while reading the server response") {
            beforeEach {
                socket.beforeRead = { throw RuntimeException() }
            }

            it("returns a failure") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Failure>()
            }
        }

        describe("when reading from the server exceeds the timeout") {
            beforeEach {
                socket.beforeRead = { throw RuntimeException() }
            }

            it("returns a failure") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Failure>()
            }
        }

        describe("when writing to the server throws an exception") {
            beforeEach {
                socket.beforeWrite = { throw RuntimeException() }
            }

            it("returns a failure") {
                expectThat(handshake.handshake(socket))
                    .isA<HandshakeResult.Failure>()
            }
        }
    }
})
