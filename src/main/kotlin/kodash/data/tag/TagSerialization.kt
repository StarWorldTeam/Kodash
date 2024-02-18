package kodash.data.tag

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import de.undercouch.bson4jackson.BsonFactory
import java.nio.CharBuffer

interface ITagSerializer <I: ITag<*>, O> {
    fun serialize(tag: I): O
}

interface ITagDeserializer <I, O : ITag<*>> {
    fun deserialize(input: I): O

}

object TagSerialization {

    fun getStringTagDeserializer() = object : ITagDeserializer <String, ITag<*>> {
        override fun deserialize(input: String) = TagParser().parseOrThrow(CharBuffer.wrap(input)).getValue()
    }

    fun getStringTagSerializer() = object : ITagSerializer<ITag<*>, String> {
        override fun serialize(tag: ITag<*>) = tag.toString()
    }

    fun getBinaryCompoundTagSerializer(): ITagSerializer<CompoundTag, ByteArray> = object : ITagSerializer<CompoundTag, ByteArray> {
        override fun serialize(tag: CompoundTag) = ObjectMapper(BsonFactory()).writeValueAsBytes(tag.read())
    }

    fun getBinaryCompoundTagDeserializer(): ITagDeserializer<ByteArray, CompoundTag> = object : ITagDeserializer<ByteArray, CompoundTag> {
        override fun deserialize(input: ByteArray) = CompoundTag().apply {
            putAll(
                ObjectMapper(BsonFactory())
                    .readValue(input, jacksonTypeRef<Map<String, Any?>>())
                    .mapValues { Tags.fromValue(it.value) }
                    .entries
            )
        }
    }

}
