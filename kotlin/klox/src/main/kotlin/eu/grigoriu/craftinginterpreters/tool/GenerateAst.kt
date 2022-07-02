package eu.grigoriu.craftinginterpreters.tool

import java.io.PrintWriter
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }

    val outputDir = args[0]
    defineAst(
        outputDir, "Expr", listOf(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Any value",
            "Unary    : Token operator, Expr right",
        )
    )
}

private fun defineAst(outputDir: String, baseName: String, types: List<String>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, Charsets.UTF_8)

    writer.println("package eu.grigoriu.craftinginterpreters.klox")
    writer.println()
    writer.println("abstract class $baseName {")

    for (type in types) {
        val className = type.split(":")[0].trim()
        val fields = type.split(":")[1].trim()
        defineType(writer, baseName, className, fields)
    }

    writer.println("}")
    writer.close()
}

fun defineType(
    writer: PrintWriter,
    baseName: String,
    className: String,
    fieldList: String) {
    writer.println("    class $className(")

    // Store parameters in fields.
    val fields = fieldList.split(", ")
    for (field in fields) {
        val type = field.split(" ")[0]
        val name = field.split(" ")[1]
        writer.println("        val $name: $type,")
    }

    writer.println("    ): $baseName()")
}
