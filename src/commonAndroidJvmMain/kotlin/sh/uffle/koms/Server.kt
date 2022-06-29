package sh.uffle.koms

//internal class dDefaultServer constructor(
//    val port: Int,
//    val host: String?,
//    private val coroutineContext: CoroutineContext
//) : Server {
//    private var server: ServerSocket? = null
//
//    private var clientHolder: List<ClientHolder> = emptyList()
//
//    private var clientScope = CoroutineScope(coroutineContext)
//
//    private val _messages = MutableSharedFlow<Message>()
//    override val messages: SharedFlow<Message> = _messages.asSharedFlow()
//    private val _connection = MutableSharedFlow<Connection>()
//    override val connection: SharedFlow<Connection> = _connection.asSharedFlow()
//
//    override val clients: List<String>
//        get() = clientHolder.map { it.name }
//
//    override fun start() {
//        CoroutineScope(coroutineContext).launch {
//            if (server != null) return@launch
//
//            server = ServerSocket(port, 50, host?.let { InetAddress.getByName(it) })
//            launch { while (server != null) server?.serve() }
//        }
//    }
//
//    private suspend fun ServerSocket.serve() {
//        val clientSocket = accept()
//        val clientHolder = ClientHolder(
//            UUID.randomUUID().toString(),
//            clientSocket,
//            clientSocket.getInputStream(),
//            clientSocket.getOutputStream()
//        )
//        clientHolder.attachListener()
//        this@dDefaultServer.clientHolder += clientHolder
//        _connection.emit(Connection(clientHolder.name))
//    }
//
//    override fun stop() {
//        server?.close()
//        server = null
//        val oldClientScope = clientScope
//        val clients = clientHolder
//        clientScope = CoroutineScope(coroutineContext)
//        clientHolder = emptyList()
//        oldClientScope.cancel()
//        clients.forEach {
//            it.socket.close()
//        }
//    }
//
//    override fun send(clientNames: List<String>, data: Data) {
//        clientScope.launch {
//            clientHolder
//                .filter { it.name in clientNames }
//                .forEach { it.sendData(data) }
//        }
//    }
//
//    private fun ClientHolder.sendData(data: Data) {
//        data.sendTo(output)
//    }
//
//    private fun ClientHolder.attachListener() {
//        clientScope.launch {
//            input.buffered().use {
//                while (true) {
//                    val data = readFrom(input)
//                    if (data == null) {
//                        println("Could not read data for client \"$name\".")
//                        return@launch
//                    }
//                    _messages.emit(Message(name, data))
//                }
//            }
//        }
//    }
//}