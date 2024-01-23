package kodash.coroutine

import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface Promise<T> {

    companion object {

        fun delay(tileMillis: Long) = promise {
            kotlinx.coroutines.delay(tileMillis)
            resolve()
        }

        fun delay(duration: Duration) = promise {
            kotlinx.coroutines.delay(duration)
            resolve()
        }

        fun <T> delay(timeMillis: Long, block: suspend () -> T) = promise {
            kotlinx.coroutines.delay(timeMillis)
            resolve(block())
        }

        fun <T> delay(duration: Duration, block: suspend () -> T) = promise {
            kotlinx.coroutines.delay(duration)
            resolve(block)
        }

        /** 创建Promise控制器 */
        fun <T> withResolvers(promiseEnvironment: PromiseEnvironment = AsyncPromise): PromiseResolver<T> {
            return (promiseEnvironment.newPromise<T> {}).getPromiseResolver()
        }

        /**
         * 接受多个 Promise 对象作为输入，并返回一个 Promise。当所有输入的 Promise 都被兑现时，返回的 Promise 也将被兑现（即使未传入 Promise 对象），并返回一个包含所有兑现值的数组。如果输入的任何 Promise 失败，则返回的 Promise 将失败，并带有第一个失败的原因。
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> all(vararg promises: Promise<T>): Promise<List<T>> = promise {
            coroutineScope {
                promises.map { async { it.await() } }.awaitAll()
            }.let(Collections::unmodifiableList).also(this@promise::resolve)
        }

        /**
         * 将多个 Promise 对象作为输入，并返回一个 Promise。当输入的任何一个 Promise 兑现时，这个返回的 Promise 将会兑现，并返回第一个兑现的值。
         * @throws AggregateError 当 Promise 都失败时（或未传入任何Promise对象），产生此错误
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> any(vararg promises: Promise<T>) = promise {
            var count = 0
            var completed = false
            val errors = mutableListOf<Throwable>()
            promises.forEach { promise ->
                promise.catch {
                    if (completed) return@catch
                    count++
                    errors.add(it)
                    if (count == promises.size)
                        reject(AggregateError(errors.toTypedArray(), "All promises were rejected"))
                }
                promise.then {
                    if (completed) return@then
                    completed = true
                    resolve(it)
                }
            }
        }

        /**
         * 接受多个 Promise 对象作为输入，并返回一个 Promise。这个返回的 Promise 会随着第一个 Promise 的触发而触发。
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> race(vararg promises: Promise<T>) = promise {
            var completed = false
            promises.forEach { promise ->
                promise.catch {
                    if (completed) return@catch
                    completed = true
                    reject(it)
                }
                promise.then {
                    if (completed) return@then
                    completed = true
                    resolve(it)
                }
            }
        }

        /** 将值转换为一个 Promise。当传入的值是 Promise 时，直接返回这个 Promise。 */
        fun <T> resolve(value: T) = promise { resolve(value) }

        /** 将值转换为一个 Promise。当传入的值是 Promise 时，直接返回这个 Promise。 */
        fun <T> resolve(promise: Promise<T>) = promise

        /** 返回一个空 Promise */
        fun resolve() = promiseVoid { resolve() }

        /** 返回一个已失败的 Promise 对象，失败原因为给定的参数。 */
        fun reject(throwable: Throwable = Throwable()) = promiseVoid { reject(throwable) }

    }

    /**
     * 获取 Promise 的状态
     * @see PromiseState
     */
    fun getState(): PromiseState = PromiseState.IDLE

    /**
     * 用于 Promise 兑现的回调函数。它立即返回一个等效的 Promise 对象，允许你链接到其他 Promise 方法，从而实现链式调用。
     * @param func 回调函数，返回一个对象
     * @return 返回一个 Promise，Promise 的值为 [func] 的返回值
     */
    fun <R> then(func: suspend (value: T) -> R): Promise<R>

    /**
     * 最多接受两个参数：用于 Promise 兑现和失败情况的回调函数。它立即返回一个等效的 Promise 对象，允许你链接到其他 Promise 方法，从而实现链式调用。
     * @param then 一个在此 Promise 对象被兑现时异步执行的函数。它的返回值将成为 then() 返回的 Promise 对象的兑现值。
     * @param catch 一个在此 Promise 对象被拒绝时异步执行的函数。它的返回值将成为 catch() 返回的 Promise 对象的兑现值。
     * @return 立即返回一个新的 Promise 对象，该对象的状态随 [then] 或 [catch] 返回的 Promise 的状态的改变而改变。
     */
    fun <R> on(then: suspend (value: T) -> R, catch: (suspend (throwable: Throwable) -> R)? = null) = promise {
        then(then).also { it.then(::resolve) }.catch(::reject)
        if (catch != null) catch(catch).also { it.then(::resolve) }.catch(::reject)
    }

    /**
     * 用于注册一个在 Promise 敲定（兑现或拒绝）时调用的函数。它会立即返回一个等效的 Promise 对象，这可以允许你链式调用其他 Promise 方法。
     *
     * 这可以让你避免在 Promise 的 [then] 和 [catch] 处理器中重复编写代码。
     */
    fun <R> finally(func: suspend () -> R): Promise<R>

    /**
     * 注册一个在 Promise 失败时调用的函数。它会立即返回一个等效的 Promise 对象，这可以允许你链式调用其他 Promise 的方法。
     * @param func 回调函数，返回一个对象
     * @return 返回一个 Promise，Promise 的状态随 [func] 返回的 Promise 的状态改变而改变
     */
    fun <R> catch(func: suspend (value: Throwable) -> R): Promise<R>

    /**
     * 同步等待 Promise 执行完毕
     * @return 返回 Promise 的兑现结果
     * @throws Throwable 如果 Promise 已失败，则抛出异常。
     */
    fun awaitSync(): T = runBlocking { await() }

    /**
     * 异步等待 Promise 执行完毕
     * @return 返回 Promise 的兑现结果
     * @throws Throwable 如果 Promise 已失败，则抛出异常。
     */
    suspend fun await(): T

    /**
     * 立即关闭 Promise 执行
     */
    fun cancel(): Promise<T> = this

    /**
     * 等待 Promise 执行完毕
     */
    fun join(): Promise<T> = this.also {
        runWithNoError {
            awaitSync()
        }
    }

    fun getPromiseResolver(): PromiseResolver<T>
    fun getPromiseEnvironment(): PromiseEnvironment

    fun measureTime(): Duration = (getEndTime().nano - getStartTime().nano).toDuration(DurationUnit.NANOSECONDS)
    fun getStartTime(): LocalDateTime = LocalDateTime.now()
    fun getEndTime(): LocalDateTime = LocalDateTime.now()

}
