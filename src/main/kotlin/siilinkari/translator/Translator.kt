package siilinkari.translator

import siilinkari.ast.RelationalOp
import siilinkari.env.Binding
import siilinkari.types.TypedExpression
import siilinkari.types.TypedStatement
import siilinkari.vm.CodeSegment
import siilinkari.vm.Label

/**
 * Translates [TypedStatement] to [CodeSegment] containing OpCodes for vm to run.
 */
fun TypedStatement.translateTo(code: CodeSegment.Builder) {
    val translator = Translator()
    translator.apply { emitCode() }
    translator.code.buildTo(code)
}

/**
 * Translates [TypedExpression] to [CodeSegment] containing OpCodes for vm to run.
 */
fun TypedExpression.translateTo(code: CodeSegment.Builder) {
    val translator = Translator()
    translator.apply { emitCode() }
    translator.code.buildTo(code)
}

class Translator() {

    val code = IRBuilder()

    fun TypedStatement.emitCode() {
        when (this) {
            is TypedStatement.Exp -> {
                expression.emitCode()

                // If we are evaluating an expression as a statement,
                // we need to discard the result of the expression.
                code += IR.Pop
            }
            is TypedStatement.StatementList ->
                statements.forEach { it.emitCode() }
            is TypedStatement.Assign -> {
                expression.emitCode()
                variable.emitStore()
            }
            is TypedStatement.Var -> {
                expression.emitCode()
                variable.emitStore()
            }
            is TypedStatement.If -> {
                val falseBranch = Label()

                condition.emitCode()
                code += IR.JumpIfFalse(falseBranch)
                consequent.emitCode()
                if (alternative != null) {
                    val afterIf = Label()
                    code += IR.Jump(afterIf)
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
                code += IR.JumpIfFalse(afterLoop)
                body.emitCode()
                code += IR.Jump(beforeLoop)
                code += afterLoop
            }
            else -> error("unknown statement: $this")
        }
    }

    fun TypedExpression.emitCode() {
        when (this) {
            is TypedExpression.Ref ->
                binding.emitLoad()
            is TypedExpression.Lit ->
                code += IR.Push(value)
            is TypedExpression.Not -> {
                exp.emitCode()
                code += IR.Not
            }
            is TypedExpression.Binary ->
                emitCode()
            is TypedExpression.Call -> {
                args.asReversed().forEach { it.emitCode() }
                func.emitCode()
                code += IR.Call
            }
            else ->
                error("unknown expression: $this")
        }
    }

    private fun TypedExpression.Binary.emitCode() {
        lhs.emitCode()
        rhs.emitCode()
        when (this) {
            is TypedExpression.Binary.Plus         -> code += IR.Add
            is TypedExpression.Binary.Minus        -> code += IR.Subtract
            is TypedExpression.Binary.Multiply     -> code += IR.Multiply
            is TypedExpression.Binary.Divide       -> code += IR.Divide
            is TypedExpression.Binary.ConcatString -> code += IR.ConcatString
            is TypedExpression.Binary.Relational   -> op.emitCode()
            else                                   -> error("unknown expression: $this")
        }
    }

    private fun RelationalOp.emitCode() {
        when (this) {
            RelationalOp.Equals             -> code += IR.Equal
            RelationalOp.NotEquals          -> { code += IR.Equal; code += IR.Not }
            RelationalOp.LessThan           -> code += IR.LessThan
            RelationalOp.LessThanOrEqual    -> code += IR.LessThanOrEqual
            RelationalOp.GreaterThan        -> { code += IR.LessThanOrEqual; code += IR.Not }
            RelationalOp.GreaterThanOrEqual -> { code += IR.LessThan; code += IR.Not }
        }
    }

    private fun Binding.emitStore() {
        code += when (this) {
            is Binding.Local    -> IR.StoreLocal(index, name)
            is Binding.Global   -> IR.StoreGlobal(index, name)
            is Binding.Argument -> error("can't store into arguments")
        }
    }

    private fun Binding.emitLoad() {
        code += when (this) {
            is Binding.Local    -> IR.LoadLocal(index, name)
            is Binding.Global   -> IR.LoadGlobal(index, name)
            is Binding.Argument -> IR.LoadArgument(index, name)
        }
    }
}
