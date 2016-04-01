package siilinkari.lexer

class SyntaxErrorException(val errorMessage: String, val sourceLocation: SourceLocation): RuntimeException("$errorMessage\n${sourceLocation.toLongString()}")
