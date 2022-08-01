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

    // declaration    → classDecl | funDecl | varDecl | statement ;
    private fun declaration(): Stmt? {
        try {
            return when {
                match(CLASS) -> {
                    classDeclaration()
                }
                match(FUN) -> {
                    function("function")
                }
                match(VAR) -> {
                    varDeclaration()
                }
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun classDeclaration(): Stmt {
        val name = consume(IDENTIFIER, "Expect class name.")
        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Stmt.Function>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }

        consume(RIGHT_BRACE, "Expect '}' after class body")

        return Stmt.Class(name, methods)
    }

    // funDecl        → "fun" function ;
    // function       → IDENTIFIER "(" parameters? ")" block ;
    // parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
    private fun function(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect $kind name.")

        consume(LEFT_PAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."))
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")

        consume(LEFT_BRACE, "Expect '{' before $kind body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
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

    // statement      → exprStmt | forStmt | ifStmt | printStmt | returnStmt | whileStmt | block ;
    private fun statement(): Stmt {
        return if (match(FOR)) {
            forStatement()
        } else if (match(IF)) {
            ifStatement()
        } else if (match(PRINT)) {
            printStatement()
        } else if (match(RETURN)) {
            returnStatement()
        } else if (match(WHILE)) {
            whileStatement()
        } else if (match(LEFT_BRACE)) {
            Stmt.Block(block())
        } else {
            expressionStatement()
        }
    }

    // forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
    //                 expression? ";"
    //                 expression? ")" statement ;
    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = if (match(SEMICOLON)) {
            null
        } else if (match(VAR)) {
            varDeclaration()
        } else {
            expressionStatement()
        }

        val condition = if (!check(SEMICOLON)) {
            expression()
        } else {
            Expr.Literal(true)
        }
        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment = if (!check(RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        if (increment != null) {
            body = Stmt.Block(listOf(body, Stmt.Expression(increment)))
        }

        body = Stmt.While(condition, body)

        if (initializer != null) {
            body = Stmt.Block(listOf(initializer, body))
        }

        return body
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

    // returnStmt     → "return" expression? ";" ;
    private fun returnStatement(): Stmt {
        val keyword = previous()
        val value = if (!check(SEMICOLON)) {
            expression()
        } else {
            null
        }

        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
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

    // assignment     → ( call "." )? IDENTIFIER "=" assignment | logic_or ;
    private fun assignment(): Expr {
        val expr = or()

        if (match(EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            } else if (expr is Expr.Get) {
                return Expr.Set(expr.obj, expr.name, value)
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

    // unary          → ( "!" | "-" ) unary | call ;
    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return call()
    }

    // call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
    private fun call(): Expr {
        var expr = primary()

        while (true) {
            expr = if (match(LEFT_PAREN)) {
                finishCall(expr)
            } else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                Expr.Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    // arguments      → expression ( "," expression )* ;
    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments.add(expression())
            } while (match(COMMA))
        }

        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    //primary        → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;
    private fun primary(): Expr {
        return when {
            match(FALSE) -> {
                Expr.Literal(false)
            }
            match(TRUE) -> {
                Expr.Literal(true)
            }
            match(NIL) -> {
                Expr.Literal(null)
            }
            match(NUMBER, STRING) -> {
                Expr.Literal(previous().literal)
            }
            match(THIS) -> {
                Expr.This(previous())
            }
            match(IDENTIFIER) -> {
                Expr.Variable(previous())
            }
            match(LEFT_PAREN) -> {
                val expr = expression()
                consume(RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression.")
        }

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

    private fun leftAssocExpr(
        operand: () -> Expr,
        ctor: (Expr, Token, Expr) -> Expr,
        vararg operators: TokenType
    ): Expr {
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
