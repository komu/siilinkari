package siilinkari.objects

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
     * Strings.
     */
    class String(val value: kotlin.String) : Value()  {
        override fun equals(other: Any?) = other is String && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = '"' + value.replace("\"", "\\\"") + '"'
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
    }
}

// Helper properties to make values easily out of Kotlin literals. (e.g. "foo".value or 123.value)

val String.value: Value.String
    get() = Value.String(this)

val Int.value: Value.Integer
    get() = Value.Integer(this)

val Boolean.value: Value.Bool
    get() = Value.Bool(this)
