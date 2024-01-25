package kodash.logic

import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class LogicUtilTest {

    @Test
    fun `Matcher Logic`() {
        match("2") {
            case("2") { next() }
            case("2", "3") { true }
            default { false }
        }.also { assertNotEquals(it, false) }
        matcher {
            case("1") { next() }
            case("1","2", null) { true }
            default { false }
        }.finally { _, output -> !output }.match(null, "1", "2").also { assertNotEquals(it.any { boolean -> boolean }, true) }
    }

}