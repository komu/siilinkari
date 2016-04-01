package siilinkari.lexer

import org.junit.Test
import siilinkari.lexer.Token.*
import siilinkari.objects.Value
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LookaheadLexerTest {

    @Test
    fun basicLookAhead() {
        val lexer = LookaheadLexer("foo 123 bar")

        assertTrue(lexer.hasMore)
        assertEquals(Identifier("foo"), lexer.peekToken().token)

        assertTrue(lexer.hasMore)
        assertEquals(Identifier("foo"), lexer.readToken().token)

        assertTrue(lexer.hasMore)
        assertEquals(Literal(Value.Integer(123)), lexer.readToken().token)

        assertTrue(lexer.hasMore)
        assertEquals(Identifier("bar"), lexer.peekToken().token)
        assertEquals(Identifier("bar"), lexer.peekToken().token)

        assertTrue(lexer.hasMore)
        assertEquals(Identifier("bar"), lexer.readToken().token)

        assertFalse(lexer.hasMore)
    }

    @Test
    fun conditionalReading() {
        val lexer = LookaheadLexer("foo fun ()")

        assertFalse(lexer.readNextIf(Identifier("bar")))
        assertFalse(lexer.readNextIf(Keyword.If))
        assertTrue(lexer.readNextIf(Identifier("foo")))

        assertFalse(lexer.readNextIf(Punctuation.LeftParen))
        assertTrue(lexer.readNextIf(Keyword.Fun))

        assertTrue(lexer.readNextIf(Punctuation.LeftParen))
        assertTrue(lexer.readNextIf(Punctuation.RightParen))

        assertFalse(lexer.hasMore)
    }

    @Test
    fun conditionalReadingWorksOnEndOfInput() {
        val lexer = LookaheadLexer("")

        assertFalse(lexer.readNextIf(Punctuation.LeftParen))
    }

    @Test
    fun expect() {
        val lexer = LookaheadLexer("()")

        assertEquals(1, lexer.expect(Punctuation.LeftParen).column)
        assertEquals(2, lexer.expect(Punctuation.RightParen).column)

        assertFalse(lexer.hasMore)
    }

    @Test
    fun unmetExpectThrowsError() {
        val lexer = LookaheadLexer("()")
        assertFailsWith<SyntaxErrorException> {
            lexer.expect(Keyword.Fun)
        }
    }
}
