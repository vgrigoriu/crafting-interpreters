package eu.grigoriu.craftinginterpreters.klox

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        TODO()
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return paranthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO()
    }

    override fun visitGetExpr(expr: Expr.Get): String {
        TODO("Not yet implemented")
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return paranthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return if (expr.value == null) {
            "nil"
        } else {
            expr.value.toString()
        }
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        TODO()
    }

    override fun visitSetExpr(expr: Expr.Set): String {
        TODO("Not yet implemented")
    }

    override fun visitThisExpr(expr: Expr.This): String {
        TODO("Not yet implemented")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return paranthesize(expr.operator.lexeme, expr.right)
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        TODO()
    }

    private fun paranthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()

        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }
}
