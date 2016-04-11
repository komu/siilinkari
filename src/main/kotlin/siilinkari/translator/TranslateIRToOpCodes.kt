package siilinkari.translator

import siilinkari.objects.Value
import siilinkari.translator.BasicBlock.BlockEnd
import siilinkari.vm.CodeSegment
import siilinkari.vm.OpCode
import java.util.*

fun BasicBlockGraph.translateToCode(argumentCount: Int): CodeSegment {
    return OpCodeTranslator(this, argumentCount).translate()
}

private class OpCodeTranslator(val code: BasicBlockGraph, val argumentCount: Int) {

    private val localCount = code.localVariablesCount()

    fun translate(): CodeSegment {
        val blocks = code.allBlocks()

        val blockAddresses = calculateAddressesForBlocks(blocks)
        val stackDepths = code.buildStackDepthMap()

        val ops = ArrayList<OpCode>()
        for (block in blocks) {
            ops += translateBlock(block, blockAddresses, stackDepths)
        }

        return CodeSegment(ops)
    }

    private fun translateBlock(block: BasicBlock, blockAddresses: Map<BasicBlock, Int>, stackDepths: Map<BasicBlock, Int>): ArrayList<OpCode> {
        val ops1 = ArrayList<OpCode>()
        val baseStackOffset = argumentCount + localCount // there's a +1 term from return address, but a cancelling -1 term from addressing convention
        var sp = baseStackOffset + stackDepths[block]!!

        for (op in block.opCodes) {
            if (sp < baseStackOffset) throw InvalidStackUseException("stack underflow")

            ops1 += op.translate(sp)
            sp += op.stackDelta
        }

        val next = block.next
        when (next) {
            BlockEnd.None -> {
            }
            is BlockEnd.Jump ->
                ops1 += OpCode.Jump(blockAddresses[next.basicBlock]!!)
            is BlockEnd.Branch -> {
                ops1 += OpCode.JumpIfFalse(sp, blockAddresses[next.falseBlock]!!)
                ops1 += OpCode.Jump(blockAddresses[next.trueBlock]!!)
            }
        }
        return ops1
    }

    private fun calculateAddressesForBlocks(blocks: Collection<BasicBlock>): Map<BasicBlock, Int> {
        val blockAddresses = HashMap<BasicBlock, Int>(blocks.size)

        var nextFreeAddress = 0
        for (block in blocks) {
            blockAddresses[block] = nextFreeAddress
            nextFreeAddress += translatedSize(block)
        }

        return blockAddresses
    }

    private fun translatedSize(block: BasicBlock): Int =
        block.opCodes.size + when (block.next) {
            BlockEnd.None       -> 0
            is BlockEnd.Jump    -> 1
            is BlockEnd.Branch  -> 2
        }

    private fun IR.translate(sp: Int): OpCode = when (this) {
        IR.Not              -> OpCode.Not(sp, sp)
        IR.Add              -> OpCode.Binary.Add(sp - 1, sp - 1, sp)
        IR.Subtract         -> OpCode.Binary.Subtract(sp - 1, sp - 1, sp)
        IR.Multiply         -> OpCode.Binary.Multiply(sp - 1, sp - 1, sp)
        IR.Divide           -> OpCode.Binary.Divide(sp - 1, sp - 1, sp)
        IR.Equal            -> OpCode.Binary.Equal(sp - 1, sp - 1, sp)
        IR.LessThan         -> OpCode.Binary.LessThan(sp - 1, sp - 1, sp)
        IR.LessThanOrEqual  -> OpCode.Binary.LessThanOrEqual(sp - 1, sp - 1, sp)
        IR.ConcatString     -> OpCode.Binary.ConcatString(sp - 1, sp - 1, sp)
        IR.Pop              -> OpCode.Nop
        IR.Dup              -> OpCode.Copy(sp + 1, sp, "dup")
        is IR.Call          -> OpCode.Call(sp, argumentCount)
        is IR.RestoreFrame  -> OpCode.RestoreFrame(sp)
        IR.Ret              -> OpCode.Ret(sp, returnAddressOffset())
        IR.PushUnit         -> OpCode.LoadConstant(sp + 1, Value.Unit)
        is IR.Push          -> OpCode.LoadConstant(sp + 1, value)
        is IR.LoadGlobal    -> OpCode.LoadGlobal(sp + 1, index, name)
        is IR.LoadLocal     -> OpCode.Copy(sp + 1, localOffset(index), "load local $name")
        is IR.StoreLocal    -> OpCode.Copy(localOffset(index), sp, "store local $name")
        is IR.LoadArgument  -> OpCode.Copy(sp + 1, argumentOffset(index), "load arg $name")
        is IR.StoreGlobal   -> OpCode.StoreGlobal(index, sp, name)
    }

    private fun argumentOffset(index: Int) = index
    private fun returnAddressOffset() = argumentCount
    private fun localOffset(index: Int) = argumentCount + 1 + index
}
