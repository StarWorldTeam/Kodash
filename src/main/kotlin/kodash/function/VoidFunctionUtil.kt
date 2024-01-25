package kodash.function

/**
 * 执行一个函数并忽视这个函数的结果
 */
fun runWithNoError(block: () -> Unit) {
    try {
        block()
    } catch (_: Throwable) {
    }
}

private val memory: MutableMap<String, Any?> = mutableMapOf()

@Suppress("UNCHECKED_CAST")
fun <T> memorize(block: () -> T): T {
    val stack = Throwable().stackTrace
    val caller = stack.getOrNull(1) ?: return block()
    val key = "module=${caller.moduleName}@${caller.moduleVersion};" +
            "classLoader=${caller.classLoaderName};" +
            "method=${caller.className}::${caller.methodName};" +
            "native=${caller.isNativeMethod};" +
            "file=${caller.fileName}@${caller.lineNumber}"
    return (memory.getOrPut(key, block) as T)!!
}
