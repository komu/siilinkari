package siilinkari.lexer

import siilinkari.lexer.Token.*
import siilinkari.objects.Value
import siilinkari.objects.Value.Bool

/**
 * Lexer converts source code to tokens to be used by parser.
 *
 * @param source the source code to parse
 * @param file name of the file containing the source code. Used for [SourceLocation].
 * @see Token
 * @see TokenInfo
 */
class Lexer(private val source: String, private val file: String = "<unknown>") {

    /**
     * Current position in the file (index to [source]).
     *
     * The lexer position always points to start of next token (or the end of input).
     *
     * The invariant is originally established by skipping all leading whitespace
     * right in the initialization of the class. Later the invariant is maintained
     * by skipping all whitespace after each token whenever a token is read.
     */
    private var position = 0

    /** Current line number. Used for [SourceLocation]. */
    private var line = 1

    /** Current column number. Used for [SourceLocation]. */
    private var column = 1

    /** Lines of the file. Used for [SourceLocation]. */
    private val lines = source.lines()

    init {
        skipWhitespace()
    }

    /**
     * Does the source contain more tokens?
     */
    val hasMore: Boolean
        get() = position < source.length

    /**
     * Read the next [Token] from the source, along with its [SourceLocation].
     */
    fun readToken(): TokenInfo<*> {
        val location = currentSourceLocation()

        val ch = peekChar()
        val token = when {
            ch.isLetter()   -> readSymbol()
            ch.isDigit()    -> readNumber()
            ch == '"'       -> readString()
            readIf('+')     -> Operator.Plus
            readIf('-')     -> Operator.Minus
            readIf('*')     -> Operator.Multiply
            readIf('/')     -> Operator.Divide
            readIf('(')     -> Punctuation.LeftParen
            readIf(')')     -> Punctuation.RightParen
            readIf('{')     -> Punctuation.LeftBrace
            readIf('}')     -> Punctuation.RightBrace
            readIf(';')     -> Punctuation.Semicolon
            readIf(',')     -> Punctuation.Comma
            readIf('=')     -> if (readIf('=')) Operator.EqualEqual else Punctuation.Equal
            readIf('!')     -> if (readIf('=')) Operator.NotEqual else Operator.Not
            readIf('<')     -> if (readIf('=')) Operator.LessThanOrEqual else Operator.LessThan
            readIf('>')     -> if (readIf('=')) Operator.GreaterThanOrEqual else Operator.GreaterThan
            else            -> fail("unexpected character '$ch'")
        }

        skipWhitespace()

        return TokenInfo(token, location)
    }

    /**
     * Reads a symbol.
     *
     * Symbols are either [keywords][Token.Keyword], [boolean literals][Token.Literal] or
     * [identifiers][Identifier].
     */
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

    /**
     * Reads a number literal.
     *
     * Currently only integers are supported.
     */
    private fun readNumber(): Token {
        val value = readWhile { it.isDigit() }.toInt()

        return Literal(Value.Integer(value))
    }

    /**
     * Reads a string literal.
     */
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

    /**
     * Returns the next character in source code without consuming it.
     */
    private fun peekChar(): Char {
        if (!hasMore) fail("unexpected end of input")
        return source[position]
    }

    /**
     * If next character is [ch], consume the character and return true.
     * Otherwise don't consume the character and return false.
     */
    private fun readIf(ch: Char): Boolean =
        if (hasMore && peekChar() == ch) {
            readChar()
            true
        } else {
            false
        }

    /**
     * Skip characters in input as long as [predicate] returns `true`.
     */
    private inline fun skipWhile(predicate: (Char) -> Boolean) {
        while (hasMore && predicate(source[position]))
            readChar()
    }

    /**
     * Read characters in input as long as [predicate] returns `true`
     * and return the string of read characters.
     */
    private inline fun readWhile(predicate: (Char) -> Boolean): String {
        val start = position
        skipWhile(predicate)
        return source.substring(start, position)
    }

    /**
     * Reads a single character from source code.
     *
     * This is the only place where [position] may be advanced, because
     * this method takes care of adjusting [line] and [column] accordingly.
     */
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

    /**
     * Consume next character if it is [ch]. Otherwise throws [SyntaxErrorException].
     */
    private fun expect(ch: Char) {
        val c = peekChar()
        if (ch == c)
            readChar()
        else
            fail("expected '$ch', but got '$c'")
    }

    /**
     * Skips all whitespace.
     */
    private fun skipWhitespace() {
        skipWhile { it.isWhitespace() }
    }

    /**
     * Returns current source location.
     */
    private fun currentSourceLocation() =
        SourceLocation(file, line, column, lines[line - 1])

    /**
     * Throws [SyntaxErrorException] with given [message] and current [SourceLocation].
     */
    private fun fail(message: String): Nothing =
        throw SyntaxErrorException(message, currentSourceLocation())
}
