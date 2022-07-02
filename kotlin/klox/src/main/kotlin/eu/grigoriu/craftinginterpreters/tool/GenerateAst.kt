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
            Type("Binary", "Expr left, Token operator, Expr right"),
            Type("Grouping", "Expr expression"),
            Type("Literal", "Any value"),
            Type("Unary", "Token operator, Expr right"),
        )
    )
}

data class Type(val className: String, val fields: String)

private fun defineAst(outputDir: String, baseName: String, types: List<Type>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, Charsets.UTF_8)

    writer.println("package eu.grigoriu.craftinginterpreters.klox")
    writer.println()
    writer.println("abstract class $baseName {")

    for (type in types) {
        defineType(writer, baseName, type)
    }

    writer.println("}")
    writer.close()
}

fun defineType(
    writer: PrintWriter,
    baseName: String,
    type: Type) {
    writer.println("    class ${type.className}(")

    // Store parameters in fields.
    val fields = type.fields.split(", ")
    for (field in fields) {
        val fieldType = field.split(" ")[0]
        val name = field.split(" ")[1]
        writer.println("        val $name: $fieldType,")
    }

    writer.println("    ) : $baseName()")
}
