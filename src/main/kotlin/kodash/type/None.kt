package kodash.type

import kodash.function.memorize

object None : Any() {

    override fun toString() = memorize {
        this::class.qualifiedName!!
    }

}
