package sh.uffle.koms.e2e

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.coroutines.yield
import sh.uffle.koms.client.Kom
import sh.uffle.koms.client.KomState
import sh.uffle.koms.server.Host
import sh.uffle.koms.server.KomEvent
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

private const val LOCAL_PORT = 54321

class ConnectionTest : BehaviorSpec({
    lateinit var host: Host

    beforeEach {
        host = Host(LOCAL_PORT, "localhost")
    }

    afterEach {
        host.stop()
    }

    given("a host") {
        beforeEach {
            host.start()
        }

        When("the host is started") {
            then("it is running") {
                expectThat(host.isRunning).isTrue()
            }
        }

        When("the host is stopped") {
            then("it is not running") {
                host.stop()
                expectThat(host.isRunning).isFalse()
            }
        }

        and("multiple clients") {
            lateinit var clients: List<Kom>

            beforeEach {
                clients = List(5) { Kom() }
            }

            When("the clients connect to the host") {
                then("the host sends a connected event for every client") {
                    host.events.test {
                        clients.forEach { it.connect(LOCAL_PORT, "localhost") }

                        repeat(clients.size) {
                            expectThat(awaitItem()).isA<KomEvent.Connected>()
                        }
                    }
                }

                then("the host sessions has an entry with the correct id") {
                    host.events.test {
                        clients.forEach { it.connect(LOCAL_PORT, "localhost") }
                        val clientIds = clients.map { awaitItem() as KomEvent.Connected }.map { it.id }

                        expectThat(host.sessions)
                            .hasSize(5)
                            .contains(clientIds)
                    }
                }
            }

            When("disconnecting a client from the host") {
                lateinit var clientIdMapping: Map<String, Kom>

                beforeEach {
                    host.events.test {
                        clientIdMapping = clients.associateBy {
                            it.connect(LOCAL_PORT, "localhost")
                            val event = awaitItem() as KomEvent.Connected
                            event.id
                        }
                    }
                }

                then("the host sends a disconnected event") {
                    suspend fun TurbineTestContext<KomEvent>.verifyCorrectDisconnectForClientAtIndex(
                        index: Int,
                    ) {
                        val client = clients[index]

                        client.disconnect()

                        val event = awaitItem()
                        expectThat(event).isA<KomEvent.Disconnected>()
                        event as KomEvent.Disconnected
                        expectThat(clientIdMapping[event.id]) isEqualTo client
                    }

                    host.events.test {
                        verifyCorrectDisconnectForClientAtIndex(3)
                        verifyCorrectDisconnectForClientAtIndex(1)
                        verifyCorrectDisconnectForClientAtIndex(4)
                    }
                }

                then("the host sessions removes the client form the session list") {
                    host.events.test {
                        clients[3].disconnect()
                        clients[1].disconnect()

                        val disconnectedIds = listOf(
                            awaitItem() as KomEvent.Disconnected,
                            awaitItem() as KomEvent.Disconnected,
                        ).map { it.id }

                        expectThat(host.sessions)
                            .hasSize(clients.size - disconnectedIds.size)
                            .not().contains(disconnectedIds)
                    }
                }
            }

            When("the host disconnects from the client") {
                lateinit var clientIdMapping: Map<Kom, String>

                beforeEach {
                    host.events.test {
                        clientIdMapping = clients.associateWith {
                            it.connect(LOCAL_PORT, "localhost")
                            val event = awaitItem() as KomEvent.Connected
                            event.id
                        }
                    }
                }

                then("the client disconnects") {
                    suspend fun verifyCorrectDisconnectForClientAtIndex(
                        index: Int,
                    ) {
                        val client = clients[index]
                        client.komState.test {
                            skipItems(1)
                            host.disconnect(clientIdMapping[client]!!)

                            val event = awaitItem()
                            expectThat(event) isEqualTo KomState.Disconnected
                        }
                    }

                    verifyCorrectDisconnectForClientAtIndex(3)
                    verifyCorrectDisconnectForClientAtIndex(1)
                }

                then("the host sends a disconnected event") {
                    host.events.test {
                        val client = clients[3]
                        val clientId = clientIdMapping[client]!!

                        host.disconnect(clientId)

                        expectThat(awaitItem())
                            .isA<KomEvent.Disconnected>()
                            .and {
                                get { id } isEqualTo clientId
                            }
                    }
                }

                then("the host removes the disconnected session from the list") {
                    host.events.test {
                        val disconnectedIds = listOf(
                            clientIdMapping[clients[3]]!!,
                            clientIdMapping[clients[1]]!!,
                        )

                        disconnectedIds.forEach {
                            host.disconnect(it)
                        }
                        skipItems(2)

                        expectThat(host.sessions)
                            .hasSize(clients.size - disconnectedIds.size)
                            .not().contains(disconnectedIds)
                    }
                }
            }

            When("the host stops") {
                beforeEach {
                    host.events.test {
                        clients.forEach {
                            it.connect(LOCAL_PORT, "localhost")
                            awaitItem() as KomEvent.Connected
                        }
                    }
                }

                then("all clients disconnects") {
                    host.stop()

                    clients.forEach {
                        it.komState.test {
                            val state = awaitItem()
                            if (state != KomState.Disconnected) {
                                expectThat(awaitItem()) isEqualTo KomState.Disconnected
                            } else {
                                expectThat(state) isEqualTo KomState.Disconnected
                            }
                        }
                    }
                }

                then("the host sends a disconnected event for each client") {
                    host.events.test {
                        host.stop()
                        skipItems(1)
                        repeat(clients.size) {
                            expectThat(awaitItem()).isA<KomEvent.Disconnected>()
                        }
                        skipItems(1)
                    }
                }

                then("the host has an empty session list") {
                    host.events.test {
                        host.stop()
                        skipItems(2 + clients.size)
                    }
                    expectThat(host.sessions).isEmpty()
                }
            }
        }

        and("a client") {
            lateinit var client: Kom

            beforeEach {
                client = Kom()
            }

            afterEach {
                client.disconnect()
            }

            When("connecting the client to the host") {
                then("the client goes to connected through connecting state") {
                    val expectedSequence = listOf(
                        KomState.Disconnected,
                        KomState.Connecting,
                        KomState.Connected,
                    )

                    client.komState.test {
                        client.connect(LOCAL_PORT, "localhost")

                        val capturedSequence = expectedSequence.map {
                            awaitItem()
                        }

                        expectThat(capturedSequence) containsExactly expectedSequence
                    }
                }
            }

            When("disconnecting the client from the host") {
                beforeEach {
                    client.komState.test {
                        client.connect(LOCAL_PORT, "localhost")
                        while (awaitItem() != KomState.Connected) {
                            yield()
                        }
                    }
                }

                then("the client disconnects") {
                    client.komState.test {
                        skipItems(1)
                        client.disconnect()
                        expectThat(awaitItem()) isEqualTo KomState.Disconnected
                    }
                }
            }
        }
    }

    given("no host") {
        and("a client") {
            lateinit var client: Kom

            beforeEach {
                client = Kom()
            }

            afterEach {
                client.disconnect()
            }

            When("connecting the client to a not running host") {
                then("the client goes to disconnected through connecting state") {
                    val expectedSequence = listOf(
                        KomState.Disconnected,
                        KomState.Connecting,
                        KomState.Disconnected,
                    )

                    client.komState.test {
                        client.connect(LOCAL_PORT, "localhost")

                        val capturedSequence = expectedSequence.map {
                            awaitItem()
                        }

                        expectThat(capturedSequence) containsExactly expectedSequence
                    }
                }
            }
        }
    }
})
