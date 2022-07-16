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

    // statement      → exprStmt | ifStmt | printStmt | whileStmt | block ;
    private fun statement(): Stmt {
        return if (match(IF)) {
            ifStatement()
        } else if (match(PRINT)) {
            printStatement()
        } else if (match(WHILE)) {
            whileStatement()
        } else if (match(LEFT_BRACE)) {
            Stmt.Block(block())
        } else {
            expressionStatement()
        }

    }

    // ifStmt         → "if" "(" expression ")" statement ( "else" statement )? ;
    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = null
        if (match(ELSE)) {
            elseBranch = statement()
        }

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    // printStmt      → "print" expression ";" ;
    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    // whileStmt      → "while" "(" expression ")" statement ;
    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    // block          → "{" declaration* "}" ;
    private fun block(): List<Stmt?> {
        val statements = mutableListOf<Stmt?>()

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration())
        }

        consume(RIGHT_BRACE, "Expect '}' after block.")
        return statements
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

    // assignment     → IDENTIFIER "=" assignment | logic_or ;
    private fun assignment(): Expr {
        val expr = or()

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

    // logic_or       → logic_and ( "or" logic_and )* ;
    private fun or(): Expr {
        return leftAssocExpr(::and, Expr::Logical, OR)
    }

    // logic_and      → equality ( "and" equality )* ;
    private fun and(): Expr {
        return leftAssocExpr(::equality, Expr::Logical, AND)
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
        return leftAssocExpr(operand, Expr::Binary, *operators)
    }

    private fun leftAssocExpr(operand: () -> Expr, ctor: (Expr, Token, Expr) -> Expr, vararg operators: TokenType): Expr {
        var expr = operand()

        while (match(*operators)) {
            val operator = previous()
            val right = operand()
            expr = ctor(expr, operator, right)
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