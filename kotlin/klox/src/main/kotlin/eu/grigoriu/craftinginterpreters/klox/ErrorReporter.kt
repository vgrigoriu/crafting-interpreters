package eu.grigoriu.craftinginterpreters.klox

abstract class ErrorReporter {
    var hadError = false

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

    private fun report(line: Int, where: String, message: String) {
        reportInternal(line, where, message)
        hadError = true
    }

    protected abstract fun reportInternal(line: Int, where: String, message: String)

    fun reset() {
        hadError = false
    }
}

class ConsoleErrorReporter: ErrorReporter() {
    override fun reportInternal(line: Int, where: String, message: String) {
        println("[line $line] Error$where: $message")
    }
}
