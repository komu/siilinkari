package siilinkari.translator

import siilinkari.ast.RelationalOp
import siilinkari.env.Binding
import siilinkari.types.TypedExpression

fun TypedExpression.translateToIR(): BasicBlockGraph {
    val translator = Translator()
    translator.run { emitCode() }
    translator.basicBlocks.end += IR.Ret
    return translator.basicBlocks
}

class Translator {

    val basicBlocks = BasicBlockGraph()
    private var currentBlock = basicBlocks.start

    fun TypedExpression.emitCode() {
        when (this) {
            is TypedExpression.Ref ->
                binding.emitLoad()
            is TypedExpression.Lit ->
                currentBlock += IR.Push(value)
            is TypedExpression.Not -> {
                exp.emitCode()
                currentBlock += IR.Not
            }
            is TypedExpression.Binary ->
                emitCode()
            is TypedExpression.Call -> {
                args.forEach { it.emitCode() }
                func.emitCode()
                currentBlock += IR.Call(args.size)
                currentBlock += IR.RestoreFrame
            }
            is TypedExpression.ExpressionList -> {
                currentBlock += IR.PushUnit
                expressions.forEach { expression ->
                    currentBlock += IR.Pop
                    expression.emitCode()
                }
            }
            is TypedExpression.Assign -> {
                expression.emitCode()
                variable.emitStore()
                currentBlock += IR.PushUnit
            }
            is TypedExpression.Var -> {
                expression.emitCode()
                variable.emitStore()
                currentBlock += IR.PushUnit
            }
            is TypedExpression.If -> {
                condition.emitCode()

                val afterBlock = BasicBlock()

                if (alternative != null) {
                    val trueBlock = BasicBlock()
                    val falseBlock = BasicBlock()

                    currentBlock.endWithBranch(trueBlock, falseBlock)

                    currentBlock = trueBlock
                    consequent.emitCode()
                    currentBlock.endWithJumpTo(afterBlock)

                    currentBlock = falseBlock
                    alternative.emitCode()

                    currentBlock.endWithJumpTo(afterBlock)

                } else {
                    val trueBlock = BasicBlock()

                    currentBlock.endWithBranch(trueBlock, afterBlock)

                    currentBlock = trueBlock
                    consequent.emitCode()
                    currentBlock += IR.Pop
                    currentBlock.endWithJumpTo(afterBlock)

                    afterBlock += IR.PushUnit
                }

                currentBlock = afterBlock
            }
            is TypedExpression.While -> {
                val loopHead = BasicBlock()
                val loopBody = BasicBlock()
                val afterLoop = BasicBlock()

                currentBlock.endWithJumpTo(loopHead)

                currentBlock = loopHead
                condition.emitCode()
                currentBlock.endWithBranch(loopBody, afterLoop)

                currentBlock = loopBody
                body.emitCode()
                currentBlock += IR.Pop
                currentBlock.endWithJumpTo(loopHead)

                currentBlock = afterLoop
                currentBlock += IR.PushUnit
            }
        }
    }

    private fun TypedExpression.Binary.emitCode() {
        lhs.emitCode()
        rhs.emitCode()
        when (this) {
            is TypedExpression.Binary.Plus         -> currentBlock += IR.Add
            is TypedExpression.Binary.Minus        -> currentBlock += IR.Subtract
            is TypedExpression.Binary.Multiply     -> currentBlock += IR.Multiply
            is TypedExpression.Binary.Divide       -> currentBlock += IR.Divide
            is TypedExpression.Binary.ConcatString -> currentBlock += IR.ConcatString
            is TypedExpression.Binary.Relational   -> op.emitCode()
        }
    }

    private fun RelationalOp.emitCode() {
        when (this) {
            RelationalOp.Equals             -> currentBlock += IR.Equal
            RelationalOp.NotEquals          -> { currentBlock += IR.Equal; currentBlock += IR.Not }
            RelationalOp.LessThan           -> currentBlock += IR.LessThan
            RelationalOp.LessThanOrEqual    -> currentBlock += IR.LessThanOrEqual
            RelationalOp.GreaterThan        -> { currentBlock += IR.LessThanOrEqual; currentBlock += IR.Not }
            RelationalOp.GreaterThanOrEqual -> { currentBlock += IR.LessThan; currentBlock += IR.Not }
        }
    }

    private fun Binding.emitStore() {
        currentBlock += when (this) {
            is Binding.Local    -> IR.StoreLocal(index, name)
            is Binding.Global   -> IR.StoreGlobal(index, name)
            is Binding.Argument -> error("can't store into arguments")
        }
    }

    private fun Binding.emitLoad() {
        currentBlock += when (this) {
            is Binding.Local    -> IR.LoadLocal(index, name)
            is Binding.Global   -> IR.LoadGlobal(index, name)
            is Binding.Argument -> IR.LoadArgument(index, name)
        }
    }
}
