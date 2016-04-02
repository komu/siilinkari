package siilinkari.types

import siilinkari.ast.Expression
import siilinkari.ast.Statement
import siilinkari.env.Binding
import siilinkari.env.StaticEnvironment
import siilinkari.env.VariableAlreadyBoundException
import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value

/**
 * Type-checker statements and expressions.
 *
 * Type-checker walks through the syntax tree, maintaining a [StaticEnvironment] mapping
 * identifiers to their types and validates that all types agree. If type-checking
 * succeeds, the checker will return a simplified and type checked tree where each
 * expression is annotated with [Type]. If the checking fails, it will throw a
 * [TypeCheckException].
 */
class TypeChecker(val environment: StaticEnvironment) {

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

private fun Expression.typeCheck(env: StaticEnvironment): TypedExpression = when (this) {
    is Expression.Lit    -> TypedExpression.Lit(value, value.type)
    is Expression.Ref    -> TypedExpression.Ref(env.lookupBinding(name, location))
    is Expression.Not    -> TypedExpression.Not(exp.typeCheckExpected(Type.Boolean, env))
    is Expression.Binary -> typeCheck(env)
    is Expression.Call   -> typeCheck(env)
}

private fun Expression.Call.typeCheck(env: StaticEnvironment): TypedExpression {
    val typedFunc = func.typeCheck(env)
    if (typedFunc.type !is Type.Function)
        throw TypeCheckException("expected function type for call, but got ${typedFunc.type}", location)

    val expectedArgTypes = typedFunc.type.argumentTypes

    if (args.size != expectedArgTypes.size)
        throw TypeCheckException("expected ${expectedArgTypes.size} arguments, but got ${args.size}", location)

    val typedArgs = args.mapIndexed { i, arg -> arg.typeCheckExpected(expectedArgTypes[i], env) }

    return TypedExpression.Call(typedFunc, typedArgs, typedFunc.type.returnType)
}

private fun Expression.Binary.typeCheck(env: StaticEnvironment): TypedExpression = when (this) {
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

private fun Expression.Binary.Plus.typeCheck(env: StaticEnvironment): TypedExpression {
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

private fun Expression.Binary.typeCheckMatching(env: StaticEnvironment): Pair<TypedExpression, TypedExpression> {
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

private fun Expression.typeCheckExpected(expectedType: Type, env: StaticEnvironment): TypedExpression =
    typeCheck(env).expectAssignableTo(expectedType, location)

private fun Statement.typeCheck(env: StaticEnvironment): TypedStatement = when (this) {
    is Statement.Exp ->
        TypedStatement.Exp(expression.typeCheck(env))
    is Statement.Assign -> {
        val binding = env.lookupBinding(variable, location)
        val typedLhs = expression.typeCheckExpected(binding.type, env)
        TypedStatement.Assign(binding, typedLhs)
    }
    is Statement.Var -> {
        val typed = expression.typeCheck(env)
        val binding = env.bindType(variable, typed.type, location)
        TypedStatement.Var(binding, typed)
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
    is Statement.StatementList -> {
        val childEnv = env.newScope()
        TypedStatement.StatementList(statements.map { it.typeCheck(childEnv) })
    }
}

private fun StaticEnvironment.lookupBinding(name: String, location: SourceLocation): Binding =
    this[name] ?: throw TypeCheckException("unbound variable '$name'", location)

private fun StaticEnvironment.bindType(name: String, type: Type, location: SourceLocation): Binding {
    try {
        return this.bind(name, type)
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
        is Value.PrimitiveFunction -> signature
    }
