package eu.grigoriu.craftinginterpreters.klox

import eu.grigoriu.craftinginterpreters.klox.TokenType.*

class Parser(private val tokens: List<Token>, private val errorReporter: ErrorReporter) {
    class ParseError : RuntimeException()

    private var current = 0

    // expression     → equality ;
    private fun expression(): Expr {
        return equality()
    }

    // equality       → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        return leftAssocExpr(::comparison, BANG_EQUAL, EQUAL_EQUAL)
    }

    // comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private fun comparison(): Expr {
        return leftAssocExpr(::term, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)
    }

    // term           → factor ( ( "-" | "+" ) factor )* ;
    private fun term(): Expr {
        return leftAssocExpr(::factor, MINUS, PLUS)
    }

    // factor         → unary ( ( "/" | "*" ) unary )* ;
    private fun factor(): Expr {
        return leftAssocExpr(::unary, SLASH, STAR)
    }

    // unary          → ( "!" | "-" ) unary | primary ;
    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    //primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
    private fun primary(): Expr {
        if (match(FALSE)) {
            return Expr.Literal(false)
        }

        if (match(TRUE)) {
            return Expr.Literal(true)
        }

        if (match(NIL)) {
            return Expr.Literal(null)
        }

        if (match(NUMBER, STRING)) {
            return Expr.Literal(previous().literal)
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw ParseError()
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) {
            return advance()
        }

        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        errorReporter.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return
            }

            when (peek().type) {
                CLASS, FUN, VAR, FOR, IF, WHILE, PRINT, RETURN -> return
                else -> advance()
            }
        }
    }

    private fun leftAssocExpr(operand: () -> Expr, vararg operators: TokenType): Expr {
        var expr = operand()

        while (match(*operators)) {
            val operator = previous()
            val right = operand()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }

        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }

        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }

        return previous()
    }

    private fun isAtEnd(): Boolean {
        return peek().type == EOF
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }
}