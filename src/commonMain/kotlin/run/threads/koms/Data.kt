package run.threads.koms

data class Message(
    val sender: String,
    val data: Data
)

data class Data(val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}