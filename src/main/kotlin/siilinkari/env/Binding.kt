package siilinkari.env

import siilinkari.types.Type

/**
 * Statically known information about a variable.
 *
 * Includes name and type for all variables, in addition to the knowledge
 * whether the variable is global or local. Local variables are accessed
 * directly with their index in frame, whereas global variables are accessed
 * in global environment by name.
 */
sealed class Binding(val name: String, val type: Type) {

    class Global(name: String, type: Type) : Binding(name, type) {
        override fun toString() = "[Global $name]"
    }

    class Local(name: String, type: Type, val index: Int) : Binding(name, type) {
        override fun toString() = "[Local $index ($name)]"
    }
}
