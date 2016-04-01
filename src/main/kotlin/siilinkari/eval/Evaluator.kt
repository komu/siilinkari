package siilinkari.eval

import siilinkari.ast.TypedStatement
import siilinkari.objects.Value
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.types.TypeChecker
import siilinkari.types.TypeEnvironment
import siilinkari.types.TypedExpression
import siilinkari.types.type

class Evaluator {

    val environment = Environment()
    val typeEnvironment = TypeEnvironment()
    val typeChecker = TypeChecker(typeEnvironment)

    fun bind(name: String, value: Value) {
        typeEnvironment.bind(name, value.type)
        environment.bind(name, value)
    }

    fun evaluateExpression(code: String): Value {
        val exp = parseExpression(code)
        val typedExp = typeChecker.typeCheck(exp)
        return typedExp.evaluate(environment)
    }

    fun evaluateStatement(code: String): Any? {
        val stmt = parseStatement(code)
        val typedStmt = typeChecker.typeCheck(stmt)
        return typedStmt.evaluate(environment)
    }
}

private fun TypedStatement.evaluate(env: Environment): Any? = when (this) {
    is TypedStatement.Exp           -> expression.evaluate(env)
    is TypedStatement.Var           -> env.bind(variable, expression.evaluate(env))
    is TypedStatement.Assign        -> env[variable] = expression.evaluate(env)
    is TypedStatement.If            -> if (condition.evaluate(env) != Value.Bool.False) consequent.evaluate(env) else alternative?.evaluate(env)
    is TypedStatement.While         -> while (condition.evaluate(env) != Value.Bool.False) body.evaluate(env)
    is TypedStatement.StatementList -> statements.forEach { it.evaluate(env) }
}

private fun TypedExpression.evaluate(env: Environment): Value = when (this) {
    is TypedExpression.Lit    -> value
    is TypedExpression.Ref    -> env[name]
    is TypedExpression.Binary -> evaluate(env)
    is TypedExpression.Not    -> !exp.evaluateTo<Value.Bool>(env)
}

private fun TypedExpression.Binary.evaluate(env: Environment): Value = when (this) {
    is TypedExpression.Binary.Plus      -> lhs.evaluateTo<Value.Integer>(env) + rhs.evaluateTo<Value.Integer>(env)
    is TypedExpression.Binary.Minus     -> lhs.evaluateTo<Value.Integer>(env) - rhs.evaluateTo<Value.Integer>(env)
    is TypedExpression.Binary.Equals    -> Value.Bool(lhs.evaluate(env) == rhs.evaluate(env))
    is TypedExpression.Binary.NotEquals -> Value.Bool(lhs.evaluate(env) != rhs.evaluate(env))
}

private inline fun <reified T : Value> TypedExpression.evaluateTo(env: Environment): T = evaluate(env) as T
