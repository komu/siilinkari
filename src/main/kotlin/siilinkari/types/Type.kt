package siilinkari.types

import siilinkari.ast.RelationalOp
import java.util.Objects.hash

/**
 * Represents the types supported in the language.
 */
sealed class Type {

    override fun toString() = javaClass.simpleName
    open fun supports(op: RelationalOp) = true

    object String : Type()
    object Int : Type()
    object Boolean : Type()
    object Unit : Type() {
        override fun supports(op: RelationalOp) = false
    }

    class Function(val argumentTypes: List<Type>, val returnType: Type) : Type() {
        override fun toString() = argumentTypes.joinToString(", ", "(", ")") + " -> " + returnType
        override fun equals(other: Any?) = other is Function && argumentTypes == other.argumentTypes && returnType == other.returnType
        override fun hashCode() = hash(argumentTypes, returnType)
        override fun supports(op: RelationalOp) = false
    }

    class Array(val elementType: Type) : Type() {
        override fun toString() = "Array<$elementType>"
        override fun equals(other: Any?) = other is Array && elementType == other.elementType
        override fun hashCode() = elementType.hashCode()
        override fun supports(op: RelationalOp) = op == RelationalOp.Equals || op == RelationalOp.NotEquals
    }
}
