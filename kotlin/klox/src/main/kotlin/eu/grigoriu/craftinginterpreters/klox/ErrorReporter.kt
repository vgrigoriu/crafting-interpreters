package eu.grigoriu.craftinginterpreters.klox

abstract class ErrorReporter {
    var hadError = false

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        reportInternal(line, where, message)
        hadError = true
    }

    protected abstract fun reportInternal(line: Int, where: String, message: String)

    fun reset() {
        hadError = true
    }
}

class ConsoleErrorReporter: ErrorReporter() {
    override fun reportInternal(line: Int, where: String, message: String) {
        println("[line $line] Error$where: $message")
    }
}
