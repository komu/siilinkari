package siilinkari.lexer

/**
 * Exception thrown when there is a syntax error.
 *
 * Syntax errors can originate either in the lexer or the parser.
 */
open class SyntaxErrorException(val errorMessage: String, val sourceLocation: SourceLocation) :
        RuntimeException("$errorMessage\n${sourceLocation.toLongString()}")
