package sh.uffle.koms

internal fun Int.asByteList(): List<Byte> = asByteArray().toList()

internal fun Int.asByteArray(): ByteArray = ByteArray(4) { index -> shr((3 - index) * 8).toByte() }
