package siilinkari.types

import siilinkari.lexer.SourceLocation

/**
 * Exception thrown if type checking code fails.
 */
class TypeCheckException(val errorMessage: String, val sourceLocation: SourceLocation) :
        RuntimeException("$errorMessage\n${sourceLocation.toLongString()}")
