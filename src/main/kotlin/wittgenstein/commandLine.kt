package wittgenstein

import java.io.File

val workingDirectory get() = File(".")

fun File?.run(vararg command: String): Process = ProcessBuilder(*command)
    .inheritIO()
    .directory(this ?: workingDirectory)
    .start()

fun Process.join() {
    val exitCode = waitFor()
    if (exitCode != 0) {
        error("Process '${this}' finished with non-zero exit code")
    }
}

