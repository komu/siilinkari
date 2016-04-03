package siilinkari.vm

import siilinkari.objects.Value

/**
 * Frame of local variables.
 */
class Frame(size: Int, initialBindings: List<Pair<Int, Value>> = emptyList()) {

    private val bindings = arrayOfNulls<Value?>(size)

    init {
        for ((index, value) in initialBindings)
            bindings[index] = value
    }

    /**
     * Assigns a new value to existing variable.
     */
    operator fun set(index: Int, value: Value) {
        bindings[index] = value
    }

    /**
     * Returns the value bound to given variable.
     */
    operator fun get(index: Int): Value =
        bindings[index]!!
}
