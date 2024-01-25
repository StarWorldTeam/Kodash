package kodash.coroutine

import kotlin.coroutines.cancellation.CancellationException

interface PromiseResolver<T> {

    /** 获取 Promise */
    fun getPromise(): Promise<T>

    /** 返回并结束执行 */
    fun resolve(value: T) = value

    /** 报错并结束执行 */
    fun reject(throwable: Throwable) = throwable

    /**
     * 报错并结束执行
     * @see CancellationException
     */
    fun reject() = reject(CancellationException())

    /**
     * @see Promise.await
     */
    suspend fun <R> await(promise: Promise<R>) = promise.await()

}

