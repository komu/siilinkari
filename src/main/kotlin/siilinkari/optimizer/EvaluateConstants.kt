package siilinkari.optimizer

import siilinkari.ast.RelationalOp
import siilinkari.objects.Value
import siilinkari.types.TypedExpression
import siilinkari.types.TypedExpression.*
import siilinkari.types.TypedStatement
import siilinkari.types.TypedStatement.*

/**
 * Performs simple optimizations on the typed AST.
 */
fun TypedStatement.evaluateConstantExpressions(): TypedStatement = when (this) {
    is Exp              -> Exp(expression.evaluateConstantExpressions())
    is Assign           -> Assign(variable, expression.evaluateConstantExpressions())
    is Var              -> Var(variable, expression.evaluateConstantExpressions())
    is If               -> evaluateConstantExpressions()
    is While            -> evaluateConstantExpressions()
    is StatementList    -> statements.singleOrNull()?.evaluateConstantExpressions() ?: StatementList(statements.map { it.evaluateConstantExpressions() })
}

fun TypedExpression.evaluateConstantExpressions(): TypedExpression = when (this) {
    is Ref      -> this
    is Lit      -> this
    is Call     -> Call(func.evaluateConstantExpressions(), args.map { it.evaluateConstantExpressions() }, type)
    is Not      -> evaluateConstantExpressions()
    is Binary   -> evaluateConstantExpressions()
}

private fun If.evaluateConstantExpressions(): TypedStatement {
    val optCondition = condition.evaluateConstantExpressions()
    return if (optCondition is Lit && optCondition.value is Value.Bool) {
        if (optCondition.value.value)
            consequent.evaluateConstantExpressions()
        else
            alternative?.evaluateConstantExpressions() ?: StatementList(emptyList())
    } else {
        If(optCondition, consequent.evaluateConstantExpressions(), alternative?.evaluateConstantExpressions());
    }
}

private fun While.evaluateConstantExpressions(): TypedStatement {
    val optCondition = condition.evaluateConstantExpressions()
    if (optCondition is Lit && optCondition.value is Value.Bool && optCondition.value.value == false) {
        return StatementList(emptyList())
    }
    return While(optCondition, body.evaluateConstantExpressions())
}

private fun Not.evaluateConstantExpressions(): TypedExpression {
    val optExp = exp.evaluateConstantExpressions()
    return if (optExp is Lit)
        Lit(!(optExp.value as Value.Bool))
    else
        optExp
}

private fun TypedExpression.Binary.evaluateConstantExpressions(): TypedExpression {
    val optLhs = lhs.evaluateConstantExpressions()
    val optRhs = rhs.evaluateConstantExpressions()

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
