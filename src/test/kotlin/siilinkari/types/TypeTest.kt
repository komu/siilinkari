package siilinkari.types

import org.junit.Test
import kotlin.test.assertEquals

class TypeTest {

    @Test
    fun primitiveTypeToString() {
        assertEquals("String", Type.String.toString())
        assertEquals("Int", Type.Int.toString())
        assertEquals("Boolean", Type.Boolean.toString())
    }

    @Test
    fun functionTypeToString() {
        assertEquals("() -> String", Type.Function(emptyList(), Type.String).toString())
        assertEquals("(Int) -> Boolean", Type.Function(listOf(Type.Int), Type.Boolean).toString())
        assertEquals("(String, Boolean) -> Int", Type.Function(listOf(Type.String,Type.Boolean), Type.Int).toString())
    }

    @Test
    fun arrayTypeToString() {
        assertEquals("Array<String>", Type.Array(Type.String).toString())
        assertEquals("Array<Array<String>>", Type.Array(Type.Array(Type.String)).toString())
    }
}
