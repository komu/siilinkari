package siilinkari.translator

import java.util.*

/**
 * Builder for building IR-stream.
 */
class BasicBlock() {
    val opCodes = ArrayList<IR>()

    var next: BlockEnd = BlockEnd.None
        private set

    /** How executing this blocks affects the depth of the stack */
    val stackDelta: Int
        get() = opCodes.sumBy { it.stackDelta } + next.stackDelta

    val maxLocalVariableOffset: Int
        get() = opCodes.filterIsInstance<IR.LocalFrameIR>().map { it.localFrameOffset }.max() ?: -1

    sealed class BlockEnd {
        abstract val blocks: List<BasicBlock>
        abstract val stackDelta: Int

        object None : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = emptyList()
            override val stackDelta = 0
            override fun toString() = "None"
        }

        class Jump(val basicBlock: BasicBlock) : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = listOf(basicBlock)
            override val stackDelta = 0
            override fun toString() = "Jump"
        }

        class Branch(val trueBlock: BasicBlock, val falseBlock: BasicBlock) : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = listOf(trueBlock, falseBlock)
            override val stackDelta = -1
            override fun toString() = "Branch"
        }
    }

    override fun toString() = (opCodes.map { it.toString() } + next.toString()).joinToString("; ")

    /**
     * Adds a new opcode.
     */
    operator fun plusAssign(op: IR) {
        opCodes += op
    }

    fun endWithJumpTo(next: BasicBlock) {
        check(this.next == BlockEnd.None)
        this.next = BlockEnd.Jump(next)
    }

    fun endWithBranch(trueBlock: BasicBlock, falseBlock: BasicBlock) {
        check(this.next == BlockEnd.None)
        this.next = BlockEnd.Branch(trueBlock, falseBlock)
    }
}
