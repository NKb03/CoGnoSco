package wittgenstein.lily

class LilypondWriterImpl(private val output: Appendable) : LilypondWriter {
    private var indent = ""
    private var newLine = true

    override fun append(str: String) {
        if (newLine) {
            output.append(indent)
            newLine = false
        }
        output.append(str)
    }

    override fun appendLine() {
        if (newLine) return
        output.appendLine()
        newLine = true
    }

    override fun appendLine(str: String) {
        append(str)
        appendLine()
    }

    override operator fun String.unaryPlus() {
        if (this.isNotEmpty()) appendLine()
        append(this)
    }

    override fun increaseIndent() {
        indent += "  "
    }

    override fun decreaseIndent() {
        indent = indent.dropLast(2)
    }

    override fun indented(block: () -> Unit) {
        increaseIndent()
        appendLine()
        block()
        appendLine()
        decreaseIndent()
    }

    override operator fun String.invoke(block: () -> Unit) {
        append(this)
        if (isNotEmpty()) append(" ")
        append("{")
        indented(block)
        appendLine("}")
    }

    override fun new(type: String, block: () -> Unit) {
        append("\\new ")
        append(type)
        appendLine(" <<")
        indented(block)
        appendLine(">>")
    }

    override fun include(resource: String) {
        val url = javaClass.getResource(resource) ?: error("resource $resource not found")
        appendLine()
        appendLine(url.readText())
    }
}