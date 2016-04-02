package siilinkari.types

/**
 * Represents the types supported in the language.
 */
sealed class Type {

    object String : Type()
    object Int : Type()
    object Boolean : Type()

    override fun toString() = javaClass.simpleName

    class Function(val argumentTypes: List<Type>, val returnType: Type) : Type() {
        override fun toString() = argumentTypes.joinToString(", ", "(", ")") + " -> " + returnType
    }
}
