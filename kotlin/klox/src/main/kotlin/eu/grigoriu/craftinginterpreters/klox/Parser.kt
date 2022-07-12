package eu.grigoriu.craftinginterpreters.klox

import eu.grigoriu.craftinginterpreters.klox.TokenType.*

class Parser(private val tokens: List<Token>, private val errorReporter: ErrorReporter) {
    // program        → declaration* EOF ;
    fun parse(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()
        while (!isAtEnd()) {
            statements.add(declaration())
        }

        return statements
    }

    private class ParseError : RuntimeException()

    private var current = 0

    // declaration    → varDecl | statement ;
    private fun declaration(): Stmt? {
        try {
            if (match(VAR)) {
                return varDeclaration()
            }

            return statement()
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    // varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
    private fun varDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null
        if (match(EQUAL)) {
            initializer = expression()
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    // statement      → exprStmt | printStmt ;
    private fun statement(): Stmt {
        return if (match(PRINT)) {
            printStatement()
        } else {
            expressionStatement()
        }

    }

    // printStmt      → "print" expression ";" ;
    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    // exprStmt       → expression ";" ;
    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    // expression     → assignment ;
    private fun expression(): Expr {
        return assignment()
    }

    // assignment     → IDENTIFIER "=" assignment | equality ;
    private fun assignment(): Expr {
        val expr = equality()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
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

    //primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
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

        if (match(IDENTIFIER)) {
            return Expr.Variable(previous())
        }

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
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