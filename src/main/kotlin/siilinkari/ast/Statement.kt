package siilinkari.ast

import siilinkari.lexer.SourceLocation

sealed class Statement(val location: SourceLocation) {

    class Exp(val expression: Expression) : Statement(expression.location) {
        override fun toString() = expression.toString()
    }

    class Assign(val variable: String, val expression: Expression, location: SourceLocation) : Statement(location) {
        override fun toString() = "[Assign $variable $expression]"
    }

    class Var(val variable: String, val expression: Expression, location: SourceLocation) : Statement(location) {
        override fun toString() = "[Var $variable $expression]"
    }

    class If(val condition: Expression, val consequent: Statement, val alternative: Statement?, location: SourceLocation) : Statement(location) {
        override fun toString() = "[If $condition $consequent ${alternative ?: "[]"}]"
    }

    class While(val condition: Expression, val body: Statement, location: SourceLocation) : Statement(location) {
        override fun toString() = "[While $condition $body]"
    }

    class StatementList(val statements: List<Statement>, location: SourceLocation) : Statement(location) {
        override fun toString() = "[StatementList $statements]"
    }
}
