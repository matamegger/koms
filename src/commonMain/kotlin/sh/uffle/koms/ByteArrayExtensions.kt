package sh.uffle.koms

internal fun ByteArray.asInt() = takeLast(4).asInt()
