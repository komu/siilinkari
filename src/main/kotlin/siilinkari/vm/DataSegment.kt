package siilinkari.vm

import siilinkari.objects.Value
import java.lang.Math.max

/**
 * Integer-addressable segment of [Value]s.
 *
 * The segment grows as needed because it is used for things for which we can't determine
 * the original size. (For example the call stack of the system.)
 */
class DataSegment {

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
        // We don't need to call ensureCapacity here because we can never read uninitialized values
        bindings[index] ?: error("uninitialized read at $index")

    private fun ensureCapacity(capacity: Int) {
        if (capacity > bindings.size) {
            val newSize = max(capacity, bindings.size * 2)
            bindings = bindings.copyOf(newSize)
        }
    }

    override fun toString(): String {
        return bindings.asSequence().take(10).joinToString(", ", "[", "]")
    }
}
