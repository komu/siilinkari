package siilinkari.ast

import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value

sealed class Expression(val location: SourceLocation) {
    class Ref(val name: String, location: SourceLocation) : Expression(location) {
        override fun toString() = "[Ref $name]"
    }

    class Lit(val value: Value, location: SourceLocation) : Expression(location) {
        override fun toString() = "[Lit $value]"
    }

    class Not(val exp: Expression, location: SourceLocation): Expression(location) {
        override fun toString() = "[Not $exp]"
    }

    sealed class Binary(val lhs: Expression, val rhs: Expression, location: SourceLocation): Expression(location) {
        override fun toString() = "[${javaClass.simpleName} $lhs $rhs]"

        class Plus(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)
        class Minus(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)
        class Equals(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)
        class NotEquals(lhs: Expression, rhs: Expression, location: SourceLocation) : Binary(lhs, rhs, location)
    }
}
