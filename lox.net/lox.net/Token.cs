namespace lox.net
{
    public class Token
    {
        public Token(
            TokenType type,
            string lexeme,
            object? literal,
            int line)
        {
            Type = type;
            Lexeme = lexeme;
            Literal = literal;
            Line = line;
        }

        public TokenType Type { get; }
        public string Lexeme { get; }
        public object? Literal { get; }
        public int Line { get; }

        public override bool Equals(object? obj)
        {
            var other = obj as Token;
            return other != null && this.Type == other.Type && this.Lexeme == other.Lexeme;
        }

        public override int GetHashCode() => Type.GetHashCode() * 31 + Lexeme.GetHashCode();

        public override string ToString()
        {
            return $"{Type} {Lexeme} {Literal}";
        }
    }
}
