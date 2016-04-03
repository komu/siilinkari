package siilinkari.vm

import siilinkari.objects.Value

/**
 * Frame of local variables.
 */
class Frame(size: Int) {

    private val bindings = arrayOfNulls<Value?>(size)

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
