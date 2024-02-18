package kodash.data.tag

import org.junit.jupiter.api.Test
import java.nio.CharBuffer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TagParserTest {

    @Test
    fun `NullTag Parser Test`() {
        val buffer1 = CharBuffer.wrap("null")
        val buffer2 = CharBuffer.wrap("unknown")
        NullTagParser().parse(buffer1).getValue()
        NullTagParser().parse(buffer2).getValue()
        assertEquals(buffer1.toString().length, 0)
        assertEquals(buffer2.toString().length, 0)
    }

    @Test
    fun `NumberTag Parser Test`() {
        val buffer1 = CharBuffer.wrap("+.1f")
        val buffer2 = CharBuffer.wrap("+11.1D")
        NumberTagParser().parseOrThrow(buffer1).getValue()
        NumberTagParser().parseOrThrow(buffer2).getValue()
        assertEquals(buffer1.toString().length, 0)
        assertEquals(buffer2.toString().length, 0)
    }

    @Test
    fun `StringTag Parser Test`() {
        val buffer = CharBuffer.wrap("""
            "a,b,c,\\,\",\n,\u1000"
        """.trimIndent())
        StringTagParser().parseOrThrow(buffer).getValue()
        assertEquals(buffer.toString().length, 0)
    }

    @Test
    fun `CompoundTag Parser Test`() {
        val buffer = CharBuffer.wrap("""
            { "ab" : "1", "cd": null, "ef": unknown, "gh": 1, "ij": [1, -.2, {}] }
        """.trimIndent())
        CompoundTagParser().parseOrThrow(buffer).getValue().let {
            CompoundTagParser().parseOrThrow(CharBuffer.wrap(it.toString())).getValue()
        }
        assertEquals(buffer.toString().length, 0)
    }

    @Test
    fun `ListTag Parser Test`() {
        val buffer = CharBuffer.wrap("""
            [1, 1i, -1, -1i, .1, -.1, .1i, -.1i]
        """.trimIndent())
        ListTagParser().parseOrThrow(buffer).getValue().let {
            ListTagParser().parseOrThrow(CharBuffer.wrap(it.toString())).getValue()
        }
        assertEquals(buffer.toString().length, 0)
    }

    @Test
    fun `TagParser Test`() {
        val buffer = CharBuffer.wrap(
            """
                [
                    +1.0i, -.2d, "s t r i n g\\\"\\", null,
                    {}, [[[[{"a":10D}]]]]
                ]
            """.trimIndent()
        )
        val parsed = TagParser().parseOrThrow(buffer).getValueOrNull()
        assertNotNull(parsed)
        assertIs<ListTag>(parsed)
        assertEquals(buffer.toString().length, 0)
    }

}