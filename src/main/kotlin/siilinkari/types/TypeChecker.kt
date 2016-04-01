package siilinkari.types

import siilinkari.ast.Expression
import siilinkari.ast.Statement
import siilinkari.ast.TypedStatement
import siilinkari.eval.UnboundVariableException
import siilinkari.eval.VariableAlreadyBoundException
import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value

class TypeChecker(val environment: TypeEnvironment) {

    fun typeCheck(stmt: Statement): TypedStatement =
        stmt.typeCheck(environment)

    fun typeCheck(stmt: Expression): TypedExpression =
        stmt.typeCheck(environment)
}

private fun Expression.typeCheck(env: TypeEnvironment): TypedExpression = when (this) {
    is Expression.Lit    -> TypedExpression.Lit(value, value.type)
    is Expression.Ref    -> TypedExpression.Ref(name, env.lookupType(name, location))
    is Expression.Not    -> TypedExpression.Not(exp.typeCheckExpected(Type.Boolean, env))
    is Expression.Binary -> typeCheck(env)
}

private fun Expression.Binary.typeCheck(env: TypeEnvironment): TypedExpression.Binary = when (this) {
    is Expression.Binary.Plus      -> TypedExpression.Binary.Plus(lhs.typeCheckExpected(Type.Int, env), rhs.typeCheckExpected(Type.Int, env), Type.Int)
    is Expression.Binary.Minus     -> TypedExpression.Binary.Minus(lhs.typeCheckExpected(Type.Int, env), rhs.typeCheckExpected(Type.Int, env), Type.Int)
    is Expression.Binary.Equals    -> { val (l, r) = typeCheckMatching(env); TypedExpression.Binary.Equals(l, r) }
    is Expression.Binary.NotEquals -> { val (l, r) = typeCheckMatching(env); TypedExpression.Binary.NotEquals(l, r) }
}

private fun Expression.Binary.typeCheckMatching(env: TypeEnvironment): Pair<TypedExpression, TypedExpression> {
    val typedLhs = lhs.typeCheck(env)
    val typedRhs = rhs.typeCheck(env)

    if (typedLhs.type != typedRhs.type)
        throw TypeCheckException("lhs type ${typedLhs.type} did not match rhs type ${typedRhs.type}", location)

    return Pair(typedLhs, typedRhs)
}

private fun TypedExpression.expectAssignableTo(expectedType: Type, location: SourceLocation): TypedExpression =
    if (type == expectedType)
        this
    else
        throw TypeCheckException("expected type $expectedType, but was $type", location)

private fun Expression.typeCheckExpected(expectedType: Type, env: TypeEnvironment): TypedExpression =
    typeCheck(env).expectAssignableTo(expectedType, location)

private fun Statement.typeCheck(env: TypeEnvironment): TypedStatement = when (this) {
    is Statement.Exp           -> TypedStatement.Exp(expression.typeCheck(env))
    is Statement.Assign        -> TypedStatement.Assign(variable, expression.typeCheckExpected(env.lookupType(variable, location), env))
    is Statement.Var           -> {
        val typed = expression.typeCheck(env)
        env.bindType(variable, typed.type, location)
        TypedStatement.Var(variable, typed)
    }
    is Statement.If            -> TypedStatement.If(condition.typeCheckExpected(Type.Boolean, env), consequent.typeCheck(env), alternative?.typeCheck(env))
    is Statement.While         -> TypedStatement.While(condition.typeCheckExpected(Type.Boolean, env), body.typeCheck(env))
    is Statement.StatementList -> TypedStatement.StatementList(statements.map { it.typeCheck(env) })
}

private fun TypeEnvironment.lookupType(name: String, location: SourceLocation): Type =
    try {
        this[name]
    } catch (e: UnboundVariableException) {
        throw TypeCheckException("unbound variable '$name'", location)
    }

private fun TypeEnvironment.bindType(name: String, type: Type, location: SourceLocation) {
    try {
        this.bind(name, type)
    } catch (e: VariableAlreadyBoundException) {
        throw TypeCheckException("variable already bound '$name'", location)
    }
}

val Value.type: Type
    get() = when (this) {
        is Value.String  -> Type.String
        is Value.Bool    -> Type.Boolean
        is Value.Integer -> Type.Int
    }
