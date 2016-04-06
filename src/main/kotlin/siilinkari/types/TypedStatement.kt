package siilinkari.types

import siilinkari.env.Binding

/**
 * Represents a statement whose expressions have been type-checked.
 *
 * This is mostly analogous to statements in the original AST.
 *
 * @see TypedExpression
 */
sealed class TypedStatement {
    class Exp(val expression: TypedExpression) : TypedStatement() {
        override fun toString() = expression.toString()
    }

    class Assign(val variable: Binding, val expression: TypedExpression) : TypedStatement() {
        override fun toString() = "[Assign $variable $expression]"
    }

    class Var(val variable: Binding, val expression: TypedExpression) : TypedStatement() {
        override fun toString() = "Var $variable $expression]"
    }

    class If(val condition: TypedExpression, val consequent: TypedStatement, val alternative: TypedStatement?) : TypedStatement() {
        override fun toString() = "[If $condition $consequent ${alternative ?: "[]"}]"
    }

    class While(val condition: TypedExpression, val body: TypedStatement) : TypedStatement() {
        override fun toString() = "[While $condition $body]"
    }

    class StatementList(val statements: List<TypedStatement>) : TypedStatement() {
        override fun toString() = "[StatementList $statements]"
    }
}
