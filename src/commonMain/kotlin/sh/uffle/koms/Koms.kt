import sh.uffle.koms.asByteList
import sh.uffle.koms.client.CLIENT_VERSION
import sh.uffle.koms.server.SERVER_VERSION

internal val KOMS: ByteArray = "KOMS".toByteArray()
internal val SERVER_HANDSHAKE = KOMS.toMutableList().plus(SERVER_VERSION.asByteList()).toByteArray()
internal val CLIENT_HANDSHAKE = KOMS.toMutableList().plus(CLIENT_VERSION.asByteList()).toByteArray()