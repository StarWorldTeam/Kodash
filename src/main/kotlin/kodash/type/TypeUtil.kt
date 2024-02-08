package kodash.type

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

inline fun <reified T : Any> getClass(): KClass<T> = T::class
inline fun <reified T> getJavaClass(): Class<T> = T::class.java

class UnsafeScope internal constructor()

@OptIn(ExperimentalContracts::class)
fun <T> unsafe(block: UnsafeScope.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(UnsafeScope())
}

@Suppress("UnusedReceiverParameter", "UNCHECKED_CAST")
fun <T> UnsafeScope.cast(value: Any?): T = value as T

@Suppress("UnusedReceiverParameter")
fun UnsafeScope.exitProcess(code: Int) = Unit.apply {
    kotlin.system.exitProcess(code)
}
