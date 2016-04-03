package siilinkari.objects

import siilinkari.types.Type

/**
 * Represents the valid runtime values in programs.
 *
 * We could Kotlin types directly instead of these and replace all references
 * to [Value] by references to [Any], but that way it would be less clear which
 * Kotlin values we are actually planning to support and the compiler could not
 * help us with exhaustiveness checks. Therefore we'll model our values explicitly.
 */
sealed class Value {

    /**
     * Returns a string representation of this value that is similar
     * to the syntax used in source code. Used when printing AST and
     * when printing values in REPL.
     */
    open fun repr(): kotlin.String = toString()

    /**
     * Unit value.
     */
    object Unit : Value()

    /**
     * Strings.
     */
    class String(val value: kotlin.String) : Value()  {
        override fun equals(other: Any?) = other is String && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value
        override fun repr() = '"' + value.replace("\"", "\\\"") + '"'

        operator fun plus(rhs: Value): String =
            String(value + rhs.toString())
    }

    /**
     * Booleans.
     */
    sealed class Bool(val value: Boolean) : Value() {
        object True : Bool(true)
        object False : Bool(false)

        companion object {
            operator fun invoke(value: Boolean): Bool = if (value) True else False
        }

        override fun equals(other: Any?) = other is Bool && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value.toString()
        operator fun not() = if (this == True) False else True
    }

    /**
     * Integers.
     */
    class Integer(val value: Int) : Value()  {
        override fun equals(other: Any?) = other is Integer && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value.toString()

        operator fun plus(other: Integer) = Integer(value + other.value)
        operator fun minus(other: Integer) = Integer(value - other.value)
        operator fun times(other: Integer) = Integer(value * other.value)
        operator fun div(other: Integer) = Integer(value / other.value)
    }

    sealed class Function(val signature: Type.Function) : Value() {
        val argumentCount: Int
            get() = signature.argumentTypes.size

        /**
         * Function whose implementation is byte-code.
         */
        class Compound(signature: Type.Function, val address: Int, val frameSize: Int) : Function(signature)

        /**
         * Function implemented as native function.
         */
        class Native(signature: Type.Function, private val func: (List<Value>) -> Value) : Value.Function(signature) {
            operator fun invoke(args: List<Value>): Value = func(args)
        }
    }
}

// Helper properties to make values easily out of Kotlin literals. (e.g. "foo".value or 123.value)

val String.value: Value.String
    get() = Value.String(this)

val Int.value: Value.Integer
    get() = Value.Integer(this)

val Boolean.value: Value.Bool
    get() = Value.Bool(this)
