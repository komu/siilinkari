package siilinkari.ast

sealed class ExpressionOrStatement {
    class Exp(val exp: Expression) : ExpressionOrStatement()
    class Stmt(val stmt: Statement) : ExpressionOrStatement()
}
