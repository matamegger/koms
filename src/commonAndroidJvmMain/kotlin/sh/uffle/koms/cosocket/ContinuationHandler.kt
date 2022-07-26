package sh.uffle.koms.cosocket

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ContinuationHandler<T> : CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }
}