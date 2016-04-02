package siilinkari.types

import siilinkari.vm.UnboundVariableException
import siilinkari.vm.VariableAlreadyBoundException
import java.util.*

/**
 * Mapping from variables to their types.
 */
class TypeEnvironment {

    private val bindings = HashMap<String, Type>()

    /**
     * Bind a type to given variable.
     *
     * @throws VariableAlreadyBoundException if variable is already bound in this scope
     */
    fun bind(name: String, value: Type) {
        if (name in bindings) throw VariableAlreadyBoundException(name)

        bindings[name] = value
    }

    /**
     * Returns the type bound to given variable.
     *
     * @throws UnboundVariableException if variable does not exist
     */
    operator fun get(name: String): Type =
        bindings[name] ?: throw UnboundVariableException(name)

}
