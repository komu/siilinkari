package siilinkari.vm

import org.junit.Test
import siilinkari.objects.Value
import kotlin.test.assertEquals

class ValueStackTest {

    @Test
    fun popMultiple() {
        val stack = ValueStack()
        stack.push(Value.String("foo"))
        stack.push(Value.String("bar"))
        stack.push(Value.String("baz"))
        stack.push(Value.String("quux"))

        val popped = stack.popValues(2)
        assertEquals(listOf(Value.String("quux"), Value.String("baz")), popped)

        assertEquals(Value.String("bar"), stack.popAny())
        assertEquals(Value.String("foo"), stack.popAny())
    }
}
