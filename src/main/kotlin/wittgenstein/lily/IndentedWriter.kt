package wittgenstein.lily

class IndentedWriter(private val output: Appendable) {
    private var indent = ""
    private var newLine = true

    fun append(str: String) {
        if (newLine) {
            output.append(indent)
            newLine = false
        }
        output.append(str)
    }

    fun appendLine(str: String) {
        append(str)
        output.appendLine()
        newLine = true
    }

    fun indented(block: () -> Unit) {
        indent += "  "
        block()
        indent = indent.dropLast(2)
    }
}