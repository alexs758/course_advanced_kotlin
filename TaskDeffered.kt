import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

interface Computation<out T> {
    val children: List<Computation<*>>

    val deferred: Deferred<T>?
    fun async(scope: CoroutineScope): Deferred<T>

    suspend fun await(): T = coroutineScope {
        async(this).await()
    }

    fun reset()
}

class LazyComputation<T>(
    override val children: List<Computation<*>>,
    private val context: CoroutineContext,
    val block: suspend () -> T,
) : Computation<T> {

    override var deferred: Deferred<T>? = null
        private set

    override fun async(scope: CoroutineScope): Deferred<T> {
        if (deferred == null) {
            val childrenStarted = children.map { child ->
                child.async(scope)
            }
            val ctx = context + Children(childrenStarted)
            deferred = scope.async(ctx, start = CoroutineStart.LAZY) {
                childrenStarted.forEach { deferred ->
                    deferred.invokeOnCompletion { throwable ->
                        throwable?.let {
                            this.cancel(CancellationException("Computation $deferred failed. Error : $throwable"))
                        }
                    }
                }
                block()
            }
        }
        return deferred!!
    }

    override fun reset() {
        deferred?.cancel()
        deferred = null
    }
}

class Children(val values: List<Deferred<*>>) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = Children

    companion object : CoroutineContext.Key<Children>
}