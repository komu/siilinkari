package siilinkari.optimizer

import siilinkari.types.TypedExpression
import siilinkari.types.TypedStatement

fun TypedStatement.optimize(): TypedStatement =
    evaluateConstantExpressions()

fun TypedExpression.optimize(): TypedExpression =
    evaluateConstantExpressions()
