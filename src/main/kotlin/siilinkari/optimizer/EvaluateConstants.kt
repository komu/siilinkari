package siilinkari.optimizer

import siilinkari.ast.RelationalOp
import siilinkari.env.Binding
import siilinkari.objects.Value
import siilinkari.types.TypedExpression
import siilinkari.types.TypedExpression.*
import java.util.*

fun TypedExpression.evaluateConstantExpressions(): TypedExpression = eval(ConstantBindingEnv())

private fun TypedExpression.eval(env: ConstantBindingEnv): TypedExpression = when (this) {
    is Ref      -> env[binding]?.let { Lit(it, type) } ?: this
    is Lit      -> this
    is Call     -> Call(func.eval(env), args.map { it.eval(env) }, type)
    is Not      -> eval(env)
    is Binary   -> eval(env)
    is Assign   -> Assign(variable, expression.eval(env))
    is Var      -> {
        val value = expression.eval(env)
        if (value is Lit && value.value.immutable && !variable.mutable)
            env[variable] = value.value
        Var(variable, value)
    }
    is If       -> eval(env)
    is While    -> eval(env)
    is ExpressionList   -> {
        val childEnv = env.child()
        expressions.singleOrNull()?.eval(childEnv) ?: ExpressionList(expressions.map { it.eval(childEnv) }, type)
    }
}

private fun If.eval(env: ConstantBindingEnv): TypedExpression {
    val optCondition = condition.eval(env)
    return if (optCondition is Lit && optCondition.value is Value.Bool) {
        if (optCondition.value.value)
            consequent.eval(env.child())
        else
            alternative?.eval(env.child()) ?: TypedExpression.Empty
    } else {
        If(optCondition, consequent.eval(env.child()), alternative?.eval(env.child()), type);
    }
}

private fun While.eval(env: ConstantBindingEnv): TypedExpression {
    val optCondition = condition.eval(env)
    if (optCondition is Lit && optCondition.value is Value.Bool && optCondition.value.value == false) {
        return TypedExpression.Empty
    }
    return While(optCondition, body.eval(env.child()))
}

private fun Not.eval(env: ConstantBindingEnv): TypedExpression {
    val optExp = exp.eval(env)
    return when (optExp) {
        is Lit -> Lit(!(optExp.value as Value.Bool))
        is Not -> optExp.exp
        else -> Not(optExp)
    }
}

private fun TypedExpression.Binary.eval(env: ConstantBindingEnv): TypedExpression {
    val optLhs = lhs.eval(env)
    val optRhs = rhs.eval(env)

    if (optLhs is Lit && optRhs is Lit) {
        if (this is Binary.Relational) {
            return Lit(Value.Bool(op.evaluate(optLhs.value, optRhs.value)))
        }

        if (optLhs.value is Value.String && this is Binary.ConcatString) {
            return Lit(optLhs.value + optRhs.value)
        }

        if (optLhs.value is Value.Integer && optRhs.value is Value.Integer) {
            when (this) {
                is Binary.Plus      -> return Lit(optLhs.value + optRhs.value)
                is Binary.Minus     -> return Lit(optLhs.value - optRhs.value)
                is Binary.Multiply  -> return Lit(optLhs.value * optRhs.value)
                is Binary.Divide    -> if (optRhs.value.value != 0) return Lit(optLhs.value / optRhs.value)
            }
        }
    }

    return when (this) {
        is Binary.Plus          -> Binary.Plus(optLhs, optRhs, type)
        is Binary.Minus         -> Binary.Minus(optLhs, optRhs, type)
        is Binary.Multiply      -> Binary.Multiply(optLhs, optRhs, type)
        is Binary.Divide        -> Binary.Divide(optLhs, optRhs, type)
        is Binary.ConcatString  -> Binary.ConcatString(optLhs, optRhs)
        is Binary.Relational    -> Binary.Relational(op, optLhs, optRhs)
    }
}

private fun RelationalOp.evaluate(lhs: Value, rhs: Value): Boolean {
    return when (this) {
        RelationalOp.Equals             -> lhs == rhs
        RelationalOp.NotEquals          -> lhs != rhs
        RelationalOp.LessThan           -> lhs.lessThan(rhs)
        RelationalOp.LessThanOrEqual    -> lhs.lessThan(rhs) || lhs == rhs
        RelationalOp.GreaterThan        -> rhs.lessThan(lhs)
        RelationalOp.GreaterThanOrEqual -> rhs.lessThan(lhs) || lhs == rhs
    }
}

/**
 * Environment containing all constant bindings.
 */
private class ConstantBindingEnv(private val parent: ConstantBindingEnv? = null) {

    private val constantBindings = HashMap<Binding, Value>()

    fun child(): ConstantBindingEnv = ConstantBindingEnv(this)

    operator fun set(binding: Binding, value: Value) {
        constantBindings[binding] = value
    }

    operator fun get(binding: Binding): Value? =
        constantBindings[binding] ?: parent?.get(binding)
}
