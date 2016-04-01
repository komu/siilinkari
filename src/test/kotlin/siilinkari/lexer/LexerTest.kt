package siilinkari.lexer

import org.junit.Test
import siilinkari.lexer.Token.*
import siilinkari.objects.Value
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LexerTest {

    @Test
    fun emptySourceHasNoTokens() {
        assertNoTokens("")
        assertNoTokens("  ")
        assertNoTokens("  \n \n \t \t ")
    }

    @Test
    fun keywords() {
        assertTokens("if", Keyword.If)
        assertTokens("else", Keyword.Else)
        assertTokens("fun", Keyword.Fun)
        assertTokens("var", Keyword.Var)
        assertTokens("while", Keyword.While)
    }

    @Test
    fun identifiers() {
        assertTokens("foo", Identifier("foo"))
        assertTokens("bar", Identifier("bar"))
    }

    @Test
    fun operators() {
        assertTokens("+", Operator.Plus)
        assertTokens("-", Operator.Minus)
        assertTokens("==", Operator.EqualEqual)
        assertTokens("!=", Operator.NotEqual)
        assertTokens("!", Operator.Not)
    }

    @Test
    fun punctuation() {
        assertTokens("(", Punctuation.LeftParen)
        assertTokens(")", Punctuation.RightParen)
        assertTokens("{", Punctuation.LeftBrace)
        assertTokens("}", Punctuation.RightBrace)
        assertTokens("=", Punctuation.Equal)
    }

    @Test
    fun literalNumbers() {
        assertTokens("42", Literal(Value.Integer(42)))
    }

    @Test
    fun literalBooleans() {
        assertTokens("true", Literal(Value.Bool.True))
        assertTokens("false", Literal(Value.Bool.False))
    }

    @Test
    fun literalStrings() {
        assertTokens("\"\"", Literal(Value.String("")))
        assertTokens("\"foo\"", Literal(Value.String("foo")))
        assertTokens("\"bar \\\"baz\\\" quux\"", Literal(Value.String("bar \"baz\" quux")))
    }

    @Test
    fun unterminatedStringLiteral() {
        assertSyntaxError("\"bar")
    }

    @Test
    fun unexpectedCharacter() {
        assertSyntaxError("€")
    }

    @Test
    fun multipleTokens() {
        assertTokens("if (foo) \"bar\" else 42",
                Keyword.If, Punctuation.LeftParen, Identifier("foo"), Punctuation.RightParen, Literal(Value.String("bar")), Keyword.Else, Literal(Value.Integer(42)))
    }

    @Test
    fun tokenLocations() {
        val source = """
            |if (foo)
            |    bar()
            |else
            |    baz()
            """.trimMargin()
        val locations = readAllTokens(source).map { it.location }

        assertEquals(listOf(1, 1, 1, 1, 2, 2, 2, 3, 4, 4, 4), locations.map { it.line }, "lines")
        assertEquals(listOf(1, 1, 1, 1, 2, 2, 2, 3, 4, 4, 4), locations.map { source.lines().indexOf(it.lineText) + 1 }, "lineTexts")
        assertEquals(listOf(1, 4, 5, 8, 5, 8, 9, 1, 5, 8, 9), locations.map { it.column }, "columns")
    }

    @Test
    fun nextTokenOnEmptyThrowsSyntaxError() {
        val lexer = Lexer(" ")
        assertFailsWith<SyntaxErrorException> {
            lexer.readToken()
        }
    }

    private fun assertTokens(source: String, vararg tokens: Token) {
        assertEquals(tokens.asList(), readAllTokens(source).map { it.token })
    }

    private fun assertNoTokens(source: String) {
        assertTokens(source)
    }

    private fun assertSyntaxError(source: String) {
        assertFailsWith<SyntaxErrorException> {
            readAllTokens(source)
        }
    }

    private fun readAllTokens(source: String): List<TokenInfo<*>> {
        val lexer = Lexer(source)
        val result = ArrayList<TokenInfo<*>>()

        while (lexer.hasMore)
            result += lexer.readToken()

        return result
    }
}
