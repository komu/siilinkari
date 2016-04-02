package siilinkari.translator

import siilinkari.types.TypedExpression
import siilinkari.types.TypedStatement
import siilinkari.vm.CodeSegment
import siilinkari.vm.Label
import siilinkari.vm.OpCode

/**
 * Translates [TypedStatement] to [CodeSegment] containing [OpCode]s for vm to run.
 */
fun TypedStatement.translate(): CodeSegment {
    val translator = Translator()
    translator.apply { emitCode() }
    return translator.code.build()
}

/**
 * Translates [TypedExpression] to [CodeSegment] containing [OpCode]s for vm to run.
 */
fun TypedExpression.translate(): CodeSegment {
    val translator = Translator()
    translator.apply { emitCode() }
    return translator.code.build()
}

private class Translator {

    val code = CodeSegment.Builder()

    fun TypedStatement.emitCode() {
        when (this) {
            is TypedStatement.Exp -> {
                expression.emitCode()

                // If we are evaluating an expression as a statement,
                // we need to discard the result of the expression.
                code += OpCode.Pop
            }
            is TypedStatement.StatementList ->
                statements.forEach { it.emitCode() }
            is TypedStatement.Assign -> {
                expression.emitCode()
                code += OpCode.Store(variable)
            }
            is TypedStatement.Var -> {
                expression.emitCode()
                code += OpCode.Store(variable)
            }
            is TypedStatement.If -> {
                val falseBranch = Label()

                condition.emitCode()
                code += OpCode.JumpIfFalse(falseBranch)
                consequent.emitCode()
                if (alternative != null) {
                    val afterIf = Label()
                    code += OpCode.Jump(afterIf)
                    code += falseBranch
                    alternative.emitCode()
                    code += afterIf
                } else {
                    code += falseBranch
                }
            }
            is TypedStatement.While -> {
                val beforeLoop = Label()
                val afterLoop = Label()

                code += beforeLoop
                condition.emitCode()
                code += OpCode.JumpIfFalse(afterLoop)
                body.emitCode()
                code += OpCode.Jump(beforeLoop)
                code += afterLoop
            }
            else -> error("unknown statement: $this")
        }
    }

    fun TypedExpression.emitCode() {
        when (this) {
            is TypedExpression.Ref ->
                code += OpCode.Load(binding)
            is TypedExpression.Lit ->
                code += OpCode.Push(value)
            is TypedExpression.Not -> {
                exp.emitCode()
                code += OpCode.Not
            }
            is TypedExpression.Binary ->
                emitCode()
            is TypedExpression.Call -> {
                args.forEach { it.emitCode() }
                func.emitCode()
                code += OpCode.Call
            }
            else ->
                error("unknown expression: $this")
        }
    }

    private fun TypedExpression.Binary.emitCode() {
        lhs.emitCode()
        rhs.emitCode()
        when (this) {
            is TypedExpression.Binary.Plus         -> code += OpCode.Add
            is TypedExpression.Binary.Minus        -> code += OpCode.Subtract
            is TypedExpression.Binary.Equals       -> code += OpCode.Equal
            is TypedExpression.Binary.ConcatString -> code += OpCode.ConcatString
            else                                   -> error("unknown expression: $this")
        }
    }
}
