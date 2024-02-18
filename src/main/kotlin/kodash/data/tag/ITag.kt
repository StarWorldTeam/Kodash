package kodash.data.tag

import kodash.type.ICopyable

interface ITag <T> : ICopyable<ITag<T>> {

    fun read(): T

}