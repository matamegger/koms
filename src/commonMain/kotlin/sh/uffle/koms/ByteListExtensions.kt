package sh.uffle.koms

internal fun List<Byte>.asInt() = takeLast(4).fold(0) { result, value -> result.shl(8).or(value.toInt()) }