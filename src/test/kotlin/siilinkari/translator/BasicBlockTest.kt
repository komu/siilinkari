package siilinkari.translator

import org.junit.Test
import siilinkari.objects.value
import kotlin.test.assertEquals

class BasicBlockTest {

    val block = BasicBlock()

    @Test
    fun stackDelta() {
        assertEquals(0, block.stackDelta)

        block += IR.Push(42.value)
        assertEquals(1, block.stackDelta)

        block += IR.Push(1.value)
        assertEquals(2, block.stackDelta)

        block += IR.Push(42.value)
        assertEquals(3, block.stackDelta)

        block += IR.Add
        assertEquals(2, block.stackDelta)

        block.endWithBranch(BasicBlock(), BasicBlock())
        assertEquals(1, block.stackDelta)
    }
}
