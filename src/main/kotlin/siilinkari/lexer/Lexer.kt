package siilinkari.lexer

import siilinkari.lexer.Token.*
import siilinkari.objects.Value
import siilinkari.objects.Value.Bool

/**
 * Lexer converts source code to tokens to be used by parser.
 *
 * # Invariants
 *
 * Each time a token is read, all whitespace after the token is skipped and
 * the current position of the lexer points at the start of next token.
 */
class Lexer(private val source: String, private val file: String = "<unknown>") {

    private val lines = source.lines()
    private var position = 0
    private var line = 1
    private var column = 1

    init {
        skipWhitespace()
    }

    val hasMore: Boolean
        get() = position < source.length

    fun readToken(): TokenInfo<*> {
        val location = currentSourceLocation()

        val ch = peekChar()
        val token = when {
            ch.isLetter()   -> readSymbol()
            ch.isDigit()    -> readNumber()
            ch == '"'       -> readString()
            readIf('+')     -> Operator.Plus
            readIf('-')     -> Operator.Minus
            readIf('(')     -> Punctuation.LeftParen
            readIf(')')     -> Punctuation.RightParen
            readIf('{')     -> Punctuation.LeftBrace
            readIf('}')     -> Punctuation.RightBrace
            readIf(';')     -> Punctuation.Semicolon
            readIf('=')     -> if (readIf('=')) Operator.Equals else Punctuation.Equal
            readIf('!')     -> if (readIf('=')) Operator.NotEquals else Operator.Not
            else            -> fail("unexpected character '$ch'")
        }

        skipWhitespace()

        return TokenInfo(token, location)
    }

    private fun currentSourceLocation() = SourceLocation(file, line, column, lines[line - 1])

    private fun readSymbol(): Token {
        val str = readWhile { it.isLetter() }

        return when (str) {
            "else"      -> Keyword.Else
            "fun"       -> Keyword.Fun
            "if"        -> Keyword.If
            "var"       -> Keyword.Var
            "while"     -> Keyword.While
            "true"      -> Literal(Bool.True)
            "false"     -> Literal(Bool.False)
            else        -> Identifier(str)
        }
    }

    private fun readNumber(): Token {
        val value = readWhile { it.isDigit() }.toInt()

        return Literal(Value.Integer(value))
    }

    private fun readString(): Token {
        val sb = StringBuilder()
        var escape = false

        expect('"')

        while (hasMore) {
            val ch = readChar()
            when {
                escape      -> { sb.append(ch); escape = false }
                ch == '\\'  -> escape = true
                ch == '"'   -> return Literal(Value.String(sb.toString()))
                else        -> sb.append(ch)
            }
        }

        fail("unexpected end of string")
    }

    private fun peekChar(): Char {
        if (!hasMore) fail("unexpected end of input")
        return source[position]
    }

    private fun readIf(ch: Char): Boolean =
        if (hasMore && peekChar() == ch) {
            readChar()
            true
        } else {
            false
        }

    private fun skipWhitespace() {
        skipWhile { it.isWhitespace() }
    }

    private inline fun skipWhile(predicate: (Char) -> Boolean) {
        while (hasMore && predicate(source[position]))
            readChar()
    }

    private inline fun readWhile(predicate: (Char) -> Boolean): String {
        val start = position
        skipWhile(predicate)
        return source.substring(start, position)
    }

    private fun readChar(): Char {
        if (!hasMore) fail("unexpected end of input")

        val ch = source[position++]

        if (ch == '\n') {
            line++
            column = 1
        } else {
            column++
        }

        return ch
    }

    private fun expect(c: Char) {
        val ch = peekChar()
        if (ch == c)
            readChar()
        else
            fail("expected '$c', but got '$ch'")
    }

    private fun fail(message: String): Nothing =
        throw SyntaxErrorException(message, currentSourceLocation())
}
