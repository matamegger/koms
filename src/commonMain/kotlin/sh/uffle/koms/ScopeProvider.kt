package sh.uffle.koms

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

internal interface ScopeProvider {
    fun createScope(name: String? = null): CoroutineScope
}

internal class DefaultScopeProvider(private val dispatcher: CoroutineDispatcher) : ScopeProvider {
    override fun createScope(name: String?): CoroutineScope {
        var context = dispatcher + SupervisorJob()
        name?.let { context += CoroutineName(it) }
        return CoroutineScope(context)
    }
}
