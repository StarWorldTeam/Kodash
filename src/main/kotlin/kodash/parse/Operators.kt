package kodash.parse

import java.nio.BufferUnderflowException
import java.nio.CharBuffer

object Operators {

    fun <T> choice(vararg parsers: Parser<T>) = ChoiceOperator(*parsers)
    fun character(vararg char: Char) = MatchCharacters(1, 1) { it in char }
    fun characters(min: Int = 1, max: Int = Integer.MAX_VALUE, matcher: (Char) -> Boolean) = MatchCharacters(min, max, matcher)
    fun whitespace(min: Int = 0, max: Int = Int.MAX_VALUE) = MatchCharacters(min, max, Char::isWhitespace)
    fun <T> optional(parser: Parser<T>) = OptionalOperator(parser)
    fun <T> parser(block: (CharBuffer) -> ParseResult<T>): Parser<T> = object : Parser<T> {
        override fun parse(buffer: CharBuffer) = block(buffer)
    }
    fun literal(literal: String) = LiteralOperator(literal)
    fun <T> repeat(parser: Parser<T>, min: Int = 0, max: Int = Int.MAX_VALUE) = RepeatingOperator(parser, min, max)

}

object Parsers {

    fun <T> gotoWhenError(buffer: CharBuffer, position: Int = buffer.position(), block: () -> ParseResult<T>): ParseResult<T> {
        return try {
            val value = block()
            if (value.getStatus()) value
            else throw IllegalStateException()
        } catch (error: Throwable) {
            buffer.position(position)
            ParseResult.failure()
        }
    }

}

class ChoiceOperator<T>(private vararg val choices: Parser<T>) : Parser<T> {

    override fun parse(buffer: CharBuffer): ParseResult<T> {
        for (i in choices) {
            try {
                val result = Parsers.gotoWhenError(buffer) {
                    val result = i.parse(buffer)
                    result
                }
                if (result.getStatus()) return result
            } catch (_: Throwable) {}
        }
        return ParseResult.failure()
    }

}

class MatchCharacters(private val min: Int = 0, private val max: Int = Int.MAX_VALUE, private val matcher: (Char) -> Boolean) : Parser<CharArray> {

    override fun parse(buffer: CharBuffer): ParseResult<CharArray> {
        var current = 0
        val value = StringBuilder()
        var status: Boolean
        while (true) {
            if (current >= max) {
                status = true
                break
            }
            val startPosition = buffer.position()
            try {
                val char = buffer.get()
                if (matcher(char)) {
                    value.append(char)
                } else {
                    buffer.position(startPosition)
                    status = current + 1 > min
                    break
                }
            } catch (e: BufferUnderflowException) {
                status = current + 1 > min
                break
            }
            current += 1
        }
        return ParseResult(status, value.toString().toCharArray())
    }

}

class OptionalOperator<T>(private val parser: Parser<T>) : Parser<T?> {

    private var defaultMapper: (() -> T)? = null

    fun orDefault(mapper: () -> T) = this.also {
        defaultMapper = mapper
    }

    override fun parse(buffer: CharBuffer): ParseResult<T?> {
        val start = buffer.position()
        try {
            val result = parser.parse(buffer)
            if (result.getStatus()) return ParseResult.success(result.getValue())
            throw IllegalStateException()
        } catch (_: Throwable) {
            buffer.position(start)
            return ParseResult.success(defaultMapper?.let { it() })
        }
    }

}

class LiteralOperator(private val literal: String) : Parser<String> {

    override fun parse(buffer: CharBuffer): ParseResult<String> {
        for (i in literal) {
            if (i != buffer.get()) return ParseResult.failure()
        }
        return ParseResult.success(literal)
    }

}

class RepeatingOperator<T>(private val parser: Parser<T>, private val min: Int = 0, private val max: Int = Int.MAX_VALUE) : Parser<List<T>> {

    override fun parse(buffer: CharBuffer): ParseResult<List<T>> {
        var current = 0
        val start = buffer.position()
        val result = mutableListOf<T>()
        while (true) {
            try {
                if (current >= max) break
                val parsed = parser.parse(buffer)
                if (parsed.getStatus()) result += parsed.getValue()
                else break
                current += 1
            } catch (_: Throwable) {
                break
            }
        }
        if (current in min .. max) return ParseResult.success(result)
        buffer.position(start)
        return ParseResult.failure()
    }

}
