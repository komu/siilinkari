package siilinkari.optimizer

import siilinkari.ast.RelationalOp
import siilinkari.env.Binding
import siilinkari.objects.Value
import siilinkari.types.TypedExpression
import siilinkari.types.TypedExpression.*
import siilinkari.types.TypedStatement
import siilinkari.types.TypedStatement.*
import java.util.*

fun TypedStatement.evaluateConstantExpressions(): TypedStatement = eval(ConstantBindingEnv())
fun TypedExpression.evaluateConstantExpressions(): TypedExpression = eval(ConstantBindingEnv())

private fun TypedStatement.eval(env: ConstantBindingEnv): TypedStatement = when (this) {
    is Exp              -> Exp(expression.eval(env))
    is Assign           -> Assign(variable, expression.eval(env))
    is Var              -> {
        val value = expression.eval(env)
        if (value is Lit && value.value.immutable && !variable.mutable)
            env[variable] = value.value
        Var(variable, value)
    }
    is If               -> eval(env)
    is While            -> eval(env)
    is StatementList    -> {
        val childEnv = env.child()
        statements.singleOrNull()?.eval(childEnv) ?: StatementList(statements.map { it.eval(childEnv) })
    }
}

private fun TypedExpression.eval(env: ConstantBindingEnv): TypedExpression = when (this) {
    is Ref      -> env[binding]?.let { Lit(it, type) } ?: this
    is Lit      -> this
    is Call     -> Call(func.eval(env), args.map { it.eval(env) }, type)
    is Not      -> eval(env)
    is Binary   -> eval(env)
}

private fun If.eval(env: ConstantBindingEnv): TypedStatement {
    val optCondition = condition.eval(env)
    return if (optCondition is Lit && optCondition.value is Value.Bool) {
        if (optCondition.value.value)
            consequent.eval(env.child())
        else
            alternative?.eval(env.child()) ?: StatementList(emptyList())
    } else {
        If(optCondition, consequent.eval(env.child()), alternative?.eval(env.child()));
    }
}

private fun While.eval(env: ConstantBindingEnv): TypedStatement {
    val optCondition = condition.eval(env)
    if (optCondition is Lit && optCondition.value is Value.Bool && optCondition.value.value == false) {
        return StatementList(emptyList())
    }
    return While(optCondition, body.eval(env.child()))
}

private fun Not.eval(env: ConstantBindingEnv): TypedExpression {
    val optExp = exp.eval(env)
    return if (optExp is Lit)
        Lit(!(optExp.value as Value.Bool))
    else
        optExp
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
                is Binary.Divide    -> return Lit(optLhs.value / optRhs.value)
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