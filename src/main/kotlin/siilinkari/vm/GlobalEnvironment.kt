package siilinkari.vm

import siilinkari.objects.Value
import java.util.*

/**
 * Runtime environment mapping global variables to their values.
 */
class GlobalEnvironment {

    private val bindings = HashMap<String, Value>()

    /**
     * Assigns a new value to existing variable.
     */
    operator fun set(name: String, value: Value) {
        bindings[name] = value
    }

    /**
     * Returns the value bound to given variable.
     */
    operator fun get(name: String): Value =
        bindings[name]!!
}
