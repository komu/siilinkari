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
fun TypedStatement.optimize(): TypedStatement = when (this) {
    is Exp              -> Exp(expression.optimize())
    is Assign           -> Assign(variable, expression.optimize())
    is Var              -> Var(variable, expression.optimize())
    is If               -> If(condition.optimize(), consequent.optimize(), alternative?.optimize());
    is While            -> While(condition.optimize(), body.optimize())
    is StatementList    -> StatementList(statements.map { it.optimize() })
}

fun TypedExpression.optimize(): TypedExpression = when (this) {
    is Ref      -> this
    is Lit      -> this
    is Call     -> Call(func.optimize(), args.map { it.optimize() }, type)
    is Not      -> optimize()
    is Binary   -> optimize()
}

fun Not.optimize(): TypedExpression {
    val optExp = exp.optimize()
    return if (optExp is Lit)
        Lit(!(optExp.value as Value.Bool))
    else
        optExp
}

fun TypedExpression.Binary.optimize(): TypedExpression {
    val optLhs = lhs.optimize()
    val optRhs = rhs.optimize()

    if (optLhs is Lit && optRhs is Lit) {
        if (this is Binary.Relational) {
            return Lit(Value.Bool(op.optimize(optLhs.value, optRhs.value)))
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

fun RelationalOp.optimize(lhs: Value, rhs: Value): Boolean {
    return when (this) {
        RelationalOp.Equals             -> lhs == rhs
        RelationalOp.NotEquals          -> lhs != rhs
        RelationalOp.LessThan           -> lhs.lessThan(rhs)
        RelationalOp.LessThanOrEqual    -> lhs.lessThan(rhs) || lhs == rhs
        RelationalOp.GreaterThan        -> rhs.lessThan(lhs)
        RelationalOp.GreaterThanOrEqual -> rhs.lessThan(lhs) || lhs == rhs
    }
}
