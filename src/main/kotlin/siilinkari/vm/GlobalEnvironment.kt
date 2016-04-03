package siilinkari.vm

import siilinkari.objects.Value
import java.lang.Math.max

/**
 * Runtime environment mapping global variables to their values.
 */
class GlobalEnvironment {

    private var bindings = arrayOfNulls<Value>(1024)

    /**
     * Assigns a new value to existing variable.
     */
    operator fun set(index: Int, value: Value) {
        ensureCapacity(index + 1)
        bindings[index] = value
    }

    /**
     * Returns the value bound to given variable.
     */
    operator fun get(index: Int): Value =
        bindings[index]!!

    private fun ensureCapacity(capacity: Int) {
        if (capacity > bindings.size) {
            val newSize = max(capacity, bindings.size * 2)
            bindings = bindings.copyOf(newSize)
        }
    }
}
