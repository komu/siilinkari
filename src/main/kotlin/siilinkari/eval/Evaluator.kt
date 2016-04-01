package siilinkari.eval

import siilinkari.ast.Expression
import siilinkari.ast.Statement
import siilinkari.objects.Value
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement

class Evaluator {

    val environment = Environment()

    fun evaluateExpression(code: String): Value =
        parseExpression(code).evaluate(environment)

    fun evaluateStatement(code: String): Any? =
        parseStatement(code).evaluate(environment)

    private fun Statement.evaluate(env: Environment): Any? =
        when (this) {
            is Statement.Exp    -> expression.evaluate(env)
            is Statement.Var    -> bind(env, expression.evaluate(env))
            is Statement.Assign -> assign(env, expression.evaluate(env))
            is Statement.If     -> if (condition.evaluate(env) != Value.Bool.False) consequent.evaluate(env) else alternative?.evaluate(env)
            is Statement.While  -> while (condition.evaluate(env) != Value.Bool.False) body.evaluate(env)
            is Statement.StatementList -> statements.forEach { it.evaluate(env) }
        }

    private fun Expression.evaluate(env: Environment): Value = when (this) {
        is Expression.Lit       -> value
        is Expression.Ref       -> lookup(env)
        is Expression.Binary    -> evaluate(env)
        is Expression.Not       -> !exp.evaluateTo<Value.Bool>(env)
    }

    private fun Expression.Binary.evaluate(env: Environment): Value = when (this) {
        is Expression.Binary.Plus       -> lhs.evaluateTo<Value.Integer>(env) + rhs.evaluateTo<Value.Integer>(env)
        is Expression.Binary.Minus      -> lhs.evaluateTo<Value.Integer>(env) - rhs.evaluateTo<Value.Integer>(env)
        is Expression.Binary.Equals     -> Value.Bool(lhs.evaluate(env) == rhs.evaluate(env))
        is Expression.Binary.NotEquals  -> Value.Bool(lhs.evaluate(env) != rhs.evaluate(env))
    }

    private inline fun <reified T : Value> Expression.evaluateTo(env: Environment): T {
        val value = evaluate(env)
        if (value is T)
            return value
        else
            throw EvaluationException("could not convert $value to ${T::class.java.simpleName}", location)
    }

    private fun Expression.Ref.lookup(env: Environment): Value =
        try {
            env[name]
        } catch (e: UnboundVariableException) {
            throw EvaluationException("unbound variable '$name'", location)
        }

    private fun Statement.Var.bind(env: Environment, value: Value) {
        try {
            env.bind(variable, value)
        } catch (e: VariableAlreadyBoundException) {
            throw EvaluationException("variable '$variable' already bound", location)
        }
    }

    private fun Statement.Assign.assign(env: Environment, value: Value) {
        try {
            env[variable] = value
        } catch (e: UnboundVariableException) {
            throw EvaluationException("unbound variable '$variable'", location)
        }
    }
}
