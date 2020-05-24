using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;

namespace lox.net.Tests
{
    public class ScannerTests
    {
        [Fact]
        public async Task ScansAnEmptyStringAsync()
        {
            var scanner = new Scanner("");
            var result = scanner.ScanTokensAsync();

            await AssertEqualAsync(new List<Token>{new Token(TokenType.EOF, "", null, 1)}, result);
        }

        [Fact]
        public async Task ScansASingleOperatorAsync()
        {
            var scanner = new Scanner("*");
            var result = scanner.ScanTokensAsync();

            await AssertEqualAsync(
                new List<Token>
                {
                    new Token(TokenType.STAR, "*", null, 1),
                    new Token(TokenType.EOF, "", null, 1),
                },
                result);
        }

        [Fact]
        public async Task ScansLineComemntsAsync()
        {
            var scanner = new Scanner("() // this is a comment");
            var result = scanner.ScanTokensAsync();

            await AssertEqualAsync(
                new List<Token>
                {
                    new Token(TokenType.LEFT_PAREN, "(", null, 1),
                    new Token(TokenType.RIGHT_PAREN, ")", null, 1),
                    new Token(TokenType.LINE_COMMENT, "// this is a comment", null, 1),
                    new Token(TokenType.EOF, "", null, 1),
                },
                result);
        }

        [Fact]
        public async Task StringsAreMultiline()
        {
            var scanner = new Scanner("\"string\n  on\n  multiple\n  lines\"");
            var result = scanner.ScanTokensAsync();

            await AssertEqualAsync(
                new List<Token>
                {
                    new Token(TokenType.STRING, "\"string\n  on\n  multiple\n  lines\"", "string\n  on\n  multiple\n  lines", 1),
                    new Token(TokenType.EOF, "", null, 1),
                },
                result);
        }

        private async Task AssertEqualAsync<T>(IEnumerable<T> expected, IAsyncEnumerable<T> actual)
        {
            var expectedEnumerator = expected.GetEnumerator();
            var actualEnumerator = actual.GetAsyncEnumerator();
            var expectedHasNext = expectedEnumerator.MoveNext();
            var actualHasNext = await actualEnumerator.MoveNextAsync();
            while (expectedHasNext && actualHasNext)
            {
                Assert.Equal(expectedEnumerator.Current, actualEnumerator.Current);
                expectedHasNext = expectedEnumerator.MoveNext();
                actualHasNext = await actualEnumerator.MoveNextAsync();
            }

            Assert.False(actualHasNext,$"Was expecting more elements: {actualEnumerator.Current}");
            Assert.False(expectedHasNext, $"Got more elements than expected: {expectedEnumerator.Current}");
        }
    }
}
