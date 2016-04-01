package siilinkari.lexer

/**
 * [Token] along its [SourceLocation] in the original source code.
 */
class TokenInfo<out T : Token>(val token: T, val location: SourceLocation) {
    operator fun component1() = token
    operator fun component2() = location

    override fun toString() = "[TokenInfo $token $location]"
}
