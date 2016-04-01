package siilinkari.vm

import siilinkari.objects.Value
import java.util.*

class Environment {

    private val bindings = HashMap<String, Value>()

    /**
     * Assigns a new value to existing variable.
     */
    operator fun set(name: String, value: Value) {
        if (name !in bindings) throw UnboundVariableException(name)

        bindings[name] = value
    }

    /**
     * Bind a new variable with given name.
     *
     * @throws VariableAlreadyBoundException if variable is already bound in this scope
     */
    fun bind(name: String, value: Value) {
        if (name in bindings) throw VariableAlreadyBoundException(name)

        bindings[name] = value
    }

    /**
     * Returns the value bound to given variable.
     *
     * @throws UnboundVariableException if variable does not exist
     */
    operator fun get(name: String): Value =
        bindings[name] ?: throw UnboundVariableException(name)
}
