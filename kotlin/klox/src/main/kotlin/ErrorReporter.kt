class ErrorReporter {
    var hadError = false

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun report(line: Int, where: String, message: String) {
        println("[line $line] Error$where: $message")
        hadError = true
    }

    fun reset() {
        hadError = true
    }
}