package siilinkari.ast

import siilinkari.types.TypedExpression

sealed class TypedStatement {
    class Exp(val expression: TypedExpression) : TypedStatement()
    class Assign(val variable: String, val expression: TypedExpression) : TypedStatement()
    class Var(val variable: String, val expression: TypedExpression) : TypedStatement()
    class If(val condition: TypedExpression, val consequent: TypedStatement, val alternative: TypedStatement?) : TypedStatement()
    class While(val condition: TypedExpression, val body: TypedStatement) : TypedStatement()
    class StatementList(val statements: List<TypedStatement>) : TypedStatement()
}
