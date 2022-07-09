package eu.grigoriu.craftinginterpreters.klox

import eu.grigoriu.craftinginterpreters.klox.TokenType.*

class Interpreter : Expr.Visitor<Any?> {
    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            BANG -> !isTruthy(right)
            MINUS -> -(right as Double)
            else -> null
        }
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            MINUS -> left as Double - right as Double
            SLASH -> left as Double / right as Double
            STAR -> left as Double * right as Double
            PLUS -> {
                if (left is Double && right is Double) {
                    left + right
                } else if (left is String && right is String) {
                    left + right
                } else {
                    null
                }
            }
            GREATER -> left as Double > right as Double
            GREATER_EQUAL -> left as Double >= right as Double
            LESS -> (left as Double) < right as Double
            LESS_EQUAL -> left as Double <= right as Double
            BANG_EQUAL -> left != right
            EQUAL_EQUAL -> left == right
            else -> null
        }
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun isTruthy(obj: Any?): Boolean {
        return when (obj) {
            null -> false
            is Boolean -> obj
            else -> true
        }
    }
}