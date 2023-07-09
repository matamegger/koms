package sh.uffle.koms.internal.socket

import java.net.InetSocketAddress

internal fun InetSocketAddress(port: Int, host: String?) = if (host == null) {
    InetSocketAddress(port)
} else {
    InetSocketAddress(host, port)
}
