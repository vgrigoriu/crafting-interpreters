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
            Type("Binary", listOf(
                Field("Expr", "left"),
                Field("Token", "operator"),
                Field("Expr", "right"))),
            Type("Grouping", listOf(
                Field("Expr", "expression"))),
            Type("Literal", listOf(
                Field("Any?", "value"))),
            Type("Unary", listOf(
                Field("Token", "operator"),
                Field("Expr", "right"))),
        )
    )
    defineAst(outputDir, "Stmt", listOf(
        Type("Expression", listOf(
            Field("Expr", "expression"))),
        Type("Print", listOf(
            Field("Expr", "expression")))
    ))
}

data class Type(val className: String, val fields: List<Field>)
data class Field(val type: String, val name: String)

private fun defineAst(outputDir: String, baseName: String, types: List<Type>) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, Charsets.UTF_8)

    writer.println("package eu.grigoriu.craftinginterpreters.klox")
    writer.println()
    writer.println("abstract class $baseName {")

    defineVisitor(writer, baseName, types)

    for (type in types) {
        writer.println()
        defineType(writer, baseName, type)
    }

    // The base accept() method.
    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")

    writer.println("}")
    writer.close()
}

fun defineVisitor(writer: PrintWriter, baseName: String, types: List<Type>) {
    writer.println("    interface Visitor<R> {")

    for (type in types) {
        writer.println("        fun visit${type.className}$baseName(${baseName.lowercase()}: ${type.className}): R")
    }

    writer.println("    }")
}

fun defineType(
    writer: PrintWriter,
    baseName: String,
    type: Type
) {
    writer.println("    class ${type.className}(")

    // Store parameters in fields.
    for (field in type.fields) {
        writer.println("        val ${field.name}: ${field.type},")
    }

    writer.println("    ) : $baseName() {")

    // Visitor pattern.
    writer.println("        override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("            return visitor.visit${type.className}$baseName(this)")
    writer.println("        }")



    writer.println("    }")
}