package siilinkari.types

import java.util.Objects.hash

/**
 * Represents the types supported in the language.
 */
sealed class Type {

    object String : Type()
    object Int : Type()
    object Boolean : Type()
    object Unit : Type()

    override fun toString() = javaClass.simpleName

    class Function(val argumentTypes: List<Type>, val returnType: Type) : Type() {
        override fun toString() = argumentTypes.joinToString(", ", "(", ")") + " -> " + returnType
        override fun equals(other: Any?) = other is Function && argumentTypes == other.argumentTypes && returnType == other.returnType
        override fun hashCode() = hash(argumentTypes, returnType)
    }
}
