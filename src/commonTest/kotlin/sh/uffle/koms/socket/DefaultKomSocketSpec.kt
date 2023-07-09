package sh.uffle.koms.socket

import io.kotest.core.spec.style.DescribeSpec
import sh.uffle.koms.Data
import sh.uffle.koms.testunits.IoAction
import sh.uffle.koms.testunits.TestHandshake
import sh.uffle.koms.testunits.TestableSocket
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class DefaultKomSocketSpec : DescribeSpec({
    lateinit var handshake: TestHandshake
    lateinit var socket: TestableSocket

    lateinit var komSocket: DefaultKomSocket

    beforeEach {
        handshake = TestHandshake()
        socket = TestableSocket()

        komSocket = DefaultKomSocket(socket, handshake)
    }

    describe("connecting") {
        describe("just the the kom session") {
            beforeEach {
                socket.isConnected = true
            }

            describe("with a successful handshake") {
                val expectedRemoteVersion = 10

                beforeEach {
                    handshake.onHandshake = { _ -> expectedRemoteVersion }
                    komSocket.connect()
                }

                it("is open") {
                    expectThat(komSocket)
                        .get { isOpen }.isTrue()
                }

                it("has a remote version") {
                    expectThat(komSocket)
                        .get { remoteVersion } isEqualTo expectedRemoteVersion
                }
            }

            describe("with a failing handshake") {
                beforeEach {
                    handshake.onHandshake = { _ -> null }
                    komSocket.connect()
                }

                it("is not open") {
                    expectThat(komSocket)
                        .get { isOpen }.isFalse()
                }

                it("has no remote version") {
                    expectThat(komSocket)
                        .get { remoteVersion }.isNull()
                }
            }

            describe("with an exception during the handshake") {
                beforeEach {
                    handshake.onHandshake = { _ -> throw RuntimeException() }
                    komSocket.connect()
                }

                it("is not open") {
                    expectThat(komSocket)
                        .get { isOpen }.isFalse()
                }

                it("has no remote version") {
                    expectThat(komSocket)
                        .get { remoteVersion }.isNull()
                }
            }
        }

        describe("to a host and port") {
            describe("with a successful socket connection") {
                beforeEach {
                    socket.onConnect = { _, _ -> true }
                }

                describe("and a successful handshake") {
                    var connectionResult: Boolean? = null
                    val expectedRemoteVersion = 10

                    beforeEach {
                        handshake.onHandshake = { expectedRemoteVersion }
                        connectionResult = komSocket.connect(1234, "host")
                    }

                    it("is open") {
                        expectThat(komSocket)
                            .get { isOpen }.isTrue()
                    }

                    it("has a remote version") {
                        expectThat(komSocket)
                            .get { remoteVersion } isEqualTo expectedRemoteVersion
                    }

                    it("returns true") {
                        expectThat(connectionResult).isTrue()
                    }
                }

                describe("and a failing handshake") {
                    var connectionResult: Boolean? = null

                    beforeEach {
                        handshake.onHandshake = { _ -> null }
                        connectionResult = komSocket.connect(1234, "host")
                    }

                    it("is not open") {
                        expectThat(komSocket)
                            .get { isOpen }.isFalse()
                    }

                    it("has no remote version") {
                        expectThat(komSocket)
                            .get { remoteVersion }.isNull()
                    }

                    it("closes the socket") {
                        expectThat(socket)
                            .get { gotClosed }.isTrue()
                    }

                    it("returns false") {
                        expectThat(connectionResult).isFalse()
                    }
                }

                describe("with an exception during the handshake") {
                    var connectionResult: Boolean? = null

                    beforeEach {
                        handshake.onHandshake = { _ -> throw RuntimeException() }
                        connectionResult = komSocket.connect(1234, "host")
                    }

                    it("is not open") {
                        expectThat(komSocket)
                            .get { isOpen }.isFalse()
                    }

                    it("has no remote version") {
                        expectThat(komSocket)
                            .get { remoteVersion }.isNull()
                    }

                    it("closes the socket") {
                        expectThat(socket)
                            .get { gotClosed }.isTrue()
                    }

                    it("returns false") {
                        expectThat(connectionResult).isFalse()
                    }
                }
            }

            describe("with an unsuccessful socket connection") {
                var connectionResult: Boolean? = null

                beforeEach {
                    socket.onConnect = { _, _ -> false }
                    connectionResult = komSocket.connect(1234, "host")
                }

                it("closes the socket") {
                    expectThat(socket)
                        .get { gotClosed }.isTrue()
                }

                it("returns false") {
                    expectThat(connectionResult).isFalse()
                }
            }
        }
    }

    describe("reading data") {
        it("reads a Data package from the socket") {
            val expectedData = listOf(
                Data(byteArrayOf(1, 2, 3, 4, 5)),
                Data(byteArrayOf(6, 7, 8, 9, 10)),
            )
            socket.addData(
                expectedData.flatMap {
                    (byteArrayOf(0, 0, 0, it.bytes.size.toByte()) + it.bytes).toList()
                },
            )

            val readData = expectedData.map {
                komSocket.read()
            }

            expectThat(readData).containsExactly(expectedData)
        }

        describe("when there are not enough bytes to read") {
            beforeEach {
                socket.addData(
                    byteArrayOf(0, 0, 0, 5, 1, 2, 3, 4).toList(),
                )
            }

            it("returns null") {
                expectThat(komSocket.read()).isNull()
            }
        }

        describe("when there is an exception while reading the data size") {
            beforeEach {
                socket.beforeRead = { throw java.lang.RuntimeException() }
            }

            it("returns null") {
                expectThat(komSocket.read()).isNull()
            }
        }

        describe("when there is a timeout while reading the data") {
            beforeEach {
                socket.addData(byteArrayOf(0, 0, 0, 5).toList())
                socket.beforeRead = {
                    if (socket.consumedDataBytes == 4) {
                        throw java.lang.RuntimeException()
                    }
                }
            }

            it("returns null") {
                expectThat(komSocket.read()).isNull()
            }
        }
    }

    describe("writing data") {
        it("writes a Data package to the socket") {
            val data = listOf(
                Data(byteArrayOf(1, 2, 3, 4, 5)),
                Data(byteArrayOf(6, 7, 8, 9, 10)),
            )
            val expectedBytes = data.flatMap {
                (byteArrayOf(0, 0, 0, it.bytes.size.toByte()) + it.bytes).toList()
            }

            data.forEach { komSocket.write(it) }

            socket.expectWritten(IoAction.Write(0, 18))
                .containsExactly(expectedBytes)
        }

        describe("when there is an exception while writing the data size") {
            beforeEach {
                socket.beforeWrite = { throw java.lang.RuntimeException() }
            }

            it("returns null") {
                expectThat(komSocket.write(Data(byteArrayOf(1, 2, 3, 4, 5))))
                    .isFalse()
            }
        }

        describe("when there is a timeout while writing the data") {
            beforeEach {
                socket.beforeWrite = { if (socket.writtenDataBytes == 4) throw java.lang.RuntimeException() }
            }

            it("returns null") {
                expectThat(komSocket.write(Data(byteArrayOf(1, 2, 3, 4, 5))))
                    .isFalse()
            }
        }
    }

    describe("closing the kom socket") {
        it("closes the socket") {
            komSocket.close()

            expectThat(socket.gotClosed).isTrue()
        }

        it("clears the remote version") {
            komSocket.connect()
            expectThat(komSocket.remoteVersion).isNotNull()

            komSocket.close()

            expectThat(komSocket.remoteVersion).isNull()
        }
    }
})
