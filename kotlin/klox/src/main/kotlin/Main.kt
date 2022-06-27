import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val errorReporter = ErrorReporter()
    if (args.size > 1) {
        println("Usage: klox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0], errorReporter)
    } else {
        runPrompt(errorReporter)
    }
}

fun runFile(path: String, errorReporter: ErrorReporter) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes, Charset.defaultCharset()), errorReporter)
    if (errorReporter.hadError) {
        exitProcess(65)
    }
}

fun runPrompt(errorReporter: ErrorReporter) {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run(line, errorReporter)
        errorReporter.reset()
    }
}

fun run(source: String, errorReporter: ErrorReporter) {
    val scanner = Scanner(source, errorReporter)
    val tokens = scanner.scanTokens()

    tokens.forEach { println(it) }
}
