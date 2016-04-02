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
    class Exp(val expression: TypedExpression) : TypedStatement()
    class Assign(val variable: Binding, val expression: TypedExpression) : TypedStatement()
    class Var(val variable: Binding, val expression: TypedExpression) : TypedStatement()
    class If(val condition: TypedExpression, val consequent: TypedStatement, val alternative: TypedStatement?) : TypedStatement()
    class While(val condition: TypedExpression, val body: TypedStatement) : TypedStatement()
    class StatementList(val statements: List<TypedStatement>) : TypedStatement()
}
