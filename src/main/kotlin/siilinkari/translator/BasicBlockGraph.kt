package siilinkari.translator

import siilinkari.optimizer.peepholeOptimize
import siilinkari.vm.CodeSegment
import java.util.*

/**
 * Graph of basic blocks for a single function, method etc.
 */
class BasicBlockGraph {

    val start = BasicBlock()

    val end: BasicBlock
        get() = allBlocksInArbitraryOrder().single { it.next == BasicBlock.BlockEnd.None }

    fun optimize() {
        for (block in allBlocksInArbitraryOrder())
            block.peepholeOptimize()
    }


    val frameSize: Int
        get() {
            // TODO: avoid building segment just to get frame-size
            val sb = CodeSegment.Builder()
            translateTo(sb)
            return sb.frameSize
        }

    fun BasicBlockGraph.allBlocks(): Collection<BasicBlock> {
        val blocks = allBlocksInArbitraryOrder()

        // move the ending block to be last
        val endBlock = blocks.filter { it.next == BasicBlock.BlockEnd.None }.single()
        blocks.remove(endBlock)
        blocks.add(endBlock)

        return blocks
    }

    private fun allBlocksInArbitraryOrder(): LinkedHashSet<BasicBlock> {
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
}
