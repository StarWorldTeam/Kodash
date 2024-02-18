package kodash.data.tag

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import kodash.parse.*
import java.nio.CharBuffer

class StringTagParser : Parser<StringTag> {

    override fun parse(buffer: CharBuffer): ParseResult<StringTag> = Parsers.gotoWhenError(buffer) {
        var position: Int = buffer.position()
        val next = { position ++ }
        val char = { try { buffer[position] } catch (_: Throwable) { null } }
        var string = char()!!.toString()
        next()
        while (char() != '"' || buffer[position - 1] == '\\') {
            if (char() == null) return@gotoWhenError ParseResult.failure()
            if (char() == '\\') {
                if (buffer[position + 1] == 'u') {
                    string += char()
                    next()
                    string += char()
                    next()
                    while (char() in '0' .. '9' || char() in 'A' .. 'F' || char() in 'a' .. 'f') {
                        string += char()
                        next()
                    }
                    position --
                } else {
                    string += char()!!
                    next()
                    string += char()!!
                }
                if (buffer[position + 1] == '"') {
                    next()
                    break
                }
            } else {
                string += char()!!
            }
            next()
        }
        string += char()
        next()
        buffer.position(position)
        return@gotoWhenError ParseResult.success(
            StringTag(jacksonObjectMapper().readValue(string, jacksonTypeRef<String>()))
        )
    }

}

class NumberTagParser : Parser<NumberTag> {

    override fun parse(buffer: CharBuffer): ParseResult<NumberTag> {
        val numberType = Operators.character('i', 'l', 'b', 's', 'd', 'f', 'I', 'D')
        return Operators.choice(
            Operators.parser { charBuffer ->
                val sign = Operators.optional(Operators.character('+', '-'))
                    .orDefault { charArrayOf('+') }
                    .parseOrThrow(charBuffer).getValue()[0]
                Operators.character('.').parseOrThrow(charBuffer)
                val right = Operators.characters { it in '0' .. '9' }.parseOrThrow(charBuffer).getValue()
                val type = Operators.optional(numberType).orDefault { charArrayOf('d') }.parseOrThrow(charBuffer).getValue()[0]
                ParseResult.success(Triple(sign, Pair("", right.joinToString("")), type))
            },
            Operators.parser { charBuffer ->
                val sign = Operators.optional(Operators.character('+', '-'))
                        .orDefault { charArrayOf('+') }
                        .parse(charBuffer).getValue()[0]
                val left = Operators.characters { it in '0' .. '9' }.parseOrThrow(charBuffer).getValue()
                val pointAndRight = Operators.optional(
                    Operators.parser { optionalCharBuffer ->
                        Operators.character('.')
                        optionalCharBuffer.position(optionalCharBuffer.position() + 1)
                        val right = Operators.characters { it in '0' .. '9' }.parseOrThrow(optionalCharBuffer).getValue()
                        ParseResult.success(right)
                    }
                ).orDefault { charArrayOf() }.parseOrThrow(charBuffer).getValue()
                val type = Operators.optional(numberType).orDefault { charArrayOf(if (pointAndRight.isNotEmpty()) 'f' else 'i') }.parseOrThrow(charBuffer).getValue()[0]
                ParseResult.success(Triple(sign, Pair(left.joinToString(""), pointAndRight.joinToString("")), type))
            },
        ).parse(buffer).let { it ->
            val value = it.getValue()
            val sign = value.first
            val left = value.second.first.ifEmpty { "0" }
            val right = value.second.second.ifEmpty { "0" }
            val type = value.third
            when (type) {
                'i' -> "$sign$left".toInt()
                's' -> "$sign$left".toShort()
                'b' -> "$sign$left".toInt().toByte()
                'l' -> "$sign$left".toLong()
                'f' -> "$sign$left.${right}".toFloat()
                'd' -> "$sign$left.${right}".toDouble()
                'I' -> "$sign$left".toBigInteger()
                'D' -> "$sign$left.${right}".toBigDecimal()
                else -> "${sign}$left.${right}".toBigDecimal()
            }.let { ParseResult.success(NumberTag(it)) }
        }
    }

}

class NullTagParser : Parser<NullTag> {

    override fun parse(buffer: CharBuffer): ParseResult<NullTag> {
        Operators.choice(Operators.literal("null"), Operators.literal("unknown")).parseOrThrow(buffer)
        return ParseResult.success(NullTag())
    }

}

class CompoundTagParser : Parser<CompoundTag> {

    override fun parse(buffer: CharBuffer): ParseResult<CompoundTag> {
        val pair = Operators.parser { charBuffer ->
            Operators.whitespace().parseOrThrow(charBuffer)
            val name = StringTagParser().parseOrThrow(charBuffer).getValue()
            Operators.whitespace().parseOrThrow(charBuffer)
            Operators.character(':').parseOrThrow(charBuffer)
            Operators.whitespace().parseOrThrow(charBuffer)
            val value = TagParser().parseOrThrow(charBuffer).getValue()
            Operators.whitespace().parseOrThrow(charBuffer)
            ParseResult.success(Pair(name.read(), value))
        }
        Operators.character('{').parseOrThrow(buffer)
        Operators.whitespace().parseOrThrow(buffer)
        val first = Operators.optional(pair).parseOrThrow(buffer)
        val second = Operators.repeat(
            Operators.parser { charBuffer ->
                Operators.whitespace().parseOrThrow(charBuffer)
                Operators.character(',').parseOrThrow(charBuffer)
                Operators.whitespace().parseOrThrow(charBuffer)
                pair.parseOrThrow(charBuffer)
            }
        ).parseOrThrow(buffer)
        Operators.whitespace().parseOrThrow(buffer)
        Operators.character('}').parseOrThrow(buffer)
        return ParseResult.success(
            CompoundTag().also { tag ->
                val firstValue = first.getValueOrNull()
                if (firstValue != null) tag.put(firstValue.first, firstValue.second)
                second.getValue().forEach { tag.put(it.first, it.second) }
            }
        )
    }

}

class ListTagParser : Parser<ListTag> {

    override fun parse(buffer: CharBuffer): ParseResult<ListTag> {
        Operators.character('[').parseOrThrow(buffer)
        Operators.whitespace().parseOrThrow(buffer)
        val first = Operators.optional(TagParser()).parseOrThrow(buffer)
        val second = Operators.repeat(
            Operators.parser { charBuffer ->
                Operators.whitespace().parseOrThrow(charBuffer)
                Operators.character(',').parseOrThrow(charBuffer)
                Operators.whitespace().parseOrThrow(charBuffer)
                TagParser().parseOrThrow(charBuffer)
            }
        ).parseOrThrow(buffer)
        Operators.whitespace().parseOrThrow(buffer)
        Operators.character(']').parseOrThrow(buffer)
        return ParseResult.success(
            ListTag().also { tag ->
                val firstValue = first.getValueOrNull()
                if (firstValue != null) tag.add(firstValue)
                tag.addAll(second.getValue())
            }
        )
    }

}

class TagParser : Parser<ITag<*>> {

    @Suppress("UNCHECKED_CAST")
    override fun parse(buffer: CharBuffer): ParseResult<ITag<*>> {
        Operators.whitespace().parseOrThrow(buffer)
        val value = Operators.choice(
            StringTagParser() as Parser<ITag<*>>,
            NumberTagParser() as Parser<ITag<*>>,
            NullTagParser() as Parser<ITag<*>>,
            CompoundTagParser() as Parser<ITag<*>>,
            ListTagParser() as Parser<ITag<*>>
        ).parseOrThrow(buffer).getValue().let {
            ParseResult.success(it)
        }
        Operators.whitespace().parseOrThrow(buffer)
        return value
    }

}
