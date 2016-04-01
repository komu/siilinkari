package siilinkari.eval

import siilinkari.lexer.SourceLocation

class EvaluationException(val errorMessage: String, val sourceLocation: SourceLocation): RuntimeException("$errorMessage\n${sourceLocation.toLongString()}")
