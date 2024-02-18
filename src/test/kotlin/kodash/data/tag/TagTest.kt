package kodash.data.tag

import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class TagTest {

    @Test
    fun `Stringify Test`() {
        runCatching {
            CompoundTag().apply {
                val list = ListTag()
                list.add(list)
                putList("list", list)
                putCompound("tag", this)
                toString().let(::println)
            }
        }.let {
            assert(it.isFailure)
            assertIs<StackOverflowError>(it.exceptionOrNull())
        }
    }

}