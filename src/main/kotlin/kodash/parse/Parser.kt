package kodash.parse

import java.nio.CharBuffer

open class ParseResult<T>(private val status: Boolean, private val value: T?) {

    companion object {

        fun <T> success(value: T): ParseResult<T> = ParseResult(true, value)

        fun <T> failure() = ParseResult<T>(false, null)

    }

    open fun getStatus() = status
    open fun getValue() = getValueOrNull()!!
    open fun getValueOrNull() = value

}

interface Parser<T> {

    fun parse(buffer: CharBuffer): ParseResult<T>

    fun parseOrThrow(buffer: CharBuffer) = parse(buffer).also {
        if (!it.getStatus()) throw IllegalStateException()
    }

}