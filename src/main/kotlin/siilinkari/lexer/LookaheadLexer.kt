package siilinkari.lexer

/**
 * Adapts [Lexer] to implement single token lookahead.
 */
class LookaheadLexer(private val lexer: Lexer) {

    constructor(source: String): this(Lexer(source)) {
    }

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
    fun peekToken(): Token =
        peekTokenInfo().token

    /**
     * Returns the next token without consuming it.
     */
    fun peekTokenInfo(): TokenInfo<*> {
        val lookahead = this.lookahead ?: lexer.readToken()
        this.lookahead = lookahead
        return lookahead
    }

    fun nextTokenIs(token: Token): Boolean =
        hasMore && peekToken() == token

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
     * If the next token is [T], consume it and return its location.
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
