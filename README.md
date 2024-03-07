!!! This project is currently in an incubating phase. API surface, functionality and tool set might change. !!!

# Koms

Koms is a TCP socket based communication library written in Kotlin and setup to be a Kotlin multiplatform library.

The main focus is on asynchronous communication, where the send messages are not directly depending on received messages.

## Get started

### Host setup

Similar to pure sockets, first a host has to be started

```kotlin
val host = Host(75973, "localhost")
host.start()
```

Processing of received data is done with the `messages` `SharedFlow`, which starts consuming
received messages with the first subscriber.

```kotlin
host.messages.collect {
    println("${it.sender}: ${it.data.bytes.decodeToString()}")
    host.send(Data("Hello Client!".toByteArray()))
}
```

The host also provides client managing functions and events. Read the [API doc](#) for more information.

Here is a small example on a host, that dismisses all but one client.
```kotlin
host.events.collect { komEvent ->
    if (komEvent is KomEvent.Connected && host.sessions.size > 1) {
        host.disconnect(komEvent.id)
    }
}
```

## Client setup

```kotlin
val kom = Kom()
kom.connect(75973, "localhost")
```

Similar to the host, the client also has a `messages` flow with all the messages received from the host.

```kotlin
host.messages.collect {
    println("Host: ${it.data.bytes.decodeToString()}")
    client.send(Data("Hello Host!".toByteArray()))
}
```

## Synchronous Koms

When a kom connection has been established already, the `sequentialMessaging` block can be used to perform synchronous communication.

```kotlin
kom.sequentialMessaging {
    do {
        send(Data("ping".toByteArray()))
    } while(receive().data.toString() == "pong")
    this.kom.disconnect()
}

host.sequentialMessaging(host.sessions.first()) {
    var counter = 10
    while(counter > 0 && receive().data.toString() == "ping") {
        counter--
        send(Data("pong".toByteArray()))
    }
    this.host.disconnect(ids)
}
```
