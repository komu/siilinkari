package siilinkari.translator

import java.util.*

/**
 * Builder for building IR-stream.
 */
class BasicBlock() {
    val opCodes = ArrayList<IR>()

    var next: BlockEnd = BlockEnd.None
        private set

    sealed class BlockEnd {
        abstract val blocks: List<BasicBlock>

        object None : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = emptyList()
        }

        class Jump(val basicBlock: BasicBlock) : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = listOf(basicBlock)
        }

        class Branch(val trueBlock: BasicBlock, val falseBlock: BasicBlock) : BlockEnd() {
            override val blocks: List<BasicBlock>
                get() = listOf(trueBlock, falseBlock)
        }
    }

    /**
     * Adds a new opcode.
     */
    operator fun plusAssign(op: IR) {
        opCodes += op
    }

    fun prepend(op: IR) {
        opCodes.add(0, op)
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
