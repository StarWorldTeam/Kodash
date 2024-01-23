package kodash.coroutine

import kotlin.coroutines.cancellation.CancellationException


object None

/**
 * 包装了多个错误对象的单个错误对象。
 */
class AggregateError(val errors: Array<Throwable>, message: String?) : RuntimeException(message)

interface PromiseEnvironment {

    fun <T> newPromise(func: PromiseFunction<T>): Promise<T>

}

/**
 * 执行一个函数并忽视这个函数的结果
 */
fun runWithNoError(block: () -> Unit) {
    try {
        block()
    } catch (_: Throwable) {
    }
}

typealias PromiseFunction <T> = suspend PromiseResolver<T>.() -> Unit

/** 异步执行 */
fun <T> promise(promiseEnvironment: PromiseEnvironment = AsyncPromise, func: PromiseFunction<T>): Promise<T> =
    promiseEnvironment.newPromise(func)

/** 异步执行（无返回） */
fun promiseVoid(promiseEnvironment: PromiseEnvironment = AsyncPromise, func: PromiseFunction<None>): Promise<None> =
    promise(promiseEnvironment, func)

/** 返回Void */
fun PromiseResolver<None>.resolve() {
    resolve(None)
}

/** Promise状态 */
enum class PromiseState(
    /** 是否已完成 */
    val done: Boolean
) {

    /** 空闲 */
    IDLE(false),

    /** 运行中 */
    RUNNING(false),

    /** 已兑现 */
    COMPLETED(true),

    /** 已失败 */
    REJECTED(true),

    /** 已停止 */
    STOPPED(true);

}

fun Promise<*>.assertNotStopped() {
    if (getState() == PromiseState.STOPPED)
        throw CancellationException()
}

var <T> Promise<T>.awaited: T
    get() {
        return this.awaitSync()
    }
    set(_) {}
