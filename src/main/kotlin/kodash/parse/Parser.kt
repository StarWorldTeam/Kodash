package kodash.parse

import java.nio.CharBuffer

open class ParseResult<T>(private val isSuccess: Boolean, private val value: T? = null, private val throwable: Throwable? = Throwable()) {

    companion object {

        fun <T> success(value: T): ParseResult<T> = ParseResult(true, value)

        fun <T> failure(throwable: Throwable? = Throwable()) = ParseResult<T>(false, null, throwable)

    }

    open fun isSuccess() = isSuccess
    fun isNotSuccess() = !isSuccess()
    open fun getValue() = getValueOrNull()!!
    open fun getValueOrNull() = value
    open fun getThrowable() = getThrowableOrNull()!!
    open fun getThrowableOrNull() = throwable

}

interface Parser<T> {

    fun parse(buffer: CharBuffer): ParseResult<T>

    fun parseOrThrow(buffer: CharBuffer) = parse(buffer).also {
        if (!it.isSuccess()) throw it.getThrowable()
    }

}