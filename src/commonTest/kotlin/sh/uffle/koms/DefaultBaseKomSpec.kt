package sh.uffle.koms

import io.kotest.core.spec.style.DescribeSpec
import sh.uffle.koms.testunits.TestableKomSocket
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

class DefaultBaseKomSpec : DescribeSpec({
    lateinit var komSocket: TestableKomSocket

    lateinit var baseKom: BaseKom

    beforeEach {
        komSocket = TestableKomSocket()

        baseKom = DefaultBaseKom(komSocket)
    }

    describe("creating a BaseKom") {
        describe("with an already connected KomSocket") {
            beforeEach {
                komSocket.remoteVersion = 1
                baseKom = DefaultBaseKom(komSocket)
            }

            it("is connected") {
                expectThat(baseKom)
                    .get { status.value }
                    .isA<ConnectionState.Connected>()
            }
        }

        describe("with a disconnected KomSocket") {
            it("is disconnected") {
                expectThat(baseKom)
                    .get { status.value }
                    .isEqualTo(ConnectionState.Disconnected)
            }
        }
    }

    describe("connecting to a host") {
        it("intermediately switches into the connecting state") {
            komSocket.onConnect = { _, _ ->
                expectThat(baseKom.status.value) isEqualTo ConnectionState.Connecting
                1
            }

            baseKom.connect(1234, "test")

            expectThat(baseKom.status.value) isNotEqualTo ConnectionState.Connecting
        }

        describe("and the connection is successful") {
            val expectedRemoteVersion = 5

            beforeEach {
                komSocket.onConnect = { _, _ -> expectedRemoteVersion }
            }

            it("has the Connected status with the hosts version") {
                baseKom.connect(1234, "test")
                expectThat(baseKom.status.value)
                    .isA<ConnectionState.Connected>()
                    .get { remoteVersion } isEqualTo expectedRemoteVersion
            }
        }

        describe("and the connection is not successful") {
            beforeEach {
                komSocket.onConnect = { _, _ -> null }
                baseKom.connect(1234, "test")
            }

            it("has the Disconnected status") {
                expectThat(baseKom.status.value)
                    .isA<ConnectionState.Disconnected>()
            }

            it("closes the kom socket") {
                expectThat(komSocket)
                    .get { closed }.isTrue()
            }
        }

        describe("when it is already connected") {
            beforeEach {
                komSocket.onConnect = { _, _ -> 1 }
                baseKom.connect(1234, "host")
                expectThat(baseKom.status.value).isA<ConnectionState.Connected>()
            }

            it("does not connect again") {
                komSocket.onConnect = { _, _ -> throw IllegalStateException() }
                baseKom.connect(456, "anotherHost")
            }
        }
    }

    describe("sending data") {
        describe("when no connection is open") {
            beforeEach {
                baseKom.send(Data(byteArrayOf(0, 1, 2, 3)))
            }

            it("has the Disconnected status") {
                expectThat(baseKom.status.value) isEqualTo ConnectionState.Disconnected
            }

            it("closes the kom socket") {
                expectThat(komSocket)
                    .get { closed }.isTrue()
            }
        }

        describe("when a connection is open") {
            val expectedData = Data(byteArrayOf(0, 1, 2, 3))

            beforeEach {
                komSocket.remoteVersion = 1
            }

            it("writes to the kom socket") {
                baseKom.send(expectedData)
                komSocket.expectWritten(expectedData)
            }

            describe("and the write action fails") {
                beforeEach {
                    komSocket.onWrite = { false }
                    baseKom.send(expectedData)
                }

                it("closes the kom socket") {
                    expectThat(komSocket)
                        .get { closed }.isTrue()
                }

                it("has the disconnected status") {
                    expectThat(baseKom)
                        .get { status.value }
                        .isEqualTo(ConnectionState.Disconnected)
                }
            }
        }
    }

    describe("reading data") {
        describe("when no connection is open") {
            it("has the Disconnected status") {
                baseKom.read()
                expectThat(baseKom.status.value) isEqualTo ConnectionState.Disconnected
            }

            it("closes the kom socket") {
                baseKom.read()
                expectThat(komSocket)
                    .get { closed }.isTrue()
            }

            it("returns null") {
                expectThat(baseKom.read()).isNull()
            }
        }

        describe("when a connection is open") {
            val expectedData = Data(byteArrayOf(0, 1, 2, 3))

            beforeEach {
                komSocket.remoteVersion = 1
                komSocket.addReadableData(expectedData)
            }

            it("reads from the kom socket") {
                expectThat(baseKom.read()) isEqualTo expectedData
            }

            describe("and the read action fails") {
                beforeEach {
                    komSocket.onRead = { false }
                }

                it("closes the kom socket") {
                    baseKom.read()
                    expectThat(komSocket)
                        .get { closed }.isTrue()
                }

                it("has the disconnected status") {
                    baseKom.read()
                    expectThat(baseKom)
                        .get { status.value }
                        .isEqualTo(ConnectionState.Disconnected)
                }

                it("returns null") {
                    expectThat(baseKom.read()).isNull()
                }
            }
        }
    }
})
