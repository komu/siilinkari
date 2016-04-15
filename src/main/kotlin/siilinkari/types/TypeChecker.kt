package siilinkari.types

import siilinkari.ast.Expression
import siilinkari.env.Binding
import siilinkari.env.StaticEnvironment
import siilinkari.env.VariableAlreadyBoundException
import siilinkari.lexer.SourceLocation
import siilinkari.objects.Value

/**
 * Type-checker for expressions.
 *
 * Type-checker walks through the syntax tree, maintaining a [StaticEnvironment] mapping
 * identifiers to their types and validates that all types agree. If type-checking
 * succeeds, the checker will return a simplified and type checked tree where each
 * expression is annotated with [Type]. If the checking fails, it will throw a
 * [TypeCheckException].
 */
fun Expression.typeCheck(env: StaticEnvironment): TypedExpression = when (this) {
    is Expression.Lit    -> TypedExpression.Lit(value, value.type)
    is Expression.Ref    -> TypedExpression.Ref(env.lookupBinding(name, location))
    is Expression.Not    -> TypedExpression.Not(exp.typeCheckExpected(Type.Boolean, env))
    is Expression.Binary -> typeCheck(env)
    is Expression.Call   -> typeCheck(env)
    is Expression.Assign -> {
        val binding = env.lookupBinding(variable, location)
        if (!binding.mutable)
            throw TypeCheckException("can't assign to immutable variable ${binding.name}", location)
        val typedLhs = expression.typeCheckExpected(binding.type, env)
        TypedExpression.Assign(binding, typedLhs)
    }
    is Expression.Var -> {
        val typed = expression.typeCheck(env)
        val binding = env.bindType(variable, typed.type, location, mutable)
        TypedExpression.Var(binding, typed)
    }
    is Expression.If -> {
        val typedCondition = condition.typeCheckExpected(Type.Boolean, env)
        val typedConsequent = consequent.typeCheck(env)
        val typedAlternative = alternative?.typeCheck(env)

        val type = if (typedAlternative != null && typedConsequent.type == typedAlternative.type)
            typedConsequent.type
        else
            Type.Unit

        TypedExpression.If(typedCondition, typedConsequent, typedAlternative, type)
    }
    is Expression.While -> {
        val typedCondition = condition.typeCheckExpected(Type.Boolean, env)
        val typedBody = body.typeCheck(env)
        TypedExpression.While(typedCondition, typedBody)
    }
    is Expression.ExpressionList -> {
        val childEnv = env.newScope()
        val expressions = expressions.map { it.typeCheck(childEnv) }
        val lastType = expressions.lastOrNull()?.type ?: Type.Unit
        TypedExpression.ExpressionList(expressions, lastType)
    }
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
    is Expression.Binary.Multiply -> {
        val typedLhs = lhs.typeCheckExpected(Type.Int, env)
        val typedRhs = rhs.typeCheckExpected(Type.Int, env)
        TypedExpression.Binary.Multiply(typedLhs, typedRhs, Type.Int)
    }
    is Expression.Binary.Divide -> {
        val typedLhs = lhs.typeCheckExpected(Type.Int, env)
        val typedRhs = rhs.typeCheckExpected(Type.Int, env)
        TypedExpression.Binary.Divide(typedLhs, typedRhs, Type.Int)
    }
    is Expression.Binary.And -> {
        val typedLhs = lhs.typeCheckExpected(Type.Boolean, env)
        val typedRhs = rhs.typeCheckExpected(Type.Boolean, env)
        TypedExpression.If(typedLhs, typedRhs, TypedExpression.Lit(Value.Bool.False), Type.Boolean)
    }
    is Expression.Binary.Or -> {
        val typedLhs = lhs.typeCheckExpected(Type.Boolean, env)
        val typedRhs = rhs.typeCheckExpected(Type.Boolean, env)
        TypedExpression.If(typedLhs, TypedExpression.Lit(Value.Bool.True), typedRhs, Type.Boolean)
    }
    is Expression.Binary.Relational -> {
        val (l, r) = typeCheckMatching(env)
        if (l.type == Type.Unit || l.type is Type.Function)
            throw TypeCheckException("can't compare values of type ${l.type}", location)
        TypedExpression.Binary.Relational(op, l, r)
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

fun TypedExpression.expectAssignableTo(expectedType: Type, location: SourceLocation): TypedExpression =
    if (type == expectedType)
        this
    else
        throw TypeCheckException("expected type $expectedType, but was $type", location)

fun Expression.typeCheckExpected(expectedType: Type, env: StaticEnvironment): TypedExpression =
    typeCheck(env).expectAssignableTo(expectedType, location)

private fun StaticEnvironment.lookupBinding(name: String, location: SourceLocation): Binding =
    this[name] ?: throw TypeCheckException("unbound variable '$name'", location)

private fun StaticEnvironment.bindType(name: String, type: Type, location: SourceLocation, mutable: Boolean): Binding {
    try {
        return this.bind(name, type, mutable)
    } catch (e: VariableAlreadyBoundException) {
        throw TypeCheckException("variable already bound '$name'", location)
    }
}
