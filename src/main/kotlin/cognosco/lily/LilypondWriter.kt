package cognosco.lily

interface LilypondWriter {
    fun append(str: String)
    fun appendLine()
    fun appendLine(str: String)

    operator fun String.unaryPlus()
    fun increaseIndent()
    fun decreaseIndent()
    fun indented(block: () -> Unit)

    operator fun String.invoke(block: () -> Unit)
    fun new(type: String, block: () -> Unit)
    fun includeResource(resource: String)
    fun includePath(path: String)
}