package kodash.coroutine

import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ThreadPromise<T>(func: PromiseFunction<T>) : Promise<T> {

    private var state = PromiseState.IDLE
        set(value) {
            if (value.done) endTimeMillis = System.currentTimeMillis()
            field = value
        }

    private var thread: Thread

    companion object : PromiseEnvironment {
        override fun <T> newPromise(func: PromiseFunction<T>) = ThreadPromise(func)
    }

    private val startTimeMillis: Long
    private var endTimeMillis: Long? = null

    init {
        thread = Thread {
            runBlocking {
                func(getPromiseResolver())
            }
        }
        thread.start()
        startTimeMillis = System.currentTimeMillis()
        thread.setUncaughtExceptionHandler { _, exception -> getPromiseResolver().reject(exception) }
    }

    private val promiseResolver = object : PromiseResolver<T> {

        override fun getPromise() = this@ThreadPromise

        val resolveCallback = arrayListOf<Runnable>()
        val rejectCallback = arrayListOf<Runnable>()

        var value: T? = null
        var throwable: Throwable? = null

        override fun resolve(value: T) = value.also {
            if (getState().done) return@also
            state = PromiseState.COMPLETED
            this.value = value
            resolveCallback.forEach(Runnable::run)
            resolveCallback.clear()
            thread.interrupt()
        }

        override fun reject(throwable: Throwable) = throwable.also {
            if (getState().done) return@also
            state = PromiseState.REJECTED
            this.throwable = throwable
            rejectCallback.forEach(Runnable::run)
            rejectCallback.clear()
            thread.interrupt()
        }

    }

    override fun getStartTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimeMillis), ZoneId.systemDefault())

    override fun getEndTime(): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(endTimeMillis ?: System.currentTimeMillis()),
        ZoneId.systemDefault()
    )


    override fun getPromiseEnvironment() = AsyncPromise

    override fun <R> then(func: suspend (value: T) -> R): Promise<R> {
        return promise {
            if (state.done) {
                assertNotStopped()
                promiseResolver.value?.let { resolve(func(it)) }
                return@promise
            }
            promiseResolver.resolveCallback.add {
                runBlocking { promiseResolver.value?.let { resolve(func(it)) } }
            }
        }
    }

    override fun <R> catch(func: suspend (value: Throwable) -> R): Promise<R> {
        return promise {
            if (state.done) {
                assertNotStopped()
                promiseResolver.throwable?.let { resolve(func(it)) }
                return@promise
            }
            promiseResolver.rejectCallback.add {
                runBlocking { promiseResolver.throwable?.let { resolve(func(it)) } }
            }
        }
    }

    override fun <R> finally(func: suspend () -> R): Promise<R> = promise {
        await()
        func()
    }

    override fun getState() = state

    override fun cancel(): Promise<T> = this.also {
        thread.interrupt()
        state = PromiseState.STOPPED
    }

    override fun getPromiseResolver() = promiseResolver

    override suspend fun await(): T = suspendCoroutine { continuation ->
        assertNotStopped()
        if (getState().done) {
            promiseResolver.throwable?.let(continuation::resumeWithException)
            promiseResolver.value?.let(continuation::resume)
        } else {
            promiseResolver.resolveCallback.add {
                promiseResolver.value?.let(continuation::resume)
            }
            promiseResolver.rejectCallback.add {
                promiseResolver.throwable?.let(continuation::resumeWithException)
            }
        }
    }

}
