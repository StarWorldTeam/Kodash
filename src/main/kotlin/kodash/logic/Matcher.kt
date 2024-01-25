package kodash.logic

open class MatcherError(reason: Throwable? = null) : RuntimeException(reason)

class MatcherBreak(reason: Throwable? = null) : MatcherError(reason)
class MatcherContinue : MatcherError()
class NotMatchException : MatcherError()

open class Matcher<T, R> {

    private val logic: MutableList<Pair<((T) -> Boolean), (Matcher<T, R>.(T) -> R)>> = mutableListOf()
    private var default: (Matcher<T, R>.(T) -> R)? = null

    /**
     * 添加用来匹配的子句
     */
    open fun case(vararg values: T, block: Matcher<T, R>.(T) -> R) = this.also {
        logic.add(Pair(values::contains, block))
    }

    /**
     * 添加用来匹配的子句
     */
    open fun case(validator: (T) -> Boolean, block: Matcher<T, R>.(T) -> R) = this.also {
        logic.add(Pair(validator, block))
    }

    /**
     * 设置默认子句；如果给定，这条子句会在 待匹配值 与任一 [case] 不匹配时执行
     */
    open fun default(block: Matcher<T, R>.(T) -> R) = this.also { default = block }

    /**
     * 进行匹配
     * @param value 待匹配值
     */
    open fun match(value: T): R {
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

    open fun match(vararg values: T): List <R> = values.map(::match)

    /** 跳过当前子句，执行下一个子句 */
    open fun next(): Nothing = throw MatcherContinue()
    /** 退出执行 */
    open fun exit(): Nothing = throw MatcherBreak()
    /** 引发错误 */
    open fun raise(reason: Throwable? = null): Nothing = throw MatcherBreak(reason)

    /**
     * 最终执行
     */
    fun <F> finally(block: Matcher<T, R>.(input: T, result: R) -> F): Matcher <T, F> {
        return object : Matcher<T, F>() {
            override fun match(value: T): F {
                val result = this@Matcher.match(value)
                return block(value, result)
            }
        }
    }

}

/**
 * @see Matcher
 * @param value 待匹配的值
 * @param block 用于构建 [Matcher] 的函数
 * @return 匹配后的值
 */
fun <T, R> match(value: T, block: Matcher<T, R>.() -> Unit) = Matcher<T, R>().also(block).match(value)

/**
 * @see Matcher
 * @param block 用于构建 [Matcher] 的函数
 * @return 匹配器
 */
fun <T, R> matcher(block: Matcher<T, R>.() -> Unit) = Matcher<T, R>().also(block)
