package siilinkari.vm

import org.junit.Test
import siilinkari.objects.value
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentTest {

    @Test
    fun bindAndLookup() {
        val env = Environment()

        env.bind("x", 42.value)
        env.bind("y", "foo".value)

        assertEquals(42.value, env["x"])
        assertEquals("foo".value, env["y"])
    }

    @Test
    fun updateBinding() {
        val env = Environment()

        env.bind("x", 42.value)

        assertEquals(42.value, env["x"])

        env["x"] = 43.value

        assertEquals(43.value, env["x"])
    }

    @Test
    fun lookupUnbound() {
        val env = Environment()

        assertFailsWith<UnboundVariableException> {
            env["x"]
        }
    }

    @Test
    fun assignUnbound() {
        val env = Environment()

        assertFailsWith<UnboundVariableException> {
            env["x"] = 0.value
        }
    }

    @Test
    fun bindAlreadyBound() {
        val env = Environment()
        env.bind("x", 0.value)

        assertFailsWith<VariableAlreadyBoundException> {
            env.bind("x", 1.value)
        }
    }

}
