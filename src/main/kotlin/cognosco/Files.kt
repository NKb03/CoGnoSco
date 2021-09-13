package cognosco

import java.io.File

object Files {
    private val root = File(System.getProperty("user.home")).resolve(".cognosco")

    fun getDirectory(dir: String) = root.resolve(dir).also { it.mkdir() }

    fun resolve(vararg path: String) = path.fold(root, File::resolve)

    init {
        root.mkdir()
    }

    val ily get() = getDirectory("ily")
}
