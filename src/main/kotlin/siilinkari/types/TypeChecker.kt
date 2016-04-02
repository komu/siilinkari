package siilinkari.types

import siilinkari.ast.Expression
import siilinkari.ast.Statement
import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value
import siilinkari.vm.UnboundVariableException
import siilinkari.vm.VariableAlreadyBoundException

/**
 * Type-checker statements and expressions.
 *
 * Type-checker walks through the syntax tree, maintaining a [TypeEnvironment] mapping
 * identifiers to their types and validates that all types agree. If type-checking
 * succeeds, the checker will return a simplified and type checked tree where each
 * expression is annotated with [Type]. If the checking fails, it will throw a
 * [TypeCheckException].
 */
class TypeChecker(val environment: TypeEnvironment) {

    /**
     * Type-check a [Statement].
     *
     * @throws TypeCheckException
     */
    fun typeCheck(stmt: Statement): TypedStatement =
        stmt.typeCheck(environment)

    /**
     * Type-check an [Expression].
     *
     * @throws TypeCheckException
     */
    fun typeCheck(stmt: Expression): TypedExpression =
        stmt.typeCheck(environment)
}

private fun Expression.typeCheck(env: TypeEnvironment): TypedExpression = when (this) {
    is Expression.Lit    -> TypedExpression.Lit(value, value.type)
    is Expression.Ref    -> TypedExpression.Ref(name, env.lookupType(name, location))
    is Expression.Not    -> TypedExpression.Not(exp.typeCheckExpected(Type.Boolean, env))
    is Expression.Binary -> typeCheck(env)
}

private fun Expression.Binary.typeCheck(env: TypeEnvironment): TypedExpression = when (this) {
    is Expression.Binary.Plus ->
        typeCheck(env)
    is Expression.Binary.Minus -> {
        val typedLhs = lhs.typeCheckExpected(Type.Int, env)
        val typedRhs = rhs.typeCheckExpected(Type.Int, env)
        TypedExpression.Binary.Minus(typedLhs, typedRhs, Type.Int)
    }
    is Expression.Binary.Equals    -> {
        val (l, r) = typeCheckMatching(env);
        TypedExpression.Binary.Equals(l, r)
    }
    is Expression.Binary.NotEquals -> {
        val (l, r) = typeCheckMatching(env);
        TypedExpression.Not(TypedExpression.Binary.Equals(l, r))
    }
}

private fun Expression.Binary.Plus.typeCheck(env: TypeEnvironment): TypedExpression {
    val typedLhs = lhs.typeCheck(env)

    return if (typedLhs.type == Type.String) {
        val typedRhs = rhs.typeCheck(env)
        TypedExpression.Binary.ConcatString(typedLhs, typedRhs)

    } else {
        val typedLhs2 = typedLhs.expectAssignableTo(Type.Int, lhs.location)
        val typedRhs = rhs.typeCheckExpected(Type.Int, env)
        TypedExpression.Binary.Plus(typedLhs2, typedRhs, Type.Int)
    }
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
    is Statement.Exp ->
        TypedStatement.Exp(expression.typeCheck(env))
    is Statement.Assign -> {
        val variableType = env.lookupType(variable, location)
        val typedLhs = expression.typeCheckExpected(variableType, env)
        TypedStatement.Assign(variable, typedLhs)
    }
    is Statement.Var -> {
        val typed = expression.typeCheck(env)
        env.bindType(variable, typed.type, location)
        TypedStatement.Var(variable, typed)
    }
    is Statement.If -> {
        val typedCondition = condition.typeCheckExpected(Type.Boolean, env)
        val typedConsequent = consequent.typeCheck(env)
        val typedAlternative = alternative?.typeCheck(env)
        TypedStatement.If(typedCondition, typedConsequent, typedAlternative)
    }
    is Statement.While -> {
        val typedCondition = condition.typeCheckExpected(Type.Boolean, env)
        val typedBody = body.typeCheck(env)
        TypedStatement.While(typedCondition, typedBody)
    }
    is Statement.StatementList ->
        TypedStatement.StatementList(statements.map { it.typeCheck(env) })
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

/**
 * Returns the [Type] associated with a literal [Value].
 */
val Value.type: Type
    get() = when (this) {
        is Value.String  -> Type.String
        is Value.Bool    -> Type.Boolean
        is Value.Integer -> Type.Int
    }
