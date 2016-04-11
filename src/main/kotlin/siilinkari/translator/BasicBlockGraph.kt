package siilinkari.translator

import siilinkari.optimizer.peepholeOptimize
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

    /**
     * Verifies that stack usage of the graph is valid.
     *
     * Each instruction should have a static stack depth: whenever it is called,
     * it should affect the same stack location. Since the paths through basic
     * blocks are static, we can just verify that each basic block has a consistent
     * start depth no matter what path we reach it through.
     */
    fun buildStackDepthMap(): Map<BasicBlock, Int> {
        val startStackDepths = mutableMapOf(start to 0)

        for (block in allBlocks()) {
            val startDepth = startStackDepths[block] ?: error("no depth assigned for $block")
            val endDepth = startDepth + block.stackDelta

            for (next in block.next.blocks) {
                val nextDepth = startStackDepths[next]
                if (nextDepth == null)
                    startStackDepths[next] = endDepth
                else if (nextDepth != endDepth)
                    throw InvalidStackUseException("expected $nextDepth, but got $endDepth for $block -> $next")
            }
        }

        val endDepth = startStackDepths[end]!! + end.stackDelta
        if (endDepth != 0)
            throw InvalidStackUseException("invalid end depth for stack: $endDepth")

        return startStackDepths
    }

    val frameSize: Int
        get() = allBlocksInArbitraryOrder().map { it.frameSize }.max() ?: 0

    fun BasicBlockGraph.allBlocks(): Collection<BasicBlock> {
        val blocks = allBlocksInArbitraryOrder()

        // move the ending block to be last
        val endBlock = blocks.filter { it.next == BasicBlock.BlockEnd.None }.singleOrNull() ?: error("no unique end block")
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
