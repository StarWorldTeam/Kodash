package kodash.logic

import org.junit.jupiter.api.Test

class LogicUtilTest {

    @Test
    fun `Matcher Logic`() {
        match("2") {
            case("2") { next() }
            case("2", "3") { true }
            default { false }
        }.also(::assert)
    }

}