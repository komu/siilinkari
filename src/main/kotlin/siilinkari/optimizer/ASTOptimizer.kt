package siilinkari.optimizer

import siilinkari.types.TypedExpression

fun TypedExpression.optimize(): TypedExpression =
    evaluateConstantExpressions()
