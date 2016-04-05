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
sealed class Binding(val name: String, val type: Type, val index: Int) {

    open val isAssignable = true

    class Global(name: String, type: Type, index: Int) : Binding(name, type, index) {
        override fun toString() = "[Global $index ($name)]"
    }

    class Local(name: String, type: Type, index: Int) : Binding(name, type, index) {
        override fun toString() = "[Local $index ($name)]"
    }

    class Argument(name: String, type: Type, index: Int) : Binding(name, type, index) {
        override fun toString() = "[Argument $index ($name)]"
        override val isAssignable = false
    }
}
