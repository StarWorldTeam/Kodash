package kodash

import java.util.*

object Kodash {

    private val properties = Properties().also {
        it.load(this::class.java.classLoader.getResourceAsStream("META-INF/kodash.properties"))
    }

    /**
     * 获取 [Kodash] 版本号
     */
    fun getVersion(): String = properties.getProperty("version")

}