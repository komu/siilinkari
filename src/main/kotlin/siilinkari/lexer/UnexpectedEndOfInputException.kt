package siilinkari.lexer

/**
 * Exception thrown when there is a syntax error.
 *
 * Syntax errors can originate either in the lexer or the parser.
 */
class UnexpectedEndOfInputException(sourceLocation: SourceLocation) : SyntaxErrorException("unexpected end of input", sourceLocation)
