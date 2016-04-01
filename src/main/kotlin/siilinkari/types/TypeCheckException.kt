package siilinkari.types

import siilinkari.lexer.SourceLocation

class TypeCheckException(val errorMessage: String, val sourceLocation: SourceLocation): RuntimeException("$errorMessage\n${sourceLocation.toLongString()}")
