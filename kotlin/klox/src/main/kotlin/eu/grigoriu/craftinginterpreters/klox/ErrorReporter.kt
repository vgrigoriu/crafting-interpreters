package eu.grigoriu.craftinginterpreters.klox

abstract class ErrorReporter {
    var hadError = false
        private set
    var hadRuntimeError = false
        private set

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun error(token: Token, message: String) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '${token.lexeme}'", message)
        }
    }

    fun runtimeError(error: RuntimeError) {
        reportRuntimeInternal(error.message, error.token.line)
        hadRuntimeError = true
    }

    private fun report(line: Int, where: String, message: String) {
        reportInternal(line, where, message)
        hadError = true
    }

    protected abstract fun reportInternal(line: Int, where: String, message: String)
    protected abstract fun reportRuntimeInternal(message: String?, line: Int)

    fun reset() {
        hadError = false
        hadRuntimeError = false
    }
}

class ConsoleErrorReporter: ErrorReporter() {
    override fun reportInternal(line: Int, where: String, message: String) {
        println("[line $line] Error$where: $message")
    }

    override fun reportRuntimeInternal(message: String?, line: Int) {
        System.err.println("$message\n[line $line]")
    }
}
