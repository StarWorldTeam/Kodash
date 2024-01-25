package kodash.type

import kodash.function.memorize

object None {

    override fun toString() = memorize {
        this::class.qualifiedName!!
    }

}
