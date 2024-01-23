package kodash

import java.util.*

object Kodash {

    private val properties = Properties().also {
        it.load(this::class.java.classLoader.getResourceAsStream("META-INF/kodash.properties"))
    }

    fun getVersion(): String = properties.getProperty("version")

}