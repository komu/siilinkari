package siilinkari.translator

import org.junit.Test
import siilinkari.objects.value
import kotlin.test.assertFailsWith

class BasicBlockGraphTest {

    val graph = BasicBlockGraph()

    @Test
    fun jumpBackwardsDoesNotMaintainBalance() {
        val end = BasicBlock()
        graph.start += IR.Push(42.value)
        graph.start += IR.Push(42.value)
        graph.start.endWithBranch(graph.start, end)
        end += IR.Push(42.value)

        assertInvalidStackUse()
    }

    @Test
    fun stackUnderflow() {
        graph.start += IR.Pop

        assertInvalidStackUse()
    }

    private fun assertInvalidStackUse() {
        assertFailsWith<InvalidStackUseException> {
            graph.buildStackDepthMap()
        }
    }
}
