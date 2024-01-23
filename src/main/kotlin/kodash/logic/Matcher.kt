package kodash.logic

open class MatcherError(reason: Throwable? = null) : RuntimeException(reason)

class MatcherBreak(reason: Throwable? = null) : MatcherError(reason)
class MatcherContinue : MatcherError()
class NotMatchException : MatcherError()

class Matcher<T, R> {

    private val logic: MutableList<Pair<((T) -> Boolean), (Matcher<T, R>.(T) -> R)>> = mutableListOf()
    private var default: (Matcher<T, R>.(T) -> R)? = null

    fun case(vararg values: T, block: Matcher<T, R>.(T) -> R) = this.also {
        logic.add(Pair(values::contains, block))
    }

    fun case(value: T, block: Matcher<T, R>.(T) -> R) = this.also {
        logic.add(Pair({ value == it }, block))
    }

    infix fun default(block: Matcher<T, R>.(T) -> R) = this.also { default = block }

    fun match(value: T): R {
        for (i in logic) {
            try {
                if (i.first(value)) return i.second(this, value)
            } catch (_: MatcherContinue) {

            } catch (error: MatcherBreak) {
                break
            }
        }
        return default?.let {
            try {
                it(value)
            } catch (error: MatcherError) {
                return@let null
            }
        } ?: throw NotMatchException()
    }

    fun next(): Nothing = throw MatcherContinue()
    fun exit(): Nothing = throw MatcherBreak()
    fun raise(reason: Throwable? = null): Nothing = throw MatcherBreak(reason)

}

fun <T, R> match(value: T, block: Matcher<T, R>.() -> Unit) = Matcher<T, R>().also(block).match(value)
