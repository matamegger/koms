import run.threads.koms.asByteList
import run.threads.koms.client.CLIENT_VERSION
import run.threads.koms.server.SERVER_VERSION

internal val KOMS: ByteArray = "KOMS".toByteArray()
internal val SERVER_HANDSHAKE = KOMS.toMutableList().plus(SERVER_VERSION.asByteList()).toByteArray()
internal val CLIENT_HANDSHAKE = KOMS.toMutableList().plus(CLIENT_VERSION.asByteList()).toByteArray()