package sh.uffle.koms

import java.net.InetSocketAddress

fun InetSocketAddress(port: Int, host: String?) = if (host == null) {
    InetSocketAddress(port)
} else {
    InetSocketAddress(host, port)
}