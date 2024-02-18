package kodash.data.tag

interface ITaggable<T : ITag<*>> {

    fun toTag(): T

}