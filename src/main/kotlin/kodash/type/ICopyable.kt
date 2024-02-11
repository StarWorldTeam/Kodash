package kodash.type

interface ICopyable<T : Any> {
    fun copy(): T
}