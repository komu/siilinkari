package siilinkari.lexer

/**
 * Adapts [Lexer] to implement single token lookahead.
 *
 * Parsers generally need to peek one or more tokens ahead before making commitments
 * on what branch to take. Our grammar is simple enough so that we can manage with
 * single token lookahead. We could implement the lookahead either directly in [Lexer]
 * or in our parser, but doing that would needlessly complicate the code there.
 *
 * @param lexer to wrap with lookahead
 */
class LookaheadLexer(private val lexer: Lexer) {

    /**
     * Convenience constructor that creates the wrapped [Lexer] using given [source].
     */
    constructor(source: String): this(Lexer(source)) {
    }

    /**
     * The lookahead token.
     *
     * If our caller wants to peek a token, we need to read it from [lexer], thus consuming it.
     * However, we'll store it here so that we can pretend that it has not been consumed yet.
     *
     * If we are in sync with [lexer], then the lookahead is `null`,
     */
    private var lookahead: TokenInfo<*>? = null

    /**
     * Are there any more tokens in the input?
     */
    val hasMore: Boolean
        get() = lookahead != null || lexer.hasMore

    /**
     * Consumes and returns the next token.
     */
    fun readToken(): TokenInfo<*> {
        val lookahead = this.lookahead
        if (lookahead != null) {
            this.lookahead = null
            return lookahead
        } else {
            return lexer.readToken()
        }
    }

    /**
     * Returns the next token without consuming it.
     */
    fun peekToken(): TokenInfo<*> {
        val lookahead = this.lookahead ?: lexer.readToken()
        this.lookahead = lookahead
        return lookahead
    }

    /**
     * Returns the location of the next token.
     */
    fun nextTokenLocation(): SourceLocation =
        peekToken().location

    /**
     * Returns true if the next token is [token].
     */
    fun nextTokenIs(token: Token): Boolean =
        hasMore && peekToken().token == token

    /**
     * If the next token is [token], consume it and return `true`. Otherwise don't
     * consume the token and return `false`.
     */
    fun readNextIf(token: Token): Boolean {
        if (nextTokenIs(token)) {
            readToken()
            return true
        } else {
            return false
        }
    }

    /**
     * If the next token is [expected], consume it and return its location.
     * Otherwise throw [SyntaxErrorException].
     */
    fun expect(expected: Token): SourceLocation {
        val (token, location) = readToken()
        if (token == expected)
            return location
        else
            throw SyntaxErrorException("expected token $expected, but got $token", location)
    }

    /**
     * If the next token is [T], consume it and return it.
     * Otherwise throw [SyntaxErrorException].
     */
    inline fun <reified T : Token> readExpected(): TokenInfo<T> {
        val (token, location) = readToken()
        if (token is T)
            return TokenInfo(token, location)
        else
            throw SyntaxErrorException("expected token ${T::class}, but got $token", location)
    }
}
