package siilinkari.ast

import siilinkari.lexer.SourceLocation

/**
 * Represents statements of the program.
 *
 * In addition to the logical structure of the source code, each statement
 * contains [location] referring to the location in source code where this
 * statement is written. It is used to provide context for error messages.
 *
 * The string representation of statements does not try to mimic the original
 * source code, but rather provides a simple, compact and unambiguous syntax for
 * trees. Apart from debugging, its used in tests to verify the expected structure
 * of the parse tree.
 *
 * @see Expression
 */
sealed class Statement(val location: SourceLocation) {

    /** Statement containing a single expression. */
    class Exp(val expression: Expression) : Statement(expression.location) {
        override fun toString() = expression.toString()
    }

    /** Assignment to a variable. */
    class Assign(val variable: String, val expression: Expression, location: SourceLocation) : Statement(location) {
        override fun toString() = "[Assign $variable $expression]"
    }

    /** Definition of a variable. */
    class Var(val variable: String, val expression: Expression, location: SourceLocation) : Statement(location) {
        override fun toString() = "[Var $variable $expression]"
    }

    /** If-statement with optional else clause. */
    class If(val condition: Expression, val consequent: Statement, val alternative: Statement?, location: SourceLocation) : Statement(location) {
        override fun toString() = "[If $condition $consequent ${alternative ?: "[]"}]"
    }

    /** While-statement. */
    class While(val condition: Expression, val body: Statement, location: SourceLocation) : Statement(location) {
        override fun toString() = "[While $condition $body]"
    }

    /** List of statements */
    class StatementList(val statements: List<Statement>, location: SourceLocation) : Statement(location) {
        override fun toString() = "[StatementList $statements]"
    }
}
