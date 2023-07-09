package sh.uffle.koms

import io.kotest.core.spec.style.DescribeSpec
import strikt.api.expectThat
import strikt.assertions.isA

class ClientFirstProtocolSpec : DescribeSpec({

    lateinit var protocol: ClientFirstProtocol

    beforeEach {
        protocol = ClientFirstProtocol()
    }

    describe("getting the server handshake") {
        it("returns the client first server handshake") {
            expectThat(protocol.serverHandshake).isA<ServerHandshakeWithClientFirstMove>()
        }
    }

    describe("getting the client handshake") {
        it("returns the client first client handshake") {
            expectThat(protocol.clientHandshake).isA<ClientHandshakeWithClientFirstMove>()
        }
    }
})
