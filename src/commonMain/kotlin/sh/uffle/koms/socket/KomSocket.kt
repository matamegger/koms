package sh.uffle.koms.socket

import sh.uffle.koms.Data
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal interface KomSocket {
    /**
     * Indicates if the socket is open and read for read/write operations.
     */
    val isOpen: Boolean

    /**
     * The kom version of the remote instance.
     */
    val remoteVersion: Int?

    /**
     * Connects to a kom server at [host] with [port].
     * Returns `true` if the connection is successfully established, else `false` is returned.
     */
    suspend fun connect(port: Int, host: String): Boolean

    /**
     * Reads and returns one [Data] package from the socket.
     * Returns `null` if reading is not possible or an error occurrs.
     * No timeout is applied.
     */
    suspend fun read(timeout: Duration = 0.milliseconds): Data?

    /**
     * Writes one [Data] package on the socket.
     * Returns `true` if writing was successful, otherwise `false` is returned.
     * No timeout is applied.
     */
    suspend fun write(data: Data): Boolean

    /**
     * Closes the socket connection.
     * Any errors are ignored.
     */
    fun close()
}
