package kodash.coroutine

import org.junit.jupiter.api.Test

class PromiseTest {

    @Test
    fun `Thread Promise`() {
        val promise = promise(ThreadPromise) {
            await(Promise.delay(1000))
            resolve(1)
        }.then { it + 1 }.then { it * 2 }.then { it / 4 }
        promise.awaitSync()
    }

    @Test
    fun `Async Promise`() {
        val promise = promise(AsyncPromise) {
            await(Promise.delay(1000))
            resolve(1)
        }.then { it + 1 }.then { it * 2 }.then { it / 4 }
        promise.awaitSync()
    }

}