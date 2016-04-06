package siilinkari.translator

import siilinkari.ast.RelationalOp
import siilinkari.types.TypedExpression
import siilinkari.types.TypedStatement
import siilinkari.vm.CodeSegment
import siilinkari.vm.Label
import siilinkari.vm.OpCode

/**
 * Translates [TypedStatement] to [CodeSegment] containing [OpCode]s for vm to run.
 */
fun TypedStatement.translateTo(code: CodeSegment.Builder) {
    Translator(code).apply { emitCode() }
}

/**
 * Translates [TypedExpression] to [CodeSegment] containing [OpCode]s for vm to run.
 */
fun TypedExpression.translateTo(code: CodeSegment.Builder) {
    Translator(code).apply { emitCode() }
}

class Translator(private val code: CodeSegment.Builder) {

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
                args.asReversed().forEach { it.emitCode() }
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
            is TypedExpression.Binary.Multiply     -> code += OpCode.Multiply
            is TypedExpression.Binary.Divide       -> code += OpCode.Divide
            is TypedExpression.Binary.ConcatString -> code += OpCode.ConcatString
            is TypedExpression.Binary.Relational   -> op.emitCode()
            else                                   -> error("unknown expression: $this")
        }
    }

    private fun RelationalOp.emitCode() {
        when (this) {
            RelationalOp.Equals             -> code += OpCode.Equal
            RelationalOp.NotEquals          -> { code += OpCode.Equal; code += OpCode.Not }
            RelationalOp.LessThan           -> code += OpCode.LessThan
            RelationalOp.LessThanOrEqual    -> code += OpCode.LessThanOrEqual
            RelationalOp.GreaterThan        -> { code += OpCode.LessThanOrEqual; code += OpCode.Not }
            RelationalOp.GreaterThanOrEqual -> { code += OpCode.LessThan; code += OpCode.Not }
        }
    }
}
