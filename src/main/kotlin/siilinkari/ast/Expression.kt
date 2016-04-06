package siilinkari.ast

import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value

/**
 * Represents expressions of the program.
 *
 * In addition to the logical structure of the source code, each expression
 * contains [location] referring to the location in source code where this
 * expression is written. It is used to provide context for error messages.
 *
 * The string representation of expressions does not try to mimic the original
 * source code, but rather provides a simple, compact and unambiguous syntax for
 * trees. Apart from debugging, its used in tests to verify the expected structure
 * of the parse tree.
 *
 * @see Statement
 */
sealed class Expression(val location: SourceLocation) {

    /** Reference to a variable. */
    class Ref(val name: String, location: SourceLocation) : Expression(location) {
        override fun toString() = "[Ref $name]"
    }

    /** Literal value. */
    class Lit(val value: Value, location: SourceLocation) : Expression(location) {
        override fun toString() = "[Lit ${value.repr()}]"
    }

    /** Logical not. */
    class Not(val exp: Expression, location: SourceLocation): Expression(location) {
        override fun toString() = "[Not $exp]"
    }

    /** Function call. */
    class Call(val func: Expression, val args: List<Expression>) : Expression(func.location) {
        override fun toString() = "[Call $func $args]"
    }

    /** Binary operators. */
    sealed class Binary(val lhs: Expression, val rhs: Expression, location: SourceLocation): Expression(location) {
        override fun toString() = "[${javaClass.simpleName} $lhs $rhs]"

        /** lhs + rhs */
        class Plus(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)

        /** lhs - rhs */
        class Minus(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)

        /** lhs * rhs */
        class Multiply(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)

        /** lhs / rhs */
        class Divide(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)

        /** =, !=, <, >, <=, >= */
        class Relational(val op: RelationalOp, lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location) {
            override fun toString() = "[$op $lhs $rhs]"
        }
    }
}
