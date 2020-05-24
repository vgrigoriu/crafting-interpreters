using System.Collections.Generic;
using System.Threading.Tasks;

namespace lox.net
{
    class Scanner
    {
        private readonly string source;

        private int start = 0;
        private int current = 0;

        private int line = 1;

        //private readonly List<Token> tokens = new List<Token>();

        public Scanner(string source)
        {
            this.source = source;
        }

        public async IAsyncEnumerable<Token> ScanTokensAsync()
        {
            while (!IsAtEnd())
            {
                // We are at the beginning of the next lexeme.
                start = current;
                var token = await ScanTokenAsync();
                if (token != null)
                {
                    yield return token;
                }
                // Else, either we ignored some whitespace, or we got an error but we keep on scanning.
            }

            yield return new Token(TokenType.EOF, "", null, line);
        }

        private bool IsAtEnd() =>
            current >= source.Length;

        private async Task<Token?> ScanTokenAsync()
        {
            char c = Advance();
            switch (c)
            {
                case '(':
                    return CreateToken(TokenType.LEFT_PAREN);
                case ')':
                    return CreateToken(TokenType.RIGHT_PAREN);
                case '{':
                    return CreateToken(TokenType.LEFT_BRACE);
                case '}':
                    return CreateToken(TokenType.RIGHT_BRACE);
                case ',':
                    return CreateToken(TokenType.COMMA);
                case '.':
                    return CreateToken(TokenType.DOT);
                case '-':
                    return CreateToken(TokenType.MINUS);
                case '+':
                    return CreateToken(TokenType.PLUS);
                case ';':
                    return CreateToken(TokenType.SEMICOLON);
                case '/':
                    if (Match('/'))
                    {
                        // Ignore everything until the end of the line.
                        while (Peek() != '\n' && !IsAtEnd())
                        {
                            Advance();
                        }
                        // TODO: Also store the content of the comment.
                        return CreateToken(TokenType.LINE_COMMENT);
                    }
                    else
                    {
                        return CreateToken(TokenType.SLASH);
                    }
                case '*':
                    return CreateToken(TokenType.STAR);
                case '!':
                    return CreateToken(Match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                case '=':
                    return CreateToken(Match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                case '<':
                    return CreateToken(Match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                case '>':
                    return CreateToken(Match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);

                case ' ':
                case '\r':
                case '\t':
                    // ignore whitespace
                    return null;

                case '\n':
                    line++;
                    return null;

                case '"':
                    return await StringAsync();

                default:
                    // TODO: Report the actual unexpected character.
                    // TODO: Group together a run of unexpected characrters in a single error.
                    await Program.ErrorAsync(line, "Unexpected character.");
                    return null;
            }
        }

        private async Task<Token?> StringAsync()
        {
            while (Peek() != '"' && !IsAtEnd())
            {
                if (Peek() == '\n')
                {
                    line++;
                }

                Advance();
            }

            // Unterminated string.
            if (IsAtEnd())
            {
                await Program.ErrorAsync(line, "Unterminated string.");
                return null;
            }

            // The closing ".
            Advance();

            var value = source.Substring(start + 1, current - start - 2);
            return CreateToken(TokenType.STRING, value);
        }

        private char Advance()
        {
            current++;
            return source[current - 1];
        }

        private Token CreateToken(TokenType type)
        {
            return CreateToken(type, null);
        }

        private Token CreateToken(TokenType type, object? literal)
        {
            var text = source.Substring(start, current - start);
            return new Token(type, text, literal, line);
        }

        private bool Match(char expected)
        {
            if (IsAtEnd())
            {
                return false;
            }
            
            if (source[current] != expected)
            {
                return false;
            }

            current++;
            return true;
        }

        private char Peek()
        {
            if (IsAtEnd())
            {
                return '\0';
            }

            return source[current];
        }
    }
}