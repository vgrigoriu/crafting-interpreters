class Scanner(
    private val source: String,
    private val errorReporter: ErrorReporter
) {
    private val tokens = mutableListOf<Token>()

    private var start = 0
    private var current = 0
    private var line = 1

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(TokenType.EOF, "", null, line))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> {
                addToken(TokenType.LEFT_PAREN)
            }
            ')' -> {
                addToken(TokenType.RIGHT_PAREN)
            }
            '{' -> {
                addToken(TokenType.LEFT_BRACE)
            }
            '}' -> {
                addToken(TokenType.RIGHT_BRACE)
            }
            ',' -> {
                addToken(TokenType.COMMA)
            }
            '.' -> {
                addToken(TokenType.DOT)
            }
            '-' -> {
                addToken(TokenType.MINUS)
            }
            '+' -> {
                addToken(TokenType.PLUS)
            }
            ';' -> {
                addToken(TokenType.SEMICOLON)
            }
            '*' -> {
                addToken(TokenType.STAR)
            }
            '!' -> {
                addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            }
            '=' -> {
                addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            }
            '<' -> {
                addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            }
            '>' -> {
                addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            }
            '/' -> {
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) {
                        advance()
                    }
                } else {
                    addToken(TokenType.SLASH)
                }
            }
            // Ignore whitespace.
            ' ', '\r', '\t' -> {}
            '\n' -> {
                line++
            }
            '"' -> {
                string()
            }
            else -> {
                if (isDigit(c)) {
                    number()
                } else {
                    errorReporter.error(line, "Unexpected character: $c")
                }
            }
        }
    }

    private fun advance(): Char {
        return source[current++]
    }

    private fun addToken(tokenType: TokenType, literal: Any? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(tokenType, text, literal, line))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) {
            return false
        }
        if (source[current] != expected) {
            return false
        }

        current++
        return true
    }

    private fun peek(): Char {
        return if (isAtEnd()) 0.toChar() else source[current]
    }

    private fun peekNext(): Char {
        if (current + 1 >= source.length) {
            return 0.toChar()
        }
        return source[current + 1]
    }

    private fun isAtEnd(): Boolean {
        return current >= source.length
    }

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            // Count lines inside of strings.
            if (peek() == '\n') {
                line++
            }
            advance()
        }

        if (isAtEnd()) {
            errorReporter.error(line, "Unterminated string.")
            return
        }

        // The closing ".
        advance()

        // Trim the surrounding quotes.
        val value = source.substring(start + 1, current - 1)
        addToken(TokenType.STRING, value)
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun number() {
        while (isDigit(peek())) {
            advance()
        }

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the ".".
            advance()

            while (isDigit(peek())) {
                advance()
            }
        }

        addToken(TokenType.NUMBER, source.substring(start, current).toDouble())
    }
}