package siilinkari.optimizer

import org.junit.Test
import siilinkari.translator.BasicBlock
import siilinkari.translator.IR
import kotlin.test.assertEquals

class PeepholeOptimizerTest {

    @Test
    fun optimizeStoreFollowedByLoad() {
        val block = BasicBlock()
        block += IR.Add
        block += IR.StoreLocal(4, "foo")
        block += IR.LoadLocal(4, "foo")
        block += IR.Multiply

        block.peepholeOptimize()

        assertEquals(listOf(IR.Add, IR.Dup, IR.StoreLocal(4, "foo"), IR.Multiply), block.opCodes)
    }

    @Test
    fun removeRedundantStoreAndLoad() {
        val block = BasicBlock()
        block += IR.Add
        block += IR.LoadLocal(4, "foo")
        block += IR.StoreLocal(4, "foo")
        block += IR.Multiply

        block.peepholeOptimize()

        assertEquals(listOf(IR.Add, IR.Multiply), block.opCodes)
    }
}
