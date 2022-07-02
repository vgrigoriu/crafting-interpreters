package eu.grigoriu.craftinginterpreters.klox

class Token(
    private val type: TokenType,
    private val lexeme: String,
    // Can we make this generic?
    private val literal: Any?,
    line: Int
) {
    override fun toString(): String {
        return "$type $lexeme $literal"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (type != other.type) return false
        if (lexeme != other.lexeme) return false
        if (literal != other.literal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + lexeme.hashCode()
        result = 31 * result + (literal?.hashCode() ?: 0)
        return result
    }
}