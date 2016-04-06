package siilinkari.translator

import siilinkari.optimizer.peepholeOptimize
import siilinkari.translator.BasicBlock.BlockEnd
import siilinkari.vm.CodeSegment
import siilinkari.vm.OpCode
import java.util.*

fun translateBasicBlocksTo(first: BasicBlock, ops: CodeSegment.Builder) {
    val blocks = findAllBlocksStartingFrom(first).toMutableList()

    // move the ending block to be last
    val endBlock = blocks.filter { it.next == BasicBlock.BlockEnd.None }.single()
    blocks.remove(endBlock)
    blocks.add(endBlock)

    val blockAddresses = HashMap<BasicBlock, Int>(blocks.size)
    var address = ops.currentAddress

    for (block in blocks)
        block.peepholeOptimize()

    for (block in blocks) {
        blockAddresses[block] = address
        address += block.translatedSize
    }

    for (block in blocks) {
        for (op in block.opCodes)
            ops += op.translate()

        val next = block.next
        when (next) {
            BlockEnd.None -> { }
            is BlockEnd.Jump ->
                ops += OpCode.Jump(blockAddresses[next.basicBlock]!!)
            is BlockEnd.Branch -> {
                ops += OpCode.JumpIfFalse(blockAddresses[next.falseBlock]!!)
                ops += OpCode.Jump(blockAddresses[next.trueBlock]!!)
            }
        }
    }
}

val BasicBlock.translatedSize: Int
    get() = opCodes.size + when (next) {
        BlockEnd.None -> 0
        is BlockEnd.Jump -> 1
        is BlockEnd.Branch -> 2
    }

fun findAllBlocksStartingFrom(start: BasicBlock): Collection<BasicBlock> {
    val blocks = LinkedHashSet<BasicBlock>()

    fun gatherBlocks(block: BasicBlock) {
        if (blocks.add(block)) {
            for (b in block.next.blocks)
                gatherBlocks(b)
        }
    }

    gatherBlocks(start)

    return blocks
}

private fun IR.translate(): OpCode = when (this) {
    IR.Not              -> OpCode.Not
    IR.Add              -> OpCode.Add
    IR.Subtract         -> OpCode.Subtract
    IR.Multiply         -> OpCode.Multiply
    IR.Divide           -> OpCode.Divide
    IR.Equal            -> OpCode.Equal
    IR.LessThan         -> OpCode.LessThan
    IR.LessThanOrEqual  -> OpCode.LessThanOrEqual
    IR.ConcatString     -> OpCode.ConcatString
    IR.Pop              -> OpCode.Pop
    IR.Dup              -> OpCode.Dup
    IR.Call             -> OpCode.Call
    is IR.Push          -> OpCode.Push(value)
    is IR.LoadLocal     -> OpCode.LoadLocal(index, name)
    is IR.LoadGlobal    -> OpCode.LoadGlobal(index, name)
    is IR.LoadArgument  -> OpCode.LoadArgument(index, name)
    is IR.StoreLocal    -> OpCode.StoreLocal(index, name)
    is IR.StoreGlobal   -> OpCode.StoreGlobal(index, name)
}
