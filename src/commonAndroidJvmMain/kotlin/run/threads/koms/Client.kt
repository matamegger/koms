package run.threads.koms
//
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.SharedFlow
//import kotlinx.coroutines.flow.asSharedFlow
//import kotlinx.coroutines.launch
//import java.net.Socket
//import kotlin.coroutines.CoroutineContext
//
//internal actual class DefaultClient actual constructor(
//    private val coroutineContext: CoroutineContext
//) : Client {
//    private var ioScope = CoroutineScope(coroutineContext)
//    private var serverSocket: Socket? = null
//
//    private val _messages = MutableSharedFlow<Message>()
//    override val messages: SharedFlow<Message> = _messages.asSharedFlow()
//
//    override fun connect(port: Int, host: String): Boolean {
//        if (serverSocket != null) return false
//        serverSocket = runCatching {
//            Socket(host, port)
//        }.getOrNull()
//        serverSocket?.attachListener()
//        return serverSocket != null
//    }
//
//    override fun disconnect() {
//        serverSocket?.close()
//        serverSocket = null
//    }
//
//    override fun send(data: Data) {
//        ioScope.launch {
//            serverSocket?.getOutputStream()?.let {
//                data.sendTo(it)
//            }
//        }
//    }
//
//    private fun Socket.attachListener() {
//        ioScope.launch {
//            getInputStream().buffered().use {
//                while (true) {
//                    val data = readFrom(it)
//                    if (data == null) {
//                        println("Could not read data from server.")
//                        return@launch
//                    }
//
//                    _messages.emit(Message("server", data))
//                }
//            }
//        }
//    }
//}