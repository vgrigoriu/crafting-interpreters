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
}