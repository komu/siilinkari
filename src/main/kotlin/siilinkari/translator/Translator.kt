package siilinkari.translator

import siilinkari.ast.TypedStatement
import siilinkari.types.TypedExpression
import siilinkari.vm.CodeSegment
import siilinkari.vm.Label
import siilinkari.vm.OpCode

fun TypedStatement.translate(): CodeSegment {
    val translator = Translator()
    translator.apply { emitCode() }
    return translator.code
}

fun TypedExpression.translate(): CodeSegment {
    val translator = Translator()
    translator.apply { emitCode() }
    return translator.code
}

private class Translator {

    val code = CodeSegment()

    fun TypedStatement.emitCode() {
        when (this) {
            is TypedStatement.Exp -> {
                expression.emitCode()
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
                code += OpCode.Bind(variable)
            }
            is TypedStatement.If -> {
                val falseBranch = Label()

                condition.emitCode()
                code += OpCode.JumpIfFalse(falseBranch)
                consequent.emitCode()
                if (alternative != null) {
                    val afterIf = Label()
                    code += OpCode.Jump(afterIf)
                    falseBranch.address = code.endAddress
                    alternative.emitCode()
                    afterIf.address = code.endAddress
                } else {
                    falseBranch.address = code.endAddress
                }
            }
            is TypedStatement.While -> {
                val beforeLoop = Label()
                val afterLoop = Label()

                beforeLoop.address = code.endAddress
                condition.emitCode()
                code += OpCode.JumpIfFalse(afterLoop)
                body.emitCode()
                code += OpCode.Jump(beforeLoop)
                afterLoop.address = code.endAddress
            }
        }
    }

    fun TypedExpression.emitCode() {
        when (this) {
            is TypedExpression.Ref    -> code += OpCode.Load(name)
            is TypedExpression.Lit    -> code += OpCode.Push(value)
            is TypedExpression.Not    -> { exp.emitCode(); code += OpCode.Not }
            is TypedExpression.Binary -> emitCode()
        }
    }

    private fun TypedExpression.Binary.emitCode() {
        lhs.emitCode()
        rhs.emitCode()
        when (this) {
            is TypedExpression.Binary.Plus      -> code += OpCode.Add
            is TypedExpression.Binary.Minus     -> code += OpCode.Subtract
            is TypedExpression.Binary.Equals    -> code += OpCode.Equal
        }
    }
}
