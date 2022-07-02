package eu.grigoriu.craftinginterpreters.klox

import kotlin.test.Test
import kotlin.test.assertContentEquals

class ScannerTest {
    @Test
    fun parseEmptyString() {
        val sut = Scanner("", TestErrorReporter())

        val result = sut.scanTokens()

        assertContentEquals(listOf(Token(TokenType.EOF, "", null, 1)), result)
    }

    class TestErrorReporter : ErrorReporter() {
        override fun reportInternal(line: Int, where: String, message: String) {
            TODO("Not yet implemented")
        }
    }
}